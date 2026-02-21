package com.geolosysscanner.network;

import com.geolosysscanner.server.ServerScanHandler;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Client -> Server: Toggle to the next target ore.
 */
public class PacketToggleTarget {

    public PacketToggleTarget() {
    }

    public static void encode(PacketToggleTarget msg, PacketBuffer buf) {
        // No data needed
    }

    public static PacketToggleTarget decode(PacketBuffer buf) {
        return new PacketToggleTarget();
    }

    public static void handle(PacketToggleTarget msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerScanHandler.handleToggleTarget(ctx.get().getSender());
        });
        ctx.get().setPacketHandled(true);
    }
}
