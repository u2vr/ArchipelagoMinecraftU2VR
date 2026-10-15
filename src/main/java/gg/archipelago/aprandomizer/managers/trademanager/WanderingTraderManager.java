package gg.archipelago.aprandomizer.managers.trademanager;

import gg.archipelago.aprandomizer.APRandomizer;
import gg.archipelago.aprandomizer.SlotData;
import gg.archipelago.aprandomizer.ap.APClient;
import gg.archipelago.aprandomizer.ap.storage.APMCData;
import gg.archipelago.aprandomizer.managers.advancementmanager.AdvancementManager;
import io.github.archipelagomw.parts.NetworkItem;
import io.github.archipelagomw.parts.NetworkSlot;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.ItemLike;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class WanderingTraderManager {

    private static final Logger LOGGER = LogManager.getLogger();

    public static final long BASE_LOCATION_ID = 139L;
    public static final int MAX_TRADES = 20;

    private static final Map<Long, NetworkItem> SCOUTED_ITEMS = new ConcurrentHashMap<>();

    public record TradeCost(ItemCost costA, Optional<ItemCost> costB) {}

    public record TradeCostDef(Supplier<ItemLike> itemA, int countA, Supplier<ItemLike> itemB, int countB) {
        public TradeCostDef(Supplier<ItemLike> itemA, int countA) {
            this(itemA, countA, null, 0);
        }

        public TradeCost createCost() {
            ItemCost costA = new ItemCost(itemA.get().asItem(), countA);
            Optional<ItemCost> costB = (itemB != null && countB > 0)
                    ? Optional.of(new ItemCost(itemB.get().asItem(), countB))
                    : Optional.empty();
            return new TradeCost(costA, costB);
        }
    }

    // Tier 1 (Trades 1-5): Non-basic tools, mechanisms, crafted items, stackable resources
    private static final List<TradeCostDef> TIER_1_COSTS = List.of(
            new TradeCostDef(() -> Items.IRON_PICKAXE, 1),
            new TradeCostDef(() -> Items.IRON_SWORD, 1, () -> Items.EMERALD, 3),
            new TradeCostDef(() -> Items.IRON_AXE, 1, () -> Items.LAPIS_LAZULI, 4),
            new TradeCostDef(() -> Items.IRON_CHESTPLATE, 1),
            new TradeCostDef(() -> Items.SHIELD, 1, () -> Items.IRON_INGOT, 4),
            new TradeCostDef(() -> Items.BOW, 1, () -> Items.ARROW, 16),
            new TradeCostDef(() -> Items.SHEARS, 1, () -> Items.IRON_INGOT, 6),
            new TradeCostDef(() -> Items.FLINT_AND_STEEL, 1, () -> Items.IRON_INGOT, 8),
            new TradeCostDef(() -> Items.COMPASS, 1, () -> Items.REDSTONE, 8),
            new TradeCostDef(() -> Items.CLOCK, 1),
            new TradeCostDef(() -> Items.SPYGLASS, 1),
            new TradeCostDef(() -> Items.FISHING_ROD, 1, () -> Items.EMERALD, 4),
            new TradeCostDef(() -> Items.LEAD, 2, () -> Items.EMERALD, 3),
            new TradeCostDef(() -> Items.NAME_TAG, 1, () -> Items.EMERALD, 4),
            new TradeCostDef(() -> Items.PISTON, 2, () -> Items.REDSTONE, 8),
            new TradeCostDef(() -> Items.CAULDRON, 1, () -> Items.IRON_INGOT, 4),
            new TradeCostDef(() -> Items.BOOKSHELF, 2, () -> Items.LAPIS_LAZULI, 6),
            new TradeCostDef(() -> Items.IRON_INGOT, 12),
            new TradeCostDef(() -> Items.GOLD_INGOT, 8),
            new TradeCostDef(() -> Items.COPPER_BLOCK.weathering().unaffected(), 4, () -> Items.IRON_INGOT, 4),
            new TradeCostDef(() -> Items.EMERALD, 8),
            new TradeCostDef(() -> Items.LAPIS_LAZULI, 16),
            new TradeCostDef(() -> Items.REDSTONE, 24),
            new TradeCostDef(() -> Items.GOLDEN_APPLE, 1, () -> Items.GOLD_INGOT, 4),
            new TradeCostDef(() -> Items.HONEY_BOTTLE, 4),
            new TradeCostDef(() -> Items.HONEYCOMB, 6, () -> Items.EMERALD, 2),
            new TradeCostDef(() -> Items.TNT, 4, () -> Items.REDSTONE, 8),
            new TradeCostDef(() -> Items.AMETHYST_SHARD, 8, () -> Items.EMERALD, 3),
            new TradeCostDef(() -> Items.SADDLE, 1, () -> Items.EMERALD, 4),
            new TradeCostDef(() -> Items.PUMPKIN_PIE, 4, () -> Items.EMERALD, 3),
            new TradeCostDef(() -> Items.CAKE, 1, () -> Items.EMERALD, 4),
            new TradeCostDef(() -> Items.TARGET, 2, () -> Items.REDSTONE, 6),
            new TradeCostDef(() -> Items.NOTE_BLOCK, 4, () -> Items.REDSTONE, 8),
            new TradeCostDef(() -> Items.CHISELED_BOOKSHELF, 2, () -> Items.EMERALD, 4),
            new TradeCostDef(() -> Items.LECTERN, 1, () -> Items.BOOK, 2)
    );

    // Tier 2 (Trades 6-12): Diamond tools, mid-game mechanisms, nether materials, rare drops
    private static final List<TradeCostDef> TIER_2_COSTS = List.of(
            new TradeCostDef(() -> Items.DIAMOND_SWORD, 1),
            new TradeCostDef(() -> Items.DIAMOND_PICKAXE, 1, () -> Items.EMERALD, 4),
            new TradeCostDef(() -> Items.DIAMOND_AXE, 1, () -> Items.LAPIS_LAZULI, 8),
            new TradeCostDef(() -> Items.DIAMOND_SHOVEL, 1, () -> Items.DIAMOND, 1),
            new TradeCostDef(() -> Items.DIAMOND_HELMET, 1),
            new TradeCostDef(() -> Items.DIAMOND_BOOTS, 1),
            new TradeCostDef(() -> Items.CROSSBOW, 1, () -> Items.FIREWORK_ROCKET, 8),
            new TradeCostDef(() -> Items.HOPPER, 2, () -> Items.IRON_INGOT, 8),
            new TradeCostDef(() -> Items.DISPENSER, 2, () -> Items.REDSTONE, 12),
            new TradeCostDef(() -> Items.OBSERVER, 3, () -> Items.QUARTZ, 6),
            new TradeCostDef(() -> Items.STICKY_PISTON, 3, () -> Items.SLIME_BALL, 3),
            new TradeCostDef(() -> Items.CRAFTER, 1, () -> Items.REDSTONE, 8),
            new TradeCostDef(() -> Items.BREWING_STAND, 1, () -> Items.BLAZE_POWDER, 4),
            new TradeCostDef(() -> Items.ANVIL, 1),
            new TradeCostDef(() -> Items.DAYLIGHT_DETECTOR, 2, () -> Items.QUARTZ, 6),
            new TradeCostDef(() -> Items.JUKEBOX, 1, () -> Items.DIAMOND, 1),
            new TradeCostDef(() -> Items.DIAMOND, 3, () -> Items.EMERALD, 6),
            new TradeCostDef(() -> Items.IRON_BLOCK, 3),
            new TradeCostDef(() -> Items.GOLD_BLOCK, 2, () -> Items.EMERALD, 4),
            new TradeCostDef(() -> Items.EMERALD_BLOCK, 2),
            new TradeCostDef(() -> Items.ENDER_PEARL, 4, () -> Items.BLAZE_POWDER, 4),
            new TradeCostDef(() -> Items.BLAZE_ROD, 4, () -> Items.NETHER_BRICK, 8),
            new TradeCostDef(() -> Items.QUARTZ_BLOCK, 6, () -> Items.GOLD_INGOT, 6),
            new TradeCostDef(() -> Items.MAGMA_CREAM, 4, () -> Items.BLAZE_ROD, 2),
            new TradeCostDef(() -> Items.FIRE_CHARGE, 8, () -> Items.GUNPOWDER, 6),
            new TradeCostDef(() -> Items.OBSIDIAN, 8, () -> Items.DIAMOND, 1),
            new TradeCostDef(() -> Items.NAUTILUS_SHELL, 1, () -> Items.PRISMARINE_SHARD, 8),
            new TradeCostDef(() -> Items.PRISMARINE_CRYSTALS, 8, () -> Items.PRISMARINE_SHARD, 8),
            new TradeCostDef(() -> Items.GOLDEN_CARROT, 8, () -> Items.GOLD_INGOT, 6),
            new TradeCostDef(() -> Items.GLISTERING_MELON_SLICE, 8, () -> Items.GOLD_INGOT, 6),
            new TradeCostDef(() -> Items.FERMENTED_SPIDER_EYE, 4, () -> Items.SUGAR, 8),
            new TradeCostDef(() -> Items.SLIME_BALL, 6, () -> Items.EMERALD, 4),
            new TradeCostDef(() -> Items.PHANTOM_MEMBRANE, 4, () -> Items.EMERALD, 4),
            new TradeCostDef(() -> Items.TURTLE_SCUTE, 2, () -> Items.EMERALD, 4),
            new TradeCostDef(() -> Items.RABBIT_FOOT, 2, () -> Items.GOLD_INGOT, 6),
            new TradeCostDef(() -> Items.AMETHYST_BLOCK, 4, () -> Items.DIAMOND, 1),
            new TradeCostDef(() -> Items.TRIAL_KEY, 1, () -> Items.COPPER_BLOCK.weathering().unaffected(), 4),
            new TradeCostDef(() -> Items.WIND_CHARGE, 12, () -> Items.BREEZE_ROD, 1)
    );

    // Tier 3 (Trades 13-20): Diamond armor, high-end artifacts, rare exploration/boss drops
    private static final List<TradeCostDef> TIER_3_COSTS = List.of(
            new TradeCostDef(() -> Items.DIAMOND_CHESTPLATE, 1, () -> Items.EMERALD, 6),
            new TradeCostDef(() -> Items.DIAMOND_LEGGINGS, 1, () -> Items.EMERALD, 6),
            new TradeCostDef(() -> Items.DIAMOND_BLOCK, 1, () -> Items.EMERALD_BLOCK, 2),
            new TradeCostDef(() -> Items.GHAST_TEAR, 2, () -> Items.QUARTZ_BLOCK, 4),
            new TradeCostDef(() -> Items.HEART_OF_THE_SEA, 1, () -> Items.NAUTILUS_SHELL, 2),
            new TradeCostDef(() -> Items.WITHER_SKELETON_SKULL, 1, () -> Items.DIAMOND, 2),
            new TradeCostDef(() -> Items.CRYING_OBSIDIAN, 6, () -> Items.ENDER_PEARL, 4),
            new TradeCostDef(() -> Items.SEA_LANTERN, 4, () -> Items.PRISMARINE_CRYSTALS, 8),
            new TradeCostDef(() -> Items.ENDER_EYE, 4, () -> Items.DIAMOND, 2),
            new TradeCostDef(() -> Items.RECOVERY_COMPASS, 1, () -> Items.ECHO_SHARD, 2),
            new TradeCostDef(() -> Items.ECHO_SHARD, 3, () -> Items.AMETHYST_BLOCK, 2),
            new TradeCostDef(() -> Items.BREEZE_ROD, 2, () -> Items.OMINOUS_TRIAL_KEY, 1),
            new TradeCostDef(() -> Items.OMINOUS_TRIAL_KEY, 1, () -> Items.DIAMOND, 2),
            new TradeCostDef(() -> Items.TRIDENT, 1, () -> Items.HEART_OF_THE_SEA, 1),
            new TradeCostDef(() -> Items.MUSIC_DISC_CAT, 1, () -> Items.DIAMOND, 2),
            new TradeCostDef(() -> Items.MUSIC_DISC_PIGSTEP, 1, () -> Items.GOLD_BLOCK, 2),
            new TradeCostDef(() -> Items.MUSIC_DISC_RELIC, 1, () -> Items.EMERALD_BLOCK, 2),
            new TradeCostDef(() -> Items.MUSIC_DISC_OTHERSIDE, 1, () -> Items.DIAMOND, 2),
            new TradeCostDef(() -> Items.CHIPPED_ANVIL, 1, () -> Items.IRON_BLOCK, 4),
            new TradeCostDef(() -> Items.GOLDEN_APPLE, 3, () -> Items.DIAMOND, 3),
            new TradeCostDef(() -> Items.GLOWSTONE, 8, () -> Items.GOLD_BLOCK, 3),
            new TradeCostDef(() -> Items.REDSTONE_BLOCK, 6, () -> Items.LAPIS_LAZULI, 24),
            new TradeCostDef(() -> Items.DIAMOND, 6, () -> Items.EMERALD, 12),
            new TradeCostDef(() -> Items.QUARTZ, 24, () -> Items.BLAZE_ROD, 4),
            new TradeCostDef(() -> Items.HONEY_BLOCK, 4, () -> Items.SLIME_BALL, 6)
    );

    public static TradeCostDef getTradeCostDef(int tradeIndex) {
        long seed = 0L;
        if (APRandomizer.getApmcData() != null) {
            seed = APRandomizer.getApmcData().world_seed;
        }

        long tradeSeed = seed ^ ((long) tradeIndex * 0x9E3779B97F4A7C15L + 0xBF58476D1CE4E5B9L);
        Random rand = new Random(tradeSeed);

        List<TradeCostDef> pool;
        if (tradeIndex <= 5) {
            pool = TIER_1_COSTS;
        } else if (tradeIndex <= 12) {
            pool = TIER_2_COSTS;
        } else {
            pool = TIER_3_COSTS;
        }

        return pool.get(rand.nextInt(pool.size()));
    }

    public static TradeCost getTradeCost(int tradeIndex) {
        return getTradeCostDef(tradeIndex).createCost();
    }

    public static long getTraderLocationId(int tradeIndex) {
        return 138L + tradeIndex;
    }

    public static int getTraderIndex(long locationId) {
        return (int) (locationId - 138L);
    }

    public static boolean isTraderLocation(long locationId) {
        return locationId >= BASE_LOCATION_ID && locationId < BASE_LOCATION_ID + MAX_TRADES;
    }

    public static int getConfiguredTradesCount() {
        if (APRandomizer.getAP() != null) {
            SlotData slotData = APRandomizer.getAP().getSlotData();
            if (slotData != null) {
                return slotData.getWanderingTraderTrades();
            }
        }
        APMCData apmcData = APRandomizer.getApmcData();
        if (apmcData != null) {
            return apmcData.getWanderingTraderTrades();
        }
        return 0;
    }

    public static boolean hasUncompletedTrades() {
        int configuredCount = getConfiguredTradesCount();
        if (configuredCount <= 0) return false;
        AdvancementManager advManager = APRandomizer.getAdvancementManager();
        if (advManager == null) return true;
        for (int i = 1; i <= configuredCount; i++) {
            if (!advManager.hasAdvancement(getTraderLocationId(i))) {
                return true;
            }
        }
        return false;
    }

    public static void onScoutReceived(List<NetworkItem> networkItems) {
        if (networkItems == null) return;
        for (NetworkItem item : networkItems) {
            if (isTraderLocation(item.locationID)) {
                SCOUTED_ITEMS.put(item.locationID, item);
                LOGGER.info("Scouted Wandering Trader trade {} (ID {}): {} for player {}",
                        getTraderIndex(item.locationID), item.locationID, item.itemName, item.playerName);
            }
        }
    }

    public static void requestScouts() {
        APClient apClient = APRandomizer.getAP();
        if (apClient == null) return;

        int configuredCount = getConfiguredTradesCount();
        if (configuredCount <= 0) return;

        ArrayList<Long> locationIds = new ArrayList<>();
        for (int i = 1; i <= configuredCount; i++) {
            locationIds.add(getTraderLocationId(i));
        }

        try {
            apClient.scoutLocations(locationIds);
            LOGGER.info("Requested scout for {} Wandering Trader trade locations.", locationIds.size());
        } catch (Exception e) {
            LOGGER.warn("Failed to scout Wandering Trader trade locations: {}", e.getMessage());
        }
    }

    public static long getOfferLocationId(MerchantOffer offer) {
        if (offer == null) return -1L;
        ItemStack result = offer.getResult();
        if (result.has(DataComponents.CUSTOM_DATA)) {
            CompoundTag tag = result.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            return tag.getLongOr("ap_location_id", -1L);
        }
        return -1L;
    }

    public static boolean isForbiddenItemStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        Item item = stack.getItem();
        if (item instanceof BucketItem || item == Items.MILK_BUCKET) {
            return true;
        }
        String path = BuiltInRegistries.ITEM.getKey(item).getPath().toLowerCase();
        if (path.contains("bucket") || path.contains("trap") || path.contains("curse")) {
            return true;
        }
        String name = stack.getHoverName().getString().toLowerCase();
        return name.contains("bucket") || name.contains("trap") || name.contains("curse");
    }

    public static boolean isForbiddenArchipelagoItem(NetworkItem scout) {
        if (scout == null) return false;
        if ((scout.flags & io.github.archipelagomw.flags.NetworkItem.TRAP) != 0) {
            return true;
        }
        if (scout.itemName != null) {
            String name = scout.itemName.toLowerCase();
            if (name.contains("trap") || name.contains("curse") || name.contains("bucket")) {
                return true;
            }
            if ("Disable Day Monster Ignition".equalsIgnoreCase(scout.itemName)
                    || "Disable Light Check for Monster Spawn".equalsIgnoreCase(scout.itemName)
                    || "Random Trap".equalsIgnoreCase(scout.itemName)) {
                return true;
            }
        }
        return false;
    }

    public static void attachArchipelagoTrades(WanderingTrader trader, Player player) {
        if (trader.level().isClientSide()) return;

        AdvancementManager advManager = APRandomizer.getAdvancementManager();
        MerchantOffers offers = trader.getOffers();

        // Remove any offers that are already earned/checked, or forbidden (buckets, traps, curses)
        offers.removeIf(offer -> {
            long locId = getOfferLocationId(offer);
            if (locId > 0) {
                if (advManager != null && advManager.hasAdvancement(locId)) {
                    return true;
                }
                NetworkItem scout = SCOUTED_ITEMS.get(locId);
                return scout != null && isForbiddenArchipelagoItem(scout);
            } else {
                return isForbiddenItemStack(offer.getResult());
            }
        });

        int configuredCount = getConfiguredTradesCount();
        if (configuredCount <= 0) return;

        int insertIndex = 0;
        for (int i = 1; i <= configuredCount; i++) {
            long locId = getTraderLocationId(i);
            if (advManager != null && advManager.hasAdvancement(locId)) {
                continue;
            }

            NetworkItem scout = SCOUTED_ITEMS.get(locId);
            if (scout != null && isForbiddenArchipelagoItem(scout)) {
                continue;
            }

            // Check if this offer already exists in trader
            int existingIndex = -1;
            for (int j = 0; j < offers.size(); j++) {
                if (getOfferLocationId(offers.get(j)) == locId) {
                    existingIndex = j;
                    break;
                }
            }

            TradeCost tradeCost = getTradeCost(i);
            ItemStack resultStack = createArchipelagoTradeStack(locId, i);

            if (existingIndex >= 0) {
                MerchantOffer existing = offers.get(existingIndex);
                if (existing.getUses() < existing.getMaxUses()) {
                    MerchantOffer updated = new MerchantOffer(
                            tradeCost.costA(),
                            tradeCost.costB(),
                            resultStack,
                            1, 1, 0.0f
                    );
                    offers.set(existingIndex, updated);
                }
            } else {
                MerchantOffer newOffer = new MerchantOffer(
                        tradeCost.costA(),
                        tradeCost.costB(),
                        resultStack,
                        1, 1, 0.0f
                );
                offers.add(insertIndex++, newOffer);
            }
        }
    }

    public static ItemStack createArchipelagoTradeStack(long locId, int tradeIndex) {
        ItemStack stack = new ItemStack(Items.ECHO_SHARD);

        CompoundTag tag = new CompoundTag();
        tag.putLong("ap_location_id", locId);
        tag.putInt("ap_trade_index", tradeIndex);
        tag.putBoolean("ap_trade", true);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));

        stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);

        NetworkItem scout = SCOUTED_ITEMS.get(locId);
        if (scout != null) {
            String itemName = scout.itemName != null && !scout.itemName.isEmpty() ? scout.itemName : "Archipelago Item";

            ChatFormatting color = ChatFormatting.WHITE;
            if ((scout.flags & io.github.archipelagomw.flags.NetworkItem.ADVANCEMENT) != 0) {
                color = ChatFormatting.GOLD;
            } else if ((scout.flags & io.github.archipelagomw.flags.NetworkItem.USEFUL) != 0) {
                color = ChatFormatting.AQUA;
            } else if ((scout.flags & io.github.archipelagomw.flags.NetworkItem.TRAP) != 0) {
                color = ChatFormatting.RED;
            }

            stack.set(DataComponents.CUSTOM_NAME, Component.literal(itemName).withStyle(color, ChatFormatting.BOLD));

            APClient client = APRandomizer.getAP();
            String playerName = scout.playerName;
            String gameName = "Archipelago";
            if (client != null && client.getSlotInfo() != null) {
                NetworkSlot slot = client.getSlotInfo().get(scout.playerID);
                if (slot != null) {
                    if (playerName == null || playerName.isEmpty()) playerName = slot.name;
                    if (slot.game != null && !slot.game.isEmpty()) gameName = slot.game;
                }
            }
            if (playerName == null || playerName.isEmpty()) {
                playerName = "Player " + scout.playerID;
            }

            List<Component> lore = new ArrayList<>();
            lore.add(Component.literal("Recipient: ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(playerName).withStyle(ChatFormatting.AQUA)));
            lore.add(Component.literal("Game: ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(gameName).withStyle(ChatFormatting.GREEN)));

            if ((scout.flags & io.github.archipelagomw.flags.NetworkItem.ADVANCEMENT) != 0) {
                lore.add(Component.literal("★ Progression Item").withStyle(ChatFormatting.GOLD));
            } else if ((scout.flags & io.github.archipelagomw.flags.NetworkItem.USEFUL) != 0) {
                lore.add(Component.literal("◆ Useful Item").withStyle(ChatFormatting.AQUA));
            } else if ((scout.flags & io.github.archipelagomw.flags.NetworkItem.TRAP) != 0) {
                lore.add(Component.literal("⚠ Trap Item").withStyle(ChatFormatting.RED));
            } else {
                lore.add(Component.literal("○ Common Item").withStyle(ChatFormatting.WHITE));
            }

            lore.add(Component.literal("Trade Location: Wandering Trader #" + tradeIndex).withStyle(ChatFormatting.DARK_GRAY));
            lore.add(Component.literal("Archipelago Multiworld").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
            stack.set(DataComponents.LORE, new ItemLore(lore));
        } else {
            stack.set(DataComponents.CUSTOM_NAME,
                    Component.literal("Archipelago Trade #" + tradeIndex).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));

            List<Component> lore = new ArrayList<>();
            lore.add(Component.literal("Multiworld Item Check").withStyle(ChatFormatting.YELLOW));
            lore.add(Component.literal("Trade requested items to check this location.").withStyle(ChatFormatting.GRAY));
            lore.add(Component.literal("Trade Location: Wandering Trader #" + tradeIndex).withStyle(ChatFormatting.DARK_GRAY));
            lore.add(Component.literal("Archipelago Multiworld").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
            stack.set(DataComponents.LORE, new ItemLore(lore));
        }

        return stack;
    }

    public static void handleTrade(MerchantOffer offer, Player player) {
        long locId = getOfferLocationId(offer);
        if (isTraderLocation(locId)) {
            AdvancementManager advManager = APRandomizer.getAdvancementManager();
            if (advManager != null && !advManager.hasAdvancement(locId)) {
                advManager.addAdvancement(locId);
            }

            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.playSound(SoundEvents.PLAYER_LEVELUP, 1.0f, 1.0f);
                NetworkItem item = SCOUTED_ITEMS.get(locId);
                int tradeIndex = getTraderIndex(locId);
                if (item != null && item.itemName != null) {
                    String recipient = item.playerName != null ? item.playerName : "Multiworld";
                    serverPlayer.sendSystemMessage(Component.literal("§6[Archipelago] §aBought §f" + item.itemName + " §afor §e" + recipient + " §a(Trade #" + tradeIndex + ")!"), true);
                } else {
                    serverPlayer.sendSystemMessage(Component.literal("§6[Archipelago] §aCompleted Wandering Trader Trade #" + tradeIndex + "!"), true);
                }
            }
        }
    }
}
