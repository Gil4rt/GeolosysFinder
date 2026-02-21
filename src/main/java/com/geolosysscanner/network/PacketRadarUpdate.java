package com.geolosysscanner.network;

import com.geolosysscanner.client.ClientScanData;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Server -> Client: Periodic radar update with proximity data for the active target.
 */
public class PacketRadarUpdate {

    private final String targetOreId;
    private final int targetIdx;
    private final double distance3d;
    private final int closestX;
    private final int closestY;
    private final int closestZ;
    private final int playerY;

    public PacketRadarUpdate(String targetOreId, int targetIdx, double distance3d,
                             int closestX, int closestY, int closestZ, int playerY) {
        this.targetOreId = targetOreId;
        this.targetIdx = targetIdx;
        this.distance3d = distance3d;
        this.closestX = closestX;
        this.closestY = closestY;
        this.closestZ = closestZ;
        this.playerY = playerY;
    }

    public static void encode(PacketRadarUpdate msg, PacketBuffer buf) {
        buf.writeUtf(msg.targetOreId);
        buf.writeVarInt(msg.targetIdx);
        buf.writeDouble(msg.distance3d);
        buf.writeInt(msg.closestX);
        buf.writeInt(msg.closestY);
        buf.writeInt(msg.closestZ);
        buf.writeInt(msg.playerY);
    }

    public static PacketRadarUpdate decode(PacketBuffer buf) {
        String targetOreId = buf.readUtf(256);
        int targetIdx = buf.readVarInt();
        double distance3d = buf.readDouble();
        int closestX = buf.readInt();
        int closestY = buf.readInt();
        int closestZ = buf.readInt();
        int playerY = buf.readInt();
        return new PacketRadarUpdate(targetOreId, targetIdx, distance3d,
                closestX, closestY, closestZ, playerY);
    }

    public static void handle(PacketRadarUpdate msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ClientScanData.receiveRadarUpdate(msg.targetOreId, msg.targetIdx,
                    msg.distance3d, msg.closestX, msg.closestY, msg.closestZ, msg.playerY);
        });
        ctx.get().setPacketHandled(true);
    }
}
