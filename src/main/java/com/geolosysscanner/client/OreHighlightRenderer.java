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

import java.util.List;

/**
 * Renders glowing wireframe outlines around ore blocks.
 * - Within 10 blocks: highlights ALL vein blocks (vein boundary mode)
 * - Within 5 blocks (no vein data): highlights closest block only
 */
public class OreHighlightRenderer {

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        if (!ClientScanData.isActive()) return;
        if (!ClientScanData.hasRadarData()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        double dist = ClientScanData.getDistance3d();
        String targetOreId = ClientScanData.getTargetOreId();

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
        RenderSystem.lineWidth(2.5f);

        buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);

        // Vein boundary mode: render ALL vein blocks when close
        List<int[]> veinBlocks = ClientScanData.getVeinBlocks();
        if (dist < 10.0 && !veinBlocks.isEmpty()) {
            Vector3d playerPos = mc.player.position();
            for (int[] pos : veinBlocks) {
                BlockState blockState = mc.level.getBlockState(new BlockPos(pos[0], pos[1], pos[2]));
                ResourceLocation regName = blockState.getBlock().getRegistryName();
                if (regName == null || !regName.toString().equals(targetOreId)) {
                    continue;
                }

                double bDist = playerPos.distanceTo(
                        new Vector3d(pos[0] + 0.5, pos[1] + 0.5, pos[2] + 0.5));
                float dimFactor = bDist < 2.0 ? 1.0f : 0.5f;

                drawWireframeBox(buffer, matrix,
                        pos[0] - expand, pos[1] - expand, pos[2] - expand,
                        pos[0] + 1.0f + expand, pos[1] + 1.0f + expand, pos[2] + 1.0f + expand,
                        r, g, b, pulse * dimFactor);
            }
        } else if (dist <= 5.0) {
            // Single closest block highlight
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

    private static void drawWireframeBox(BufferBuilder buffer, Matrix4f matrix,
                                          float x0, float y0, float z0,
                                          float x1, float y1, float z1,
                                          float r, float g, float b, float a) {
        // Bottom face
        line(buffer, matrix, x0, y0, z0, x1, y0, z0, r, g, b, a);
        line(buffer, matrix, x1, y0, z0, x1, y0, z1, r, g, b, a);
        line(buffer, matrix, x1, y0, z1, x0, y0, z1, r, g, b, a);
        line(buffer, matrix, x0, y0, z1, x0, y0, z0, r, g, b, a);
        // Top face
        line(buffer, matrix, x0, y1, z0, x1, y1, z0, r, g, b, a);
        line(buffer, matrix, x1, y1, z0, x1, y1, z1, r, g, b, a);
        line(buffer, matrix, x1, y1, z1, x0, y1, z1, r, g, b, a);
        line(buffer, matrix, x0, y1, z1, x0, y1, z0, r, g, b, a);
        // Vertical edges
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
