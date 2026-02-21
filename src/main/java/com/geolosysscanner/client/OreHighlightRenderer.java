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

/**
 * Renders a glowing wireframe outline around the closest ore block
 * when the scanner radar is active and the player is within 5 blocks.
 */
public class OreHighlightRenderer {

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        if (!ClientScanData.isActive()) return;
        if (!ClientScanData.hasRadarData()) return;
        if (ClientScanData.getDistance3d() > 5.0) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        int bx = ClientScanData.getClosestX();
        int by = ClientScanData.getClosestY();
        int bz = ClientScanData.getClosestZ();

        // Verify block is still ore (prevents highlighting mined blocks)
        if (mc.level != null) {
            BlockState blockState = mc.level.getBlockState(new BlockPos(bx, by, bz));
            ResourceLocation regName = blockState.getBlock().getRegistryName();
            if (regName == null || !regName.toString().equals(ClientScanData.getTargetOreId())) {
                return;
            }
        }

        // Get ore color
        int oreColor = ScannerHudRenderer.getOreColor(ClientScanData.getTargetOreId());

        // Pulsing alpha
        float pulse = 0.6f + 0.4f * (float) Math.abs(Math.sin(System.currentTimeMillis() / 300.0));
        float r = ((oreColor >> 16) & 0xFF) / 255.0f;
        float g = ((oreColor >> 8) & 0xFF) / 255.0f;
        float b = (oreColor & 0xFF) / 255.0f;
        float a = pulse;

        MatrixStack ms = event.getMatrixStack();
        ActiveRenderInfo camera = mc.gameRenderer.getMainCamera();
        Vector3d camPos = camera.getPosition();

        ms.pushPose();
        ms.translate(-camPos.x, -camPos.y, -camPos.z);

        // Slightly expand box to avoid z-fighting
        float expand = 0.005f;
        float x0 = bx - expand;
        float y0 = by - expand;
        float z0 = bz - expand;
        float x1 = bx + 1.0f + expand;
        float y1 = by + 1.0f + expand;
        float z1 = bz + 1.0f + expand;

        drawWireframeBox(ms, x0, y0, z0, x1, y1, z1, r, g, b, a);

        ms.popPose();
    }

    private void drawWireframeBox(MatrixStack ms, float x0, float y0, float z0,
                                   float x1, float y1, float z1,
                                   float r, float g, float b, float a) {
        Matrix4f matrix = ms.last().pose();

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuilder();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableTexture();
        RenderSystem.disableDepthTest();
        RenderSystem.lineWidth(2.5f);

        buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);

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

        tessellator.end();

        RenderSystem.enableDepthTest();
        RenderSystem.enableTexture();
        RenderSystem.disableBlend();
    }

    private void line(BufferBuilder buffer, Matrix4f matrix,
                      float x1, float y1, float z1,
                      float x2, float y2, float z2,
                      float r, float g, float b, float a) {
        buffer.vertex(matrix, x1, y1, z1).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, x2, y2, z2).color(r, g, b, a).endVertex();
    }
}
