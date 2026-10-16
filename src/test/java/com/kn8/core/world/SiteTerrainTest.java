package com.kn8.core.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SiteTerrainTest {

    @Test
    void flatDryGroundIsAccepted() {
        SiteTerrain terrain = new SiteTerrain(6, 0);
        for (int ground : new int[] {64, 66, 63, 69, 65}) {
            assertTrue(terrain.add(ground, false));
        }
        assertTrue(terrain.accepted());
        assertEquals(6, terrain.slope());
    }

    @Test
    void waterAboveTheAllowanceRejects() {
        SiteTerrain terrain = new SiteTerrain(20, 1);
        assertTrue(terrain.add(64, false));
        assertTrue(terrain.add(62, true));
        assertFalse(terrain.add(62, true));
        assertFalse(terrain.add(64, false));
        assertFalse(terrain.accepted());
    }

    @Test
    void wetSamplesDoNotCountForTheSlope() {
        SiteTerrain terrain = new SiteTerrain(4, 2);
        assertTrue(terrain.add(64, false));
        assertTrue(terrain.add(40, true));
        assertTrue(terrain.add(66, false));
        assertTrue(terrain.accepted());
        assertEquals(2, terrain.slope());
    }

    @Test
    void onlyTheCoreCountsForTheSlope() {
        SiteTerrain terrain = new SiteTerrain(4, 0);
        assertTrue(terrain.add(64, false, true));
        assertTrue(terrain.add(90, false, false));
        assertTrue(terrain.accepted());
        assertFalse(terrain.add(70, false, true));
    }

    @Test
    void steepGroundRejects() {
        SiteTerrain terrain = new SiteTerrain(6, 0);
        assertTrue(terrain.add(64, false));
        assertFalse(terrain.add(71, false));
        assertFalse(terrain.accepted());
    }

    @Test
    void onlyWaterOrNoSamplesIsNotAccepted() {
        assertFalse(new SiteTerrain(6, 0).accepted());
        SiteTerrain allWet = new SiteTerrain(6, 5);
        allWet.add(60, true);
        assertFalse(allWet.accepted());
    }

    @Test
    void allowanceFollowsTheFraction() {
        assertEquals(12, SiteTerrain.allowedWet(241, 0.05));
        assertEquals(0, SiteTerrain.allowedWet(241, 0.0));
    }

    @Test
    void gridCoversTheCircle() {
        int[][] grid = SiteTerrain.grid(24, 12);
        assertEquals(13, grid.length);
        for (int[] point : grid) {
            assertTrue(point[0] * point[0] + point[1] * point[1] <= 24 * 24);
        }
    }

    @Test
    void withinIsASquareInChunks() {
        assertTrue(SiteTerrain.within(0, 0, 10, -10, 10));
        assertFalse(SiteTerrain.within(0, 0, 11, 0, 10));
    }
}
