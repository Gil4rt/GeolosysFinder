package com.geolosysscanner.client;

import com.geolosysscanner.network.NetworkHandler;
import com.geolosysscanner.network.PacketDeactivate;
import com.geolosysscanner.network.PacketScanRequest;
import com.geolosysscanner.network.PacketToggleTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvents;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Client-side event handler for input processing and sound effects.
 */
public class ClientEventHandler {

    private long lastClickTime = 0;
    private static final long CLICK_COOLDOWN_MS = 500;

    /**
     * Handle right-click (use item) on client side.
     * We intercept before the packet is sent to check for scanner items.
     */
    @SubscribeEvent
    public void onRightClick(InputEvent.ClickInputEvent event) {
        if (!event.isUseItem()) return;

        Minecraft mc = Minecraft.getInstance();
        ClientPlayerEntity player = mc.player;
        if (player == null) return;

        if (!isHoldingScannerClient(player)) return;

        // Cooldown check
        long now = System.currentTimeMillis();
        if (now - lastClickTime < CLICK_COOLDOWN_MS) return;
        lastClickTime = now;

        // Cancel the vanilla use-item action
        event.setCanceled(true);
        event.setSwingHand(false);

        if (player.isShiftKeyDown()) {
            // Shift + RMB: Toggle target
            if (ClientScanData.isActive() && !ClientScanData.getOres().isEmpty()) {
                NetworkHandler.CHANNEL.sendToServer(new PacketToggleTarget());
            }
        } else {
            // RMB: Start scan at player's position
            int blockX = (int) Math.floor(player.getX());
            int blockZ = (int) Math.floor(player.getZ());
            NetworkHandler.CHANNEL.sendToServer(new PacketScanRequest(blockX, blockZ));
        }
    }

    /**
     * Handle left-click: deactivate scanner.
     */
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

    /**
     * Play radar ping sound when data updates.
     * Pitch increases as the player gets closer.
     */
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        if (ClientScanData.consumeRadarUpdate() && ClientScanData.isActive()
                && ClientScanData.hasRadarData()) {
            // Calculate pitch based on distance
            double dist = ClientScanData.getDistance3d();
            double maxDist = 30.0;
            double pct = 1.0 - Math.min(dist / maxDist, 1.0);

            // Pitch: 0.5 (far) to 2.0 (close)
            float pitch = 0.5f + (float) pct * 1.5f;
            // Volume: quiet ping
            float volume = 0.3f + (float) pct * 0.4f;

            mc.player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, volume, pitch);
        }
    }

    /**
     * Check if the player is holding a scanner item (client-side check using registry names).
     * The server does its own authoritative check.
     */
    private boolean isHoldingScannerClient(ClientPlayerEntity player) {
        return isAllowedItem(player.getMainHandItem()) || isAllowedItem(player.getOffhandItem());
    }

    private boolean isAllowedItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        ResourceLocation regName = stack.getItem().getRegistryName();
        if (regName == null) return false;
        String id = regName.toString();
        // Client-side we check against known defaults.
        // The server performs the authoritative config check.
        return id.equals("geolosys:prospectors_pick") || id.equals("minecraft:stick");
    }
}
