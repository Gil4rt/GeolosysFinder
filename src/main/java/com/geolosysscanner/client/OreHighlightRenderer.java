package com.geolosysscanner.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

/**
 * Renders glowing wireframe outlines around ore blocks (through walls).
 * Within 10 blocks: locks onto up to 3 closest connected ore blocks.
 * Locked blocks stay highlighted until mined, then replaced by neighbors.
 */
public class OreHighlightRenderer {

    private static final int MAX_HIGHLIGHT_BLOCKS = 3;

    // Locked selection — persists between frames
    private static final List<int[]> lockedBlocks = new ArrayList<>();
    private static String lockedOreId = "";

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        if (!ClientScanData.isActive()) return;
        if (!ClientScanData.hasRadarData()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        double dist = ClientScanData.getDistance3d();
        String targetOreId = ClientScanData.getTargetOreId();

        // Reset lock if ore type changed or moved out of range
        if (!targetOreId.equals(lockedOreId) || dist >= 10.0) {
            lockedBlocks.clear();
            lockedOreId = "";
        }

        int oreColor = ScannerHudRenderer.getOreColor(targetOreId);
        float pulse = 0.6f + 0.4f * (float) Math.abs(Math.sin(System.currentTimeMillis() / 300.0));
        float r = ((oreColor >> 16) & 0xFF) / 255.0f;
        float g = ((oreColor >> 8) & 0xFF) / 255.0f;
        float b = (oreColor & 0xFF) / 255.0f;

        MatrixStack ms = event.getMatrixStack();
        ActiveRenderInfo camera = mc.gameRenderer.getMainCamera();
        Vector3d camPos = camera.getPosition();

        ms.pushPose();
        ms.translate(-camPos.x, -camPos.y, -camPos.z);

        float expand = 0.005f;
        Matrix4f matrix = ms.last().pose();

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuilder();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableTexture();
        RenderSystem.disableDepthTest();
        RenderSystem.lineWidth(3.0f);

        buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);

        List<int[]> veinBlocks = ClientScanData.getVeinBlocks();
        if (dist < 10.0 && !veinBlocks.isEmpty()) {
            // Build list of valid ore blocks in the vein
            List<int[]> valid = new ArrayList<>();
            for (int[] pos : veinBlocks) {
                BlockState blockState = mc.level.getBlockState(new BlockPos(pos[0], pos[1], pos[2]));
                ResourceLocation regName = blockState.getBlock().getRegistryName();
                if (regName != null && regName.toString().equals(targetOreId)) {
                    valid.add(pos);
                }
            }

            // Remove mined blocks from lock
            lockedBlocks.removeIf(locked -> !isStillOre(mc, locked, targetOreId));

            // If lock is empty, pick new seed (closest to player)
            if (lockedBlocks.isEmpty() && !valid.isEmpty()) {
                Vector3d playerPos = mc.player.position();
                int[] closest = null;
                double closestDist = Double.MAX_VALUE;
                for (int[] pos : valid) {
                    double d = playerPos.distanceToSqr(pos[0] + 0.5, pos[1] + 0.5, pos[2] + 0.5);
                    if (d < closestDist) {
                        closestDist = d;
                        closest = pos;
                    }
                }
                if (closest != null) {
                    lockedBlocks.add(closest);
                    lockedOreId = targetOreId;
                }
            }

            // Fill up to MAX by adding adjacent neighbors from valid list
            while (lockedBlocks.size() < MAX_HIGHLIGHT_BLOCKS) {
                int[] best = null;
                double bestDist = Double.MAX_VALUE;
                Vector3d playerPos = mc.player.position();

                for (int[] candidate : valid) {
                    if (containsPos(lockedBlocks, candidate)) continue;
                    if (!isAdjacentToAny(lockedBlocks, candidate)) continue;
                    double d = playerPos.distanceToSqr(
                            candidate[0] + 0.5, candidate[1] + 0.5, candidate[2] + 0.5);
                    if (d < bestDist) {
                        bestDist = d;
                        best = candidate;
                    }
                }

                if (best == null) break;
                lockedBlocks.add(best);
            }

            lockedOreId = targetOreId;

            // Render locked blocks
            for (int i = 0; i < lockedBlocks.size(); i++) {
                int[] pos = lockedBlocks.get(i);
                float alpha = (i == 0) ? pulse : pulse * 0.65f;
                drawWireframeBox(buffer, matrix,
                        pos[0] - expand, pos[1] - expand, pos[2] - expand,
                        pos[0] + 1.0f + expand, pos[1] + 1.0f + expand, pos[2] + 1.0f + expand,
                        r, g, b, alpha);
            }
        } else if (dist <= 5.0) {
            int bx = ClientScanData.getClosestX();
            int by = ClientScanData.getClosestY();
            int bz = ClientScanData.getClosestZ();

            BlockState blockState = mc.level.getBlockState(new BlockPos(bx, by, bz));
            ResourceLocation regName = blockState.getBlock().getRegistryName();
            if (regName != null && regName.toString().equals(targetOreId)) {
                drawWireframeBox(buffer, matrix,
                        bx - expand, by - expand, bz - expand,
                        bx + 1.0f + expand, by + 1.0f + expand, bz + 1.0f + expand,
                        r, g, b, pulse);
            }
        }

        tessellator.end();

        RenderSystem.enableDepthTest();
        RenderSystem.enableTexture();
        RenderSystem.disableBlend();

        ms.popPose();
    }

    private static boolean isStillOre(Minecraft mc, int[] pos, String oreId) {
        BlockState blockState = mc.level.getBlockState(new BlockPos(pos[0], pos[1], pos[2]));
        ResourceLocation regName = blockState.getBlock().getRegistryName();
        return regName != null && regName.toString().equals(oreId);
    }

    private static boolean containsPos(List<int[]> list, int[] pos) {
        for (int[] p : list) {
            if (p[0] == pos[0] && p[1] == pos[1] && p[2] == pos[2]) return true;
        }
        return false;
    }

    private static boolean isAdjacentToAny(List<int[]> list, int[] pos) {
        for (int[] p : list) {
            int dx = Math.abs(p[0] - pos[0]);
            int dy = Math.abs(p[1] - pos[1]);
            int dz = Math.abs(p[2] - pos[2]);
            if (dx + dy + dz == 1) return true;
        }
        return false;
    }

    // --- Drawing ---

    private static void drawWireframeBox(BufferBuilder buffer, Matrix4f matrix,
                                          float x0, float y0, float z0,
                                          float x1, float y1, float z1,
                                          float r, float g, float b, float a) {
        line(buffer, matrix, x0, y0, z0, x1, y0, z0, r, g, b, a);
        line(buffer, matrix, x1, y0, z0, x1, y0, z1, r, g, b, a);
        line(buffer, matrix, x1, y0, z1, x0, y0, z1, r, g, b, a);
        line(buffer, matrix, x0, y0, z1, x0, y0, z0, r, g, b, a);
        line(buffer, matrix, x0, y1, z0, x1, y1, z0, r, g, b, a);
        line(buffer, matrix, x1, y1, z0, x1, y1, z1, r, g, b, a);
        line(buffer, matrix, x1, y1, z1, x0, y1, z1, r, g, b, a);
        line(buffer, matrix, x0, y1, z1, x0, y1, z0, r, g, b, a);
        line(buffer, matrix, x0, y0, z0, x0, y1, z0, r, g, b, a);
        line(buffer, matrix, x1, y0, z0, x1, y1, z0, r, g, b, a);
        line(buffer, matrix, x1, y0, z1, x1, y1, z1, r, g, b, a);
        line(buffer, matrix, x0, y0, z1, x0, y1, z1, r, g, b, a);
    }

    private static void line(BufferBuilder buffer, Matrix4f matrix,
                              float x1, float y1, float z1,
                              float x2, float y2, float z2,
                              float r, float g, float b, float a) {
        buffer.vertex(matrix, x1, y1, z1).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, x2, y2, z2).color(r, g, b, a).endVertex();
    }
}
