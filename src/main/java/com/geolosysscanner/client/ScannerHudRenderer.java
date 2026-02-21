package com.geolosysscanner.client;

import com.geolosysscanner.network.OreEntry;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MainWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.List;

/**
 * Renders the scanner HUD overlay on the client.
 * Styled as a "metal detector" interface with heat bar and proximity data.
 */
public class ScannerHudRenderer {

    private static final int BG_COLOR = 0xAA000000;         // Semi-transparent black
    private static final int BORDER_COLOR = 0xFF3A3A3A;      // Dark gray border
    private static final int ACCENT_COLOR = 0xFFFFAA00;      // Gold accent
    private static final int HEADER_COLOR = 0xFFFFD700;      // Gold header text
    private static final int TEXT_COLOR = 0xFFCCCCCC;        // Light gray text
    private static final int ACTIVE_COLOR = 0xFF55FF55;      // Green for active target
    private static final int INACTIVE_COLOR = 0xFF888888;    // Gray for inactive
    private static final int HINT_COLOR = 0xFF777777;        // Dim hint text

    private static final int PANEL_WIDTH = 180;
    private static final int PADDING = 6;
    private static final int LINE_HEIGHT = 11;

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) return;
        if (!ClientScanData.isActive()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        MatrixStack matrixStack = event.getMatrixStack();
        FontRenderer font = mc.font;
        MainWindow window = mc.getWindow();

        int screenWidth = window.getGuiScaledWidth();
        // Position: top-right corner
        int panelX = screenWidth - PANEL_WIDTH - 10;
        int panelY = 10;

        if (ClientScanData.hasRadarData()) {
            renderRadarMode(matrixStack, font, panelX, panelY);
        } else {
            renderScanListMode(matrixStack, font, panelX, panelY);
        }
    }

    /**
     * Render the ore list right after scanning (before first radar update).
     */
    private void renderScanListMode(MatrixStack ms, FontRenderer font, int x, int y) {
        List<OreEntry> ores = ClientScanData.getOres();
        int targetIdx = ClientScanData.getTargetIdx();

        // Calculate panel height
        int lines = 3; // header + separator + footer hint
        if (ores.isEmpty()) {
            lines += 2;
        } else {
            int showing = Math.min(ores.size(), 5);
            lines += showing * 2; // ore name + Y range per ore
            if (ores.size() > 5) lines += 1; // "+N more"
        }
        int panelHeight = PADDING * 2 + lines * LINE_HEIGHT;

        // Background
        drawPanel(ms, x, y, PANEL_WIDTH, panelHeight);

        int textX = x + PADDING;
        int textY = y + PADDING;

        // Header
        String header = new TranslationTextComponent("geolosys_scanner.hud.title").getString();
        font.drawShadow(ms, header, textX, textY, HEADER_COLOR);
        textY += LINE_HEIGHT;

        // Separator
        drawHorizontalLine(ms, x + 4, x + PANEL_WIDTH - 4, textY, BORDER_COLOR);
        textY += LINE_HEIGHT;

        if (ores.isEmpty()) {
            String noOres = new TranslationTextComponent("geolosys_scanner.hud.no_ores").getString();
            font.drawShadow(ms, noOres, textX, textY, 0xFFFF5555);
            textY += LINE_HEIGHT;
            String tryElsewhere = new TranslationTextComponent("geolosys_scanner.hud.try_elsewhere").getString();
            font.drawShadow(ms, tryElsewhere, textX + 6, textY, TEXT_COLOR);
        } else {
            int maxShow = Math.min(ores.size(), 5);
            for (int i = 0; i < maxShow; i++) {
                OreEntry ore = ores.get(i);
                boolean isTarget = (i == targetIdx);

                // Marker + ore name
                String marker = isTarget ? "\u25B6 " : "  ";
                String oreName = getOreDisplayName(ore.oreId);
                int nameColor = isTarget ? ACTIVE_COLOR : TEXT_COLOR;
                font.drawShadow(ms, marker + oreName, textX, textY, nameColor);
                textY += LINE_HEIGHT;

                // Y range and count
                String info = "  Y:" + ore.minY + "-" + ore.maxY + " (" + ore.count + ")";
                font.drawShadow(ms, info, textX, textY, INACTIVE_COLOR);
                textY += LINE_HEIGHT;
            }

            if (ores.size() > maxShow) {
                String more = new TranslationTextComponent("geolosys_scanner.hud.more",
                        ores.size() - maxShow).getString();
                font.drawShadow(ms, "  " + more, textX, textY, INACTIVE_COLOR);
                textY += LINE_HEIGHT;
            }

            // Hint
            drawHorizontalLine(ms, x + 4, x + PANEL_WIDTH - 4, textY, BORDER_COLOR);
            textY += LINE_HEIGHT;
            String hint = new TranslationTextComponent("geolosys_scanner.hud.hint_shift").getString();
            font.drawShadow(ms, hint, textX, textY, HINT_COLOR);
        }
    }

    /**
     * Render radar / proximity mode with heat bar.
     */
    private void renderRadarMode(MatrixStack ms, FontRenderer font, int x, int y) {
        int panelHeight = PADDING * 2 + 11 * LINE_HEIGHT;
        drawPanel(ms, x, y, PANEL_WIDTH, panelHeight);

        int textX = x + PADDING;
        int textY = y + PADDING;

        // Header
        String header = new TranslationTextComponent("geolosys_scanner.hud.title").getString();
        font.drawShadow(ms, header, textX, textY, HEADER_COLOR);
        textY += LINE_HEIGHT;

        // Separator
        drawHorizontalLine(ms, x + 4, x + PANEL_WIDTH - 4, textY, BORDER_COLOR);
        textY += LINE_HEIGHT;

        // Target ore name
        String targetName = getOreDisplayName(ClientScanData.getTargetOreId());
        String targetLabel = new TranslationTextComponent("geolosys_scanner.hud.target").getString();
        font.drawShadow(ms, targetLabel + " " + targetName, textX, textY, ACTIVE_COLOR);
        textY += LINE_HEIGHT + 2;

        // Heat bar
        double dist = ClientScanData.getDistance3d();
        renderHeatBar(ms, font, textX, textY, PANEL_WIDTH - PADDING * 2, dist);
        textY += LINE_HEIGHT + 4;

        // Heat label
        String heatLabel = getHeatLabel(dist);
        int heatColor = getHeatColor(dist);
        font.drawShadow(ms, heatLabel, textX, textY, heatColor);
        textY += LINE_HEIGHT;

        // Separator
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

        // Closest block coordinates
        String coords = new TranslationTextComponent("geolosys_scanner.hud.nearest",
                ClientScanData.getClosestX(), ClientScanData.getClosestY(),
                ClientScanData.getClosestZ()).getString();
        font.drawShadow(ms, coords, textX, textY, ACCENT_COLOR);
        textY += LINE_HEIGHT;

        // Separator
        drawHorizontalLine(ms, x + 4, x + PANEL_WIDTH - 4, textY, BORDER_COLOR);
        textY += LINE_HEIGHT;

        // Hints
        String hint1 = new TranslationTextComponent("geolosys_scanner.hud.hint_shift_short").getString();
        font.drawShadow(ms, hint1, textX, textY, HINT_COLOR);
    }

    /**
     * Draw the graphical heat bar (gradient from red through yellow to green).
     */
    private void renderHeatBar(MatrixStack ms, FontRenderer font, int x, int y,
                               int totalWidth, double dist3d) {
        double maxDist = 30.0;
        double pct = 1.0 - Math.min(dist3d / maxDist, 1.0); // 1.0 = very close

        int barHeight = 8;
        int segments = 20;
        int segWidth = totalWidth / segments;

        int filledSegments = (int) Math.round(pct * segments);

        for (int i = 0; i < segments; i++) {
            int segX = x + i * segWidth;
            int color;

            if (i < filledSegments) {
                // Color gradient: red (low) -> yellow (mid) -> green (high)
                float segPct = (float) i / segments;
                color = interpolateHeatColor(segPct);
            } else {
                color = 0xFF333333; // Dark unfilled
            }

            // Draw segment with 1px gap between
            AbstractGui.fill(ms, segX, y, segX + segWidth - 1, y + barHeight, color);
        }

        // Draw border around the entire bar
        drawHorizontalLine(ms, x - 1, x + totalWidth, y - 1, BORDER_COLOR);
        drawHorizontalLine(ms, x - 1, x + totalWidth, y + barHeight, BORDER_COLOR);
        drawVerticalLine(ms, x - 1, y - 1, y + barHeight + 1, BORDER_COLOR);
        drawVerticalLine(ms, x + totalWidth, y - 1, y + barHeight + 1, BORDER_COLOR);
    }

    /**
     * Interpolate color from red -> yellow -> green based on percentage.
     */
    private int interpolateHeatColor(float pct) {
        int r, g, b;
        if (pct < 0.5f) {
            // Red to Yellow
            float t = pct * 2.0f;
            r = 255;
            g = (int) (255 * t);
            b = 0;
        } else {
            // Yellow to Green
            float t = (pct - 0.5f) * 2.0f;
            r = (int) (255 * (1.0f - t));
            g = 255;
            b = 0;
        }
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private String getHeatLabel(double dist) {
        double maxDist = 30.0;
        double pct = 1.0 - Math.min(dist / maxDist, 1.0);

        if (pct > 0.85) return new TranslationTextComponent("geolosys_scanner.hud.heat_fire").getString();
        if (pct > 0.6) return new TranslationTextComponent("geolosys_scanner.hud.heat_hot").getString();
        if (pct > 0.35) return new TranslationTextComponent("geolosys_scanner.hud.heat_warm").getString();
        return new TranslationTextComponent("geolosys_scanner.hud.heat_cold").getString();
    }

    private int getHeatColor(double dist) {
        double maxDist = 30.0;
        double pct = 1.0 - Math.min(dist / maxDist, 1.0);

        if (pct > 0.85) return 0xFF55FF55;   // Bright green
        if (pct > 0.6) return 0xFF55FF55;    // Green
        if (pct > 0.35) return 0xFFFFFF55;   // Yellow
        return 0xFFFF5555;                     // Red
    }

    // --- Drawing helpers ---

    private void drawPanel(MatrixStack ms, int x, int y, int width, int height) {
        // Background fill
        AbstractGui.fill(ms, x, y, x + width, y + height, BG_COLOR);
        // Border
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

    /**
     * Get the localized display name for an ore ID.
     * Falls back to a cleaned-up version of the registry name.
     */
    private String getOreDisplayName(String oreId) {
        // Try localization key first: geolosys_scanner.ore.<ore_name>
        String key = oreId.replace(":", ".");
        String localized = new TranslationTextComponent("geolosys_scanner.ore." + key).getString();
        // If translation exists (doesn't return the key itself), use it
        if (!localized.equals("geolosys_scanner.ore." + key)) {
            return localized;
        }
        // Fallback: strip namespace and clean up
        String name = oreId;
        if (name.contains(":")) {
            name = name.substring(name.indexOf(':') + 1);
        }
        name = name.replace('_', ' ');
        // Capitalize first letter
        if (!name.isEmpty()) {
            name = Character.toUpperCase(name.charAt(0)) + name.substring(1);
        }
        return name;
    }
}
