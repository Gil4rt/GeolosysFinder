package com.geolosysscanner.network;

import com.geolosysscanner.server.ServerScanHandler;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Client -> Server: Deactivate the scanner.
 */
public class PacketDeactivate {

    public PacketDeactivate() {
    }

    public static void encode(PacketDeactivate msg, PacketBuffer buf) {
        // No data needed
    }

    public static PacketDeactivate decode(PacketBuffer buf) {
        return new PacketDeactivate();
    }

    public static void handle(PacketDeactivate msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerScanHandler.handleDeactivate(ctx.get().getSender());
        });
        ctx.get().setPacketHandled(true);
    }
}
