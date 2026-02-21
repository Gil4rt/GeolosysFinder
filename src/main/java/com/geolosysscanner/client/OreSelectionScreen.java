package com.geolosysscanner.client;

import com.geolosysscanner.config.ClientConfig;
import com.geolosysscanner.network.NetworkHandler;
import com.geolosysscanner.network.OreEntry;
import com.geolosysscanner.network.PacketSelectTarget;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/**
 * Full-screen GUI for selecting which ore to track.
 * Shows all found ores with block icons, counts, and density ratings.
 * Includes a sound mute toggle button.
 */
public class OreSelectionScreen extends Screen {

    private static final int ROW_HEIGHT = 32;
    private static final int PANEL_WIDTH = 280;
    private static final int HEADER_HEIGHT = 34;

    private static final int BG_COLOR = 0xCC0A0A0A;
    private static final int ROW_BG = 0x88111111;
    private static final int ROW_HOVER = 0x88333333;
    private static final int ROW_SELECTED = 0x88224422;
    private static final int BORDER_COLOR = 0xFF3A3A3A;
    private static final int ACCENT_COLOR = 0xFFFFAA00;
    private static final int HEADER_COLOR = 0xFFFFD700;
    private static final int TEXT_COLOR = 0xFFCCCCCC;

    private static final int SOUND_BTN_SIZE = 20;

    private final List<OreEntry> ores;
    private final int currentTargetIdx;
    private int scrollOffset = 0;
    private int panelX, panelY, panelHeight;
    private int maxVisibleRows;
    private boolean soundEnabled;

    // Sound button bounds
    private int soundBtnX, soundBtnY;

    public OreSelectionScreen(List<OreEntry> ores, int currentTargetIdx) {
        super(new TranslationTextComponent("geolosys_scanner.gui.title"));
        this.ores = new ArrayList<>(ores);
        this.currentTargetIdx = currentTargetIdx;
        this.soundEnabled = ClientConfig.CLIENT.soundEnabled.get();
    }

    @Override
    protected void init() {
        super.init();
        maxVisibleRows = Math.min(ores.size(), 8);
        panelHeight = HEADER_HEIGHT + maxVisibleRows * ROW_HEIGHT + 14;
        panelX = (this.width - PANEL_WIDTH) / 2;
        panelY = (this.height - panelHeight) / 2;

        // Sound button in top-right of panel
        soundBtnX = panelX + PANEL_WIDTH - SOUND_BTN_SIZE - 8;
        soundBtnY = panelY + 7;
    }

    @Override
    public void render(MatrixStack ms, int mouseX, int mouseY, float partialTicks) {
        // Darken background
        this.renderBackground(ms);

        // Panel background
        fill(ms, panelX, panelY, panelX + PANEL_WIDTH, panelY + panelHeight, BG_COLOR);

        // Gold top border
        fill(ms, panelX, panelY, panelX + PANEL_WIDTH, panelY + 2, ACCENT_COLOR);
        // Side borders
        fill(ms, panelX, panelY, panelX + 1, panelY + panelHeight, ACCENT_COLOR);
        fill(ms, panelX + PANEL_WIDTH - 1, panelY, panelX + PANEL_WIDTH, panelY + panelHeight, BORDER_COLOR);
        fill(ms, panelX, panelY + panelHeight - 1, panelX + PANEL_WIDTH, panelY + panelHeight, BORDER_COLOR);

        // Header title
        String title = new TranslationTextComponent("geolosys_scanner.gui.title").getString();
        int titleWidth = this.font.width(title);
        this.font.drawShadow(ms, title, panelX + (PANEL_WIDTH - titleWidth) / 2.0f,
                panelY + 12, HEADER_COLOR);

        // Sound toggle button
        renderSoundButton(ms, mouseX, mouseY);

        // Separator under header
        fill(ms, panelX + 10, panelY + HEADER_HEIGHT - 2,
                panelX + PANEL_WIDTH - 10, panelY + HEADER_HEIGHT - 1, BORDER_COLOR);

        // Ore rows
        int rowStartY = panelY + HEADER_HEIGHT;
        for (int i = 0; i < maxVisibleRows; i++) {
            int oreIdx = i + scrollOffset;
            if (oreIdx >= ores.size()) break;

            OreEntry ore = ores.get(oreIdx);
            int rowY = rowStartY + i * ROW_HEIGHT;

            boolean hovered = mouseX >= panelX && mouseX <= panelX + PANEL_WIDTH
                    && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT;
            boolean selected = (oreIdx == currentTargetIdx);

            // Row background
            int rowColor = selected ? ROW_SELECTED : (hovered ? ROW_HOVER : ROW_BG);
            fill(ms, panelX + 4, rowY + 2, panelX + PANEL_WIDTH - 4, rowY + ROW_HEIGHT - 2, rowColor);

            // Block icon
            ItemStack iconStack = getOreItemStack(ore.oreId);
            if (!iconStack.isEmpty()) {
                Minecraft.getInstance().getItemRenderer().renderGuiItem(iconStack,
                        panelX + 10, rowY + (ROW_HEIGHT - 16) / 2);
            }

            // Ore name
            String oreName = getOreDisplayName(ore.oreId);
            int nameColor = selected ? 0xFF55FF55 : (hovered ? 0xFFFFFFFF : TEXT_COLOR);
            this.font.drawShadow(ms, oreName, panelX + 34, rowY + 5, nameColor);

            // Info line: localized count + Y range + density
            String density = getDensityLabel(ore.count);
            int densityColor = getDensityColor(ore.count);
            String info = new TranslationTextComponent("geolosys_scanner.format.ore_info",
                    ore.count, ore.minY, ore.maxY).getString() + " | ";
            this.font.drawShadow(ms, info, panelX + 34, rowY + 18, 0xFF888888);

            int infoWidth = this.font.width(info);
            this.font.drawShadow(ms, density, panelX + 34 + infoWidth, rowY + 18, densityColor);

            // Selected marker
            if (selected) {
                this.font.drawShadow(ms, "\u25B6", panelX + PANEL_WIDTH - 20, rowY + 10, 0xFF55FF55);
            }
        }

        // Scroll hint
        if (ores.size() > maxVisibleRows) {
            String scrollHint = new TranslationTextComponent("geolosys_scanner.gui.scroll_hint").getString();
            int hintWidth = this.font.width(scrollHint);
            this.font.drawShadow(ms, scrollHint,
                    panelX + (PANEL_WIDTH - hintWidth) / 2.0f,
                    panelY + panelHeight - 12, 0xFF555555);
        }

        super.render(ms, mouseX, mouseY, partialTicks);
    }

    private void renderSoundButton(MatrixStack ms, int mouseX, int mouseY) {
        boolean hovered = mouseX >= soundBtnX && mouseX < soundBtnX + SOUND_BTN_SIZE
                && mouseY >= soundBtnY && mouseY < soundBtnY + SOUND_BTN_SIZE;

        // Button background
        int btnBg = hovered ? 0x88444444 : 0x88222222;
        fill(ms, soundBtnX, soundBtnY, soundBtnX + SOUND_BTN_SIZE, soundBtnY + SOUND_BTN_SIZE, btnBg);
        // Button border
        fill(ms, soundBtnX, soundBtnY, soundBtnX + SOUND_BTN_SIZE, soundBtnY + 1, BORDER_COLOR);
        fill(ms, soundBtnX, soundBtnY + SOUND_BTN_SIZE - 1, soundBtnX + SOUND_BTN_SIZE, soundBtnY + SOUND_BTN_SIZE, BORDER_COLOR);
        fill(ms, soundBtnX, soundBtnY, soundBtnX + 1, soundBtnY + SOUND_BTN_SIZE, BORDER_COLOR);
        fill(ms, soundBtnX + SOUND_BTN_SIZE - 1, soundBtnY, soundBtnX + SOUND_BTN_SIZE, soundBtnY + SOUND_BTN_SIZE, BORDER_COLOR);

        // Sound icon: green ♪ when on, red ✕ when off
        String icon;
        int iconColor;
        if (soundEnabled) {
            icon = "\u266A";
            iconColor = 0xFF55FF55;
        } else {
            icon = "\u2715";
            iconColor = 0xFFFF5555;
        }
        int iconW = this.font.width(icon);
        this.font.drawShadow(ms, icon,
                soundBtnX + (SOUND_BTN_SIZE - iconW) / 2.0f,
                soundBtnY + (SOUND_BTN_SIZE - 8) / 2.0f,
                iconColor);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            // Check sound button
            if (mouseX >= soundBtnX && mouseX < soundBtnX + SOUND_BTN_SIZE
                    && mouseY >= soundBtnY && mouseY < soundBtnY + SOUND_BTN_SIZE) {
                soundEnabled = !soundEnabled;
                ClientConfig.CLIENT.soundEnabled.set(soundEnabled);
                return true;
            }

            // Check ore rows
            int rowStartY = panelY + HEADER_HEIGHT;
            for (int i = 0; i < maxVisibleRows; i++) {
                int oreIdx = i + scrollOffset;
                if (oreIdx >= ores.size()) break;

                int rowY = rowStartY + i * ROW_HEIGHT;
                if (mouseX >= panelX && mouseX <= panelX + PANEL_WIDTH
                        && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT) {
                    NetworkHandler.CHANNEL.sendToServer(new PacketSelectTarget(oreIdx));
                    this.onClose();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (delta > 0 && scrollOffset > 0) {
            scrollOffset--;
        } else if (delta < 0 && scrollOffset < ores.size() - maxVisibleRows) {
            scrollOffset++;
        }
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private ItemStack getOreItemStack(String oreId) {
        try {
            ResourceLocation resLoc = new ResourceLocation(oreId);
            Block block = ForgeRegistries.BLOCKS.getValue(resLoc);
            if (block != null) {
                return new ItemStack(block);
            }
        } catch (Exception ignored) {
        }
        return ItemStack.EMPTY;
    }

    static String getDensityLabel(int count) {
        if (count > 30) return new TranslationTextComponent("geolosys_scanner.gui.density_rich").getString();
        if (count > 15) return new TranslationTextComponent("geolosys_scanner.gui.density_moderate").getString();
        return new TranslationTextComponent("geolosys_scanner.gui.density_poor").getString();
    }

    static int getDensityColor(int count) {
        if (count > 30) return 0xFFFFD700; // Gold
        if (count > 15) return 0xFFCCCCCC; // White
        return 0xFF888888;                   // Gray
    }

    private String getOreDisplayName(String oreId) {
        String key = oreId.replace(":", ".");
        String localized = new TranslationTextComponent("geolosys_scanner.ore." + key).getString();
        if (!localized.equals("geolosys_scanner.ore." + key)) {
            return localized;
        }
        String name = oreId;
        if (name.contains(":")) {
            name = name.substring(name.indexOf(':') + 1);
        }
        name = name.replace('_', ' ');
        if (!name.isEmpty()) {
            name = Character.toUpperCase(name.charAt(0)) + name.substring(1);
        }
        return name;
    }
}
