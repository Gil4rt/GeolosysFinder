package com.geolosysscanner.network;

import com.geolosysscanner.server.ServerScanHandler;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Client -> Server: Select a specific ore target by index.
 */
public class PacketSelectTarget {

    private final int targetIdx;

    public PacketSelectTarget(int targetIdx) {
        this.targetIdx = targetIdx;
    }

    public static void encode(PacketSelectTarget msg, PacketBuffer buf) {
        buf.writeVarInt(msg.targetIdx);
    }

    public static PacketSelectTarget decode(PacketBuffer buf) {
        return new PacketSelectTarget(buf.readVarInt());
    }

    public static void handle(PacketSelectTarget msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerScanHandler.handleSelectTarget(ctx.get().getSender(), msg.targetIdx);
        });
        ctx.get().setPacketHandled(true);
    }
}
