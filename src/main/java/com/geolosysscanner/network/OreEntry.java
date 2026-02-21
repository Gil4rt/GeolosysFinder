package com.geolosysscanner.network;

import net.minecraft.network.PacketBuffer;

/**
 * Serializable ore data sent from server to client.
 */
public class OreEntry {

    public final String oreId;
    public final int count;
    public final int minY;
    public final int maxY;

    public OreEntry(String oreId, int count, int minY, int maxY) {
        this.oreId = oreId;
        this.count = count;
        this.minY = minY;
        this.maxY = maxY;
    }

    public void encode(PacketBuffer buf) {
        buf.writeUtf(oreId);
        buf.writeVarInt(count);
        buf.writeVarInt(minY);
        buf.writeVarInt(maxY);
    }

    public static OreEntry decode(PacketBuffer buf) {
        String oreId = buf.readUtf(256);
        int count = buf.readVarInt();
        int minY = buf.readVarInt();
        int maxY = buf.readVarInt();
        return new OreEntry(oreId, count, minY, maxY);
    }
}
