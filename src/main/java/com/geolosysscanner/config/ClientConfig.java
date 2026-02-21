package com.geolosysscanner.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class ClientConfig {

    public static final ForgeConfigSpec CLIENT_SPEC;
    public static final Client CLIENT;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        CLIENT = new Client(builder);
        CLIENT_SPEC = builder.build();
    }

    public static class Client {

        public final ForgeConfigSpec.BooleanValue soundEnabled;
        public final ForgeConfigSpec.DoubleValue soundVolume;
        public final ForgeConfigSpec.BooleanValue hudEnabled;

        public Client(ForgeConfigSpec.Builder builder) {
            builder.push("client");

            soundEnabled = builder
                    .comment("Enable radar ping sound")
                    .define("soundEnabled", true);

            soundVolume = builder
                    .comment("Sound volume multiplier (0.1 = quiet, 1.0 = loud)")
                    .defineInRange("soundVolume", 0.5, 0.1, 1.0);

            hudEnabled = builder
                    .comment("Show the scanner HUD overlay")
                    .define("hudEnabled", true);

            builder.pop();
        }
    }
}
