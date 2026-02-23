package com.geolosysscanner.server;

import net.minecraft.util.RegistryKey;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Per-player scan state stored on the server.
 */
public class PlayerScanState {

    public boolean active;
    public int targetIdx;
    public final List<String> oreIds = new ArrayList<>();
    public final Map<String, ScanResult> oreData = new LinkedHashMap<>();
    public int tickCounter;
    public long lastScanTimeMs;

    /** Dimension where the scan was performed. */
    public RegistryKey<World> scanDimension;
    /** Center of the scan area (player position at scan time). */
    public double scanCenterX;
    public double scanCenterZ;

    public void clear() {
        active = false;
        targetIdx = 0;
        oreIds.clear();
        oreData.clear();
        tickCounter = 0;
        scanDimension = null;
    }

    public void setResults(List<String> ids, Map<String, ScanResult> data,
                           RegistryKey<World> dimension, double centerX, double centerZ) {
        oreIds.clear();
        oreIds.addAll(ids);
        oreData.clear();
        oreData.putAll(data);
        targetIdx = 0;
        active = true;
        tickCounter = 0;
        scanDimension = dimension;
        scanCenterX = centerX;
        scanCenterZ = centerZ;
    }
}
