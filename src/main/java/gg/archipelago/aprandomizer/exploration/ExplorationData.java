package gg.archipelago.aprandomizer.exploration;

import gg.archipelago.aprandomizer.APRandomizer;
import gg.archipelago.aprandomizer.tags.APStructureTags;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.StructureTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Supplier;

public class ExplorationData {

    public enum BiomeCategory {
        OVERWORLD("overworld_biomes", "Overworld Biomes", () -> Items.GRASS_BLOCK),
        CAVES_AND_PEAKS("caves_biomes", "Caves & Peaks", () -> Items.POINTED_DRIPSTONE),
        NETHER("nether_biomes", "Nether Biomes", () -> Items.NETHERRACK),
        THE_END("end_biomes", "The End Biomes", () -> Items.END_STONE);

        private final String path;
        private final String displayName;
        private final Supplier<Item> iconSupplier;

        BiomeCategory(String path, String displayName, Supplier<Item> iconSupplier) {
            this.path = path;
            this.displayName = displayName;
            this.iconSupplier = iconSupplier;
        }

        public String getPath() {
            return path;
        }

        public String getDisplayName() {
            return displayName;
        }

        public Item getIcon() {
            return iconSupplier.get();
        }

        public Identifier getAdvancementId() {
            return Identifier.fromNamespaceAndPath(APRandomizer.MODID, "exploration/" + path);
        }
    }

    public enum BiomeCheckGroup {
        MAJOR,
        SMALL,
        ENDGAME
    }

    public record BiomeEntry(
            String id,
            ResourceKey<Biome> key,
            long locationId,
            String path,
            String displayName,
            Supplier<Item> iconSupplier,
            BiomeCategory category,
            BiomeCheckGroup group
    ) {
        public Item icon() {
            return iconSupplier.get();
        }

        public Identifier getAdvancementId() {
            return Identifier.fromNamespaceAndPath(APRandomizer.MODID, "exploration/biome/" + path);
        }

        public Identifier getCompassAdvancementId() {
            return Identifier.fromNamespaceAndPath(APRandomizer.MODID, "exploration/biome/" + path + "/compass");
        }

        public String getLocationName() {
            return "Explore " + displayName;
        }

        public boolean isMajor() {
            return group == BiomeCheckGroup.MAJOR;
        }

        public boolean isSmall() {
            return group == BiomeCheckGroup.SMALL;
        }

        public boolean isEndGame() {
            return group == BiomeCheckGroup.ENDGAME;
        }
    }

    public record StructureCheckEntry(
            String id,
            List<ResourceKey<Structure>> structureKeys,
            @Nullable TagKey<Structure> tag,
            long locationId,
            String path,
            String displayName,
            Supplier<Item> iconSupplier
    ) {
        public Item icon() {
            return iconSupplier.get();
        }

        public Identifier getAdvancementId() {
            return Identifier.fromNamespaceAndPath(APRandomizer.MODID, "exploration/structure/" + path);
        }

        public String getLocationName() {
            return "Explore " + displayName;
        }
    }

    public static final Identifier ROOT_ADVANCEMENT_ID = Identifier.fromNamespaceAndPath(APRandomizer.MODID, "exploration/root");
    public static final Identifier STRUCTURES_CATEGORY_ID = Identifier.fromNamespaceAndPath(APRandomizer.MODID, "exploration/structures");

    public static final List<BiomeEntry> BIOMES = new ArrayList<>();
    public static final List<StructureCheckEntry> STRUCTURES = new ArrayList<>();
    private static final Map<String, BiomeEntry> BIOME_BY_ID = new HashMap<>();
    private static final Map<String, StructureCheckEntry> STRUCTURE_BY_ID = new HashMap<>();
    private static final Map<Long, BiomeEntry> BIOME_BY_LOC_ID = new HashMap<>();
    private static final Map<Long, StructureCheckEntry> STRUCTURE_BY_LOC_ID = new HashMap<>();

    private static void registerBiome(ResourceKey<Biome> key, long locId, String path, String name, Supplier<Item> icon, BiomeCategory cat, BiomeCheckGroup group) {
        String id = key.identifier().toString();
        BiomeEntry entry = new BiomeEntry(id, key, locId, path, name, icon, cat, group);
        BIOMES.add(entry);
        BIOME_BY_ID.put(id, entry);
        BIOME_BY_LOC_ID.put(locId, entry);
    }

    private static void registerStructure(String id, List<ResourceKey<Structure>> keys, @Nullable TagKey<Structure> tag, long locId, String path, String name, Supplier<Item> icon) {
        StructureCheckEntry entry = new StructureCheckEntry(id, keys, tag, locId, path, name, icon);
        STRUCTURES.add(entry);
        STRUCTURE_BY_ID.put(id, entry);
        STRUCTURE_BY_LOC_ID.put(locId, entry);
    }

    static {
        // === 1. OVERWORLD BIOMES (501..548) ===
        registerBiome(Biomes.PLAINS, 501L, "plains", "Plains", () -> Items.GRASS_BLOCK, BiomeCategory.OVERWORLD, BiomeCheckGroup.MAJOR);
        registerBiome(Biomes.SUNFLOWER_PLAINS, 502L, "sunflower_plains", "Sunflower Plains", () -> Items.SUNFLOWER, BiomeCategory.OVERWORLD, BiomeCheckGroup.SMALL);
        registerBiome(Biomes.SNOWY_PLAINS, 503L, "snowy_plains", "Snowy Plains", () -> Items.SNOW_BLOCK, BiomeCategory.OVERWORLD, BiomeCheckGroup.MAJOR);
        registerBiome(Biomes.ICE_SPIKES, 504L, "ice_spikes", "Ice Spikes", () -> Items.PACKED_ICE, BiomeCategory.OVERWORLD, BiomeCheckGroup.SMALL);
        registerBiome(Biomes.DESERT, 505L, "desert", "Desert", () -> Items.SAND, BiomeCategory.OVERWORLD, BiomeCheckGroup.MAJOR);
        registerBiome(Biomes.SWAMP, 506L, "swamp", "Swamp", () -> Items.LILY_PAD, BiomeCategory.OVERWORLD, BiomeCheckGroup.MAJOR);
        registerBiome(Biomes.MANGROVE_SWAMP, 507L, "mangrove_swamp", "Mangrove Swamp", () -> Items.MANGROVE_PROPAGULE, BiomeCategory.OVERWORLD, BiomeCheckGroup.MAJOR);
        registerBiome(Biomes.FOREST, 508L, "forest", "Forest", () -> Items.OAK_SAPLING, BiomeCategory.OVERWORLD, BiomeCheckGroup.MAJOR);
        registerBiome(Biomes.FLOWER_FOREST, 509L, "flower_forest", "Flower Forest", () -> Items.ALLIUM, BiomeCategory.OVERWORLD, BiomeCheckGroup.SMALL);
        registerBiome(Biomes.BIRCH_FOREST, 510L, "birch_forest", "Birch Forest", () -> Items.BIRCH_SAPLING, BiomeCategory.OVERWORLD, BiomeCheckGroup.MAJOR);
        registerBiome(Biomes.DAPPLED_FOREST, 511L, "dappled_forest", "Dappled Forest", () -> Items.OAK_LEAVES, BiomeCategory.OVERWORLD, BiomeCheckGroup.MAJOR);
        registerBiome(Biomes.DARK_FOREST, 512L, "dark_forest", "Dark Forest", () -> Items.DARK_OAK_SAPLING, BiomeCategory.OVERWORLD, BiomeCheckGroup.MAJOR);
        registerBiome(Biomes.PALE_GARDEN, 513L, "pale_garden", "Pale Garden", () -> Items.PALE_OAK_SAPLING, BiomeCategory.OVERWORLD, BiomeCheckGroup.MAJOR);
        registerBiome(Biomes.OLD_GROWTH_BIRCH_FOREST, 514L, "old_growth_birch_forest", "Old Growth Birch Forest", () -> Items.BIRCH_LOG, BiomeCategory.OVERWORLD, BiomeCheckGroup.MAJOR);
        registerBiome(Biomes.OLD_GROWTH_PINE_TAIGA, 515L, "old_growth_pine_taiga", "Old Growth Pine Taiga", () -> Items.PODZOL, BiomeCategory.OVERWORLD, BiomeCheckGroup.MAJOR);
        registerBiome(Biomes.OLD_GROWTH_SPRUCE_TAIGA, 516L, "old_growth_spruce_taiga", "Old Growth Spruce Taiga", () -> Items.SPRUCE_LOG, BiomeCategory.OVERWORLD, BiomeCheckGroup.MAJOR);
        registerBiome(Biomes.TAIGA, 517L, "taiga", "Taiga", () -> Items.SPRUCE_SAPLING, BiomeCategory.OVERWORLD, BiomeCheckGroup.MAJOR);
        registerBiome(Biomes.SNOWY_TAIGA, 518L, "snowy_taiga", "Snowy Taiga", () -> Items.SNOW, BiomeCategory.OVERWORLD, BiomeCheckGroup.MAJOR);
        registerBiome(Biomes.SAVANNA, 519L, "savanna", "Savanna", () -> Items.ACACIA_SAPLING, BiomeCategory.OVERWORLD, BiomeCheckGroup.MAJOR);
        registerBiome(Biomes.SAVANNA_PLATEAU, 520L, "savanna_plateau", "Savanna Plateau", () -> Items.ACACIA_LOG, BiomeCategory.OVERWORLD, BiomeCheckGroup.SMALL);
        registerBiome(Biomes.WINDSWEPT_HILLS, 521L, "windswept_hills", "Windswept Hills", () -> Items.EMERALD_ORE, BiomeCategory.OVERWORLD, BiomeCheckGroup.SMALL);
        registerBiome(Biomes.WINDSWEPT_GRAVELLY_HILLS, 522L, "windswept_gravelly_hills", "Windswept Gravelly Hills", () -> Items.GRAVEL, BiomeCategory.OVERWORLD, BiomeCheckGroup.SMALL);
        registerBiome(Biomes.WINDSWEPT_FOREST, 523L, "windswept_forest", "Windswept Forest", () -> Items.OAK_LOG, BiomeCategory.OVERWORLD, BiomeCheckGroup.SMALL);
        registerBiome(Biomes.WINDSWEPT_SAVANNA, 524L, "windswept_savanna", "Windswept Savanna", () -> Items.ACACIA_LEAVES, BiomeCategory.OVERWORLD, BiomeCheckGroup.SMALL);
        registerBiome(Biomes.JUNGLE, 525L, "jungle", "Jungle", () -> Items.JUNGLE_SAPLING, BiomeCategory.OVERWORLD, BiomeCheckGroup.MAJOR);
        registerBiome(Biomes.SPARSE_JUNGLE, 526L, "sparse_jungle", "Sparse Jungle", () -> Items.MELON, BiomeCategory.OVERWORLD, BiomeCheckGroup.SMALL);
        registerBiome(Biomes.BAMBOO_JUNGLE, 527L, "bamboo_jungle", "Bamboo Jungle", () -> Items.BAMBOO, BiomeCategory.OVERWORLD, BiomeCheckGroup.MAJOR);
        registerBiome(Biomes.BADLANDS, 528L, "badlands", "Badlands", () -> Items.TERRACOTTA, BiomeCategory.OVERWORLD, BiomeCheckGroup.MAJOR);
        registerBiome(Biomes.ERODED_BADLANDS, 529L, "eroded_badlands", "Eroded Badlands", () -> Items.RED_SAND, BiomeCategory.OVERWORLD, BiomeCheckGroup.SMALL);
        registerBiome(Biomes.WOODED_BADLANDS, 530L, "wooded_badlands", "Wooded Badlands", () -> Items.COARSE_DIRT, BiomeCategory.OVERWORLD, BiomeCheckGroup.MAJOR);

        registerBiome(Biomes.MEADOW, 531L, "meadow", "Meadow", () -> Items.DANDELION, BiomeCategory.OVERWORLD, BiomeCheckGroup.MAJOR);
        registerBiome(Biomes.CHERRY_GROVE, 532L, "cherry_grove", "Cherry Grove", () -> Items.CHERRY_SAPLING, BiomeCategory.OVERWORLD, BiomeCheckGroup.MAJOR);
        registerBiome(Biomes.GROVE, 533L, "grove", "Grove", () -> Items.POWDER_SNOW_BUCKET, BiomeCategory.OVERWORLD, BiomeCheckGroup.SMALL);
        registerBiome(Biomes.RIVER, 534L, "river", "River", () -> Items.WATER_BUCKET, BiomeCategory.OVERWORLD, BiomeCheckGroup.SMALL);
        registerBiome(Biomes.FROZEN_RIVER, 535L, "frozen_river", "Frozen River", () -> Items.ICE, BiomeCategory.OVERWORLD, BiomeCheckGroup.SMALL);
        registerBiome(Biomes.BEACH, 536L, "beach", "Beach", () -> Items.SANDSTONE, BiomeCategory.OVERWORLD, BiomeCheckGroup.SMALL);
        registerBiome(Biomes.SNOWY_BEACH, 537L, "snowy_beach", "Snowy Beach", () -> Items.SNOW, BiomeCategory.OVERWORLD, BiomeCheckGroup.SMALL);
        registerBiome(Biomes.STONY_SHORE, 538L, "stony_shore", "Stony Shore", () -> Items.STONE, BiomeCategory.OVERWORLD, BiomeCheckGroup.SMALL);
        registerBiome(Biomes.WARM_OCEAN, 539L, "warm_ocean", "Warm Ocean", () -> Items.TUBE_CORAL, BiomeCategory.OVERWORLD, BiomeCheckGroup.MAJOR);
        registerBiome(Biomes.LUKEWARM_OCEAN, 540L, "lukewarm_ocean", "Lukewarm Ocean", () -> Items.SEAGRASS, BiomeCategory.OVERWORLD, BiomeCheckGroup.MAJOR);
        registerBiome(Biomes.DEEP_LUKEWARM_OCEAN, 541L, "deep_lukewarm_ocean", "Deep Lukewarm Ocean", () -> Items.SEA_PICKLE, BiomeCategory.OVERWORLD, BiomeCheckGroup.MAJOR);
        registerBiome(Biomes.OCEAN, 542L, "ocean", "Ocean", () -> Items.KELP, BiomeCategory.OVERWORLD, BiomeCheckGroup.MAJOR);
        registerBiome(Biomes.DEEP_OCEAN, 543L, "deep_ocean", "Deep Ocean", () -> Items.PRISMARINE_SHARD, BiomeCategory.OVERWORLD, BiomeCheckGroup.MAJOR);
        registerBiome(Biomes.COLD_OCEAN, 544L, "cold_ocean", "Cold Ocean", () -> Items.SALMON, BiomeCategory.OVERWORLD, BiomeCheckGroup.MAJOR);
        registerBiome(Biomes.DEEP_COLD_OCEAN, 545L, "deep_cold_ocean", "Deep Cold Ocean", () -> Items.COOKED_SALMON, BiomeCategory.OVERWORLD, BiomeCheckGroup.MAJOR);
        registerBiome(Biomes.FROZEN_OCEAN, 546L, "frozen_ocean", "Frozen Ocean", () -> Items.ICE, BiomeCategory.OVERWORLD, BiomeCheckGroup.MAJOR);
        registerBiome(Biomes.DEEP_FROZEN_OCEAN, 547L, "deep_frozen_ocean", "Deep Frozen Ocean", () -> Items.PACKED_ICE, BiomeCategory.OVERWORLD, BiomeCheckGroup.MAJOR);
        registerBiome(Biomes.MUSHROOM_FIELDS, 548L, "mushroom_fields", "Mushroom Fields", () -> Items.MYCELIUM, BiomeCategory.OVERWORLD, BiomeCheckGroup.SMALL);

        // === 2. CAVES & PEAKS (549..556) ===
        registerBiome(Biomes.DRIPSTONE_CAVES, 549L, "dripstone_caves", "Dripstone Caves", () -> Items.POINTED_DRIPSTONE, BiomeCategory.CAVES_AND_PEAKS, BiomeCheckGroup.SMALL);
        registerBiome(Biomes.LUSH_CAVES, 550L, "lush_caves", "Lush Caves", () -> Items.GLOW_BERRIES, BiomeCategory.CAVES_AND_PEAKS, BiomeCheckGroup.SMALL);
        registerBiome(Biomes.DEEP_DARK, 551L, "deep_dark", "Deep Dark", () -> Items.SCULK_CATALYST, BiomeCategory.CAVES_AND_PEAKS, BiomeCheckGroup.SMALL);
        registerBiome(Biomes.SULFUR_CAVES, 552L, "sulfur_caves", "Sulfur Caves", () -> Items.RAW_GOLD, BiomeCategory.CAVES_AND_PEAKS, BiomeCheckGroup.SMALL);
        registerBiome(Biomes.SNOWY_SLOPES, 553L, "snowy_slopes", "Snowy Slopes", () -> Items.SNOW_BLOCK, BiomeCategory.CAVES_AND_PEAKS, BiomeCheckGroup.SMALL);
        registerBiome(Biomes.FROZEN_PEAKS, 554L, "frozen_peaks", "Frozen Peaks", () -> Items.BLUE_ICE, BiomeCategory.CAVES_AND_PEAKS, BiomeCheckGroup.SMALL);
        registerBiome(Biomes.JAGGED_PEAKS, 555L, "jagged_peaks", "Jagged Peaks", () -> Items.IRON_ORE, BiomeCategory.CAVES_AND_PEAKS, BiomeCheckGroup.SMALL);
        registerBiome(Biomes.STONY_PEAKS, 556L, "stony_peaks", "Stony Peaks", () -> Items.CALCITE, BiomeCategory.CAVES_AND_PEAKS, BiomeCheckGroup.SMALL);

        // === 3. NETHER BIOMES (557..561) ===
        registerBiome(Biomes.NETHER_WASTES, 557L, "nether_wastes", "Nether Wastes", () -> Items.NETHERRACK, BiomeCategory.NETHER, BiomeCheckGroup.MAJOR);
        registerBiome(Biomes.WARPED_FOREST, 558L, "warped_forest", "Warped Forest", () -> Items.WARPED_FUNGUS, BiomeCategory.NETHER, BiomeCheckGroup.MAJOR);
        registerBiome(Biomes.CRIMSON_FOREST, 559L, "crimson_forest", "Crimson Forest", () -> Items.CRIMSON_FUNGUS, BiomeCategory.NETHER, BiomeCheckGroup.MAJOR);
        registerBiome(Biomes.SOUL_SAND_VALLEY, 560L, "soul_sand_valley", "Soul Sand Valley", () -> Items.SOUL_SAND, BiomeCategory.NETHER, BiomeCheckGroup.MAJOR);
        registerBiome(Biomes.BASALT_DELTAS, 561L, "basalt_deltas", "Basalt Deltas", () -> Items.BASALT, BiomeCategory.NETHER, BiomeCheckGroup.MAJOR);

        // === 4. THE END BIOMES (562..566) ===
        registerBiome(Biomes.THE_END, 562L, "the_end", "The End", () -> Items.END_STONE, BiomeCategory.THE_END, BiomeCheckGroup.ENDGAME);
        registerBiome(Biomes.END_HIGHLANDS, 563L, "end_highlands", "End Highlands", () -> Items.CHORUS_FRUIT, BiomeCategory.THE_END, BiomeCheckGroup.ENDGAME);
        registerBiome(Biomes.END_MIDLANDS, 564L, "end_midlands", "End Midlands", () -> Items.CHORUS_FLOWER, BiomeCategory.THE_END, BiomeCheckGroup.ENDGAME);
        registerBiome(Biomes.SMALL_END_ISLANDS, 565L, "small_end_islands", "Small End Islands", () -> Items.ENDER_EYE, BiomeCategory.THE_END, BiomeCheckGroup.ENDGAME);
        registerBiome(Biomes.END_BARRENS, 566L, "end_barrens", "End Barrens", () -> Items.END_STONE_BRICKS, BiomeCategory.THE_END, BiomeCheckGroup.ENDGAME);

        // === 5. STRUCTURES (601..621) ===
        registerStructure("village", List.of(
                BuiltinStructures.VILLAGE_PLAINS,
                BuiltinStructures.VILLAGE_DESERT,
                BuiltinStructures.VILLAGE_SAVANNA,
                BuiltinStructures.VILLAGE_SNOWY,
                BuiltinStructures.VILLAGE_TAIGA
        ), APStructureTags.VILLAGE, 601L, "village", "Village", () -> Items.EMERALD);

        registerStructure("pillager_outpost", List.of(BuiltinStructures.PILLAGER_OUTPOST), APStructureTags.PILLAGER_OUTPOST, 602L, "pillager_outpost", "Pillager Outpost", () -> Items.CROSSBOW);
        registerStructure("mineshaft", List.of(BuiltinStructures.MINESHAFT, BuiltinStructures.MINESHAFT_MESA), APStructureTags.MINESHAFT, 603L, "mineshaft", "Mineshaft", () -> Items.CHEST_MINECART);
        registerStructure("woodland_mansion", List.of(BuiltinStructures.WOODLAND_MANSION), APStructureTags.WOODLAND_MANSION, 604L, "woodland_mansion", "Woodland Mansion", () -> Items.TOTEM_OF_UNDYING);
        registerStructure("jungle_temple", List.of(BuiltinStructures.JUNGLE_TEMPLE), APStructureTags.JUNGLE_TEMPLE, 605L, "jungle_temple", "Jungle Temple", () -> Items.CHISELED_STONE_BRICKS);
        registerStructure("desert_pyramid", List.of(BuiltinStructures.DESERT_PYRAMID), APStructureTags.DESERT_PYRAMID, 606L, "desert_pyramid", "Desert Pyramid", () -> Items.CHISELED_SANDSTONE);
        registerStructure("igloo", List.of(BuiltinStructures.IGLOO), APStructureTags.IGLOO, 607L, "igloo", "Igloo", () -> Items.SNOW_BLOCK);
        registerStructure("shipwreck", List.of(BuiltinStructures.SHIPWRECK, BuiltinStructures.SHIPWRECK_BEACHED), APStructureTags.SHIPWRECK, 608L, "shipwreck", "Shipwreck", () -> Items.OAK_BOAT);
        registerStructure("swamp_hut", List.of(BuiltinStructures.SWAMP_HUT), APStructureTags.SWAMP_HUT, 609L, "swamp_hut", "Swamp Hut", () -> Items.CAULDRON);
        registerStructure("stronghold", List.of(BuiltinStructures.STRONGHOLD), APStructureTags.STRONGHOLD, 610L, "stronghold", "Stronghold", () -> Items.ENDER_EYE);
        registerStructure("ocean_monument", List.of(BuiltinStructures.OCEAN_MONUMENT), APStructureTags.OCEAN_MONUMENT, 611L, "ocean_monument", "Ocean Monument", () -> Items.PRISMARINE_BRICKS);
        registerStructure("ocean_ruin", List.of(BuiltinStructures.OCEAN_RUIN_COLD, BuiltinStructures.OCEAN_RUIN_WARM), APStructureTags.OCEAN_RUIN, 612L, "ocean_ruin", "Ocean Ruin", () -> Items.PRISMARINE);
        registerStructure("nether_fortress", List.of(BuiltinStructures.FORTRESS), APStructureTags.FORTRESS, 613L, "nether_fortress", "Nether Fortress", () -> Items.NETHER_BRICKS);
        registerStructure("nether_fossil", List.of(BuiltinStructures.NETHER_FOSSIL), APStructureTags.NETHER_FOSSIL, 614L, "nether_fossil", "Nether Fossil", () -> Items.BONE_BLOCK);
        registerStructure("end_city", List.of(BuiltinStructures.END_CITY), APStructureTags.END_CITY, 615L, "end_city", "End City", () -> Items.PURPUR_BLOCK);
        registerStructure("buried_treasure", List.of(BuiltinStructures.BURIED_TREASURE), APStructureTags.BURIED_TREASURE, 616L, "buried_treasure", "Buried Treasure", () -> Items.HEART_OF_THE_SEA);
        registerStructure("bastion_remnant", List.of(BuiltinStructures.BASTION_REMNANT), APStructureTags.BASTION_REMNANT, 617L, "bastion_remnant", "Bastion Remnant", () -> Items.GILDED_BLACKSTONE);
        registerStructure("ruined_portal", List.of(
                BuiltinStructures.RUINED_PORTAL_STANDARD,
                BuiltinStructures.RUINED_PORTAL_DESERT,
                BuiltinStructures.RUINED_PORTAL_JUNGLE,
                BuiltinStructures.RUINED_PORTAL_SWAMP,
                BuiltinStructures.RUINED_PORTAL_MOUNTAIN,
                BuiltinStructures.RUINED_PORTAL_OCEAN,
                BuiltinStructures.RUINED_PORTAL_NETHER
        ), APStructureTags.RUINED_PORTAL, 618L, "ruined_portal", "Ruined Portal", () -> Items.CRYING_OBSIDIAN);
        registerStructure("ancient_city", List.of(BuiltinStructures.ANCIENT_CITY), APStructureTags.ANCIENT_CITY, 619L, "ancient_city", "Ancient City", () -> Items.ECHO_SHARD);
        registerStructure("trail_ruins", List.of(BuiltinStructures.TRAIL_RUINS), APStructureTags.TRAIL_RUINS, 620L, "trail_ruins", "Trail Ruins", () -> Items.BRUSH);
        registerStructure("trial_chambers", List.of(BuiltinStructures.TRIAL_CHAMBERS), APStructureTags.TRIAL_CHAMBERS, 621L, "trial_chambers", "Trial Chambers", () -> Items.TRIAL_KEY);
        registerStructure("abandoned_camp", List.of(
                BuiltinStructures.ABANDONED_CAMP_BAMBOO_JUNGLE,
                BuiltinStructures.ABANDONED_CAMP_BIRCH_FOREST,
                BuiltinStructures.ABANDONED_CAMP_CHERRY_GROVE,
                BuiltinStructures.ABANDONED_CAMP_DAPPLED_FOREST,
                BuiltinStructures.ABANDONED_CAMP_FLOWER_FOREST,
                BuiltinStructures.ABANDONED_CAMP_FOREST,
                BuiltinStructures.ABANDONED_CAMP_MEADOW,
                BuiltinStructures.ABANDONED_CAMP_OLD_GROWTH_BIRCH_FOREST,
                BuiltinStructures.ABANDONED_CAMP_OLD_GROWTH_PINE_TAIGA,
                BuiltinStructures.ABANDONED_CAMP_OLD_GROWTH_SPRUCE_TAIGA,
                BuiltinStructures.ABANDONED_CAMP_PALE_GARDEN,
                BuiltinStructures.ABANDONED_CAMP_SAVANNA,
                BuiltinStructures.ABANDONED_CAMP_SNOWY_TAIGA,
                BuiltinStructures.ABANDONED_CAMP_SPARSE_JUNGLE,
                BuiltinStructures.ABANDONED_CAMP_SWAMP,
                BuiltinStructures.ABANDONED_CAMP_TAIGA,
                BuiltinStructures.ABANDONED_CAMP_WINDSWEPT_FOREST,
                BuiltinStructures.ABANDONED_CAMP_WOODED_BADLANDS
        ), APStructureTags.ABANDONED_CAMP, 622L, "abandoned_camp", "Abandoned Camp", () -> Items.CAMPFIRE);
    }

    public static boolean isKnownBiome(String id) {
        return BIOME_BY_ID.containsKey(id);
    }

    public static BiomeEntry getBiomeById(String id) {
        return BIOME_BY_ID.get(id);
    }

    public static BiomeEntry getBiomeByLocationId(long id) {
        return BIOME_BY_LOC_ID.get(id);
    }

    public static boolean isKnownStructure(String id) {
        return STRUCTURE_BY_ID.containsKey(id);
    }

    public static StructureCheckEntry getStructureById(String id) {
        return STRUCTURE_BY_ID.get(id);
    }

    public static StructureCheckEntry getStructureByLocationId(long id) {
        return STRUCTURE_BY_LOC_ID.get(id);
    }
}
