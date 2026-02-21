package com.geolosysscanner.network;

import com.geolosysscanner.server.ServerScanHandler;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Client -> Server: Request a chunk scan at the given block position.
 */
public class PacketScanRequest {

    private final int blockX;
    private final int blockZ;

    public PacketScanRequest(int blockX, int blockZ) {
        this.blockX = blockX;
        this.blockZ = blockZ;
    }

    public static void encode(PacketScanRequest msg, PacketBuffer buf) {
        buf.writeInt(msg.blockX);
        buf.writeInt(msg.blockZ);
    }

    public static PacketScanRequest decode(PacketBuffer buf) {
        return new PacketScanRequest(buf.readInt(), buf.readInt());
    }

    public static void handle(PacketScanRequest msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerScanHandler.handleScanRequest(ctx.get().getSender(), msg.blockX, msg.blockZ);
        });
        ctx.get().setPacketHandled(true);
    }
}
