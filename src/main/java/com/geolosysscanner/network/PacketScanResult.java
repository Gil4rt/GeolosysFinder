package com.geolosysscanner.network;

import com.geolosysscanner.client.ClientScanData;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Server -> Client: Full scan results (ore list with counts and Y ranges).
 */
public class PacketScanResult {

    private final List<OreEntry> ores;
    private final int targetIdx;
    private final boolean empty;

    public PacketScanResult(List<OreEntry> ores, int targetIdx) {
        this.ores = ores;
        this.targetIdx = targetIdx;
        this.empty = ores.isEmpty();
    }

    public static void encode(PacketScanResult msg, PacketBuffer buf) {
        buf.writeVarInt(msg.ores.size());
        for (OreEntry ore : msg.ores) {
            ore.encode(buf);
        }
        buf.writeVarInt(msg.targetIdx);
    }

    public static PacketScanResult decode(PacketBuffer buf) {
        int size = buf.readVarInt();
        List<OreEntry> ores = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            ores.add(OreEntry.decode(buf));
        }
        int targetIdx = buf.readVarInt();
        return new PacketScanResult(ores, targetIdx);
    }

    public static void handle(PacketScanResult msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ClientScanData.receiveScanResult(msg.ores, msg.targetIdx);
        });
        ctx.get().setPacketHandled(true);
    }
}
