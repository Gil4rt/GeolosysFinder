package com.geolosysscanner.client;

import com.geolosysscanner.config.ClientConfig;
import com.geolosysscanner.config.ScannerConfig;
import com.geolosysscanner.network.NetworkHandler;
import com.geolosysscanner.network.PacketDeactivate;
import com.geolosysscanner.network.PacketScanRequest;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvents;
import net.minecraftforge.client.event.InputEvent;

import java.util.List;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class ClientEventHandler {

    private long lastClickTime = 0;
    private static final long CLICK_COOLDOWN_MS = 500;

    @SubscribeEvent
    public void onRightClick(InputEvent.ClickInputEvent event) {
        if (!event.isUseItem()) return;

        Minecraft mc = Minecraft.getInstance();
        ClientPlayerEntity player = mc.player;
        if (player == null) return;

        if (!isHoldingScannerClient(player)) return;

        long now = System.currentTimeMillis();
        if (now - lastClickTime < CLICK_COOLDOWN_MS) return;
        lastClickTime = now;

        event.setCanceled(true);
        event.setSwingHand(false);

        if (player.isShiftKeyDown()) {
            // Shift + RMB: Open ore selection GUI
            if (ClientScanData.isActive() && !ClientScanData.getOres().isEmpty()) {
                mc.setScreen(new OreSelectionScreen(
                        ClientScanData.getOres(),
                        ClientScanData.getTargetIdx()
                ));
            }
        } else {
            // RMB: Start scan at player's position
            int blockX = (int) Math.floor(player.getX());
            int blockZ = (int) Math.floor(player.getZ());
            NetworkHandler.CHANNEL.sendToServer(new PacketScanRequest(blockX, blockZ));
        }
    }

    @SubscribeEvent
    public void onLeftClick(InputEvent.ClickInputEvent event) {
        if (!event.isAttack()) return;

        Minecraft mc = Minecraft.getInstance();
        ClientPlayerEntity player = mc.player;
        if (player == null) return;

        if (!isHoldingScannerClient(player)) return;

        if (ClientScanData.isActive()) {
            NetworkHandler.CHANNEL.sendToServer(new PacketDeactivate());
            event.setCanceled(true);
            event.setSwingHand(false);
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        if (ClientScanData.consumeRadarUpdate() && ClientScanData.isActive()
                && ClientScanData.hasRadarData()) {
            if (!ClientConfig.CLIENT.soundEnabled.get()) return;

            double dist = ClientScanData.getDistance3d();
            double maxDist = 30.0;
            double pct = 1.0 - Math.min(dist / maxDist, 1.0);

            float pitch = 0.5f + (float) pct * 1.5f;
            float volumeMultiplier = ClientConfig.CLIENT.soundVolume.get().floatValue();
            float volume = (0.3f + (float) pct * 0.4f) * volumeMultiplier;

            mc.player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, volume, pitch);
        }
    }

    private boolean isHoldingScannerClient(ClientPlayerEntity player) {
        return isAllowedItem(player.getMainHandItem()) || isAllowedItem(player.getOffhandItem());
    }

    private boolean isAllowedItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        ResourceLocation regName = stack.getItem().getRegistryName();
        if (regName == null) return false;
        List<? extends String> allowed = ScannerConfig.SERVER.allowedItems.get();
        return allowed.contains(regName.toString());
    }
}
