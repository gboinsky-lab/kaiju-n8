package com.kn8.core.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SiteTerrainTest {

    @Test
    void flatDryGroundIsAccepted() {
        SiteTerrain terrain = new SiteTerrain(6);
        for (int ground : new int[] {64, 66, 63, 69, 65}) {
            assertTrue(terrain.add(ground, false));
        }
        assertTrue(terrain.accepted());
        assertEquals(6, terrain.slope());
    }

    @Test
    void anyWaterRejects() {
        SiteTerrain terrain = new SiteTerrain(20);
        assertTrue(terrain.add(64, false));
        assertFalse(terrain.add(62, true));
        assertFalse(terrain.add(64, false));
        assertFalse(terrain.accepted());
    }

    @Test
    void steepGroundRejects() {
        SiteTerrain terrain = new SiteTerrain(6);
        assertTrue(terrain.add(64, false));
        assertFalse(terrain.add(71, false));
        assertFalse(terrain.accepted());
    }

    @Test
    void noSamplesIsNotAccepted() {
        assertFalse(new SiteTerrain(6).accepted());
    }

    @Test
    void gridCoversTheCircle() {
        int[][] grid = SiteTerrain.grid(24, 12);
        assertEquals(13, grid.length);
        for (int[] point : grid) {
            assertTrue(point[0] * point[0] + point[1] * point[1] <= 24 * 24);
        }
    }
}
