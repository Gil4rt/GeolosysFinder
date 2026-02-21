package com.geolosysscanner.server;

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

    public void clear() {
        active = false;
        targetIdx = 0;
        oreIds.clear();
        oreData.clear();
        tickCounter = 0;
    }

    public void setResults(List<String> ids, Map<String, ScanResult> data) {
        oreIds.clear();
        oreIds.addAll(ids);
        oreData.clear();
        oreData.putAll(data);
        targetIdx = 0;
        active = true;
        tickCounter = 0;
    }
}
