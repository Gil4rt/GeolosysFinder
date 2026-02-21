package com.geolosysscanner.config;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.Arrays;
import java.util.List;

public class ScannerConfig {

    public static final ForgeConfigSpec SERVER_SPEC;
    public static final Server SERVER;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        SERVER = new Server(builder);
        SERVER_SPEC = builder.build();
    }

    public static class Server {

        public final ForgeConfigSpec.ConfigValue<List<? extends String>> allowedItems;
        public final ForgeConfigSpec.IntValue scanRadius;
        public final ForgeConfigSpec.IntValue minDepth;
        public final ForgeConfigSpec.IntValue maxDepth;
        public final ForgeConfigSpec.IntValue updateIntervalTicks;

        public Server(ForgeConfigSpec.Builder builder) {
            builder.push("scanner");

            allowedItems = builder
                    .comment("List of item IDs that can be used as a scanner trigger")
                    .defineList("allowedItems",
                            Arrays.asList("geolosys:prospectors_pick", "minecraft:stick"),
                            o -> o instanceof String);

            scanRadius = builder
                    .comment("Scan radius in chunks (1 = current chunk only, 2 = 3x3 area, etc.)")
                    .defineInRange("scanRadius", 1, 1, 5);

            minDepth = builder
                    .comment("Minimum Y level to scan")
                    .defineInRange("minDepth", 1, 0, 255);

            maxDepth = builder
                    .comment("Maximum Y level to scan")
                    .defineInRange("maxDepth", 80, 1, 256);

            updateIntervalTicks = builder
                    .comment("Radar update interval in ticks (20 = 1 second)")
                    .defineInRange("updateIntervalTicks", 20, 5, 100);

            builder.pop();
        }
    }
}
