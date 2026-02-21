package com.geolosysscanner;

import com.geolosysscanner.config.ClientConfig;
import com.geolosysscanner.config.ScannerConfig;
import com.geolosysscanner.network.NetworkHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(GeolosysScanner.MOD_ID)
public class GeolosysScanner {

    public static final String MOD_ID = "geolosys_scanner";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public GeolosysScanner() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, ScannerConfig.SERVER_SPEC);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ClientConfig.CLIENT_SPEC);

        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::commonSetup);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::clientSetup);

        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        NetworkHandler.init();
        LOGGER.info("Geolosys Scanner Addon initialized");
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        MinecraftForge.EVENT_BUS.register(new com.geolosysscanner.client.ClientEventHandler());
        MinecraftForge.EVENT_BUS.register(new com.geolosysscanner.client.ScannerHudRenderer());
    }
}
