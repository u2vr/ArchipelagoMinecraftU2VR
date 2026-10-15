package gg.archipelago.aprandomizer;

import gg.archipelago.aprandomizer.data.WorldData;
import gg.archipelago.aprandomizer.managers.GoalManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class GoalManagerTest {

    @Test
    public void testCompletionDisplayDuration() {
        assertEquals(5 * 60 * 1000L, GoalManager.COMPLETION_DISPLAY_DURATION_MS);
    }

    @Test
    public void testWorldDataGoalCompletedTimeDefaults() {
        WorldData worldData = new WorldData();
        assertEquals(0L, worldData.getGoalCompletedTime());

        long now = System.currentTimeMillis();
        worldData.setGoalCompletedTime(now);
        assertEquals(now, worldData.getGoalCompletedTime());
    }
}
