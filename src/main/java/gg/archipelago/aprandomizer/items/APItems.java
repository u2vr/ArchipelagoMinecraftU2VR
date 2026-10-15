package gg.archipelago.aprandomizer.items;

import gg.archipelago.aprandomizer.APRandomizer;
import gg.archipelago.aprandomizer.APRegistries;
import gg.archipelago.aprandomizer.exploration.ExplorationData;
import gg.archipelago.aprandomizer.items.compass.ConstantBiomeTarget;
import gg.archipelago.aprandomizer.items.compass.StructureTarget;
import gg.archipelago.aprandomizer.items.compass.UnvisitedBiomeTarget;
import gg.archipelago.aprandomizer.items.compass.UnvisitedStructureTarget;
import gg.archipelago.aprandomizer.items.traps.MobTrap;
import gg.archipelago.aprandomizer.modifiers.APStructureModifiers;
import gg.archipelago.aprandomizer.structures.level.ConstantLevel;
import gg.archipelago.aprandomizer.structures.level.CurrentLevel;
import gg.archipelago.aprandomizer.structures.level.RandomizedStructureLevel;
import gg.archipelago.aprandomizer.tags.APStructureTags;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;

import java.util.List;
import java.util.Optional;

public class APItems {
    // Group Recipes
    public static final ResourceKey<APItem> GROUP_RECIPES_ARCHERY = id("group_recipes/archery");
    public static final ResourceKey<APItem> GROUP_RECIPES_BREWING = id("group_recipes/brewing");
    public static final ResourceKey<APItem> GROUP_RECIPES_ENCHANTING = id("group_recipes/enchanting");
    public static final ResourceKey<APItem> GROUP_RECIPES_BUCKET = id("group_recipes/bucket");
    public static final ResourceKey<APItem> GROUP_RECIPES_FLINT_AND_STEEL = id("group_recipes/flint_and_steel");
    public static final ResourceKey<APItem> GROUP_RECIPES_BEDS = id("group_recipes/beds");
    public static final ResourceKey<APItem> GROUP_RECIPES_BOTTLES = id("group_recipes/bottles");
    public static final ResourceKey<APItem> GROUP_RECIPES_SHIELD = id("group_recipes/shield");
    public static final ResourceKey<APItem> GROUP_RECIPES_FISHING = id("group_recipes/fishing");
    public static final ResourceKey<APItem> GROUP_RECIPES_CAMPFIRES = id("group_recipes/campfires");
    public static final ResourceKey<APItem> GROUP_RECIPES_SADDLE = id("group_recipes/saddle");
    public static final ResourceKey<APItem> GROUP_RECIPES_SPYGLASS = id("group_recipes/spyglass");
    public static final ResourceKey<APItem> GROUP_RECIPES_LEAD = id("group_recipes/lead");
    public static final ResourceKey<APItem> GROUP_RECIPES_BRUSH = id("group_recipes/brush");

    // Progressive Recipes
    public static final ResourceKey<APItem> PROGRESSIVE_RECIPES_WEAPONS = id("progressive_recipes/weapons");
    public static final ResourceKey<APItem> PROGRESSIVE_RECIPES_TOOLS = id("progressive_recipes/tools");
    public static final ResourceKey<APItem> PROGRESSIVE_RECIPES_ARMOR = id("progressive_recipes/armor");
    public static final ResourceKey<APItem> PROGRESSIVE_RECIPES_RESOURCE_CRAFTING = id("progressive_recipes/resource_crafting");

    // ItemStacks
    public static final ResourceKey<APItem> ITEMSTACK_NETHERITE_SCRAP = id("itemstack/netherite_scrap");
    public static final ResourceKey<APItem> ITEMSTACK_EIGHT_EMERALD = id("itemstack/eight_emerald");
    public static final ResourceKey<APItem> ITEMSTACK_FOUR_EMERALD = id("itemstack/four_emerald");
    public static final ResourceKey<APItem> ITEMSTACK_ENCHANTMENT_CHANNELING_ONE = id("itemstack/enchantment/channeling_one");
    public static final ResourceKey<APItem> ITEMSTACK_ENCHANTMENT_SILK_TOUCH_ONE = id("itemstack/enchantment/silk_touch_one");
    public static final ResourceKey<APItem> ITEMSTACK_ENCHANTMENT_SHARPNESS_THREE = id("itemstack/enchantment/sharpness_three");
    public static final ResourceKey<APItem> ITEMSTACK_ENCHANTMENT_PIERCING_FOUR = id("itemstack/enchantment/piercing_four");
    public static final ResourceKey<APItem> ITEMSTACK_ENCHANTMENT_MOB_LOOTING_THREE = id("itemstack/enchantment/mob_looting_three");
    public static final ResourceKey<APItem> ITEMSTACK_ENCHANTMENT_INFINITY_ARROWS_ONE = id("itemstack/enchantment/infinity_arrows_one");
    public static final ResourceKey<APItem> ITEMSTACK_DIAMOND_ORE = id("itemstack/diamond_ore");
    public static final ResourceKey<APItem> ITEMSTACK_IRON_ORE = id("itemstack/iron_ore");
    public static final ResourceKey<APItem> ITEMSTACK_ENDER_PEARL = id("itemstack/ender_pearl");
    public static final ResourceKey<APItem> ITEMSTACK_LAPIS_LAZULI = id("itemstack/lapis_lazuli");
    public static final ResourceKey<APItem> ITEMSTACK_COOKED_PORKCHOP = id("itemstack/cooked_porkchop");
    public static final ResourceKey<APItem> ITEMSTACK_GOLD_ORE = id("itemstack/gold_ore");
    public static final ResourceKey<APItem> ITEMSTACK_ROTTEN_FLESH = id("itemstack/rotten_flesh");
    public static final ResourceKey<APItem> ITEMSTACK_THE_ARROW = id("itemstack/the_arrow");
    public static final ResourceKey<APItem> ITEMSTACK_THIRTY_TWO_ARROW = id("itemstack/thirty_two_arrow");
    public static final ResourceKey<APItem> ITEMSTACK_SHULKER_BOX = id("itemstack/shulker_box");

    // Experience
    public static final ResourceKey<APItem> EXPERIENCE_FIVE_HUNDRED = id("experience/five_hundred");
    public static final ResourceKey<APItem> EXPERIENCE_ONE_HUNDRED = id("experience/one_hundred");
    public static final ResourceKey<APItem> EXPERIENCE_FIFTY = id("experience/fifty");

    // Compasses
    public static final ResourceKey<APItem> COMPASS_VILLAGE = id("compass/village");
    public static final ResourceKey<APItem> COMPASS_PILLAGER_OUTPOST = id("compass/pillager_outpost");
    public static final ResourceKey<APItem> COMPASS_FORTRESS = id("compass/fortress");
    public static final ResourceKey<APItem> COMPASS_BASTION_REMNANT = id("compass/bastion_remnant");
    public static final ResourceKey<APItem> COMPASS_END_CITY = id("compass/end_city");
    public static final ResourceKey<APItem> COMPASS_OCEAN_MONUMENT = id("compass/ocean_monument");
    public static final ResourceKey<APItem> COMPASS_WOODLAND_MANSION = id("compass/woodland_mansion");
    public static final ResourceKey<APItem> COMPASS_ANCIENT_CITY = id("compass/ancient_city");
    public static final ResourceKey<APItem> COMPASS_TRAIL_RUINS = id("compass/trail_ruins");
    public static final ResourceKey<APItem> COMPASS_TRIAL_CHAMBERS = id("compass/trial_chambers");
    public static final ResourceKey<APItem> COMPASS_UNVISITED_BIOMES = id("compass/unvisited_biomes");

    public static ResourceKey<APItem> biomeCompassKey(String path) {
        return id("compass/biome/" + path);
    }

    public static final ResourceKey<APItem> COMPASS_SULFUR_CAVES = biomeCompassKey("sulfur_caves");

    // New Structure Compasses
    public static final ResourceKey<APItem> COMPASS_STRONGHOLD = id("compass/stronghold");
    public static final ResourceKey<APItem> COMPASS_MINESHAFT = id("compass/mineshaft");
    public static final ResourceKey<APItem> COMPASS_DESERT_PYRAMID = id("compass/desert_pyramid");
    public static final ResourceKey<APItem> COMPASS_JUNGLE_TEMPLE = id("compass/jungle_temple");
    public static final ResourceKey<APItem> COMPASS_SWAMP_HUT = id("compass/swamp_hut");
    public static final ResourceKey<APItem> COMPASS_IGLOO = id("compass/igloo");
    public static final ResourceKey<APItem> COMPASS_SHIPWRECK = id("compass/shipwreck");
    public static final ResourceKey<APItem> COMPASS_OCEAN_RUIN = id("compass/ocean_ruin");
    public static final ResourceKey<APItem> COMPASS_BURIED_TREASURE = id("compass/buried_treasure");
    public static final ResourceKey<APItem> COMPASS_NETHER_FOSSIL = id("compass/nether_fossil");
    public static final ResourceKey<APItem> COMPASS_RUINED_PORTAL = id("compass/ruined_portal");
    public static final ResourceKey<APItem> COMPASS_ABANDONED_CAMP = id("compass/abandoned_camp");
    public static final ResourceKey<APItem> COMPASS_UNVISITED_STRUCTURES = id("compass/unvisited_structures");

    public static String getBiomeDisplayCategory(ExplorationData.BiomeEntry entry) {
        if (entry.category() == ExplorationData.BiomeCategory.NETHER) {
            return "Nether Biomes";
        }
        if (entry.category() == ExplorationData.BiomeCategory.THE_END) {
            return "End Biomes";
        }
        ResourceKey<Biome> key = entry.key();
        if (key.equals(Biomes.DRIPSTONE_CAVES) || key.equals(Biomes.LUSH_CAVES)
                || key.equals(Biomes.DEEP_DARK) || key.equals(Biomes.SULFUR_CAVES)) {
            return "Caves";
        }
        if (key.equals(Biomes.SNOWY_SLOPES) || key.equals(Biomes.FROZEN_PEAKS)
                || key.equals(Biomes.JAGGED_PEAKS) || key.equals(Biomes.STONY_PEAKS)) {
            return "Mountain Peaks";
        }
        if (key.equals(Biomes.DESERT) || key.equals(Biomes.BADLANDS) || key.equals(Biomes.ERODED_BADLANDS)
                || key.equals(Biomes.WOODED_BADLANDS) || key.equals(Biomes.SAVANNA) || key.equals(Biomes.SAVANNA_PLATEAU)
                || key.equals(Biomes.WINDSWEPT_SAVANNA) || key.equals(Biomes.JUNGLE) || key.equals(Biomes.SPARSE_JUNGLE)
                || key.equals(Biomes.BAMBOO_JUNGLE) || key.equals(Biomes.WARM_OCEAN) || key.equals(Biomes.MANGROVE_SWAMP)) {
            return "Warm Biomes";
        }
        if (key.equals(Biomes.SNOWY_PLAINS) || key.equals(Biomes.ICE_SPIKES) || key.equals(Biomes.SNOWY_TAIGA)
                || key.equals(Biomes.SNOWY_BEACH) || key.equals(Biomes.FROZEN_RIVER) || key.equals(Biomes.COLD_OCEAN)
                || key.equals(Biomes.DEEP_COLD_OCEAN) || key.equals(Biomes.FROZEN_OCEAN) || key.equals(Biomes.DEEP_FROZEN_OCEAN)
                || key.equals(Biomes.GROVE)) {
            return "Cold Biomes";
        }
        return "Temperate Biomes";
    }

    // Traps
    public static final ResourceKey<APItem> TRAP_BEES = id("trap/bees");

    public static final ResourceKey<APItem> DRAGON_EGG_SHARD = id("dragon_egg_shard");

    // Custom Archipelago Upgrade and Modifier Items
    public static final ResourceKey<APItem> RECIPE_UNLOCK = id("upgrade/recipe_unlock");
    public static final ResourceKey<APItem> HP_UPGRADE = id("upgrade/hp");
    public static final ResourceKey<APItem> HUNGER_UPGRADE = id("upgrade/hunger");
    public static final ResourceKey<APItem> REACH_UPGRADE = id("upgrade/reach");
    public static final ResourceKey<APItem> WORLD_BORDER_UPGRADE = id("upgrade/world_border");
    public static final ResourceKey<APItem> STRUCTURE_UNLOCK = id("upgrade/structure_unlock");
    public static final ResourceKey<APItem> OFFHAND_UNLOCK = id("upgrade/offhand");
    public static final ResourceKey<APItem> INVENTORY_SLOT_UPGRADE = id("upgrade/inventory_slot");
    public static final ResourceKey<APItem> TRAP_ITEM = id("trap/random");
    public static final ResourceKey<APItem> MONSTER_SUN_BURNING_DISABLED = id("modifier/monster_sun_burning");
    public static final ResourceKey<APItem> MONSTER_SPAWN_LIGHT_DISABLED = id("modifier/monster_spawn_light");

    // Extra Abilities
    public static final ResourceKey<APItem> PROGRESSIVE_MINING = id("ability/progressive_mining");
    public static final ResourceKey<APItem> TREE_FELLER = id("ability/tree_feller");
    public static final ResourceKey<APItem> PROGRESSIVE_JUMP = id("ability/progressive_jump");
    public static final ResourceKey<APItem> ITEM_MAGNET = id("ability/item_magnet");
    public static final ResourceKey<APItem> STEP_ASSIST = id("ability/step_assist");
    public static final ResourceKey<APItem> TRASH_CAN = id("ability/trash_can");
    public static final ResourceKey<APItem> LIGHT_HOLDER = id("ability/light_holder");

    private static ResourceKey<APItem> id(String name) {
        return ResourceKey.create(APRegistries.ARCHIPELAGO_ITEM, Identifier.fromNamespaceAndPath(APRandomizer.MODID, name));
    }

    public static void bootstrap(BootstrapContext<APItem> context) {
        HolderGetter<Enchantment> enchantments = context.lookup(Registries.ENCHANTMENT);
        HolderGetter<Biome> biomes = context.lookup(Registries.BIOME);

        context.register(GROUP_RECIPES_ARCHERY,
                APItem.ofRewards(List.of(
                        new RecipeReward(RecipeBuilder.getDefaultRecipeId(new ItemStackTemplate(Items.BOW))),
                        new RecipeReward(RecipeBuilder.getDefaultRecipeId(new ItemStackTemplate(Items.ARROW))),
                        new RecipeReward(RecipeBuilder.getDefaultRecipeId(new ItemStackTemplate(Items.CROSSBOW))))));

        context.register(GROUP_RECIPES_BREWING,
                APItem.ofRewards(List.of(
                        new RecipeReward(RecipeBuilder.getDefaultRecipeId(new ItemStackTemplate(Items.BLAZE_POWDER))),
                        new RecipeReward(RecipeBuilder.getDefaultRecipeId(new ItemStackTemplate(Items.BREWING_STAND))))));

        context.register(GROUP_RECIPES_ENCHANTING,
                APItem.ofRewards(List.of(
                        new RecipeReward(RecipeBuilder.getDefaultRecipeId(new ItemStackTemplate(Items.ENCHANTING_TABLE))),
                        new RecipeReward(RecipeBuilder.getDefaultRecipeId(new ItemStackTemplate(Items.BOOKSHELF))),
                        new ItemReward(new ItemStackTemplate(Items.LAPIS_LAZULI, 4)))));

        context.register(GROUP_RECIPES_BUCKET,
                APItem.ofRewards(List.of(
                        new RecipeReward(RecipeBuilder.getDefaultRecipeId(new ItemStackTemplate(Items.BUCKET))))));

        context.register(GROUP_RECIPES_FLINT_AND_STEEL,
                APItem.ofRewards(List.of(
                        new RecipeReward(RecipeBuilder.getDefaultRecipeId(new ItemStackTemplate(Items.FLINT_AND_STEEL))))));

        context.register(GROUP_RECIPES_BEDS,
                APItem.ofRewards(Items.BED
                        .map(ItemStackTemplate::new)
                        .map(RecipeBuilder::getDefaultRecipeId)
                        .<APReward>map(RecipeReward::new)
                        .asList()));

        context.register(GROUP_RECIPES_BOTTLES,
                APItem.ofRewards(List.of(
                        new RecipeReward(RecipeBuilder.getDefaultRecipeId(new ItemStackTemplate(Items.GLASS_BOTTLE))))));

        context.register(GROUP_RECIPES_SHIELD,
                APItem.ofRewards(List.of(
                        new RecipeReward(RecipeBuilder.getDefaultRecipeId(new ItemStackTemplate(Items.SHIELD))))));

        context.register(GROUP_RECIPES_FISHING,
                APItem.ofRewards(List.of(
                        new RecipeReward(RecipeBuilder.getDefaultRecipeId(new ItemStackTemplate(Items.FISHING_ROD))),
                        new RecipeReward(RecipeBuilder.getDefaultRecipeId(new ItemStackTemplate(Items.CARROT_ON_A_STICK))),
                        new RecipeReward(RecipeBuilder.getDefaultRecipeId(new ItemStackTemplate(Items.WARPED_FUNGUS_ON_A_STICK))))));

        context.register(GROUP_RECIPES_CAMPFIRES,
                APItem.ofRewards(List.of(
                        new RecipeReward(RecipeBuilder.getDefaultRecipeId(new ItemStackTemplate(Items.CAMPFIRE))),
                        new RecipeReward(RecipeBuilder.getDefaultRecipeId(new ItemStackTemplate(Items.SOUL_CAMPFIRE))))));

        context.register(GROUP_RECIPES_SPYGLASS,
                APItem.ofRewards(List.of(
                        new RecipeReward(RecipeBuilder.getDefaultRecipeId(new ItemStackTemplate(Items.SPYGLASS))),
                        new ItemReward(new ItemStackTemplate(Items.AMETHYST_SHARD, 1)))));

        context.register(GROUP_RECIPES_LEAD,
                APItem.ofRewards(List.of(
                        new RecipeReward(RecipeBuilder.getDefaultRecipeId(new ItemStackTemplate(Items.LEAD))))));

        context.register(GROUP_RECIPES_BRUSH,
                APItem.ofRewards(List.of(
                        new RecipeReward(RecipeBuilder.getDefaultRecipeId(new ItemStackTemplate(Items.BRUSH))))));

        context.register(GROUP_RECIPES_SADDLE,
                APItem.ofRewards(List.of(
                        new RecipeReward(RecipeBuilder.getDefaultRecipeId(new ItemStackTemplate(Items.SADDLE))))));

        context.register(PROGRESSIVE_RECIPES_WEAPONS,
                APItem.ofTiers(List.of(
                        new APTier(List.of(
                                new RecipeReward(RecipeBuilder.getDefaultRecipeId(new ItemStackTemplate(Items.STONE_SWORD))),
                                new RecipeReward(RecipeBuilder.getDefaultRecipeId(new ItemStackTemplate(Items.STONE_AXE))),
                                new RecipeReward(RecipeBuilder.getDefaultRecipeId(new ItemStackTemplate(Items.STONE_SPEAR))),
                                new RecipeReward(RecipeBuilder.getDefaultRecipeId(new ItemStackTemplate(Items.COPPER_SWORD))),
                                new RecipeReward(RecipeBuilder.getDefaultRecipeId(new ItemStackTemplate(Items.COPPER_AXE))),
                                new RecipeReward(RecipeBuilder.getDefaultRecipeId(new ItemStackTemplate(Items.COPPER_SPEAR))))),
                        new APTier(List.of(
                                new RecipeReward(RecipeBuilder.getDefaultRecipeId(new ItemStackTemplate(Items.IRON_SWORD))),
                                new RecipeReward(RecipeBuilder.getDefaultRecipeId(new ItemStackTemplate(Items.IRON_AXE))),
                                new RecipeReward(RecipeBuilder.getDefaultRecipeId(new ItemStackTemplate(Items.IRON_SPEAR))))),
                        new APTier(List.of(
                                new RecipeReward(RecipeBuilder.getDefaultRecipeId(new ItemStackTemplate(Items.DIAMOND_SWORD))),
                                new RecipeReward(RecipeBuilder.getDefaultRecipeId(new ItemStackTemplate(Items.DIAMOND_AXE))),
                                new RecipeReward(RecipeBuilder.getDefaultRecipeId(new ItemStackTemplate(Items.DIAMOND_SPEAR))))))));

        context.register(PROGRESSIVE_RECIPES_TOOLS,
                APItem.ofTiers(List.of(
                        new APTier(List.of(
                                new RecipeReward(RecipeBuilder.getDefaultRecipeId(new ItemStackTemplate(Items.STONE_PICKAXE))),
                                new RecipeReward(RecipeBuilder.getDefaultRecipeId(new ItemStackTemplate(Items.STONE_SHOVEL))),
                                new RecipeReward(RecipeBuilder.getDefaultRecipeId(new ItemStackTemplate(Items.STONE_HOE))),
                                new RecipeReward(RecipeBuilder.getDefaultRecipeId(new ItemStackTemplate(Items.COPPER_PICKAXE))),
                                new RecipeReward(RecipeBuilder.getDefaultRecipeId(new ItemStackTemplate(Items.COPPER_SHOVEL))),
                                new RecipeReward(RecipeBuilder.getDefaultRecipeId(new ItemStackTemplate(Items.COPPER_HOE))))),
                        new APTier(List.of(
                                new RecipeReward(RecipeBuilder.getDefaultRecipeId(new ItemStackTemplate(Items.IRON_PICKAXE))),
                                new RecipeReward(RecipeBuilder.getDefaultRecipeId(new ItemStackTemplate(Items.IRON_SHOVEL))),
                                new RecipeReward(RecipeBuilder.getDefaultRecipeId(new ItemStackTemplate(Items.IRON_HOE))))),
                        new APTier(List.of(
                                new RecipeReward(RecipeBuilder.getDefaultRecipeId(new ItemStackTemplate(Items.DIAMOND_PICKAXE))),
                                new RecipeReward(RecipeBuilder.getDefaultRecipeId(new ItemStackTemplate(Items.DIAMOND_SHOVEL))),
                                new RecipeReward(RecipeBuilder.getDefaultRecipeId(new ItemStackTemplate(Items.DIAMOND_HOE))),
                                new RecipeReward(RecipeBuilder.getDefaultRecipeId(new ItemStackTemplate(Items.NETHERITE_INGOT))))))));

        context.register(PROGRESSIVE_RECIPES_ARMOR,
                APItem.ofTiers(List.of(
                        /*new APTier(List.of(
                                new RecipeReward(RecipeBuilder.getDefaultRecipeId(new ItemStackTemplate(Items.COPPER_HELMET))),
                                new RecipeReward(RecipeBuilder.getDefaultRecipeId(new ItemStackTemplate(Items.COPPER_CHESTPLATE))),
                                new RecipeReward(RecipeBuilder.getDefaultRecipeId(new ItemStackTemplate(Items.COPPER_LEGGINGS))),
                                new RecipeReward(RecipeBuilder.getDefaultRecipeId(new ItemStackTemplate(Items.COPPER_BOOTS))),
                                new RecipeReward(RecipeBuilder.getDefaultRecipeId(new ItemStackTemplate(Items.GOLD_HELMET))),
                                new RecipeReward(RecipeBuilder.getDefaultRecipeId(new ItemStackTemplate(Items.GOLD_CHESTPLATE)))
                                new RecipeReward(RecipeBuilder.getDefaultRecipeId(new ItemStackTemplate(Items.GOLD_LEGGINGS)))
                                new RecipeReward(RecipeBuilder.getDefaultRecipeId(new ItemStackTemplate(Items.GOLD_BOOTS))))),*/
                        new APTier(List.of(
                                new RecipeReward(RecipeBuilder.getDefaultRecipeId(new ItemStackTemplate(Items.IRON_HELMET))),
                                new RecipeReward(RecipeBuilder.getDefaultRecipeId(new ItemStackTemplate(Items.IRON_CHESTPLATE))),
                                new RecipeReward(RecipeBuilder.getDefaultRecipeId(new ItemStackTemplate(Items.IRON_LEGGINGS))),
                                new RecipeReward(RecipeBuilder.getDefaultRecipeId(new ItemStackTemplate(Items.IRON_BOOTS))))),
                        new APTier(List.of(
                                new RecipeReward(RecipeBuilder.getDefaultRecipeId(new ItemStackTemplate(Items.DIAMOND_HELMET))),
                                new RecipeReward(RecipeBuilder.getDefaultRecipeId(new ItemStackTemplate(Items.DIAMOND_CHESTPLATE))),
                                new RecipeReward(RecipeBuilder.getDefaultRecipeId(new ItemStackTemplate(Items.DIAMOND_LEGGINGS))),
                                new RecipeReward(RecipeBuilder.getDefaultRecipeId(new ItemStackTemplate(Items.DIAMOND_BOOTS))))))));

        context.register(PROGRESSIVE_RECIPES_RESOURCE_CRAFTING,
                APItem.ofTiers(List.of(
                        new APTier(List.of(
                                new RecipeReward(Identifier.withDefaultNamespace("copper_ingot_from_nuggets")),
                                new RecipeReward(RecipeBuilder.getDefaultRecipeId(new ItemStackTemplate(Items.COPPER_NUGGET))),
                                new RecipeReward(Identifier.withDefaultNamespace("iron_ingot_from_nuggets")),
                                new RecipeReward(RecipeBuilder.getDefaultRecipeId(new ItemStackTemplate(Items.IRON_NUGGET))),
                                new RecipeReward(Identifier.withDefaultNamespace("gold_ingot_from_nuggets")),
                                new RecipeReward(RecipeBuilder.getDefaultRecipeId(new ItemStackTemplate(Items.GOLD_NUGGET))),
                                new RecipeReward(RecipeBuilder.getDefaultRecipeId(new ItemStackTemplate(Items.FURNACE))),
                                new RecipeReward(RecipeBuilder.getDefaultRecipeId(new ItemStackTemplate(Items.BLAST_FURNACE))))),
                        new APTier(List.of(
                                new RecipeReward(RecipeBuilder.getDefaultRecipeId(new ItemStackTemplate(Items.REDSTONE))),
                                new RecipeReward(RecipeBuilder.getDefaultRecipeId(new ItemStackTemplate(Items.REDSTONE_BLOCK))),
                                new RecipeReward(RecipeBuilder.getDefaultRecipeId(new ItemStackTemplate(Items.GLOWSTONE))),
                                new RecipeReward(Identifier.withDefaultNamespace("copper_ingot_from_copper_block")),
                                new RecipeReward(RecipeBuilder.getDefaultRecipeId(new ItemStackTemplate(Items.COPPER_BLOCK.weathering().unaffected()))),
                                new RecipeReward(Identifier.withDefaultNamespace("iron_ingot_from_iron_block")),
                                new RecipeReward(RecipeBuilder.getDefaultRecipeId(new ItemStackTemplate(Items.IRON_BLOCK))),
                                new RecipeReward(Identifier.withDefaultNamespace("gold_ingot_from_gold_block")),
                                new RecipeReward(RecipeBuilder.getDefaultRecipeId(new ItemStackTemplate(Items.GOLD_BLOCK))),
                                new RecipeReward(RecipeBuilder.getDefaultRecipeId(new ItemStackTemplate(Items.DIAMOND))),
                                new RecipeReward(RecipeBuilder.getDefaultRecipeId(new ItemStackTemplate(Items.DIAMOND_BLOCK))),
                                new RecipeReward(RecipeBuilder.getDefaultRecipeId(new ItemStackTemplate(Items.NETHERITE_BLOCK))),
                                new RecipeReward(Identifier.withDefaultNamespace("netherite_ingot_from_netherite_block")),
                                new RecipeReward(RecipeBuilder.getDefaultRecipeId(new ItemStackTemplate(Items.ANVIL))),
                                new RecipeReward(RecipeBuilder.getDefaultRecipeId(new ItemStackTemplate(Items.RESIN_CLUMP))),
                                new RecipeReward(RecipeBuilder.getDefaultRecipeId(new ItemStackTemplate(Items.RESIN_BLOCK))),
                                new RecipeReward(RecipeBuilder.getDefaultRecipeId(new ItemStackTemplate(Items.EMERALD))),
                                new RecipeReward(RecipeBuilder.getDefaultRecipeId(new ItemStackTemplate(Items.EMERALD_BLOCK))))))));

        context.register(ITEMSTACK_NETHERITE_SCRAP,
                APItem.ofReward(
                        new ItemReward(new ItemStackTemplate(Items.NETHERITE_SCRAP, 8))));

        context.register(ITEMSTACK_EIGHT_EMERALD,
                APItem.ofReward(
                        new ItemReward(new ItemStackTemplate(Items.EMERALD, 8))));

        context.register(ITEMSTACK_FOUR_EMERALD,
                APItem.ofReward(
                        new ItemReward(new ItemStackTemplate(Items.EMERALD, 4))));

        context.register(ITEMSTACK_ENCHANTMENT_CHANNELING_ONE,
                APItem.ofReward(new ItemReward(enchantment(enchantments.getOrThrow(Enchantments.CHANNELING), 1))));

       context.register(ITEMSTACK_ENCHANTMENT_SILK_TOUCH_ONE,
                APItem.ofReward(
                        new ItemReward(enchantment(enchantments.getOrThrow(Enchantments.SILK_TOUCH), 1))));

        context.register(ITEMSTACK_ENCHANTMENT_SHARPNESS_THREE,
                APItem.ofReward(
                        new ItemReward(enchantment(enchantments.getOrThrow(Enchantments.SHARPNESS), 3))));

        context.register(ITEMSTACK_ENCHANTMENT_PIERCING_FOUR,
                APItem.ofReward(
                        new ItemReward(enchantment(enchantments.getOrThrow(Enchantments.PIERCING), 4))));

        context.register(ITEMSTACK_ENCHANTMENT_MOB_LOOTING_THREE,
                APItem.ofReward(
                        new ItemReward(enchantment(enchantments.getOrThrow(Enchantments.LOOTING), 3))));

        context.register(ITEMSTACK_ENCHANTMENT_INFINITY_ARROWS_ONE,
                APItem.ofReward(
                        new ItemReward(enchantment(enchantments.getOrThrow(Enchantments.INFINITY), 1))));

        context.register(ITEMSTACK_DIAMOND_ORE,
                APItem.ofReward(
                        new ItemReward(new ItemStackTemplate(Items.DIAMOND_ORE, 4))));

        context.register(ITEMSTACK_IRON_ORE,
                APItem.ofReward(
                        new ItemReward(new ItemStackTemplate(Items.IRON_ORE, 16))));

        context.register(ITEMSTACK_ENDER_PEARL,
                APItem.ofReward(
                        new ItemReward(new ItemStackTemplate(Items.ENDER_PEARL, 3))));

        context.register(ITEMSTACK_LAPIS_LAZULI,
                APItem.ofReward(
                        new ItemReward(new ItemStackTemplate(Items.LAPIS_LAZULI, 4))));

        context.register(ITEMSTACK_COOKED_PORKCHOP,
                APItem.ofReward(
                        new ItemReward(new ItemStackTemplate(Items.COOKED_PORKCHOP, 16))));

        context.register(ITEMSTACK_GOLD_ORE,
                APItem.ofReward(
                        new ItemReward(new ItemStackTemplate(Items.GOLD_ORE, 8))));

        context.register(ITEMSTACK_ROTTEN_FLESH,
                APItem.ofReward(
                        new ItemReward(new ItemStackTemplate(Items.ROTTEN_FLESH, 8))));

        context.register(ITEMSTACK_THE_ARROW,
                APItem.ofReward(
                        new ItemReward(new ItemStackTemplate(BuiltInRegistries.ITEM.wrapAsHolder(Items.ARROW), 1,
                                DataComponentPatch.builder()
                                        .set(DataComponents.ITEM_NAME, Component.literal("The Arrow"))
                                        .build()))));

        context.register(ITEMSTACK_THIRTY_TWO_ARROW,
                APItem.ofReward(
                        new ItemReward(new ItemStackTemplate(Items.ARROW, 32))));

        context.register(ITEMSTACK_SHULKER_BOX,
                APItem.ofReward(
                        new ItemReward(new ItemStackTemplate(Items.SHULKER_BOX, 1))));

        context.register(EXPERIENCE_FIVE_HUNDRED,
                APItem.ofReward(
                        new ExperienceReward(500)));

        context.register(EXPERIENCE_ONE_HUNDRED,
                APItem.ofReward(
                        new ExperienceReward(100)));

        context.register(EXPERIENCE_FIFTY,
                APItem.ofReward(
                        new ExperienceReward(50)));
        
        context.register(COMPASS_VILLAGE, 
                APItem.ofReward(
                        new CompassReward(
                                Identifier.fromNamespaceAndPath(APRandomizer.MODID, "village"),
                                new StructureTarget(APStructureTags.VILLAGE, new RandomizedStructureLevel(APStructureModifiers.VILLAGE_NAME)),
                                Component.literal("Village"),
                                "Structures")));

        context.register(COMPASS_PILLAGER_OUTPOST,
                APItem.ofReward(
                        new CompassReward(
                                Identifier.fromNamespaceAndPath(APRandomizer.MODID, "pillager_outpost"),
                                new StructureTarget(APStructureTags.PILLAGER_OUTPOST, new RandomizedStructureLevel(APStructureModifiers.PILLAGER_OUTPOST_NAME)),
                                Component.literal("Pillager Outpost"),
                                "Structures")));

        context.register(COMPASS_FORTRESS,
                APItem.ofReward(
                        new CompassReward(
                                Identifier.fromNamespaceAndPath(APRandomizer.MODID, "fortress"),
                                new StructureTarget(APStructureTags.FORTRESS, new RandomizedStructureLevel(APStructureModifiers.FORTRESS_NAME)),
                                Component.literal("Nether Fortress"),
                                "Structures")));

        context.register(COMPASS_BASTION_REMNANT,
                APItem.ofReward(
                        new CompassReward(
                                Identifier.fromNamespaceAndPath(APRandomizer.MODID, "bastion_remnant"),
                                new StructureTarget(APStructureTags.BASTION_REMNANT, new RandomizedStructureLevel(APStructureModifiers.BASTION_REMNANT_NAME)),
                                Component.literal("Bastion Remnant"),
                                "Structures")));

        context.register(COMPASS_END_CITY,
                APItem.ofReward(
                        new CompassReward(
                                Identifier.fromNamespaceAndPath(APRandomizer.MODID, "end_city"),
                                new StructureTarget(APStructureTags.END_CITY, new RandomizedStructureLevel(APStructureModifiers.END_CITY_NAME)),
                                Component.literal("End City"),
                                "Structures")));

        context.register(COMPASS_WOODLAND_MANSION,
                APItem.ofReward(
                        new CompassReward(
                                Identifier.fromNamespaceAndPath(APRandomizer.MODID, "woodland_mansion"),
                                new StructureTarget(APStructureTags.WOODLAND_MANSION, new ConstantLevel(Level.OVERWORLD)),
                                Component.literal("Woodland Mansion"),
                                "Structures")));

        context.register(COMPASS_OCEAN_MONUMENT,
                APItem.ofReward(
                        new CompassReward(
                                Identifier.fromNamespaceAndPath(APRandomizer.MODID, "ocean_monument"),
                                new StructureTarget(APStructureTags.OCEAN_MONUMENT, new ConstantLevel(Level.OVERWORLD)),
                                Component.literal("Ocean Monument"),
                                "Structures")));

        context.register(COMPASS_ANCIENT_CITY,
                APItem.ofReward(
                        new CompassReward(
                                Identifier.fromNamespaceAndPath(APRandomizer.MODID, "ancient_city"),
                                new StructureTarget(APStructureTags.ANCIENT_CITY, new ConstantLevel(Level.OVERWORLD)),
                                Component.literal("Ancient City"),
                                "Structures")));

        context.register(COMPASS_TRAIL_RUINS,
                APItem.ofReward(
                        new CompassReward(
                                Identifier.fromNamespaceAndPath(APRandomizer.MODID, "trail_ruins"),
                                new StructureTarget(APStructureTags.TRAIL_RUINS, new ConstantLevel(Level.OVERWORLD)),
                                Component.literal("Trail Ruins"),
                                "Structures")));

        context.register(COMPASS_TRIAL_CHAMBERS,
                APItem.ofReward(
                        new CompassReward(
                                Identifier.fromNamespaceAndPath(APRandomizer.MODID, "trial_chambers"),
                                new StructureTarget(APStructureTags.TRIAL_CHAMBERS, new ConstantLevel(Level.OVERWORLD)),
                                Component.literal("Trial Chambers"),
                                "Structures")));

        context.register(COMPASS_UNVISITED_BIOMES,
                APItem.ofReward(
                        new CompassReward(
                                Identifier.fromNamespaceAndPath(APRandomizer.MODID, "unvisited_biomes"),
                                new UnvisitedBiomeTarget(),
                                Component.literal("Unvisited Biomes"),
                                "Biomes")));

        // New Structure Compasses
        context.register(COMPASS_STRONGHOLD,
                APItem.ofReward(
                        new CompassReward(
                                Identifier.fromNamespaceAndPath(APRandomizer.MODID, "stronghold"),
                                new StructureTarget(APStructureTags.STRONGHOLD, new ConstantLevel(Level.OVERWORLD)),
                                Component.literal("Stronghold"),
                                "Structures")));

        context.register(COMPASS_MINESHAFT,
                APItem.ofReward(
                        new CompassReward(
                                Identifier.fromNamespaceAndPath(APRandomizer.MODID, "mineshaft"),
                                new StructureTarget(APStructureTags.MINESHAFT, new ConstantLevel(Level.OVERWORLD)),
                                Component.literal("Mineshaft"),
                                "Structures")));

        context.register(COMPASS_DESERT_PYRAMID,
                APItem.ofReward(
                        new CompassReward(
                                Identifier.fromNamespaceAndPath(APRandomizer.MODID, "desert_pyramid"),
                                new StructureTarget(APStructureTags.DESERT_PYRAMID, new ConstantLevel(Level.OVERWORLD)),
                                Component.literal("Desert Pyramid"),
                                "Structures")));

        context.register(COMPASS_JUNGLE_TEMPLE,
                APItem.ofReward(
                        new CompassReward(
                                Identifier.fromNamespaceAndPath(APRandomizer.MODID, "jungle_temple"),
                                new StructureTarget(APStructureTags.JUNGLE_TEMPLE, new ConstantLevel(Level.OVERWORLD)),
                                Component.literal("Jungle Temple"),
                                "Structures")));

        context.register(COMPASS_SWAMP_HUT,
                APItem.ofReward(
                        new CompassReward(
                                Identifier.fromNamespaceAndPath(APRandomizer.MODID, "swamp_hut"),
                                new StructureTarget(APStructureTags.SWAMP_HUT, new ConstantLevel(Level.OVERWORLD)),
                                Component.literal("Swamp Hut"),
                                "Structures")));

        context.register(COMPASS_IGLOO,
                APItem.ofReward(
                        new CompassReward(
                                Identifier.fromNamespaceAndPath(APRandomizer.MODID, "igloo"),
                                new StructureTarget(APStructureTags.IGLOO, new ConstantLevel(Level.OVERWORLD)),
                                Component.literal("Igloo"),
                                "Structures")));

        context.register(COMPASS_SHIPWRECK,
                APItem.ofReward(
                        new CompassReward(
                                Identifier.fromNamespaceAndPath(APRandomizer.MODID, "shipwreck"),
                                new StructureTarget(APStructureTags.SHIPWRECK, new ConstantLevel(Level.OVERWORLD)),
                                Component.literal("Shipwreck"),
                                "Structures")));

        context.register(COMPASS_OCEAN_RUIN,
                APItem.ofReward(
                        new CompassReward(
                                Identifier.fromNamespaceAndPath(APRandomizer.MODID, "ocean_ruin"),
                                new StructureTarget(APStructureTags.OCEAN_RUIN, new ConstantLevel(Level.OVERWORLD)),
                                Component.literal("Ocean Ruin"),
                                "Structures")));

        context.register(COMPASS_BURIED_TREASURE,
                APItem.ofReward(
                        new CompassReward(
                                Identifier.fromNamespaceAndPath(APRandomizer.MODID, "buried_treasure"),
                                new StructureTarget(APStructureTags.BURIED_TREASURE, new ConstantLevel(Level.OVERWORLD)),
                                Component.literal("Buried Treasure"),
                                "Structures")));

        context.register(COMPASS_NETHER_FOSSIL,
                APItem.ofReward(
                        new CompassReward(
                                Identifier.fromNamespaceAndPath(APRandomizer.MODID, "nether_fossil"),
                                new StructureTarget(APStructureTags.NETHER_FOSSIL, new ConstantLevel(Level.NETHER)),
                                Component.literal("Nether Fossil"),
                                "Structures")));

        context.register(COMPASS_RUINED_PORTAL,
                APItem.ofReward(
                        new CompassReward(
                                Identifier.fromNamespaceAndPath(APRandomizer.MODID, "ruined_portal"),
                                new StructureTarget(APStructureTags.RUINED_PORTAL, new CurrentLevel()),
                                Component.literal("Ruined Portal"),
                                "Structures")));

        context.register(COMPASS_ABANDONED_CAMP,
                APItem.ofReward(
                        new CompassReward(
                                Identifier.fromNamespaceAndPath(APRandomizer.MODID, "abandoned_camp"),
                                new StructureTarget(APStructureTags.ABANDONED_CAMP, new ConstantLevel(Level.OVERWORLD)),
                                Component.literal("Abandoned Camp"),
                                "Structures")));

        context.register(COMPASS_UNVISITED_STRUCTURES,
                APItem.ofReward(
                        new CompassReward(
                                Identifier.fromNamespaceAndPath(APRandomizer.MODID, "unvisited_structures"),
                                new UnvisitedStructureTarget(),
                                Component.literal("Unvisited Structures"),
                                "Structures")));

        // Biome Compasses
        for (ExplorationData.BiomeEntry biome : ExplorationData.BIOMES) {
            context.register(biomeCompassKey(biome.path()),
                    APItem.ofReward(
                            new CompassReward(
                                    Identifier.fromNamespaceAndPath(APRandomizer.MODID, "biome/" + biome.path()),
                                    new ConstantBiomeTarget(HolderSet.direct(biomes.getOrThrow(biome.key()))),
                                    Component.literal(biome.displayName()),
                                    getBiomeDisplayCategory(biome))));
        }


        context.register(TRAP_BEES,
                APItem.ofReward(
                        new MobTrap(EntityTypes.BEE, 3, 5, true, Optional.of(1200))));

        context.register(DRAGON_EGG_SHARD,
                APItem.ofReward(
                        new DragonEggShardReward()));

        context.register(RECIPE_UNLOCK, APItem.ofReward(new RecipeUnlockReward()));
        context.register(HP_UPGRADE, APItem.ofReward(new HpReward()));
        context.register(HUNGER_UPGRADE, APItem.ofReward(new HungerReward()));
        context.register(REACH_UPGRADE, APItem.ofReward(new ReachReward()));
        context.register(WORLD_BORDER_UPGRADE, APItem.ofReward(new WorldBorderReward()));
        context.register(STRUCTURE_UNLOCK, APItem.ofReward(new StructureUnlockReward()));
        context.register(OFFHAND_UNLOCK, APItem.ofReward(new OffhandReward()));
        context.register(INVENTORY_SLOT_UPGRADE, APItem.ofReward(new InventorySlotReward()));
        context.register(TRAP_ITEM, APItem.ofReward(new RandomTrapReward()));
        context.register(MONSTER_SUN_BURNING_DISABLED, APItem.ofReward(new MonsterSunBurningReward()));
        context.register(MONSTER_SPAWN_LIGHT_DISABLED, APItem.ofReward(new MonsterSpawnLightReward()));

        context.register(PROGRESSIVE_MINING, APItem.ofReward(new AbilityReward(AbilityReward.AbilityType.PROGRESSIVE_MINING)));
        context.register(TREE_FELLER, APItem.ofReward(new AbilityReward(AbilityReward.AbilityType.TREE_FELLER)));
        context.register(PROGRESSIVE_JUMP, APItem.ofReward(new AbilityReward(AbilityReward.AbilityType.PROGRESSIVE_JUMP)));
        context.register(ITEM_MAGNET, APItem.ofReward(new AbilityReward(AbilityReward.AbilityType.ITEM_MAGNET)));
        context.register(STEP_ASSIST, APItem.ofReward(new AbilityReward(AbilityReward.AbilityType.STEP_ASSIST)));
        context.register(TRASH_CAN, APItem.ofReward(new AbilityReward(AbilityReward.AbilityType.TRASH_CAN)));
        context.register(LIGHT_HOLDER, APItem.ofReward(new AbilityReward(AbilityReward.AbilityType.LIGHT_HOLDER)));
    }

    private static ItemStackTemplate enchantment(Holder<Enchantment> enchantment, int level) {
        var mutable = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        mutable.set(enchantment, level);
        return new ItemStackTemplate(Items.ENCHANTED_BOOK, 1,
                DataComponentPatch.builder()
                        .set(DataComponents.STORED_ENCHANTMENTS, mutable.toImmutable())
                        .build());
    }
}
