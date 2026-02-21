package com.geolosysscanner.item;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

public class GeoScannerItem extends Item {

    public GeoScannerItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable World world, List<ITextComponent> tooltip, ITooltipFlag flag) {
        tooltip.add(new TranslationTextComponent("item.geolosys_scanner.geo_scanner.tooltip1").withStyle(TextFormatting.GRAY));
        tooltip.add(new TranslationTextComponent("item.geolosys_scanner.geo_scanner.tooltip2").withStyle(TextFormatting.GRAY));
        tooltip.add(new TranslationTextComponent("item.geolosys_scanner.geo_scanner.tooltip3").withStyle(TextFormatting.DARK_GRAY));
    }

    @Override
    public boolean isRepairable(ItemStack stack) {
        return true;
    }
}
