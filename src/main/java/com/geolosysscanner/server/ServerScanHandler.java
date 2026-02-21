package com.geolosysscanner.server;

import com.geolosysscanner.GeolosysScanner;
import com.geolosysscanner.config.ScannerConfig;
import com.geolosysscanner.network.NetworkHandler;
import com.geolosysscanner.network.OreEntry;
import com.geolosysscanner.network.PacketRadarUpdate;
import com.geolosysscanner.network.PacketScanResult;
import com.geolosysscanner.network.PacketScannerDeactivated;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.PacketDistributor;

import java.util.*;

/**
 * All scan logic runs on the server side.
 */
public class ServerScanHandler {

    private static final Map<UUID, PlayerScanState> playerStates = new HashMap<>();

    public static PlayerScanState getState(ServerPlayerEntity player) {
        return playerStates.computeIfAbsent(player.getUUID(), k -> new PlayerScanState());
    }

    public static boolean isHoldingScanner(ServerPlayerEntity player) {
        return isAllowedItemStack(player.getMainHandItem())
                || isAllowedItemStack(player.getOffhandItem());
    }

    private static boolean isAllowedItemStack(ItemStack stack) {
        if (stack.isEmpty()) return false;
        ResourceLocation regName = stack.getItem().getRegistryName();
        if (regName == null) return false;
        List<? extends String> allowed = ScannerConfig.SERVER.allowedItems.get();
        return allowed.contains(regName.toString());
    }

    public static void handleScanRequest(ServerPlayerEntity player, int blockX, int blockZ) {
        if (player == null) return;
        if (!isHoldingScanner(player)) return;

        // Cooldown check
        PlayerScanState state = getState(player);
        long now = System.currentTimeMillis();
        long cooldownMs = ScannerConfig.SERVER.cooldownSeconds.get() * 1000L;
        if (now - state.lastScanTimeMs < cooldownMs) {
            player.displayClientMessage(
                    new TranslationTextComponent("geolosys_scanner.hud.cooldown"), true);
            return;
        }
        state.lastScanTimeMs = now;

        // Damage the scanner item
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();
        boolean mainHandIsScanner = isAllowedItemStack(mainHand);
        ItemStack scanner = mainHandIsScanner ? mainHand : offHand;
        if (scanner.isDamageableItem()) {
            Hand hand = mainHandIsScanner ? Hand.MAIN_HAND : Hand.OFF_HAND;
            scanner.hurtAndBreak(1, player, (p) -> p.broadcastBreakEvent(hand));
        }

        World world = player.level;
        int chunkX = blockX >> 4;
        int chunkZ = blockZ >> 4;
        int radius = ScannerConfig.SERVER.scanRadius.get();
        int minY = ScannerConfig.SERVER.minDepth.get();
        int maxY = ScannerConfig.SERVER.maxDepth.get();

        Map<String, ScanResult> oreData = new LinkedHashMap<>();
        List<String> oreIds = new ArrayList<>();

        for (int cx = chunkX - (radius - 1); cx <= chunkX + (radius - 1); cx++) {
            for (int cz = chunkZ - (radius - 1); cz <= chunkZ + (radius - 1); cz++) {
                scanChunk(world, cx, cz, minY, maxY, oreData, oreIds);
            }
        }

        oreIds.sort((a, b) -> Integer.compare(oreData.get(b).count, oreData.get(a).count));

        state.setResults(oreIds, oreData);

        List<OreEntry> entries = new ArrayList<>();
        for (String id : oreIds) {
            ScanResult r = oreData.get(id);
            entries.add(new OreEntry(id, r.count, r.minY, r.maxY));
        }

        NetworkHandler.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new PacketScanResult(entries, 0)
        );

        GeolosysScanner.LOGGER.debug("Scan complete for {}: {} ore types found",
                player.getName().getString(), oreIds.size());
    }

    private static void scanChunk(World world, int chunkX, int chunkZ,
                                  int minY, int maxY,
                                  Map<String, ScanResult> oreData,
                                  List<String> oreIds) {
        int sx = chunkX << 4;
        int sz = chunkZ << 4;
        BlockPos.Mutable mutablePos = new BlockPos.Mutable();

        for (int x = sx; x < sx + 16; x++) {
            for (int z = sz; z < sz + 16; z++) {
                for (int y = minY; y < maxY; y++) {
                    mutablePos.set(x, y, z);
                    BlockState blockState = world.getBlockState(mutablePos);
                    ResourceLocation regName = blockState.getBlock().getRegistryName();
                    if (regName == null) continue;

                    String id = regName.toString();
                    if (id.startsWith("geolosys:") && id.contains("_ore") && !id.contains("sample")) {
                        if (!oreData.containsKey(id)) {
                            oreData.put(id, new ScanResult(id));
                            oreIds.add(id);
                        }
                        oreData.get(id).addBlock(mutablePos.immutable());
                    }
                }
            }
        }
    }

    public static void handleSelectTarget(ServerPlayerEntity player, int targetIdx) {
        if (player == null) return;
        PlayerScanState state = getState(player);
        if (!state.active || state.oreIds.isEmpty()) return;

        if (targetIdx < 0 || targetIdx >= state.oreIds.size()) return;

        state.targetIdx = targetIdx;
        sendRadarUpdate(player, state);
    }

    public static void handleDeactivate(ServerPlayerEntity player) {
        if (player == null) return;
        PlayerScanState state = getState(player);
        state.active = false;

        NetworkHandler.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new PacketScannerDeactivated()
        );
    }

    public static void tickPlayer(ServerPlayerEntity player) {
        PlayerScanState state = getState(player);
        if (!state.active || state.oreIds.isEmpty()) return;

        state.tickCounter++;
        int interval = ScannerConfig.SERVER.updateIntervalTicks.get();
        if (state.tickCounter % interval != 0) return;

        sendRadarUpdate(player, state);
    }

    private static void sendRadarUpdate(ServerPlayerEntity player, PlayerScanState state) {
        String targetId = state.oreIds.get(state.targetIdx);
        ScanResult result = state.oreData.get(targetId);
        if (result == null || result.blocks.isEmpty()) return;

        World world = player.level;

        // Validate blocks — remove any that have been mined
        Iterator<BlockPos> iter = result.blocks.iterator();
        while (iter.hasNext()) {
            BlockPos pos = iter.next();
            BlockState bs = world.getBlockState(pos);
            ResourceLocation rn = bs.getBlock().getRegistryName();
            if (rn == null || !rn.toString().equals(targetId)) {
                iter.remove();
                result.count = Math.max(0, result.count - 1);
            }
        }

        if (result.blocks.isEmpty()) return;

        double px = player.getX();
        double py = player.getY();
        double pz = player.getZ();

        double bestDist = Double.MAX_VALUE;
        BlockPos bestBlock = result.blocks.get(0);

        for (BlockPos pos : result.blocks) {
            double dx = pos.getX() + 0.5 - px;
            double dy = pos.getY() + 0.5 - py;
            double dz = pos.getZ() + 0.5 - pz;
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (dist < bestDist) {
                bestDist = dist;
                bestBlock = pos;
            }
        }

        NetworkHandler.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new PacketRadarUpdate(
                        targetId, state.targetIdx, bestDist,
                        bestBlock.getX(), bestBlock.getY(), bestBlock.getZ(),
                        (int) Math.floor(py), result.count
                )
        );
    }

    public static void removePlayer(UUID uuid) {
        playerStates.remove(uuid);
    }
}
