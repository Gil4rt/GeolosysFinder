package com.geolosysscanner.server;

import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

/**
 * Server-side scan data for a single ore type within a scan area.
 */
public class ScanResult {

    public final String oreId;
    public int count;
    public int minY = 256;
    public int maxY = 0;
    public final List<BlockPos> blocks = new ArrayList<>();

    public ScanResult(String oreId) {
        this.oreId = oreId;
    }

    public void addBlock(BlockPos pos) {
        count++;
        int y = pos.getY();
        if (y < minY) minY = y;
        if (y > maxY) maxY = y;
        blocks.add(pos);
    }
}
