package gg.archipelago.aprandomizer;

import com.google.gson.Gson;
import gg.archipelago.aprandomizer.ap.storage.APMCData;
import gg.archipelago.aprandomizer.data.WorldData;
import gg.archipelago.aprandomizer.exploration.ExplorationData;
import gg.archipelago.aprandomizer.managers.advancementmanager.AdvancementManager;
import gg.archipelago.aprandomizer.managers.recipemanager.RecipeTreeManager;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class RestrictionSettingsTest {

    @Test
    public void testParseBoolean() {
        assertTrue(SlotData.parseBoolean(true, false));
        assertFalse(SlotData.parseBoolean(false, true));

        assertTrue(SlotData.parseBoolean(1, false));
        assertFalse(SlotData.parseBoolean(0, true));

        assertTrue(SlotData.parseBoolean("true", false));
        assertFalse(SlotData.parseBoolean("false", true));
        assertTrue(SlotData.parseBoolean("1", false));
        assertFalse(SlotData.parseBoolean("0", true));
        assertTrue(SlotData.parseBoolean("yes", false));
        assertFalse(SlotData.parseBoolean("no", true));

        assertTrue(SlotData.parseBoolean(null, true));
        assertFalse(SlotData.parseBoolean(null, false));
    }

    @Test
    public void testSlotDataDeserializationStandard() {
        String json = """
        {
            "hp_restriction": false,
            "hunger_restriction": 0,
            "offhand_restriction": true,
            "reach_restriction": 1,
            "inventory_slot_restriction": "false",
            "structure_restriction": 0,
            "world_border_restriction": false,
            "traps_enabled": 0,
            "curses_enabled": false,
            "structure_unlock_shuffle": 1
        }
        """;
        Gson gson = new Gson();
        SlotData slotData = gson.fromJson(json, SlotData.class);

        assertFalse(slotData.isHpRestrictionEnabled());
        assertFalse(slotData.isHungerRestrictionEnabled());
        assertTrue(slotData.isOffhandRestrictionEnabled());
        assertTrue(slotData.isReachRestrictionEnabled());
        assertFalse(slotData.isInventorySlotRestrictionEnabled());
        assertFalse(slotData.isStructureRestrictionEnabled());
        assertFalse(slotData.isWorldBorderRestrictionEnabled());
        assertFalse(slotData.isTrapsEnabled());
        assertFalse(slotData.isCursesEnabled());
        assertTrue(slotData.isStructureUnlockShuffle());
    }

    @Test
    public void testSlotDataDeserializationAlternates() {
        String json = """
        {
            "hp_shuffle": 0,
            "hunger_shuffle": 0,
            "offhand_shuffle": 0,
            "mining_distance_restriction": 0,
            "inventory_slot_shuffle": 0,
            "structure_shuffle": 0,
            "world_border_shuffle": 0,
            "traps_shuffle": 0,
            "permanent_debuffs": 0
        }
        """;
        Gson gson = new Gson();
        SlotData slotData = gson.fromJson(json, SlotData.class);

        assertFalse(slotData.isHpRestrictionEnabled());
        assertFalse(slotData.isHungerRestrictionEnabled());
        assertFalse(slotData.isOffhandRestrictionEnabled());
        assertFalse(slotData.isReachRestrictionEnabled());
        assertFalse(slotData.isInventorySlotRestrictionEnabled());
        assertFalse(slotData.isStructureRestrictionEnabled());
        assertFalse(slotData.isWorldBorderRestrictionEnabled());
        assertFalse(slotData.isTrapsEnabled());
        assertFalse(slotData.isCursesEnabled());
    }

    @Test
    public void testAPMCDataDeserialization() {
        String json = """
        {
            "world_seed": 12345,
            "hp_restriction": false,
            "hunger_restriction": 0,
            "offhand_restriction": 1,
            "reach_restriction": 0,
            "inventory_slot_restriction": 0,
            "structure_restriction": 0,
            "world_border_restriction": 0,
            "traps_enabled": 0,
            "curses_enabled": 0,
            "structure_unlock_shuffle": 1
        }
        """;
        Gson gson = new Gson();
        APMCData apmcData = gson.fromJson(json, APMCData.class);

        assertFalse(apmcData.isHpRestrictionEnabled());
        assertFalse(apmcData.isHungerRestrictionEnabled());
        assertTrue(apmcData.isOffhandRestrictionEnabled());
        assertFalse(apmcData.isReachRestrictionEnabled());
        assertFalse(apmcData.isInventorySlotRestrictionEnabled());
        assertFalse(apmcData.isStructureRestrictionEnabled());
        assertFalse(apmcData.isWorldBorderRestrictionEnabled());
        assertFalse(apmcData.isTrapsEnabled());
        assertFalse(apmcData.isCursesEnabled());
        assertTrue(apmcData.isStructureUnlockShuffle());
        assertEquals("minecraft_u2vr", apmcData.getGame());

        APMCData defaultApmc = new APMCData();
        assertEquals("minecraft_u2vr", defaultApmc.getGame());

        String jsonCustom = "{\"game\": \"custom_game\"}";
        APMCData customApmc = gson.fromJson(jsonCustom, APMCData.class);
        assertEquals("custom_game", customApmc.getGame());
    }

    @Test
    public void testRecipeTreeManagerSettings() {
        RecipeTreeManager.ensureLoaded();
        // Defaults in recipe_tree.json are all true
        assertTrue(RecipeTreeManager.isSettingEnabled("hp_restriction", false));
        assertTrue(RecipeTreeManager.isSettingEnabled("hunger_restriction", false));
        assertTrue(RecipeTreeManager.isSettingEnabled("offhand_restriction", false));
        assertTrue(RecipeTreeManager.isSettingEnabled("reach_restriction", false));
        assertTrue(RecipeTreeManager.isSettingEnabled("inventory_slot_restriction", false));
        assertTrue(RecipeTreeManager.isSettingEnabled("structure_restriction", false));
        assertTrue(RecipeTreeManager.isSettingEnabled("world_border_restriction", false));
        assertTrue(RecipeTreeManager.isSettingEnabled("traps_enabled", false));
        assertTrue(RecipeTreeManager.isSettingEnabled("curses_enabled", false));

        // Non-existent key should return fallback
        assertFalse(RecipeTreeManager.isSettingEnabled("non_existent_key", false));
        assertTrue(RecipeTreeManager.isSettingEnabled("non_existent_key", true));
    }

    @Test
    public void testAllSettingsDefaults() {
        SlotData defaultSlotData = new SlotData();
        assertTrue(defaultSlotData.isHpRestrictionEnabled());
        assertTrue(defaultSlotData.isHungerRestrictionEnabled());
        assertTrue(defaultSlotData.isOffhandRestrictionEnabled());
        assertTrue(defaultSlotData.isReachRestrictionEnabled());
        assertTrue(defaultSlotData.isInventorySlotRestrictionEnabled());
        assertTrue(defaultSlotData.isStructureRestrictionEnabled());
        assertTrue(defaultSlotData.isWorldBorderRestrictionEnabled());
        assertTrue(defaultSlotData.isTrapsEnabled());
        assertTrue(defaultSlotData.isCursesEnabled());
        assertFalse(defaultSlotData.isStructureUnlockShuffle());

        APMCData defaultApmcData = new APMCData();
        assertTrue(defaultApmcData.isHpRestrictionEnabled());
        assertTrue(defaultApmcData.isHungerRestrictionEnabled());
        assertTrue(defaultApmcData.isOffhandRestrictionEnabled());
        assertTrue(defaultApmcData.isReachRestrictionEnabled());
        assertTrue(defaultApmcData.isInventorySlotRestrictionEnabled());
        assertTrue(defaultApmcData.isStructureRestrictionEnabled());
        assertTrue(defaultApmcData.isWorldBorderRestrictionEnabled());
        assertTrue(defaultApmcData.isTrapsEnabled());
        assertTrue(defaultApmcData.isCursesEnabled());
        assertFalse(defaultApmcData.isStructureUnlockShuffle());
        assertEquals(10, defaultSlotData.getWanderingTraderTrades());
        assertEquals(10, defaultApmcData.getWanderingTraderTrades());
        assertTrue(defaultSlotData.isBacapAdvancements());
        assertTrue(defaultApmcData.isBacapAdvancements());
        assertEquals(70.0, defaultSlotData.recipeTierBias);
        assertEquals(70.0, defaultApmcData.recipe_tier_bias);
    }

    @Test
    public void testWanderingTraderSettings() {
        String json = """
        {
            "wandering_trader_trades": 7
        }
        """;
        Gson gson = new Gson();
        SlotData slotData = gson.fromJson(json, SlotData.class);
        assertEquals(7, slotData.getWanderingTraderTrades());

        APMCData apmcData = gson.fromJson(json, APMCData.class);
        assertEquals(7, apmcData.getWanderingTraderTrades());

        // Test alternate key
        String jsonAlt = """
        {
            "trader_trades": 12
        }
        """;
        SlotData slotDataAlt = gson.fromJson(jsonAlt, SlotData.class);
        assertEquals(12, slotDataAlt.getWanderingTraderTrades());

        APMCData apmcDataAlt = gson.fromJson(jsonAlt, APMCData.class);
        assertEquals(12, apmcDataAlt.getWanderingTraderTrades());

        // Test WanderingTraderManager location ID helpers
        assertEquals(139L, gg.archipelago.aprandomizer.managers.trademanager.WanderingTraderManager.getTraderLocationId(1));
        assertEquals(158L, gg.archipelago.aprandomizer.managers.trademanager.WanderingTraderManager.getTraderLocationId(20));
        assertEquals(1, gg.archipelago.aprandomizer.managers.trademanager.WanderingTraderManager.getTraderIndex(139L));
        assertEquals(20, gg.archipelago.aprandomizer.managers.trademanager.WanderingTraderManager.getTraderIndex(158L));
        assertTrue(gg.archipelago.aprandomizer.managers.trademanager.WanderingTraderManager.isTraderLocation(139L));
        assertTrue(gg.archipelago.aprandomizer.managers.trademanager.WanderingTraderManager.isTraderLocation(158L));
        assertFalse(gg.archipelago.aprandomizer.managers.trademanager.WanderingTraderManager.isTraderLocation(138L));
        assertFalse(gg.archipelago.aprandomizer.managers.trademanager.WanderingTraderManager.isTraderLocation(159L));

        // In test environment, default apmcData has 10 trades and no advancements yet, so hasUncompletedTrades is true
        assertEquals(10, gg.archipelago.aprandomizer.managers.trademanager.WanderingTraderManager.getConfiguredTradesCount());
        assertTrue(gg.archipelago.aprandomizer.managers.trademanager.WanderingTraderManager.hasUncompletedTrades());

        // ArchipelagoTraderSpawner can be instantiated
        gg.archipelago.aprandomizer.managers.trademanager.ArchipelagoTraderSpawner spawner = new gg.archipelago.aprandomizer.managers.trademanager.ArchipelagoTraderSpawner();
        assertNotNull(spawner);
    }

    @Test
    public void testStartingSharedChestSettings() {
        // Defaults
        SlotData defaultSlotData = new SlotData();
        assertTrue(defaultSlotData.isStartingSharedChestEnabled());

        APMCData defaultApmcData = new APMCData();
        assertTrue(defaultApmcData.isStartingSharedChestEnabled());

        // RecipeTree fallback
        RecipeTreeManager.ensureLoaded();
        assertTrue(RecipeTreeManager.isSettingEnabled("starting_shared_chest", false));

        // Deserialization with false
        String jsonDisabled = """
        {
            "starting_shared_chest": false
        }
        """;
        Gson gson = new Gson();
        SlotData slotDisabled = gson.fromJson(jsonDisabled, SlotData.class);
        assertFalse(slotDisabled.isStartingSharedChestEnabled());

        APMCData apmcDisabled = gson.fromJson(jsonDisabled, APMCData.class);
        assertFalse(apmcDisabled.isStartingSharedChestEnabled());

        // Alternates
        String jsonAlt = """
        {
            "give_shared_chest": 0
        }
        """;
        SlotData slotAlt = gson.fromJson(jsonAlt, SlotData.class);
        assertFalse(slotAlt.isStartingSharedChestEnabled());

        APMCData apmcAlt = gson.fromJson(jsonAlt, APMCData.class);
        assertFalse(apmcAlt.isStartingSharedChestEnabled());
    }

    @Test
    public void testBacapSettings() {
        String jsonBacap = """
        {
            "advancement_type": "bacap",
            "bacap_advancement_step": 10,
            "bacap_check_count": 150
        }
        """;
        Gson gson = new Gson();
        SlotData slotBacap = gson.fromJson(jsonBacap, SlotData.class);
        assertTrue(slotBacap.isBacapAdvancements());
        assertEquals(10, slotBacap.getBacapStep());
        assertEquals(150, slotBacap.getBacapCheckCount());

        APMCData apmcBacap = gson.fromJson(jsonBacap, APMCData.class);
        assertTrue(apmcBacap.isBacapAdvancements());
        assertEquals(10, apmcBacap.getBacapStep());
        assertEquals(150, apmcBacap.getBacapCheckCount());

        // Default BACAP checks without explicit bacap_check_count: 1200 / 10 = 120
        SlotData defaultSlot = new SlotData();
        assertEquals(120, defaultSlot.getBacapCheckCount());
        APMCData defaultApmc = new APMCData();
        assertEquals(120, defaultApmc.getBacapCheckCount());

        // Dynamic calculation when bacap_check_count is 0 with step 20: 1200 / 20 = 60
        String jsonStep20 = """
        {
            "advancement_type": "bacap",
            "bacap_advancement_step": 20,
            "bacap_check_count": 0
        }
        """;
        SlotData slotStep20 = gson.fromJson(jsonStep20, SlotData.class);
        assertEquals(60, slotStep20.getBacapCheckCount());
        APMCData apmcStep20 = gson.fromJson(jsonStep20, APMCData.class);
        assertEquals(60, apmcStep20.getBacapCheckCount());

        // Default vanilla
        String jsonVanilla = """
        {
            "advancement_type": "vanilla"
        }
        """;
        SlotData slotVanilla = gson.fromJson(jsonVanilla, SlotData.class);
        assertFalse(slotVanilla.isBacapAdvancements());
        assertEquals(10, slotVanilla.getBacapStep());
        assertEquals(120, slotVanilla.getBacapCheckCount());

        // Milestone location IDs
        assertEquals(201L, gg.archipelago.aprandomizer.managers.advancementmanager.AdvancementManager.getBacapMilestoneLocationID(1));
        assertEquals(202L, gg.archipelago.aprandomizer.managers.advancementmanager.AdvancementManager.getBacapMilestoneLocationID(2));
        assertEquals(300L, gg.archipelago.aprandomizer.managers.advancementmanager.AdvancementManager.getBacapMilestoneLocationID(100));
        assertEquals(320L, gg.archipelago.aprandomizer.managers.advancementmanager.AdvancementManager.getBacapMilestoneLocationID(120));
        assertEquals(500L, gg.archipelago.aprandomizer.managers.advancementmanager.AdvancementManager.getBacapMilestoneLocationID(300));
        assertEquals(1200, gg.archipelago.aprandomizer.managers.advancementmanager.AdvancementManager.TOTAL_BACAP_ADVANCEMENTS);
        assertEquals(300, gg.archipelago.aprandomizer.managers.advancementmanager.AdvancementManager.MAX_BACAP_MILESTONES);
    }

    @Test
    public void testRandomBacapSettings() {
        String jsonRandomBacap = """
        {
            "advancement_type": "random_bacap",
            "selected_bacap_advancements": [
                "blazeandcave:animal/kill_cow",
                "blazeandcave:mining/iron_man",
                "minecraft:adventure/bullseye"
            ],
            "selected_bacap_map": {
                "blazeandcave:animal/kill_cow": 1005,
                "blazeandcave:mining/iron_man": 1010,
                "minecraft:adventure/bullseye": 1015
            }
        }
        """;
        Gson gson = new Gson();
        SlotData slot = gson.fromJson(jsonRandomBacap, SlotData.class);
        assertTrue(slot.isRandomBacap());
        assertTrue(slot.isBacapAdvancements());
        assertFalse(slot.isBacapMilestones());
        assertFalse(slot.isVanillaAdvancements());
        assertEquals(3, slot.selectedBacapAdvancements.size());
        assertEquals(3, slot.selectedBacapMap.size());
        assertEquals(1005L, slot.selectedBacapMap.get("blazeandcave:animal/kill_cow"));

        APMCData apmc = gson.fromJson(jsonRandomBacap, APMCData.class);
        assertTrue(apmc.isRandomBacap());
        assertTrue(apmc.isBacapAdvancements());
        assertFalse(apmc.isBacapMilestones());
        assertFalse(apmc.isVanillaAdvancements());
        assertEquals(3, apmc.selected_bacap_advancements.size());
        assertEquals(3, apmc.selected_bacap_map.size());
        assertEquals(1010L, apmc.selected_bacap_map.get("blazeandcave:mining/iron_man"));

        AdvancementManager am = new AdvancementManager(null, apmc);
        Identifier cowAdv = Identifier.tryParse("blazeandcave:animal/kill_cow");
        Identifier ironAdv = Identifier.tryParse("blazeandcave:mining/iron_man");
        Identifier nonTarget = Identifier.tryParse("blazeandcave:building/build_house");

        assertTrue(am.isRandomBacapTarget(cowAdv));
        assertTrue(am.isRandomBacapTarget(ironAdv));
        assertFalse(am.isRandomBacapTarget(nonTarget));

        assertEquals(1005L, am.getRandomBacapLocationId(cowAdv));
        assertEquals(1010L, am.getRandomBacapLocationId(ironAdv));
        assertEquals(0L, am.getRandomBacapLocationId(nonTarget));

        assertFalse(am.isRandomBacapCompleted(cowAdv));
        am.addAdvancement(1005L);
        assertTrue(am.isRandomBacapCompleted(cowAdv));
    }

    @Test
    public void testExplorationSettings() {
        // Defaults
        SlotData defaultSlotData = new SlotData();
        assertFalse(defaultSlotData.isBiomeChecksEnabled());
        assertFalse(defaultSlotData.isStructureChecksEnabled());

        APMCData defaultApmcData = new APMCData();
        assertFalse(defaultApmcData.isBiomeChecksEnabled());
        assertFalse(defaultApmcData.isStructureChecksEnabled());

        // Deserialization with boolean true
        String jsonEnabled = """
        {
            "biome_checks": true,
            "structure_checks": true
        }
        """;
        Gson gson = new Gson();
        SlotData slotEnabled = gson.fromJson(jsonEnabled, SlotData.class);
        assertTrue(slotEnabled.isBiomeChecksEnabled());
        assertTrue(slotEnabled.isStructureChecksEnabled());

        APMCData apmcEnabled = gson.fromJson(jsonEnabled, APMCData.class);
        assertTrue(apmcEnabled.isBiomeChecksEnabled());
        assertTrue(apmcEnabled.isStructureChecksEnabled());

        // Deserialization with numeric values
        String jsonNumeric = """
        {
            "biome_checks": 1,
            "structure_checks": 0
        }
        """;
        SlotData slotNumeric = gson.fromJson(jsonNumeric, SlotData.class);
        assertTrue(slotNumeric.isBiomeChecksEnabled());
        assertFalse(slotNumeric.isStructureChecksEnabled());

        APMCData apmcNumeric = gson.fromJson(jsonNumeric, APMCData.class);
        assertTrue(apmcNumeric.isBiomeChecksEnabled());
        assertFalse(apmcNumeric.isStructureChecksEnabled());
    }

    @Test
    public void testExplorationDataCatalog() {
        assertEquals(66, ExplorationData.BIOMES.size());
        assertEquals(21, ExplorationData.STRUCTURES.size());

        java.util.Set<Long> seenLocationIds = new java.util.HashSet<>();
        java.util.Set<String> seenPaths = new java.util.HashSet<>();

        for (ExplorationData.BiomeEntry biome : ExplorationData.BIOMES) {
            assertTrue(biome.locationId() >= 501L && biome.locationId() <= 566L, "Biome loc ID out of range: " + biome.locationId());
            assertTrue(seenLocationIds.add(biome.locationId()), "Duplicate location ID: " + biome.locationId());
            assertTrue(seenPaths.add(biome.path()), "Duplicate biome path: " + biome.path());
            assertNotNull(biome.displayName());
            assertNotNull(biome.category());
            assertNotNull(biome.key());
            assertEquals("minecraft:" + biome.path(), biome.id());

            assertTrue(ExplorationData.isKnownBiome(biome.id()));
            assertSame(biome, ExplorationData.getBiomeById(biome.id()));
            assertSame(biome, ExplorationData.getBiomeByLocationId(biome.locationId()));

            ResourceKey<gg.archipelago.aprandomizer.locations.APLocation> key = ResourceKey.create(
                    APRegistries.ARCHIPELAGO_LOCATION,
                    Identifier.fromNamespaceAndPath(APRandomizer.MODID, "exploration/biome/" + biome.path())
            );
            assertEquals(biome.locationId(), AdvancementManager.getDefaultLocationId(key));
        }

        for (ExplorationData.StructureCheckEntry structure : ExplorationData.STRUCTURES) {
            assertTrue(structure.locationId() >= 601L && structure.locationId() <= 621L, "Structure loc ID out of range: " + structure.locationId());
            assertTrue(seenLocationIds.add(structure.locationId()), "Duplicate location ID: " + structure.locationId());
            assertTrue(seenPaths.add(structure.path()), "Duplicate structure path: " + structure.path());
            assertNotNull(structure.displayName());
            assertFalse(structure.structureKeys().isEmpty());

            assertTrue(ExplorationData.isKnownStructure(structure.id()));
            assertSame(structure, ExplorationData.getStructureById(structure.id()));
            assertSame(structure, ExplorationData.getStructureByLocationId(structure.locationId()));

            ResourceKey<gg.archipelago.aprandomizer.locations.APLocation> key = ResourceKey.create(
                    APRegistries.ARCHIPELAGO_LOCATION,
                    Identifier.fromNamespaceAndPath(APRandomizer.MODID, "exploration/structure/" + structure.path())
            );
            assertEquals(structure.locationId(), AdvancementManager.getDefaultLocationId(key));
        }
    }

    @Test
    public void testCheckGoalAndFinishedAmount() {
        // Test SlotData check_goal deserialization and default
        SlotData defaultSlotData = new SlotData();
        assertEquals(40, defaultSlotData.getAdvancementGoal());

        String jsonCheckGoal = """
        {
            "check_goal": 25
        }
        """;
        Gson gson = new Gson();
        SlotData slotData1 = gson.fromJson(jsonCheckGoal, SlotData.class);
        assertEquals(25, slotData1.getAdvancementGoal());

        APMCData apmcData1 = gson.fromJson(jsonCheckGoal, APMCData.class);
        assertEquals(25, apmcData1.getAdvancementGoal());

        // Test backward compatibility alternate key advancement_goal
        String jsonAdvGoal = """
        {
            "advancement_goal": 35
        }
        """;
        SlotData slotData2 = gson.fromJson(jsonAdvGoal, SlotData.class);
        assertEquals(35, slotData2.getAdvancementGoal());

        APMCData apmcData2 = gson.fromJson(jsonAdvGoal, APMCData.class);
        assertEquals(35, apmcData2.getAdvancementGoal());

        // Test AdvancementManager finished amount simultaneous checks
        WorldData worldData = new WorldData();
        AdvancementManager advancementManager = new AdvancementManager(worldData);
        assertEquals(0, advancementManager.getFinishedAmount());

        it.unimi.dsi.fastutil.longs.LongSet checks = new it.unimi.dsi.fastutil.longs.LongOpenHashSet();
        checks.add(1L);   // Vanilla advancement check
        checks.add(201L); // BACAP milestone check 1
        checks.add(202L); // BACAP milestone check 2
        checks.add(501L); // Biome check
        checks.add(601L); // Structure check
        checks.add(139L); // Trader trade check

        advancementManager.setCheckedAdvancements(checks);
        assertEquals(6, advancementManager.getFinishedAmount());
    }

    @Test
    public void testHungerLevels() {
        WorldData worldData = new WorldData();
        assertEquals(0, worldData.getHungerUpgrades());
        int baseFood = Math.min(20, 8 + worldData.getHungerUpgrades() * 2);
        assertEquals(8, baseFood);

        worldData.incrementHungerUpgrades();
        assertEquals(1, worldData.getHungerUpgrades());
        int upgrade1Food = Math.min(20, 8 + worldData.getHungerUpgrades() * 2);
        assertEquals(10, upgrade1Food);

        for (int i = 0; i < 5; i++) {
            worldData.incrementHungerUpgrades();
        }
        assertEquals(6, worldData.getHungerUpgrades());
        int maxFood = Math.min(20, 8 + worldData.getHungerUpgrades() * 2);
        assertEquals(20, maxFood);

        // Test respawn saturation scaling
        assertEquals(2.0f, gg.archipelago.aprandomizer.common.Utils.Utils.getRespawnSaturation(8), 0.001f);
        assertEquals(2.5f, gg.archipelago.aprandomizer.common.Utils.Utils.getRespawnSaturation(10), 0.001f);
        assertEquals(3.0f, gg.archipelago.aprandomizer.common.Utils.Utils.getRespawnSaturation(12), 0.001f);
        assertEquals(5.0f, gg.archipelago.aprandomizer.common.Utils.Utils.getRespawnSaturation(20), 0.001f);
    }

    @Test
    public void testBossesEnum() {
        assertFalse(APMCData.Bosses.NONE.hasDragon());
        assertFalse(APMCData.Bosses.NONE.hasWither());

        assertTrue(APMCData.Bosses.ENDER_DRAGON.hasDragon());
        assertFalse(APMCData.Bosses.ENDER_DRAGON.hasWither());

        assertFalse(APMCData.Bosses.WITHER.hasDragon());
        assertTrue(APMCData.Bosses.WITHER.hasWither());

        assertTrue(APMCData.Bosses.BOTH.hasDragon());
        assertTrue(APMCData.Bosses.BOTH.hasWither());
    }

    @Test
    public void testMonsterTrapPercentage() {
        SlotData defaultSlot = new SlotData();
        assertEquals(20, defaultSlot.getMonsterTrapPercentage());

        APMCData defaultApmc = new APMCData();
        assertEquals(20, defaultApmc.getMonsterTrapPercentage());

        String json = """
        {
            "monster_trap_percentage": 35
        }
        """;
        Gson gson = new Gson();
        SlotData customSlot = gson.fromJson(json, SlotData.class);
        assertEquals(35, customSlot.getMonsterTrapPercentage());

        APMCData customApmc = gson.fromJson(json, APMCData.class);
        assertEquals(35, customApmc.getMonsterTrapPercentage());

        // Test clamping to 0-100
        String jsonHigh = "{\"monster_trap_percentage\": 150}";
        SlotData clampedHigh = gson.fromJson(jsonHigh, SlotData.class);
        assertEquals(100, clampedHigh.getMonsterTrapPercentage());

        String jsonLow = "{\"monster_trap_percentage\": -20}";
        SlotData clampedLow = gson.fromJson(jsonLow, SlotData.class);
        assertEquals(0, clampedLow.getMonsterTrapPercentage());

        // Test alternate key mob_trap_percentage
        String jsonAlt = "{\"mob_trap_percentage\": 15}";
        SlotData altSlot = gson.fromJson(jsonAlt, SlotData.class);
        assertEquals(15, altSlot.getMonsterTrapPercentage());
    }

    @Test
    public void testLockedRecipesOnlyFromRecipeTree() {
        RecipeTreeManager.ensureLoaded();
        var allTreeRecipes = RecipeTreeManager.getAllTreeRecipes();
        assertFalse(allTreeRecipes.isEmpty(), "Tree recipes should not be empty");

        WorldData worldData = new WorldData();
        gg.archipelago.aprandomizer.managers.itemmanager.ItemManager itemManager =
                new gg.archipelago.aprandomizer.managers.itemmanager.ItemManager(null, null, worldData);

        var lockedRecipes = itemManager.getLockedRecipes(null);

        // Tier 0 initial recipes must not be locked
        for (var initRecipe : RecipeTreeManager.getInitialUnlockedRecipes()) {
            assertFalse(lockedRecipes.contains(initRecipe), "Initial tier 0 recipe should not be locked: " + initRecipe);
        }

        // Recipes NOT in recipe_tree.json must NOT be locked (e.g. stone_pickaxe, torch)
        ResourceKey<net.minecraft.world.item.crafting.Recipe<?>> stonePickaxe =
                ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE,
                        Identifier.withDefaultNamespace("stone_pickaxe"));
        ResourceKey<net.minecraft.world.item.crafting.Recipe<?>> torch =
                ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE,
                        Identifier.withDefaultNamespace("torch"));

        assertFalse(allTreeRecipes.contains(stonePickaxe), "stone_pickaxe should not be in recipe_tree.json");
        assertFalse(allTreeRecipes.contains(torch), "torch should not be in recipe_tree.json");
        assertFalse(lockedRecipes.contains(stonePickaxe), "stone_pickaxe must NOT be locked");
        assertFalse(lockedRecipes.contains(torch), "torch must NOT be locked");

        // Recipes in recipe_tree.json (tier > 0) should be locked initially
        ResourceKey<net.minecraft.world.item.crafting.Recipe<?>> copperPickaxe =
                ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE,
                        Identifier.withDefaultNamespace("copper_pickaxe"));
        if (allTreeRecipes.contains(copperPickaxe)) {
            assertTrue(lockedRecipes.contains(copperPickaxe), "copper_pickaxe should be locked initially");

            // When unlocked in worldData, it should no longer be locked
            worldData.addUnlockedRecipe(copperPickaxe);
            var updatedLocked = itemManager.getLockedRecipes(null);
            assertFalse(updatedLocked.contains(copperPickaxe), "copper_pickaxe should be unlocked after being added to worldData");
        }
    }

    @Test
    public void testLegacyArchipelagoAdvancementsRemoved() {
        java.io.File legacyAdvDir = new java.io.File("src/generated/resources/data/aprandomizer/advancement/archipelago");
        assertFalse(legacyAdvDir.exists(), "Legacy advancement tab directory 'archipelago' must not exist");
    }

    @Test
    public void testPluralizeAdvancements() {
        assertEquals("1 ачивка", gg.archipelago.aprandomizer.common.Utils.Utils.pluralizeAdvancements(1));
        assertEquals("2 ачивки", gg.archipelago.aprandomizer.common.Utils.Utils.pluralizeAdvancements(2));
        assertEquals("3 ачивки", gg.archipelago.aprandomizer.common.Utils.Utils.pluralizeAdvancements(3));
        assertEquals("4 ачивки", gg.archipelago.aprandomizer.common.Utils.Utils.pluralizeAdvancements(4));
        assertEquals("5 ачивок", gg.archipelago.aprandomizer.common.Utils.Utils.pluralizeAdvancements(5));
        assertEquals("11 ачивок", gg.archipelago.aprandomizer.common.Utils.Utils.pluralizeAdvancements(11));
        assertEquals("12 ачивок", gg.archipelago.aprandomizer.common.Utils.Utils.pluralizeAdvancements(12));
        assertEquals("20 ачивок", gg.archipelago.aprandomizer.common.Utils.Utils.pluralizeAdvancements(20));
        assertEquals("21 ачивка", gg.archipelago.aprandomizer.common.Utils.Utils.pluralizeAdvancements(21));
        assertEquals("22 ачивки", gg.archipelago.aprandomizer.common.Utils.Utils.pluralizeAdvancements(22));
        assertEquals("25 ачивок", gg.archipelago.aprandomizer.common.Utils.Utils.pluralizeAdvancements(25));
    }

    @Test
    public void testFormatActionBarMessage() {
        net.minecraft.network.chat.Component title = net.minecraft.network.chat.Component.literal("Received");
        net.minecraft.network.chat.Component subTitle = net.minecraft.network.chat.Component.literal("Diamond Sword");
        net.minecraft.network.chat.Component result = gg.archipelago.aprandomizer.common.Utils.QueuedTitle.formatActionBarMessage(title, subTitle);
        assertTrue(result.getString().contains("Получено: Diamond Sword"));

        net.minecraft.network.chat.Component hpTitle = net.minecraft.network.chat.Component.literal("+1 Сердце (HP)");
        net.minecraft.network.chat.Component hpSub = net.minecraft.network.chat.Component.literal("Максимальное здоровье увеличено!");
        net.minecraft.network.chat.Component hpResult = gg.archipelago.aprandomizer.common.Utils.QueuedTitle.formatActionBarMessage(hpTitle, hpSub);
        assertTrue(hpResult.getString().contains("+1 Сердце (HP)"));
        assertTrue(hpResult.getString().contains("Максимальное здоровье увеличено!"));
    }

    @Test
    public void testLocationCheckPoints() {
        Gson gson = new Gson();
        SlotData defaultSlot = new SlotData();
        assertEquals(10, defaultSlot.getHintCost());

        String json = "{\"hint_cost\": 5}";
        SlotData customSlot = gson.fromJson(json, SlotData.class);
        assertEquals(5, customSlot.getHintCost());

        String jsonAlt = "{\"hint_points_per_check\": 15}";
        SlotData altSlot = gson.fromJson(jsonAlt, SlotData.class);
        assertEquals(15, altSlot.getHintCost());

        String jsonZero = "{\"hint_cost\": 0}";
        SlotData zeroSlot = gson.fromJson(jsonZero, SlotData.class);
        assertEquals(0, zeroSlot.getHintCost());

        gg.archipelago.aprandomizer.ap.storage.APMCData apmcData = new gg.archipelago.aprandomizer.ap.storage.APMCData();
        assertEquals(10, apmcData.getHintCost());

        gg.archipelago.aprandomizer.ap.storage.APMCData customApmc =
                gson.fromJson("{\"hint_cost\": 8}", gg.archipelago.aprandomizer.ap.storage.APMCData.class);
        assertEquals(8, customApmc.getHintCost());
    }

    @Test
    public void testStructureNamesMapping() {
        assertEquals("Village",
                gg.archipelago.aprandomizer.common.Utils.Utils.getAPStructureName(Identifier.fromNamespaceAndPath("aprandomizer", "village")));
        assertEquals("Pillager Outpost",
                gg.archipelago.aprandomizer.common.Utils.Utils.getAPStructureName(Identifier.fromNamespaceAndPath("aprandomizer", "pillager_outpost")));
        assertEquals("Nether Fortress",
                gg.archipelago.aprandomizer.common.Utils.Utils.getAPStructureName(Identifier.fromNamespaceAndPath("aprandomizer", "fortress")));
        assertEquals("Bastion Remnant",
                gg.archipelago.aprandomizer.common.Utils.Utils.getAPStructureName(Identifier.fromNamespaceAndPath("aprandomizer", "bastion_remnant")));
        assertEquals("End City",
                gg.archipelago.aprandomizer.common.Utils.Utils.getAPStructureName(Identifier.fromNamespaceAndPath("aprandomizer", "end_city")));
    }

    @Test
    public void testTierColorsAndNonRecipeWhite() {
        RecipeTreeManager.ensureLoaded();

        // Exact colors requested by user:
        // Tier 1: коричневый (0xFF8D5524)
        // Tier 2: серый (0xFFA0A0A0)
        // Tier 3: зелёный (0xFF4CAF50)
        // Tier 4: синий (0xFF2196F3)
        // Tier 5: фиолетовый (0xFF9C27B0)
        // Tier 6: оранжевый (0xFFFF9800)
        // Tier 7: красный (0xFFF44336)
        // Non-recipe / Tier 0: белый (0xFFFFFFFF)
        assertEquals(0xFF8D5524, RecipeTreeManager.getTierColor(1));
        assertEquals(0xFFA0A0A0, RecipeTreeManager.getTierColor(2));
        assertEquals(0xFF4CAF50, RecipeTreeManager.getTierColor(3));
        assertEquals(0xFF2196F3, RecipeTreeManager.getTierColor(4));
        assertEquals(0xFF9C27B0, RecipeTreeManager.getTierColor(5));
        assertEquals(0xFFFF9800, RecipeTreeManager.getTierColor(6));
        assertEquals(0xFFF44336, RecipeTreeManager.getTierColor(7));

        assertEquals(0xFFFFFFFF, RecipeTreeManager.getTierColor(0));
        assertEquals(0xFFFFFFFF, RecipeTreeManager.getTierColor(-1));
        assertEquals(0xFFFFFFFF, RecipeTreeManager.getTierColor(8));

        // Test stripe color helper matches tile color
        for (int t = 0; t <= 7; t++) {
            assertEquals(RecipeTreeManager.getTierColor(t), RecipeTreeManager.getTierStripeColor(t));
        }

        // Non-recipe advancement IDs must return Tier 0 (White)
        Identifier nonRecipeAdv1 = Identifier.fromNamespaceAndPath("minecraft", "story/root");
        Identifier nonRecipeAdv2 = Identifier.fromNamespaceAndPath("aprandomizer", "received/second_hand");
        assertEquals(0, RecipeTreeManager.getAdvancementTier(nonRecipeAdv1));
        assertEquals(0xFFFFFFFF, RecipeTreeManager.getTierColor(RecipeTreeManager.getAdvancementTier(nonRecipeAdv1)));
        assertEquals(0, RecipeTreeManager.getAdvancementTier(nonRecipeAdv2));
        assertEquals(0xFFFFFFFF, RecipeTreeManager.getTierColor(RecipeTreeManager.getAdvancementTier(nonRecipeAdv2)));

        // Recipe advancement IDs must return their respective tier (> 0)
        Identifier woodenSwordAdv = Identifier.fromNamespaceAndPath("aprandomizer", "received/wooden_sword");
        int woodenSwordTier = RecipeTreeManager.getAdvancementTier(woodenSwordAdv);
        assertEquals(1, woodenSwordTier);
        assertEquals(0xFF8D5524, RecipeTreeManager.getTierColor(woodenSwordTier));

        var node = RecipeTreeManager.getNodeByAdvancement(woodenSwordAdv);
        assertNotNull(node);
        assertEquals("Wooden Sword", node.name());
    }

    @Test
    public void testTicketModeAndThresholdSettings() {
        Gson gson = new Gson();

        // Default settings
        SlotData defaultSlot = new SlotData();
        assertFalse(defaultSlot.isTicketMode());
        assertEquals(50, defaultSlot.getRecipeTierUnlockPercentage());

        APMCData defaultApmc = new APMCData();
        assertFalse(defaultApmc.isTicketMode());
        assertEquals(50, defaultApmc.getRecipeTierUnlockPercentage());

        // Deserialization with Ticket Mode (int 1) and threshold 75%
        String jsonTicket1 = """
        {
            "recipe_unlock_mode": 1,
            "recipe_tier_unlock_percentage": 75
        }
        """;
        SlotData slot1 = gson.fromJson(jsonTicket1, SlotData.class);
        assertTrue(slot1.isTicketMode());
        assertEquals(75, slot1.getRecipeTierUnlockPercentage());

        APMCData apmc1 = gson.fromJson(jsonTicket1, APMCData.class);
        assertTrue(apmc1.isTicketMode());
        assertEquals(75, apmc1.getRecipeTierUnlockPercentage());

        // Deserialization with string "ticket_mode_skilltree" and clamping
        String jsonTicketStr = """
        {
            "recipe_unlock_mode": "ticket_mode_skilltree",
            "recipe_tier_unlock_percentage": 150
        }
        """;
        SlotData slot2 = gson.fromJson(jsonTicketStr, SlotData.class);
        assertTrue(slot2.isTicketMode());
        assertEquals(100, slot2.getRecipeTierUnlockPercentage());

        APMCData apmc2 = gson.fromJson(jsonTicketStr, APMCData.class);
        assertTrue(apmc2.isTicketMode());
        assertEquals(100, apmc2.getRecipeTierUnlockPercentage());

        // Deserialization with "random_by_bias"
        String jsonRandom = """
        {
            "recipe_unlock_mode": "random_by_bias",
            "recipe_tier_unlock_percentage": -10
        }
        """;
        SlotData slot3 = gson.fromJson(jsonRandom, SlotData.class);
        assertFalse(slot3.isTicketMode());
        assertEquals(0, slot3.getRecipeTierUnlockPercentage());

        APMCData apmc3 = gson.fromJson(jsonRandom, APMCData.class);
        assertFalse(apmc3.isTicketMode());
        assertEquals(0, apmc3.getRecipeTierUnlockPercentage());
    }

    @Test
    public void testRecipeTreeProgressionRules() {
        RecipeTreeManager.ensureLoaded();

        // Golden pickaxe node (node_14, Tier 5) requires Iron pickaxe (node_13, Tier 4)
        var goldPick = RecipeTreeManager.getNodeById("node_14");
        var ironPick = RecipeTreeManager.getNodeById("node_13");
        assertNotNull(goldPick);
        assertNotNull(ironPick);

        java.util.Set<net.minecraft.resources.ResourceKey<net.minecraft.world.item.crafting.Recipe<?>>> unlocked = new java.util.HashSet<>();

        // Cannot unlock gold pickaxe if iron pickaxe is not unlocked
        assertFalse(RecipeTreeManager.canUnlockInTree(goldPick, unlocked));

        // Unlock iron pickaxe
        unlocked.addAll(ironPick.recipes());
        assertTrue(RecipeTreeManager.canUnlockInTree(goldPick, unlocked));

        // Tier progression rule: with 50% threshold, tier 2 node cannot unlock if 0% of tier 1 is unlocked
        var t1Nodes = RecipeTreeManager.getNodesByTier(1);
        var t2Nodes = RecipeTreeManager.getNodesByTier(2);
        assertFalse(t1Nodes.isEmpty());
        assertFalse(t2Nodes.isEmpty());

        var sampleT2 = t2Nodes.get(0);
        java.util.Set<net.minecraft.resources.ResourceKey<net.minecraft.world.item.crafting.Recipe<?>>> freshUnlocked = new java.util.HashSet<>();
        assertFalse(RecipeTreeManager.canUnlockInTierProgression(sampleT2, freshUnlocked, 50));

        // Unlock all T1 nodes -> now T2 progression check passes
        for (var t1 : t1Nodes) {
            freshUnlocked.addAll(t1.recipes());
        }
        assertTrue(RecipeTreeManager.canUnlockInTierProgression(sampleT2, freshUnlocked, 50));
    }

    @Test
    public void testTicketModeSkillTreeStatus() {
        RecipeTreeManager.ensureLoaded();
        RecipeTreeManager.setTicketMode(true);
        try {
            var t1Nodes = RecipeTreeManager.getNodesByTier(1);
            assertFalse(t1Nodes.isEmpty());
            var t1Node = t1Nodes.get(0);

            var leatherHelmet = RecipeTreeManager.getNodeById("node_130"); // Tier 2 root
            var copperPick = RecipeTreeManager.getNodeById("node_12"); // Tier 3 root
            var ironPick = RecipeTreeManager.getNodeById("node_13"); // Tier 4 child of copperPick
            assertNotNull(leatherHelmet);
            assertNotNull(copperPick);
            assertNotNull(ironPick);

            java.util.Set<net.minecraft.resources.ResourceKey<net.minecraft.world.item.crafting.Recipe<?>>> unlocked = new java.util.HashSet<>();

            // Tier 1 node with tickets > 0 is AVAILABLE
            assertEquals(RecipeTreeManager.UnlockStatus.AVAILABLE, RecipeTreeManager.getUnlockStatus(t1Node, unlocked, 1, 50));
            // Tier 1 node with 0 tickets is NO_TICKETS
            assertEquals(RecipeTreeManager.UnlockStatus.NO_TICKETS, RecipeTreeManager.getUnlockStatus(t1Node, unlocked, 0, 50));

            // Higher tier nodes without meeting tier 1 threshold are LOCKED_TIER
            assertEquals(RecipeTreeManager.UnlockStatus.LOCKED_TIER, RecipeTreeManager.getUnlockStatus(leatherHelmet, unlocked, 5, 50));
            assertEquals(RecipeTreeManager.UnlockStatus.LOCKED_TIER, RecipeTreeManager.getUnlockStatus(copperPick, unlocked, 5, 50));

            // If threshold is 0, tier requirement is disabled -> AVAILABLE
            assertEquals(RecipeTreeManager.UnlockStatus.AVAILABLE, RecipeTreeManager.getUnlockStatus(leatherHelmet, unlocked, 1, 0));

            // Unlock all T1 nodes -> leatherHelmet (T2) becomes AVAILABLE
            for (var t1 : t1Nodes) {
                unlocked.addAll(t1.recipes());
            }
            assertEquals(RecipeTreeManager.UnlockStatus.AVAILABLE, RecipeTreeManager.getUnlockStatus(leatherHelmet, unlocked, 1, 50));

            // Child node without parent unlocked is LOCKED_TREE
            assertEquals(RecipeTreeManager.UnlockStatus.LOCKED_TREE, RecipeTreeManager.getUnlockStatus(ironPick, unlocked, 5, 50));

            // Unlock all T2 and T3 nodes plus copperPick (parent) -> ironPick becomes AVAILABLE
            for (var t2 : RecipeTreeManager.getNodesByTier(2)) {
                unlocked.addAll(t2.recipes());
            }
            for (var t3 : RecipeTreeManager.getNodesByTier(3)) {
                unlocked.addAll(t3.recipes());
            }
            assertEquals(RecipeTreeManager.UnlockStatus.AVAILABLE, RecipeTreeManager.getUnlockStatus(ironPick, unlocked, 5, 50));

            // Unlock child -> child becomes UNLOCKED
            unlocked.addAll(ironPick.recipes());
            assertEquals(RecipeTreeManager.UnlockStatus.UNLOCKED, RecipeTreeManager.getUnlockStatus(ironPick, unlocked, 5, 50));
        } finally {
            RecipeTreeManager.setTicketMode(false);
        }
    }
}














