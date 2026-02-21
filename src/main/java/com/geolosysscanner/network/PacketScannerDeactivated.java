package com.geolosysscanner.network;

import com.geolosysscanner.client.ClientScanData;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Server -> Client: Scanner has been deactivated.
 */
public class PacketScannerDeactivated {

    public PacketScannerDeactivated() {
    }

    public static void encode(PacketScannerDeactivated msg, PacketBuffer buf) {
        // No data
    }

    public static PacketScannerDeactivated decode(PacketBuffer buf) {
        return new PacketScannerDeactivated();
    }

    public static void handle(PacketScannerDeactivated msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(ClientScanData::deactivate);
        ctx.get().setPacketHandled(true);
    }
}
