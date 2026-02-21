package com.geolosysscanner.network;

import com.geolosysscanner.GeolosysScanner;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;

import java.util.Optional;

public class NetworkHandler {

    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(GeolosysScanner.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;

    public static void init() {
        // Client -> Server: player requests a scan
        CHANNEL.registerMessage(packetId++, PacketScanRequest.class,
                PacketScanRequest::encode,
                PacketScanRequest::decode,
                PacketScanRequest::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        // Client -> Server: player toggles target ore
        CHANNEL.registerMessage(packetId++, PacketToggleTarget.class,
                PacketToggleTarget::encode,
                PacketToggleTarget::decode,
                PacketToggleTarget::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        // Client -> Server: player deactivates scanner
        CHANNEL.registerMessage(packetId++, PacketDeactivate.class,
                PacketDeactivate::encode,
                PacketDeactivate::decode,
                PacketDeactivate::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        // Server -> Client: full scan results
        CHANNEL.registerMessage(packetId++, PacketScanResult.class,
                PacketScanResult::encode,
                PacketScanResult::decode,
                PacketScanResult::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));

        // Server -> Client: radar proximity update
        CHANNEL.registerMessage(packetId++, PacketRadarUpdate.class,
                PacketRadarUpdate::encode,
                PacketRadarUpdate::decode,
                PacketRadarUpdate::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));

        // Server -> Client: scanner deactivated confirmation
        CHANNEL.registerMessage(packetId++, PacketScannerDeactivated.class,
                PacketScannerDeactivated::encode,
                PacketScannerDeactivated::decode,
                PacketScannerDeactivated::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }
}
