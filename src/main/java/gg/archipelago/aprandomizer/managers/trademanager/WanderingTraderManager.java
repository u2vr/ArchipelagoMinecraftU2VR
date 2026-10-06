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
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WanderingTraderManager {

    private static final Logger LOGGER = LogManager.getLogger();

    public static final long BASE_LOCATION_ID = 139L;
    public static final int MAX_TRADES = 20;

    private static final Map<Long, NetworkItem> SCOUTED_ITEMS = new ConcurrentHashMap<>();

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

    public static void attachArchipelagoTrades(WanderingTrader trader, Player player) {
        if (trader.level().isClientSide()) return;

        int configuredCount = getConfiguredTradesCount();
        if (configuredCount <= 0) return;

        AdvancementManager advManager = APRandomizer.getAdvancementManager();
        MerchantOffers offers = trader.getOffers();

        // Remove any offers that are already earned/checked
        offers.removeIf(offer -> {
            long locId = getOfferLocationId(offer);
            return locId > 0 && advManager != null && advManager.hasAdvancement(locId);
        });

        int insertIndex = 0;
        for (int i = 1; i <= configuredCount; i++) {
            long locId = getTraderLocationId(i);
            if (advManager != null && advManager.hasAdvancement(locId)) {
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

            int emeraldCost = Math.min(i, 10);
            ItemStack resultStack = createArchipelagoTradeStack(locId, i);

            if (existingIndex >= 0) {
                MerchantOffer existing = offers.get(existingIndex);
                if (existing.getUses() < existing.getMaxUses()) {
                    MerchantOffer updated = new MerchantOffer(
                            new ItemCost(Items.EMERALD, emeraldCost),
                            resultStack,
                            1, 1, 0.0f
                    );
                    offers.set(existingIndex, updated);
                }
            } else {
                MerchantOffer newOffer = new MerchantOffer(
                        new ItemCost(Items.EMERALD, emeraldCost),
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
            lore.add(Component.literal("Trade emeralds to check this location.").withStyle(ChatFormatting.GRAY));
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
