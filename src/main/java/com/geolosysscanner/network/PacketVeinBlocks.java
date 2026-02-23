package com.geolosysscanner.network;

import com.geolosysscanner.client.ClientScanData;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Server -> Client: All block positions for the currently tracked ore vein.
 * Sent when the player is within 10 blocks of the closest ore block.
 */
public class PacketVeinBlocks {

    private final List<int[]> blocks;

    public PacketVeinBlocks(List<int[]> blocks) {
        this.blocks = blocks;
    }

    public static void encode(PacketVeinBlocks msg, PacketBuffer buf) {
        buf.writeVarInt(msg.blocks.size());
        for (int[] pos : msg.blocks) {
            buf.writeInt(pos[0]);
            buf.writeShort(pos[1]);
            buf.writeInt(pos[2]);
        }
    }

    public static PacketVeinBlocks decode(PacketBuffer buf) {
        int size = buf.readVarInt();
        List<int[]> blocks = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            blocks.add(new int[]{buf.readInt(), buf.readShort(), buf.readInt()});
        }
        return new PacketVeinBlocks(blocks);
    }

    public static void handle(PacketVeinBlocks msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ClientScanData.receiveVeinBlocks(msg.blocks);
        });
        ctx.get().setPacketHandled(true);
    }
}
