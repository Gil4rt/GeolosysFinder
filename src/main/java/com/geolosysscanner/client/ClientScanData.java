package com.geolosysscanner.client;

import com.geolosysscanner.network.OreEntry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Client-side scan state. Updated via network packets from the server.
 * Only accessed on the render/client thread.
 */
public class ClientScanData {

    // --- Scan result data ---
    private static boolean active = false;
    private static List<OreEntry> ores = Collections.emptyList();
    private static int targetIdx = 0;

    // --- Radar proximity data ---
    private static String targetOreId = "";
    private static double distance3d = 0;
    private static int closestX = 0;
    private static int closestY = 0;
    private static int closestZ = 0;
    private static int playerY = 0;
    private static boolean hasRadarData = false;

    // --- Sound/animation ---
    private static boolean radarUpdatedThisTick = false;

    public static boolean isActive() {
        return active;
    }

    public static List<OreEntry> getOres() {
        return ores;
    }

    public static int getTargetIdx() {
        return targetIdx;
    }

    public static String getTargetOreId() {
        return targetOreId;
    }

    public static double getDistance3d() {
        return distance3d;
    }

    public static int getClosestX() {
        return closestX;
    }

    public static int getClosestY() {
        return closestY;
    }

    public static int getClosestZ() {
        return closestZ;
    }

    public static int getPlayerY() {
        return playerY;
    }

    public static boolean hasRadarData() {
        return hasRadarData;
    }

    public static boolean consumeRadarUpdate() {
        if (radarUpdatedThisTick) {
            radarUpdatedThisTick = false;
            return true;
        }
        return false;
    }

    // --- Called from packet handlers (on client thread) ---

    public static void receiveScanResult(List<OreEntry> newOres, int newTargetIdx) {
        ores = new ArrayList<>(newOres);
        targetIdx = newTargetIdx;
        active = true;
        hasRadarData = false;
        radarUpdatedThisTick = true;
    }

    public static void receiveRadarUpdate(String oreId, int idx, double dist,
                                          int cx, int cy, int cz, int py) {
        targetOreId = oreId;
        targetIdx = idx;
        distance3d = dist;
        closestX = cx;
        closestY = cy;
        closestZ = cz;
        playerY = py;
        hasRadarData = true;
        radarUpdatedThisTick = true;
    }

    public static void deactivate() {
        active = false;
        hasRadarData = false;
        ores = Collections.emptyList();
        targetIdx = 0;
    }
}
