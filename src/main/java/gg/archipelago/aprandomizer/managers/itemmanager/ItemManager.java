package gg.archipelago.aprandomizer.managers.itemmanager;

import com.google.common.base.Predicates;
import gg.archipelago.aprandomizer.APRandomizer;
import gg.archipelago.aprandomizer.APRegistries;
import gg.archipelago.aprandomizer.advancements.APCriteriaTriggers;
import gg.archipelago.aprandomizer.attachments.APAttachmentTypes;
import gg.archipelago.aprandomizer.attachments.APPlayerAttachment;
import gg.archipelago.aprandomizer.common.Utils.Utils;
import gg.archipelago.aprandomizer.data.WorldData;
import gg.archipelago.aprandomizer.exploration.ExplorationData;
import gg.archipelago.aprandomizer.items.*;
import gg.archipelago.aprandomizer.managers.GoalManager;
import gg.archipelago.aprandomizer.managers.RestrictionManager;
import gg.archipelago.aprandomizer.managers.recipemanager.RecipeTreeManager;
import gg.archipelago.aprandomizer.managers.recipemanager.RecipeTreeNode;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import gg.archipelago.aprandomizer.ap.APClient;
import gg.archipelago.aprandomizer.managers.AbilityManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Util;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class ItemManager {
    // Directly reference a log4j logger.
    private static final Logger LOGGER = LogManager.getLogger();

    public static final long RECIPE_UNLOCK = 1L;
    public static final long HP_UPGRADE = 2L;
    public static final long HUNGER_UPGRADE = 3L;
    public static final long REACH_UPGRADE = 4L;
    public static final long WORLD_BORDER_UPGRADE = 5L;
    public static final long STRUCTURE_UNLOCK = 6L;
    public static final long OFFHAND_UNLOCK = 7L;
    public static final long INVENTORY_SLOT_UPGRADE = 8L;
    public static final long TRAP_ITEM = 9L;
    public static final long MONSTER_SUN_BURNING_DISABLED = 10L;
    public static final long MONSTER_SPAWN_LIGHT_DISABLED = 11L;

    public static final long COMPASS_VILLAGE = 12L;
    public static final long COMPASS_PILLAGER_OUTPOST = 13L;
    public static final long COMPASS_FORTRESS = 14L;
    public static final long COMPASS_BASTION_REMNANT = 15L;
    public static final long COMPASS_END_CITY = 16L;
    public static final long COMPASS_OCEAN_MONUMENT = 17L;
    public static final long COMPASS_WOODLAND_MANSION = 18L;
    public static final long COMPASS_ANCIENT_CITY = 19L;
    public static final long COMPASS_TRAIL_RUINS = 20L;
    public static final long COMPASS_TRIAL_CHAMBERS = 21L;
    public static final long COMPASS_STRONGHOLD = 22L;
    public static final long COMPASS_MINESHAFT = 23L;
    public static final long COMPASS_DESERT_PYRAMID = 24L;
    public static final long COMPASS_JUNGLE_TEMPLE = 25L;
    public static final long COMPASS_SWAMP_HUT = 26L;
    public static final long COMPASS_IGLOO = 27L;
    public static final long COMPASS_SHIPWRECK = 28L;
    public static final long COMPASS_OCEAN_RUIN = 29L;
    public static final long COMPASS_BURIED_TREASURE = 30L;
    public static final long COMPASS_NETHER_FOSSIL = 31L;
    public static final long COMPASS_RUINED_PORTAL = 32L;
    public static final long COMPASS_ABANDONED_CAMP = 33L;

    public static final long COMPASS_UNVISITED_STRUCTURES = 34L;
    public static final long DRAGON_EGG_SHARD = 35L;
    public static final long COMPASS_UNVISITED_BIOMES = 36L;

    // Biome Compasses (37..102)
    public static final long BIOME_COMPASS_START = 37L;

    // Extra Abilities (103..109)
    public static final long PROGRESSIVE_MINING = 103L;
    public static final long TREE_FELLER = 104L;
    public static final long PROGRESSIVE_JUMP = 105L;
    public static final long ITEM_MAGNET = 106L;
    public static final long STEP_ASSIST = 107L;
    public static final long TRASH_CAN = 108L;
    public static final long LIGHT_HOLDER = 109L;

    private static final Long2ObjectMap<ResourceKey<APItem>> DEFAULT_ITEMS = Util.make(new Long2ObjectOpenHashMap<>(), map -> {
        // Active Archipelago Item Pool (1..11)
        map.put(RECIPE_UNLOCK, APItems.RECIPE_UNLOCK);
        map.put(HP_UPGRADE, APItems.HP_UPGRADE);
        map.put(HUNGER_UPGRADE, APItems.HUNGER_UPGRADE);
        map.put(REACH_UPGRADE, APItems.REACH_UPGRADE);
        map.put(WORLD_BORDER_UPGRADE, APItems.WORLD_BORDER_UPGRADE);
        map.put(STRUCTURE_UNLOCK, APItems.STRUCTURE_UNLOCK);
        map.put(OFFHAND_UNLOCK, APItems.OFFHAND_UNLOCK);
        map.put(INVENTORY_SLOT_UPGRADE, APItems.INVENTORY_SLOT_UPGRADE);
        map.put(TRAP_ITEM, APItems.TRAP_ITEM);
        map.put(MONSTER_SUN_BURNING_DISABLED, APItems.MONSTER_SUN_BURNING_DISABLED);
        map.put(MONSTER_SPAWN_LIGHT_DISABLED, APItems.MONSTER_SPAWN_LIGHT_DISABLED);

        // 22 Structure Compasses (12..33)
        map.put(COMPASS_VILLAGE, APItems.COMPASS_VILLAGE);
        map.put(COMPASS_PILLAGER_OUTPOST, APItems.COMPASS_PILLAGER_OUTPOST);
        map.put(COMPASS_FORTRESS, APItems.COMPASS_FORTRESS);
        map.put(COMPASS_BASTION_REMNANT, APItems.COMPASS_BASTION_REMNANT);
        map.put(COMPASS_END_CITY, APItems.COMPASS_END_CITY);
        map.put(COMPASS_OCEAN_MONUMENT, APItems.COMPASS_OCEAN_MONUMENT);
        map.put(COMPASS_WOODLAND_MANSION, APItems.COMPASS_WOODLAND_MANSION);
        map.put(COMPASS_ANCIENT_CITY, APItems.COMPASS_ANCIENT_CITY);
        map.put(COMPASS_TRAIL_RUINS, APItems.COMPASS_TRAIL_RUINS);
        map.put(COMPASS_TRIAL_CHAMBERS, APItems.COMPASS_TRIAL_CHAMBERS);
        map.put(COMPASS_STRONGHOLD, APItems.COMPASS_STRONGHOLD);
        map.put(COMPASS_MINESHAFT, APItems.COMPASS_MINESHAFT);
        map.put(COMPASS_DESERT_PYRAMID, APItems.COMPASS_DESERT_PYRAMID);
        map.put(COMPASS_JUNGLE_TEMPLE, APItems.COMPASS_JUNGLE_TEMPLE);
        map.put(COMPASS_SWAMP_HUT, APItems.COMPASS_SWAMP_HUT);
        map.put(COMPASS_IGLOO, APItems.COMPASS_IGLOO);
        map.put(COMPASS_SHIPWRECK, APItems.COMPASS_SHIPWRECK);
        map.put(COMPASS_OCEAN_RUIN, APItems.COMPASS_OCEAN_RUIN);
        map.put(COMPASS_BURIED_TREASURE, APItems.COMPASS_BURIED_TREASURE);
        map.put(COMPASS_NETHER_FOSSIL, APItems.COMPASS_NETHER_FOSSIL);
        map.put(COMPASS_RUINED_PORTAL, APItems.COMPASS_RUINED_PORTAL);
        map.put(COMPASS_ABANDONED_CAMP, APItems.COMPASS_ABANDONED_CAMP);

        // Special Compasses & Dragon Egg Shard (34..36)
        map.put(COMPASS_UNVISITED_STRUCTURES, APItems.COMPASS_UNVISITED_STRUCTURES);
        map.put(DRAGON_EGG_SHARD, APItems.DRAGON_EGG_SHARD);
        map.put(COMPASS_UNVISITED_BIOMES, APItems.COMPASS_UNVISITED_BIOMES);

        // Biome Compasses (37..102: 66 biomes)
        long currentBiomeId = BIOME_COMPASS_START;
        for (ExplorationData.BiomeEntry biome : ExplorationData.BIOMES) {
            map.put(currentBiomeId++, APItems.biomeCompassKey(biome.path()));
        }

        // Extra Abilities (103..109)
        map.put(PROGRESSIVE_MINING, APItems.PROGRESSIVE_MINING);
        map.put(TREE_FELLER, APItems.TREE_FELLER);
        map.put(PROGRESSIVE_JUMP, APItems.PROGRESSIVE_JUMP);
        map.put(ITEM_MAGNET, APItems.ITEM_MAGNET);
        map.put(STEP_ASSIST, APItems.STEP_ASSIST);
        map.put(TRASH_CAN, APItems.TRASH_CAN);
        map.put(LIGHT_HOLDER, APItems.LIGHT_HOLDER);

        /* Legacy item pool preserved for reference without deleting code:
        map.put(1L, APItems.GROUP_RECIPES_ARCHERY);
        map.put(2L, APItems.PROGRESSIVE_RECIPES_RESOURCE_CRAFTING);
        map.put(3L, APItems.GROUP_RECIPES_BREWING);
        map.put(4L, APItems.GROUP_RECIPES_ENCHANTING);
        map.put(5L, APItems.GROUP_RECIPES_BUCKET);
        map.put(6L, APItems.GROUP_RECIPES_FLINT_AND_STEEL);
        map.put(7L, APItems.GROUP_RECIPES_BEDS);
        map.put(8L, APItems.GROUP_RECIPES_BOTTLES);
        map.put(9L, APItems.GROUP_RECIPES_SHIELD);
        map.put(10L, APItems.GROUP_RECIPES_FISHING);
        map.put(11L, APItems.GROUP_RECIPES_CAMPFIRES);
        map.put(12L, APItems.PROGRESSIVE_RECIPES_WEAPONS);
        map.put(13L, APItems.PROGRESSIVE_RECIPES_TOOLS);
        map.put(14L, APItems.PROGRESSIVE_RECIPES_ARMOR);
        map.put(15L, APItems.ITEMSTACK_NETHERITE_SCRAP);
        map.put(16L, APItems.ITEMSTACK_EIGHT_EMERALD);
        map.put(17L, APItems.ITEMSTACK_FOUR_EMERALD);
        map.put(18L, APItems.ITEMSTACK_ENCHANTMENT_CHANNELING_ONE);
        map.put(19L, APItems.ITEMSTACK_ENCHANTMENT_SILK_TOUCH_ONE);
        map.put(20L, APItems.ITEMSTACK_ENCHANTMENT_SHARPNESS_THREE);
        map.put(21L, APItems.ITEMSTACK_ENCHANTMENT_PIERCING_FOUR);
        map.put(22L, APItems.ITEMSTACK_ENCHANTMENT_MOB_LOOTING_THREE);
        map.put(23L, APItems.ITEMSTACK_ENCHANTMENT_INFINITY_ARROWS_ONE);
        map.put(24L, APItems.ITEMSTACK_DIAMOND_ORE);
        map.put(25L, APItems.ITEMSTACK_IRON_ORE);
        map.put(26L, APItems.EXPERIENCE_FIVE_HUNDRED);
        map.put(27L, APItems.EXPERIENCE_ONE_HUNDRED);
        map.put(28L, APItems.EXPERIENCE_FIFTY);
        map.put(29L, APItems.ITEMSTACK_ENDER_PEARL);
        map.put(30L, APItems.ITEMSTACK_LAPIS_LAZULI);
        map.put(31L, APItems.ITEMSTACK_COOKED_PORKCHOP);
        map.put(32L, APItems.ITEMSTACK_GOLD_ORE);
        map.put(33L, APItems.ITEMSTACK_ROTTEN_FLESH);
        map.put(34L, APItems.ITEMSTACK_THE_ARROW);
        map.put(35L, APItems.ITEMSTACK_THIRTY_TWO_ARROW);
        map.put(36L, APItems.GROUP_RECIPES_SADDLE);
        map.put(37L, APItems.COMPASS_VILLAGE);
        map.put(38L, APItems.COMPASS_PILLAGER_OUTPOST);
        map.put(39L, APItems.COMPASS_FORTRESS);
        map.put(40L, APItems.COMPASS_BASTION_REMNANT);
        map.put(41L, APItems.COMPASS_END_CITY);
        map.put(42L, APItems.ITEMSTACK_SHULKER_BOX);
        map.put(DRAGON_EGG_SHARD, APItems.DRAGON_EGG_SHARD);
        map.put(44L, APItems.GROUP_RECIPES_SPYGLASS);
        map.put(45L, APItems.GROUP_RECIPES_LEAD);
        map.put(46L, APItems.TRAP_BEES);
        map.put(47L, APItems.GROUP_RECIPES_BRUSH);
        map.put(48L, APItems.COMPASS_OCEAN_MONUMENT);
        map.put(49L, APItems.COMPASS_WOODLAND_MANSION);
        map.put(50L, APItems.COMPASS_ANCIENT_CITY);
        map.put(51L, APItems.COMPASS_TRAIL_RUINS);
        map.put(52L, APItems.COMPASS_TRIAL_CHAMBERS);
        map.put(53L, APItems.COMPASS_SULFUR_CAVES);
        map.put(54L, APItems.COMPASS_UNVISITED_BIOMES);
        */
    });

//    long index = 51L;
//    private final HashMap<Long, Callable<Trap>> trapData = new HashMap<>() {{
//        put(index++, BeeTrap::new);
//        put(index++, CreeperTrap::new);
//        put(index++, SandRain::new);
//        put(index++, FakeWither::new);
//        put(index++, GoonTrap::new);
//        put(index++, FishFountainTrap::new);
//        put(index++, MiningFatigueTrap::new);
//        put(index++, BlindnessTrap::new);
//        put(index++, PhantomTrap::new);
//        put(index++, WaterTrap::new);
//        put(index++, GhastTrap::new);
//        put(index++, LevitateTrap::new);
//        put(index++, AboutFaceTrap::new);
//        put(index++, AnvilTrap::new);
//    }};

    private Object2IntMap<ResourceKey<APItem>> tiers = new Object2IntOpenHashMap<>();
    private List<Tier> receivedItems = new ArrayList<>();
    private int index = 0;
    private final MinecraftServer server;
    private final GoalManager goalManager;
    private final WorldData worldData;

    public ItemManager(MinecraftServer server, GoalManager goalManager, WorldData worldData) {
        this.server = server;
        this.goalManager = goalManager;
        this.worldData = worldData;
    }

    public int getIndex() {
        return this.index;
    }

    public void setReceivedItems(LongList items) {
        tiers = new Object2IntOpenHashMap<>();
        receivedItems = items.longStream()
                .mapToObj(this::getTier)
                .filter(Predicates.notNull())
                .collect(Collectors.toList());
        goalManager.updateGoal(false);
    }

    private Tier getTier(long itemID) {
        MinecraftServer currentServer = this.server != null ? this.server : APRandomizer.getServer();
        if (currentServer != null && DEFAULT_ITEMS.containsKey(itemID)) {
            ResourceKey<APItem> itemKey = DEFAULT_ITEMS.get(itemID);
            Optional<Holder.Reference<APItem>> itemOptional = currentServer.registryAccess().get(itemKey);
            if (itemOptional.isPresent()) {
                int tierIndex = tiers.getInt(itemKey);
                tiers.put(itemKey, tierIndex + 1);
                APItem item = itemOptional.get().value();
                APTier tier = item.tiers().get(Math.min(item.tiers().size() - 1, tierIndex));
                return new Tier(tier, itemKey, tierIndex);
            } else {
                LOGGER.error("ResourceKey {} not found in dynamic registry", itemKey);
            }
        }
        return null;
    }

    private void reconcileMissingHistoricalItem(Tier tier, long itemID, WorldData worldData) {
        if (worldData == null || tier == null) return;
        MinecraftServer currentServer = this.server != null ? this.server : APRandomizer.getServer();
        boolean changed = false;

        if (itemID == PROGRESSIVE_MINING) {
            int targetTier = tier.tierIndex + 1;
            if (worldData.getMiningLevel() < targetTier) {
                worldData.setMiningLevel(targetTier);
                changed = true;
            }
        } else if (itemID == TREE_FELLER) {
            if (!worldData.isTreeFellerUnlocked()) {
                worldData.setTreeFellerUnlocked(true);
                changed = true;
            }
        } else if (itemID == PROGRESSIVE_JUMP) {
            int targetTier = tier.tierIndex + 1;
            if (worldData.getJumpLevel() < targetTier) {
                worldData.setJumpLevel(targetTier);
                changed = true;
            }
        } else if (itemID == ITEM_MAGNET) {
            if (!worldData.isItemMagnetUnlocked()) {
                worldData.setItemMagnetUnlocked(true);
                changed = true;
            }
        } else if (itemID == STEP_ASSIST) {
            if (!worldData.isStepAssistUnlocked()) {
                worldData.setStepAssistUnlocked(true);
                changed = true;
            }
        } else if (itemID == TRASH_CAN) {
            if (!worldData.isTrashCanUnlocked()) {
                worldData.setTrashCanUnlocked(true);
                changed = true;
            }
        } else if (itemID == LIGHT_HOLDER) {
            if (!worldData.isLightHolderUnlocked()) {
                worldData.setLightHolderUnlocked(true);
                changed = true;
            }
        } else if (itemID >= BIOME_COMPASS_START && itemID < BIOME_COMPASS_START + ExplorationData.BIOMES.size()
                || (itemID >= COMPASS_VILLAGE && itemID <= COMPASS_ABANDONED_CAMP)
                || itemID == COMPASS_UNVISITED_STRUCTURES || itemID == COMPASS_UNVISITED_BIOMES) {
            if (currentServer != null) {
                for (ServerPlayer player : currentServer.getPlayerList().getPlayers()) {
                    for (APReward reward : tier.tier.rewards()) {
                        if (reward instanceof CompassReward cr) {
                            var map = player.getData(APAttachmentTypes.AP_PLAYER).getUnlockedCompassRewards();
                            if (!map.containsKey(cr.id())) {
                                map.put(cr.id(), cr);
                                changed = true;
                            }
                        }
                    }
                    ensurePlayerHasCompasses(player);
                }
            }
        }

        if (changed && currentServer != null) {
            AbilityManager.syncToAll(currentServer);
            syncAllPlayerAdvancements();
        }
    }

    public void giveItem(APTier tier, ResourceKey<APItem> key, ServerPlayer player, int itemIndex, int tierIndex) {
        if (APRandomizer.isJailPlayers()) {
            //don't send items to players if game has not started.
            return;
        }

        APPlayerAttachment attachment = player.getData(APAttachmentTypes.AP_PLAYER);

        if (attachment.getIndex() >= itemIndex)
            return;

        APCriteriaTriggers.RECEIVED_ITEM.get().trigger(player, key, tierIndex + 1);

        //update the player's index of received items for syncing later.
        attachment.setIndex(itemIndex);
        for (APReward reward : tier.rewards()) {
            reward.give(player);
        }
        try {
            player.getAdvancements().save();
        } catch (Exception ignored) {}
    }


    public boolean giveItemToAll(long itemID, int index) {
        return giveItemToAll(itemID, index, null);
    }

    public boolean giveItemToAll(long itemID, int index, String senderSlotName) {
        if (index <= this.index)
            return false;
        this.index = index;
        Tier tier = getTier(itemID);
        if (tier != null) {
            receivedItems.add(tier);
            // Don't fire if we have already received this location
            WorldData worldData = APRandomizer.getWorldData();
            if (worldData == null)
                return false;
            if (index <= worldData.getItemIndex()) {
                reconcileMissingHistoricalItem(tier, itemID, worldData);
                return false;
            }

            RestrictionManager.setCurrentTrapSender(senderSlotName);
            try {
                for (APReward reward : tier.tier.rewards()) {
                    reward.give(server);
                }
                for (ServerPlayer serverPlayerEntity : server.getPlayerList().getPlayers()) {
                    giveItem(tier.tier, tier.key, serverPlayerEntity, index, tier.tierIndex);
                }
            } finally {
                RestrictionManager.setCurrentTrapSender(null);
            }
            worldData.setItemIndex(index);
            syncAllPlayerAdvancements();
        } else {
            return false;
        }

        goalManager.updateGoal(true);
        return true;
    }

    /***
     fetches the index form the player's capability then makes sure they have all items after that index.
     * @param player ServerPlayer to catch up
     */
    public void catchUpPlayer(ServerPlayer player) {
        WorldData wd = APRandomizer.getWorldData();
        if (wd != null && !wd.getUnlockedRecipes().isEmpty()) {
            player.awardRecipesByKey(wd.getUnlockedRecipes());
        }
        grantAllInitialRecipes(player);
        int playerIndex = player.getData(APAttachmentTypes.AP_PLAYER).getIndex();

        for (int i = playerIndex; i < receivedItems.size(); i++) {
            Tier tier = receivedItems.get(i);
            if (APItems.TRAP_ITEM.equals(tier.key)) {
                player.getData(APAttachmentTypes.AP_PLAYER).setIndex(i + 1);
                continue;
            }
            giveItem(tier.tier, tier.key, player, i + 1, tier.tierIndex);
        }

        ensurePlayerHasCompasses(player);
        syncPlayerAdvancements(player);
    }

    public void syncPlayerAdvancements(ServerPlayer player) {
        if (player == null || APRandomizer.isJailPlayers()) return;
        WorldData data = APRandomizer.getWorldData();
        if (data == null) return;

        // 1. HP Upgrades
        if (RestrictionManager.isHpRestrictionEnabled()) {
            int hp = data.getHpUpgrades();
            for (int i = 1; i <= hp; i++) {
                APCriteriaTriggers.RECEIVED_ITEM.get().trigger(player, APItems.HP_UPGRADE, i);
            }
        }

        // 2. Hunger Upgrades
        if (RestrictionManager.isHungerRestrictionEnabled()) {
            int hunger = data.getHungerUpgrades();
            for (int i = 1; i <= hunger; i++) {
                APCriteriaTriggers.RECEIVED_ITEM.get().trigger(player, APItems.HUNGER_UPGRADE, i);
            }
        }

        // 3. Reach Upgrades
        if (RestrictionManager.isReachRestrictionEnabled()) {
            int reach = data.getReachUpgrades();
            for (int i = 1; i <= reach; i++) {
                APCriteriaTriggers.RECEIVED_ITEM.get().trigger(player, APItems.REACH_UPGRADE, i);
            }
        }

        // 4. World Border Upgrades
        if (RestrictionManager.isWorldBorderRestrictionEnabled()) {
            int border = data.getWorldBorderUpgrades();
            for (int i = 1; i <= border; i++) {
                APCriteriaTriggers.RECEIVED_ITEM.get().trigger(player, APItems.WORLD_BORDER_UPGRADE, i);
            }
        }

        // 5. Structure Unlocks
        if (RestrictionManager.isStructureRestrictionEnabled()) {
            int structures = data.getStructureUpgrades();
            for (int i = 1; i <= structures; i++) {
                APCriteriaTriggers.RECEIVED_ITEM.get().trigger(player, APItems.STRUCTURE_UNLOCK, i);
            }
        }

        // 6. Offhand Unlock
        if (RestrictionManager.isOffhandRestrictionEnabled() && data.isOffhandUnlocked()) {
            APCriteriaTriggers.RECEIVED_ITEM.get().trigger(player, APItems.OFFHAND_UNLOCK, 1);
        }

        // 7. Inventory Slot Upgrades
        if (RestrictionManager.isInventorySlotRestrictionEnabled()) {
            int slots = data.getInventorySlotUpgrades();
            for (int i = 1; i <= slots; i++) {
                APCriteriaTriggers.RECEIVED_ITEM.get().trigger(player, APItems.INVENTORY_SLOT_UPGRADE, i);
            }
        }

        // 8. Curses
        if (RestrictionManager.isCursesEnabled()) {
            if (data.isMonsterSunBurningDisabled()) {
                APCriteriaTriggers.RECEIVED_ITEM.get().trigger(player, APItems.MONSTER_SUN_BURNING_DISABLED, 1);
            }
            if (data.isIgnoreLightSpawningEnabled()) {
                APCriteriaTriggers.RECEIVED_ITEM.get().trigger(player, APItems.MONSTER_SPAWN_LIGHT_DISABLED, 1);
            }
        }

        // 9. Recipe Tree Nodes
        Set<ResourceKey<Recipe<?>>> unlocked = new HashSet<>(data.getUnlockedRecipes());
        for (RecipeTreeNode node : RecipeTreeManager.getAllNodes()) {
            if (node.isUnlocked(unlocked)) {
                APCriteriaTriggers.RECIPE_NODE.get().trigger(player, node.id());
            }
        }

        // 10. Extra Abilities
        if (RestrictionManager.isExtraAbilitiesEnabled()) {
            int mining = data.getMiningLevel();
            for (int i = 1; i <= mining; i++) {
                APCriteriaTriggers.RECEIVED_ITEM.get().trigger(player, APItems.PROGRESSIVE_MINING, i);
            }
            if (data.isTreeFellerUnlocked()) {
                APCriteriaTriggers.RECEIVED_ITEM.get().trigger(player, APItems.TREE_FELLER, 1);
            }
            int jump = data.getJumpLevel();
            for (int i = 1; i <= jump; i++) {
                APCriteriaTriggers.RECEIVED_ITEM.get().trigger(player, APItems.PROGRESSIVE_JUMP, i);
            }
            if (data.isItemMagnetUnlocked()) {
                APCriteriaTriggers.RECEIVED_ITEM.get().trigger(player, APItems.ITEM_MAGNET, 1);
            }
            if (data.isStepAssistUnlocked()) {
                APCriteriaTriggers.RECEIVED_ITEM.get().trigger(player, APItems.STEP_ASSIST, 1);
            }
            if (data.isTrashCanUnlocked()) {
                APCriteriaTriggers.RECEIVED_ITEM.get().trigger(player, APItems.TRASH_CAN, 1);
            }
            if (data.isLightHolderUnlocked()) {
                APCriteriaTriggers.RECEIVED_ITEM.get().trigger(player, APItems.LIGHT_HOLDER, 1);
            }
        }

        // 11. Biome Compasses
        Map<Identifier, CompassReward> compasses = player.getData(APAttachmentTypes.AP_PLAYER).getUnlockedCompassRewards();
        Set<ResourceKey<APItem>> receivedKeySet = receivedItems.stream()
                .map(Tier::key)
                .collect(Collectors.toSet());
        for (gg.archipelago.aprandomizer.exploration.ExplorationData.BiomeEntry biome : gg.archipelago.aprandomizer.exploration.ExplorationData.BIOMES) {
            Identifier compassId = Identifier.fromNamespaceAndPath(APRandomizer.MODID, "biome/" + biome.path());
            ResourceKey<APItem> itemKey = APItems.biomeCompassKey(biome.path());
            if (compasses.containsKey(compassId) || receivedKeySet.contains(itemKey)) {
                APCriteriaTriggers.RECEIVED_ITEM.get().trigger(player, itemKey, 1);
            }
        }

        // 12. Persist advancements to disk
        try {
            player.getAdvancements().save();
            player.getAdvancements().flushDirty(player, false);
        } catch (Exception e) {
            LOGGER.error("Failed to save advancements for player {}", player.getScoreboardName(), e);
        }
    }

    public void syncAllPlayerAdvancements() {
        if (APRandomizer.isJailPlayers()) return;
        MinecraftServer currentServer = APRandomizer.getServer();
        if (currentServer != null) {
            for (ServerPlayer player : currentServer.getPlayerList().getPlayers()) {
                syncPlayerAdvancements(player);
            }
        }
    }

    public void reconcileReceivedItems(MinecraftServer server) {
        if (server == null) server = this.server != null ? this.server : APRandomizer.getServer();
        if (server == null) return;
        WorldData wd = APRandomizer.getWorldData();
        if (wd == null) return;

        APClient apClient = APRandomizer.getAP();
        List<Long> allItemIds = new ArrayList<>();
        if (apClient != null && apClient.getItemManager() != null) {
            allItemIds.addAll(apClient.getItemManager().getReceivedItemIDs());
        }

        if (allItemIds.isEmpty() && !this.receivedItems.isEmpty()) {
            for (Tier t : this.receivedItems) {
                for (var entry : DEFAULT_ITEMS.long2ObjectEntrySet()) {
                    if (entry.getValue().equals(t.key)) {
                        allItemIds.add(entry.getLongKey());
                        break;
                    }
                }
            }
        }

        if (allItemIds.isEmpty()) return;

        // Rebuild internal tiers and receivedItems
        this.tiers = new Object2IntOpenHashMap<>();
        this.receivedItems = new ArrayList<>();
        for (long id : allItemIds) {
            Tier t = getTier(id);
            if (t != null) {
                this.receivedItems.add(t);
            }
        }
        this.index = Math.max(this.index, allItemIds.size());
        if (wd.getItemIndex() < allItemIds.size()) {
            wd.setItemIndex(allItemIds.size());
        }

        // Count occurrences
        Long2IntMap counts = new Long2IntOpenHashMap();
        for (long id : allItemIds) {
            counts.put(id, counts.get(id) + 1);
        }

        // Extra Abilities
        int expMining = Math.min(5, counts.get(PROGRESSIVE_MINING));
        if (wd.getMiningLevel() < expMining) {
            wd.setMiningLevel(expMining);
        }

        boolean expTreeFeller = counts.get(TREE_FELLER) > 0;
        if (!wd.isTreeFellerUnlocked() && expTreeFeller) {
            wd.setTreeFellerUnlocked(true);
        }

        int expJump = Math.min(2, counts.get(PROGRESSIVE_JUMP));
        if (wd.getJumpLevel() < expJump) {
            wd.setJumpLevel(expJump);
        }

        boolean expMagnet = counts.get(ITEM_MAGNET) > 0;
        if (!wd.isItemMagnetUnlocked() && expMagnet) {
            wd.setItemMagnetUnlocked(true);
        }

        boolean expStep = counts.get(STEP_ASSIST) > 0;
        if (!wd.isStepAssistUnlocked() && expStep) {
            wd.setStepAssistUnlocked(true);
        }

        boolean expTrash = counts.get(TRASH_CAN) > 0;
        if (!wd.isTrashCanUnlocked() && expTrash) {
            wd.setTrashCanUnlocked(true);
        }

        boolean expLight = counts.get(LIGHT_HOLDER) > 0;
        if (!wd.isLightHolderUnlocked() && expLight) {
            wd.setLightHolderUnlocked(true);
        }

        // Vanilla Upgrades / Restrictions
        int expHp = counts.get(HP_UPGRADE);
        if (wd.getHpUpgrades() < expHp) {
            wd.setHpUpgrades(expHp);
        }

        int expHunger = counts.get(HUNGER_UPGRADE);
        if (wd.getHungerUpgrades() < expHunger) {
            wd.setHungerUpgrades(expHunger);
        }

        int expReach = counts.get(REACH_UPGRADE);
        if (wd.getReachUpgrades() < expReach) {
            wd.setReachUpgrades(expReach);
        }

        int expBorder = counts.get(WORLD_BORDER_UPGRADE);
        if (wd.getWorldBorderUpgrades() < expBorder) {
            wd.setWorldBorderUpgrades(expBorder);
        }

        int expInv = counts.get(INVENTORY_SLOT_UPGRADE);
        if (wd.getInventorySlotUpgrades() < expInv) {
            wd.setInventorySlotUpgrades(expInv);
        }

        if (counts.get(OFFHAND_UNLOCK) > 0 && !wd.isOffhandUnlocked()) {
            wd.setOffhandUnlocked(true);
        }

        if (counts.get(MONSTER_SUN_BURNING_DISABLED) > 0 && !wd.isMonsterSunBurningDisabled()) {
            wd.setMonsterSunBurningDisabled(true);
        }

        if (counts.get(MONSTER_SPAWN_LIGHT_DISABLED) > 0 && !wd.isIgnoreLightSpawningEnabled()) {
            wd.setIgnoreLightSpawningEnabled(true);
        }

        int expShards = counts.get(DRAGON_EGG_SHARD);
        if (wd.getDragonEggShards() < expShards) {
            wd.setDragonEggShards(expShards);
        }

        // Structure Unlocks
        int expStructures = counts.get(STRUCTURE_UNLOCK);
        while (wd.getStructureUpgrades() < expStructures) {
            if (RestrictionManager.unlockNextStructure() == null) break;
        }

        // Compasses
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            var playerCompasses = player.getData(APAttachmentTypes.AP_PLAYER).getUnlockedCompassRewards();
            for (Tier tier : this.receivedItems) {
                for (APReward reward : tier.tier.rewards()) {
                    if (reward instanceof CompassReward cr) {
                        playerCompasses.put(cr.id(), cr);
                    }
                }
            }
            ensurePlayerHasCompasses(player);
        }

        RestrictionManager.applyAllRestrictions(server);
        AbilityManager.syncToAll(server);
        syncAllPlayerAdvancements();
    }

    public void catchUp(MinecraftServer server) {
        reconcileReceivedItems(server);
        WorldData data = APRandomizer.getWorldData();
        if (data != null) {
            for (int i = data.getItemIndex(); i < receivedItems.size(); i++) {
                for (APReward reward : receivedItems.get(i).tier.rewards()) {
                    reward.give(server);
                }
            }
            data.setItemIndex(receivedItems.size());
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                catchUpPlayer(player);
            }
        }
    }

    public Set<ResourceKey<Recipe<?>>> getLockedRecipes(RegistryAccess registryAccess) {
        if (!RecipeTreeManager.isRecipeRestrictionEnabled()) {
            return Collections.emptySet();
        }
        Set<ResourceKey<Recipe<?>>> lockedRecipes = new HashSet<>(RecipeTreeManager.getAllTreeRecipes());
        lockedRecipes.removeAll(RecipeTreeManager.getInitialUnlockedRecipes());
        WorldData wd = APRandomizer.getWorldData();
        if (wd != null) {
            lockedRecipes.removeAll(wd.getUnlockedRecipes());
        } else if (worldData != null) {
            lockedRecipes.removeAll(worldData.getUnlockedRecipes());
        }
        return lockedRecipes;
    }

    public void grantAllInitialRecipes(ServerPlayer player) {
        Set<ResourceKey<Recipe<?>>> lockedRecipes = getLockedRecipes(player.registryAccess());
        Set<RecipeHolder<?>> recipes = player.level().getServer().getRecipeManager().getRecipes().stream().filter(recipe -> !lockedRecipes.contains(recipe.id())).collect(Collectors.toSet());
        player.awardRecipes(recipes);
    }

    public static void updateCompassLocation(CompassReward compassReward, ServerPlayer player, ItemStack compass) {
        //get our local custom structure if needed.
        Optional<BlockPos> pos = compassReward.target().findTarget(player.level(), player.blockPosition(), player);
        List<String> lore = new ArrayList<>(List.of(
                "Right click with compass in hand to",
                "select a target from unlocked compasses."));
        Component displayName = Component.empty()
                .append("Compass (")
                .append(compassReward.name())
                .append(")");

        if (pos.isPresent()) {
            lore.addFirst("Location X: " + pos.get().getX() + (compassReward.target().includeY() ? ", Y: " + pos.get().getY() : "") + ", Z: " + pos.get().getZ());
            List<String> extraLore = compassReward.target().getExtraLore(player.level(), pos.get(), player);
            if (!extraLore.isEmpty()) {
                lore.addAll(1, extraLore);
            }
        } else {
            displayName = Component.empty()
                    .append("Compass (")
                    .append(compassReward.name())
                    .append(") Not Found")
                    .withStyle(ChatFormatting.YELLOW);
            List<String> notFoundLore = compassReward.target().getNotFoundLore(player.level(), player);
            if (!notFoundLore.isEmpty()) {
                lore.addAll(0, notFoundLore);
            }
        }

        //update the nbt data with our new structure.
        CompoundTag nbt = compass.has(DataComponents.CUSTOM_DATA) ? compass.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag() : new CompoundTag();
        nbt.store("target", CompassReward.CODEC, player.registryAccess().createSerializationContext(NbtOps.INSTANCE), compassReward);
        compass.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt));

        //update the nbt data with our new structure.
        compass.set(DataComponents.LODESTONE_TRACKER, new LodestoneTracker(pos.map(block -> GlobalPos.of(player.level().dimension(), block)), false));
        Utils.setNameAndLore(compass, displayName, lore);
        player.containerMenu.broadcastChanges();
    }

    public static boolean isArchipelagoCompass(ItemStack compass, @org.jetbrains.annotations.Nullable ServerPlayer player) {
        if (!compass.is(Items.COMPASS)) return false;
        CustomData customData = compass.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag nbt = customData.copyTag();
            if (nbt.contains("target") || nbt.contains("compass_type")) {
                return true;
            }
        }
        Component customName = compass.get(DataComponents.CUSTOM_NAME);
        if (customName != null) {
            String name = customName.getString();
            if (name.startsWith("Compass (") || name.startsWith("Structure Compass (") || name.startsWith("Biome Compass (")) {
                return true;
            }
        }
        Component itemName = compass.get(DataComponents.ITEM_NAME);
        if (itemName != null) {
            String name = itemName.getString();
            if (name.startsWith("Compass (") || name.startsWith("Structure Compass (") || name.startsWith("Biome Compass (") || name.equals("uninitialized compass")) {
                return true;
            }
        }
        return false;
    }

    public static boolean isStructureCompass(ItemStack compass, @org.jetbrains.annotations.Nullable ServerPlayer player) {
        return isArchipelagoCompass(compass, player);
    }

    public static boolean isBiomeCompass(ItemStack compass, @org.jetbrains.annotations.Nullable ServerPlayer player) {
        return isArchipelagoCompass(compass, player);
    }

    // Keep at most one Archipelago Compass in player inventory
    public static void cleanCompasses(ServerPlayer player) {
        int count = 0;
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack item = inv.getItem(i);
            if (!item.is(Items.COMPASS)) continue;

            if (isArchipelagoCompass(item, player)) {
                count++;
                if (count > 1) {
                    inv.setItem(i, ItemStack.EMPTY);
                }
            }
        }
    }

    // Ensures the player has the Archipelago Compass in their inventory if any compass reward is unlocked
    public static void ensurePlayerHasCompasses(ServerPlayer player) {
        if (player == null) return;
        Map<Identifier, CompassReward> unlocked = player.getData(APAttachmentTypes.AP_PLAYER).getUnlockedCompassRewards();
        if (unlocked.isEmpty()) return;

        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (isArchipelagoCompass(stack, player)) {
                return; // Player already has a compass
            }
        }

        CompassReward lastReward = null;
        for (CompassReward reward : unlocked.values()) {
            lastReward = reward;
        }

        if (lastReward != null) {
            ItemStack compass = CompassReward.DEFAULT_COMPASS.create().copy();
            updateCompassLocation(lastReward, player, compass);
            Utils.giveItemToPlayer(player, compass);
        }
    }

    // refresh all compasses in player inventory
    public static void refreshCompasses(ServerPlayer player) {
        player.getInventory().forEach((item) -> {
            if (!item.is(Items.COMPASS)) return;
            if (!item.has(DataComponents.CUSTOM_DATA)) return;
            CompoundTag nbt = item.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            Optional<CompassReward> structure = nbt.read("target", CompassReward.CODEC, player.registryAccess().createSerializationContext(NbtOps.INSTANCE));
            if (structure.isEmpty()) return;

            updateCompassLocation(structure.get(), player, item);
        });
    }

    private record Tier(APTier tier, ResourceKey<APItem> key, int tierIndex) {

    }
}
