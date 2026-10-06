package gg.archipelago.aprandomizer;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import gg.archipelago.aprandomizer.data.WorldData;
import gg.archipelago.aprandomizer.managers.chest.SharedChestContainer;
import gg.archipelago.aprandomizer.managers.chest.SharedChestManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SharedChestTest {

    @Test
    public void testSharedChestContainerDefaults() {
        SharedChestContainer container = new SharedChestContainer();
        assertEquals(1, SharedChestContainer.CONTAINER_SIZE);
        assertEquals(1, container.getContainerSize());
        assertTrue(container.isEmpty());
        assertTrue(container.stillValid(null));
    }

    @Test
    public void testStorageKey() {
        assertEquals("mc_shared_chest", SharedChestManager.STORAGE_KEY);
    }

    @Test
    public void testWorldDataSharedChestDefaults() {
        WorldData worldData = new WorldData();
        assertEquals("[]", worldData.getSharedChestJson());

        worldData.setSharedChestJson("[{\"Slot\":0,\"id\":\"minecraft:diamond\",\"count\":10}]");
        assertEquals("[{\"Slot\":0,\"id\":\"minecraft:diamond\",\"count\":10}]", worldData.getSharedChestJson());

        // Null should normalize to "[]"
        worldData.setSharedChestJson(null);
        assertEquals("[]", worldData.getSharedChestJson());

        // Blank should normalize to "[]"
        worldData.setSharedChestJson("   ");
        assertEquals("[]", worldData.getSharedChestJson());
    }

    @Test
    public void testJsonParsingStructure() {
        String testJson = """
        [
            {
                "Slot": 0,
                "id": "minecraft:diamond",
                "count": 64
            },
            {
                "Slot": 5,
                "id": "minecraft:golden_apple",
                "count": 16
            }
        ]
        """;

        JsonArray array = JsonParser.parseString(testJson).getAsJsonArray();
        assertEquals(2, array.size());

        JsonObject first = array.get(0).getAsJsonObject();
        assertEquals(0, first.get("Slot").getAsInt());
        assertEquals("minecraft:diamond", first.get("id").getAsString());
        assertEquals(64, first.get("count").getAsInt());

        JsonObject second = array.get(1).getAsJsonObject();
        assertEquals(5, second.get("Slot").getAsInt());
        assertEquals("minecraft:golden_apple", second.get("id").getAsString());
        assertEquals(16, second.get("count").getAsInt());
    }

    @Test
    public void testJsonRoundTripAndEmptyChecks() {
        String emptyJson = "[]";
        JsonArray emptyArray = JsonParser.parseString(emptyJson).getAsJsonArray();
        assertEquals(0, emptyArray.size());

        JsonArray array = new JsonArray();
        JsonObject item = new JsonObject();
        item.addProperty("Slot", 12);
        item.addProperty("id", "minecraft:iron_ingot");
        item.addProperty("count", 32);
        array.add(item);

        Gson gson = new Gson();
        String json = gson.toJson(array);
        assertTrue(json.contains("\"Slot\":12"));
        assertTrue(json.contains("\"id\":\"minecraft:iron_ingot\""));
        assertTrue(json.contains("\"count\":32"));

        JsonArray parsed = JsonParser.parseString(json).getAsJsonArray();
        assertEquals(1, parsed.size());
        assertEquals(12, parsed.get(0).getAsJsonObject().get("Slot").getAsInt());
    }

    @Test
    public void testStartingSharedChestEnabled() {
        gg.archipelago.aprandomizer.managers.recipemanager.RecipeTreeManager.ensureLoaded();
        assertTrue(SharedChestManager.isStartingSharedChestEnabled());
    }

    @Test
    public void testLockKeyAndLockDataParsing() {
        assertEquals("mc_shared_chest_lock", SharedChestManager.LOCK_KEY);

        // Null and empty values should return null
        assertNull(SharedChestManager.parseLock(null));
        assertNull(SharedChestManager.parseLock(""));
        assertNull(SharedChestManager.parseLock("   "));
        assertNull(SharedChestManager.parseLock("\"\""));
        assertNull(SharedChestManager.parseLock("{}"));
        assertNull(SharedChestManager.parseLock("[]"));

        // Valid lock JSON
        long now = System.currentTimeMillis();
        String lockJson = String.format("{\"player\":\"Player1\",\"uuid\":\"uuid-123\",\"token\":\"tok-abc\",\"timestamp\":%d}", now);
        SharedChestManager.LockData lock = SharedChestManager.parseLock(lockJson);
        assertNotNull(lock);
        assertEquals("Player1", lock.player);
        assertEquals("uuid-123", lock.uuid);
        assertEquals("tok-abc", lock.token);
        assertEquals(now, lock.timestamp);
        assertFalse(lock.isExpired(now + 1000));
        assertTrue(lock.isExpired(now + SharedChestManager.LOCK_EXPIRY_MS + 1));
    }

    @Test
    public void testLockExpirationConstants() {
        assertTrue(SharedChestManager.LOCK_EXPIRY_MS > SharedChestManager.HEARTBEAT_INTERVAL_MS);
        assertTrue(SharedChestManager.AFK_TIMEOUT_MS > SharedChestManager.LOCK_EXPIRY_MS);
        assertTrue(SharedChestManager.PENDING_TIMEOUT_MS > 0);
    }
}
