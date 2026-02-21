package com.geolosysscanner.item;

import com.geolosysscanner.GeolosysScanner;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class ModItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, GeolosysScanner.MOD_ID);

    public static final RegistryObject<Item> GEO_SCANNER = ITEMS.register("geo_scanner",
            () -> new GeoScannerItem(new Item.Properties()
                    .tab(ItemGroup.TAB_TOOLS)
                    .stacksTo(1)
                    .durability(200)));
}
