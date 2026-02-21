package com.geolosysscanner.client;

import com.geolosysscanner.config.ClientConfig;
import com.geolosysscanner.network.OreEntry;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.MainWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.List;

public class ScannerHudRenderer {

    private static final int BG_COLOR = 0xAA000000;
    private static final int BORDER_COLOR = 0xFF3A3A3A;
    private static final int ACCENT_COLOR = 0xFFFFAA00;
    private static final int HEADER_COLOR = 0xFFFFD700;
    private static final int TEXT_COLOR = 0xFFCCCCCC;
    private static final int ACTIVE_COLOR = 0xFF55FF55;
    private static final int INACTIVE_COLOR = 0xFF888888;

    private static final int PANEL_WIDTH = 200;
    private static final int PADDING = 8;
    private static final int LINE_HEIGHT = 12;

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) return;
        if (!ClientScanData.isActive()) return;
        if (!ClientConfig.CLIENT.hudEnabled.get()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        MatrixStack matrixStack = event.getMatrixStack();
        FontRenderer font = mc.font;
        MainWindow window = mc.getWindow();

        int screenWidth = window.getGuiScaledWidth();
        int screenHeight = window.getGuiScaledHeight();

        int panelX = screenWidth - PANEL_WIDTH - 10;

        if (ClientScanData.hasRadarData()) {
            renderRadarMode(matrixStack, font, panelX, screenHeight);
        } else {
            renderScanListMode(matrixStack, font, panelX, screenHeight);
        }
    }

    private void renderScanListMode(MatrixStack ms, FontRenderer font, int x, int screenHeight) {
        List<OreEntry> ores = ClientScanData.getOres();
        int targetIdx = ClientScanData.getTargetIdx();

        // Calculate panel height: header(2 lines) + ores(2 lines each) + overflow
        int lines = 2; // header + separator
        if (ores.isEmpty()) {
            lines += 2;
        } else {
            int showing = Math.min(ores.size(), 5);
            lines += showing * 2;
            if (ores.size() > 5) lines += 1;
        }
        int panelHeight = PADDING * 2 + lines * LINE_HEIGHT + 4;
        int y = (screenHeight - panelHeight) / 2;

        drawPanel(ms, x, y, PANEL_WIDTH, panelHeight);

        int textX = x + PADDING;
        int textY = y + PADDING;

        // Header
        String header = new TranslationTextComponent("geolosys_scanner.hud.title").getString();
        font.drawShadow(ms, header, textX, textY, HEADER_COLOR);
        textY += LINE_HEIGHT + 2;

        drawHorizontalLine(ms, x + 4, x + PANEL_WIDTH - 4, textY, BORDER_COLOR);
        textY += LINE_HEIGHT;

        if (ores.isEmpty()) {
            String noOres = new TranslationTextComponent("geolosys_scanner.hud.no_ores").getString();
            font.drawShadow(ms, noOres, textX, textY, 0xFFFF5555);
            textY += LINE_HEIGHT;
            String tryElsewhere = new TranslationTextComponent("geolosys_scanner.hud.try_elsewhere").getString();
            font.drawShadow(ms, tryElsewhere, textX + 8, textY, TEXT_COLOR);
        } else {
            int maxShow = Math.min(ores.size(), 5);
            for (int i = 0; i < maxShow; i++) {
                OreEntry ore = ores.get(i);
                boolean isTarget = (i == targetIdx);

                String marker = isTarget ? "\u25B6 " : "  ";
                String oreName = getOreDisplayName(ore.oreId);
                int nameColor = isTarget ? ACTIVE_COLOR : getOreColor(ore.oreId);
                font.drawShadow(ms, marker + oreName, textX, textY, nameColor);
                textY += LINE_HEIGHT;

                // Localized info line
                String density = OreSelectionScreen.getDensityLabel(ore.count);
                int densityColor = OreSelectionScreen.getDensityColor(ore.count);
                String info = "  Y:" + ore.minY + "\u2013" + ore.maxY + " ";
                font.drawShadow(ms, info, textX, textY, INACTIVE_COLOR);
                int infoW = font.width(info);
                font.drawShadow(ms, density, textX + infoW, textY, densityColor);
                textY += LINE_HEIGHT;
            }

            if (ores.size() > maxShow) {
                String more = new TranslationTextComponent("geolosys_scanner.hud.more",
                        ores.size() - maxShow).getString();
                font.drawShadow(ms, "  " + more, textX, textY, INACTIVE_COLOR);
            }
        }
    }

    private void renderRadarMode(MatrixStack ms, FontRenderer font, int x, int screenHeight) {
        int panelHeight = PADDING * 2 + 11 * LINE_HEIGHT + 8;
        int y = (screenHeight - panelHeight) / 2;

        drawPanel(ms, x, y, PANEL_WIDTH, panelHeight);

        int textX = x + PADDING;
        int textY = y + PADDING;

        // Header
        String header = new TranslationTextComponent("geolosys_scanner.hud.title").getString();
        font.drawShadow(ms, header, textX, textY, HEADER_COLOR);
        textY += LINE_HEIGHT + 2;

        drawHorizontalLine(ms, x + 4, x + PANEL_WIDTH - 4, textY, BORDER_COLOR);
        textY += LINE_HEIGHT;

        // Target ore name (color-coded)
        String targetName = getOreDisplayName(ClientScanData.getTargetOreId());
        int oreColor = getOreColor(ClientScanData.getTargetOreId());
        String targetLabel = new TranslationTextComponent("geolosys_scanner.hud.target").getString();
        font.drawShadow(ms, targetLabel + " ", textX, textY, ACTIVE_COLOR);
        int labelW = font.width(targetLabel + " ");
        font.drawShadow(ms, targetName, textX + labelW, textY, oreColor);
        textY += LINE_HEIGHT;

        // Deposit density — localized block count
        int blockCount = ClientScanData.getBlockCount();
        String density = OreSelectionScreen.getDensityLabel(blockCount);
        int densityColor = OreSelectionScreen.getDensityColor(blockCount);
        String blocksStr = new TranslationTextComponent("geolosys_scanner.format.blocks", blockCount).getString();
        String densityPrefix = "  " + blocksStr + " | ";
        font.drawShadow(ms, densityPrefix, textX, textY, INACTIVE_COLOR);
        int dpW = font.width(densityPrefix);
        font.drawShadow(ms, density, textX + dpW, textY, densityColor);
        textY += LINE_HEIGHT + 4;

        // Heat bar
        double dist = ClientScanData.getDistance3d();
        renderHeatBar(ms, textX, textY, PANEL_WIDTH - PADDING * 2, dist);
        textY += LINE_HEIGHT + 6;

        // Heat label
        String heatLabel = getHeatLabel(dist);
        int heatColor = getHeatColor(dist);
        font.drawShadow(ms, heatLabel, textX, textY, heatColor);
        textY += LINE_HEIGHT + 2;

        drawHorizontalLine(ms, x + 4, x + PANEL_WIDTH - 4, textY, BORDER_COLOR);
        textY += LINE_HEIGHT;

        // Distance
        String distLabel = new TranslationTextComponent("geolosys_scanner.hud.distance",
                (int) Math.floor(dist)).getString();
        font.drawShadow(ms, distLabel, textX, textY, TEXT_COLOR);
        textY += LINE_HEIGHT;

        // Depth
        int depthDiff = ClientScanData.getPlayerY() - ClientScanData.getClosestY();
        String depthStr;
        int depthColor;
        if (depthDiff > 0) {
            depthStr = new TranslationTextComponent("geolosys_scanner.hud.dig_down", depthDiff).getString();
            depthColor = TEXT_COLOR;
        } else if (depthDiff < 0) {
            depthStr = new TranslationTextComponent("geolosys_scanner.hud.above", Math.abs(depthDiff)).getString();
            depthColor = TEXT_COLOR;
        } else {
            depthStr = new TranslationTextComponent("geolosys_scanner.hud.same_level").getString();
            depthColor = ACTIVE_COLOR;
        }
        font.drawShadow(ms, depthStr, textX, textY, depthColor);
        textY += LINE_HEIGHT;

        // Camera-relative direction
        String direction = getDirectionString();
        font.drawShadow(ms, direction, textX, textY, ACCENT_COLOR);
    }

    private void renderHeatBar(MatrixStack ms, int x, int y, int totalWidth, double dist3d) {
        double maxDist = 30.0;
        double pct = 1.0 - Math.min(dist3d / maxDist, 1.0);

        int barHeight = 8;
        int segments = 20;
        int segWidth = totalWidth / segments;
        int filledSegments = (int) Math.round(pct * segments);

        // Pulsing when very close
        boolean pulsing = dist3d < 5.0;
        float pulseAlpha = 1.0f;
        if (pulsing) {
            pulseAlpha = 0.6f + 0.4f * (float) Math.abs(Math.sin(System.currentTimeMillis() / 200.0));
        }

        for (int i = 0; i < segments; i++) {
            int segX = x + i * segWidth;
            int color;

            if (i < filledSegments) {
                float segPct = (float) i / segments;
                color = interpolateHeatColor(segPct);
                if (pulsing) {
                    color = applyAlpha(color, pulseAlpha);
                }
            } else {
                color = 0xFF333333;
            }

            AbstractGui.fill(ms, segX, y, segX + segWidth - 1, y + barHeight, color);
        }

        drawHorizontalLine(ms, x - 1, x + totalWidth, y - 1, BORDER_COLOR);
        drawHorizontalLine(ms, x - 1, x + totalWidth, y + barHeight, BORDER_COLOR);
        drawVerticalLine(ms, x - 1, y - 1, y + barHeight + 1, BORDER_COLOR);
        drawVerticalLine(ms, x + totalWidth, y - 1, y + barHeight + 1, BORDER_COLOR);
    }

    private int applyAlpha(int color, float alpha) {
        int r = (int) (((color >> 16) & 0xFF) * alpha);
        int g = (int) (((color >> 8) & 0xFF) * alpha);
        int b = (int) ((color & 0xFF) * alpha);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private int interpolateHeatColor(float pct) {
        int r, g, b;
        if (pct < 0.5f) {
            float t = pct * 2.0f;
            r = 255; g = (int) (255 * t); b = 0;
        } else {
            float t = (pct - 0.5f) * 2.0f;
            r = (int) (255 * (1.0f - t)); g = 255; b = 0;
        }
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private String getHeatLabel(double dist) {
        double pct = 1.0 - Math.min(dist / 30.0, 1.0);
        if (pct > 0.85) return new TranslationTextComponent("geolosys_scanner.hud.heat_fire").getString();
        if (pct > 0.6) return new TranslationTextComponent("geolosys_scanner.hud.heat_hot").getString();
        if (pct > 0.35) return new TranslationTextComponent("geolosys_scanner.hud.heat_warm").getString();
        return new TranslationTextComponent("geolosys_scanner.hud.heat_cold").getString();
    }

    private int getHeatColor(double dist) {
        double pct = 1.0 - Math.min(dist / 30.0, 1.0);
        if (pct > 0.85) return 0xFF55FF55;
        if (pct > 0.6) return 0xFF55FF55;
        if (pct > 0.35) return 0xFFFFFF55;
        return 0xFFFF5555;
    }

    /**
     * Camera-relative direction to the ore.
     * Calculates the angle between where the player is looking and where the ore is,
     * then maps it to 8 relative directions (ahead, behind, left, right, and diagonals).
     */
    private String getDirectionString() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return "";

        int dx = ClientScanData.getClosestX() - (int) Math.floor(mc.player.getX());
        int dz = ClientScanData.getClosestZ() - (int) Math.floor(mc.player.getZ());

        if (Math.abs(dx) <= 1 && Math.abs(dz) <= 1) {
            return new TranslationTextComponent("geolosys_scanner.direction.here").getString();
        }

        // World angle to ore (degrees). atan2(dx, dz) gives 0° = +Z (south in MC)
        double worldAngle = Math.toDegrees(Math.atan2(dx, dz));

        // Player yaw: 0° = +Z (south), 90° = -X (west), -90°/270° = +X (east)
        double playerYaw = mc.player.yRot;

        // Relative angle: how far the ore is from where the player is looking
        double relAngle = worldAngle - playerYaw;

        // Normalize to -180..180
        while (relAngle > 180) relAngle -= 360;
        while (relAngle < -180) relAngle += 360;

        String arrow;
        String dirKey;

        if (relAngle >= -22.5 && relAngle < 22.5)         { arrow = "\u2191"; dirKey = "ahead"; }
        else if (relAngle >= 22.5 && relAngle < 67.5)      { arrow = "\u2196"; dirKey = "ahead_left"; }
        else if (relAngle >= 67.5 && relAngle < 112.5)     { arrow = "\u2190"; dirKey = "left"; }
        else if (relAngle >= 112.5 && relAngle < 157.5)    { arrow = "\u2199"; dirKey = "behind_left"; }
        else if (relAngle >= 157.5 || relAngle < -157.5)   { arrow = "\u2193"; dirKey = "behind"; }
        else if (relAngle >= -157.5 && relAngle < -112.5)  { arrow = "\u2198"; dirKey = "behind_right"; }
        else if (relAngle >= -112.5 && relAngle < -67.5)   { arrow = "\u2192"; dirKey = "right"; }
        else                                                { arrow = "\u2197"; dirKey = "ahead_right"; }

        String dirName = new TranslationTextComponent("geolosys_scanner.direction." + dirKey).getString();
        int horizDist = (int) Math.sqrt(dx * dx + dz * dz);
        String blocksStr = new TranslationTextComponent("geolosys_scanner.format.blocks", horizDist).getString();
        return arrow + " " + dirName + " (" + blocksStr + ")";
    }

    private int getOreColor(String oreId) {
        if (oreId.contains("coal") || oreId.contains("lignite") || oreId.contains("anthracite")
                || oreId.contains("bituminous")) return 0xFF999999;
        if (oreId.contains("hematite") || oreId.contains("limonite") || oreId.contains("magnetite"))
            return 0xFFDDDDDD;
        if (oreId.contains("gold")) return 0xFFFFD700;
        if (oreId.contains("kimberlite")) return 0xFF55FFFF;
        if (oreId.contains("beryl")) return 0xFF55FF55;
        if (oreId.contains("cinnabar")) return 0xFFFF5555;
        if (oreId.contains("lapis")) return 0xFF5555FF;
        if (oreId.contains("azurite") || oreId.contains("malachite")) return 0xFFFF8800;
        if (oreId.contains("cassiterite") || oreId.contains("teallite")) return 0xFFBBBBBB;
        if (oreId.contains("galena")) return 0xFF8888BB;
        if (oreId.contains("platinum")) return 0xFFAADDDD;
        if (oreId.contains("sphalerite")) return 0xFFBBBB88;
        if (oreId.contains("autunite")) return 0xFF88FF88;
        if (oreId.contains("quartz")) return 0xFFFFEEDD;
        return TEXT_COLOR;
    }

    private void drawPanel(MatrixStack ms, int x, int y, int width, int height) {
        AbstractGui.fill(ms, x, y, x + width, y + height, BG_COLOR);
        drawHorizontalLine(ms, x, x + width, y, ACCENT_COLOR);
        drawHorizontalLine(ms, x, x + width, y + height, BORDER_COLOR);
        drawVerticalLine(ms, x, y, y + height, ACCENT_COLOR);
        drawVerticalLine(ms, x + width, y, y + height, BORDER_COLOR);
    }

    private void drawHorizontalLine(MatrixStack ms, int x1, int x2, int y, int color) {
        AbstractGui.fill(ms, x1, y, x2, y + 1, color);
    }

    private void drawVerticalLine(MatrixStack ms, int x, int y1, int y2, int color) {
        AbstractGui.fill(ms, x, y1, x + 1, y2, color);
    }

    private String getOreDisplayName(String oreId) {
        String key = oreId.replace(":", ".");
        String localized = new TranslationTextComponent("geolosys_scanner.ore." + key).getString();
        if (!localized.equals("geolosys_scanner.ore." + key)) return localized;
        String name = oreId;
        if (name.contains(":")) name = name.substring(name.indexOf(':') + 1);
        name = name.replace('_', ' ');
        if (!name.isEmpty()) name = Character.toUpperCase(name.charAt(0)) + name.substring(1);
        return name;
    }
}
