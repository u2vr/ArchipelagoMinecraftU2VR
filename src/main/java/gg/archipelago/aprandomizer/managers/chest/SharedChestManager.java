package gg.archipelago.aprandomizer.managers.chest;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import gg.archipelago.aprandomizer.APRandomizer;
import gg.archipelago.aprandomizer.ap.APClient;
import gg.archipelago.aprandomizer.data.WorldData;
import gg.archipelago.aprandomizer.network.RecipientEntry;
import gg.archipelago.aprandomizer.network.SyncChestRecipientsPayload;
import io.github.archipelagomw.events.RetrievedEvent;
import io.github.archipelagomw.events.SetReplyEvent;
import io.github.archipelagomw.network.client.SetPacket;
import io.github.archipelagomw.parts.NetworkSlot;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.ItemStackWithSlot;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = APRandomizer.MODID)
public class SharedChestManager {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final String STORAGE_KEY = "mc_shared_chest";
    public static final String LOCK_KEY = "mc_shared_chest_lock";
    public static final long LOCK_EXPIRY_MS = 45_000L;
    public static final long HEARTBEAT_INTERVAL_MS = 15_000L;
    public static final long AFK_TIMEOUT_MS = 120_000L;
    public static final long PENDING_TIMEOUT_MS = 4_000L;

    public static final String ONLINE_KEY_PREFIX = "mc_chest_online_";
    public static final String TRANSFER_KEY_PREFIX = "mc_chest_tx_";
    public static final String ACK_KEY_PREFIX = "mc_chest_ack_";
    public static final long ONLINE_TIMEOUT_MS = 15_000L;
    public static final long TRANSFER_TIMEOUT_MS = 4_000L;

    public static class LockData {
        public String player;
        public String uuid;
        public String token;
        public long timestamp;

        public LockData() {}

        public LockData(String player, String uuid, String token, long timestamp) {
            this.player = player;
            this.uuid = uuid;
            this.token = token;
            this.timestamp = timestamp;
        }

        public boolean isExpired(long now) {
            return now - timestamp > LOCK_EXPIRY_MS;
        }
    }

    public record PendingTransfer(
            String txId,
            int targetSlotId,
            String targetName,
            UUID senderUuid,
            long startTime
    ) {}

    private static final SharedChestContainer CONTAINER = new SharedChestContainer();
    private static final Gson GSON = new Gson();

    private static MinecraftServer server;
    private static WorldData worldData;
    private static APClient apClient;

    private static final Map<Integer, Long> ONLINE_PLAYERS = new ConcurrentHashMap<>();
    private static final Set<String> PROCESSED_TX_IDS = Collections.synchronizedSet(new HashSet<>());
    private static PendingTransfer pendingTransfer = null;

    private static long lastPresenceTime = 0L;
    private static int tickCounter = 0;

    public static SharedChestContainer getContainer() {
        return CONTAINER;
    }

    public static boolean isTransferPending() {
        return pendingTransfer != null;
    }

    public static boolean isStartingSharedChestEnabled() {
        if (APRandomizer.isConnected() && APRandomizer.getAP() != null && APRandomizer.getAP().getSlotData() != null) {
            var sd = APRandomizer.getAP().getSlotData();
            if (sd.startingSharedChest != null) return sd.isStartingSharedChestEnabled();
        }
        if (APRandomizer.getApmcData() != null && APRandomizer.getApmcData().starting_shared_chest != null) {
            return APRandomizer.getApmcData().isStartingSharedChestEnabled();
        }
        return gg.archipelago.aprandomizer.managers.recipemanager.RecipeTreeManager.isSettingEnabled("starting_shared_chest", true);
    }

    public static void init(MinecraftServer s, WorldData wd) {
        server = s;
        worldData = wd;
        if (worldData != null) {
            String savedJson = worldData.getSharedChestJson();
            if (savedJson != null && !savedJson.isBlank() && !savedJson.equals("[]")) {
                applyJsonToContainer(savedJson);
                LOGGER.info("Loaded shared chest from local world data ({} items)", countNonEmpty());
            }
        }
    }

    public static void onConnected(APClient client) {
        apClient = client;
        if (apClient != null && apClient.isConnected()) {
            int mySlot = apClient.getSlot();
            List<String> keys = new ArrayList<>();
            keys.add(TRANSFER_KEY_PREFIX + mySlot);
            keys.add(ACK_KEY_PREFIX + mySlot);

            Map<Integer, NetworkSlot> slotInfo = apClient.getSlotInfo();
            if (slotInfo != null) {
                for (int slotId : slotInfo.keySet()) {
                    if (slotId != mySlot) {
                        keys.add(ONLINE_KEY_PREFIX + slotId);
                    }
                }
            }

            apClient.dataStorageSetNotify(keys);
            apClient.dataStorageGet(keys);
            sendPresence();
            LOGGER.info("Shared chest initialized Archipelago DataStorage keys for slot {}", mySlot);
        }
    }

    public static void onContainerChanged() {
        saveWorldData();
    }

    public static void onPlayerClosed(ContainerUser user) {
        saveWorldData();
    }

    public static void onPlayerClosed() {
        saveWorldData();
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (server != null && server.getPlayerList().getPlayerCount() <= 1) {
            sendOfflinePresence();
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        tick();
    }

    public static void tick() {
        long now = System.currentTimeMillis();
        tickCounter++;

        // Send presence heartbeat every 5 seconds (100 ticks)
        if (now - lastPresenceTime > 5_000L) {
            lastPresenceTime = now;
            sendPresence();
        }

        // Check pending transfer timeout
        if (pendingTransfer != null && (now - pendingTransfer.startTime() > TRANSFER_TIMEOUT_MS)) {
            PendingTransfer pt = pendingTransfer;
            pendingTransfer = null;

            if (server != null) {
                ServerPlayer sender = server.getPlayerList().getPlayer(pt.senderUuid());
                if (sender != null) {
                    sender.sendSystemMessage(Component.literal("§6[Общий сундук] §cИгрок §e" + pt.targetName() + " §cне отвечает или вышел из сети!"), true);
                    sender.playSound(SoundEvents.CHEST_LOCKED, 0.8f, 1.0f);
                }
            }
        }

        // Clean expired online players every 2 seconds
        if (tickCounter % 40 == 0) {
            boolean changed = false;
            for (Map.Entry<Integer, Long> entry : ONLINE_PLAYERS.entrySet()) {
                if (now - entry.getValue() > ONLINE_TIMEOUT_MS) {
                    ONLINE_PLAYERS.remove(entry.getKey());
                    changed = true;
                }
            }
            if (changed) {
                syncRecipientsToAllOpen();
            }
        }
    }

    private static void sendPresence() {
        if (apClient != null && apClient.isConnected()) {
            int mySlot = apClient.getSlot();
            String myName = apClient.getAlias() != null && !apClient.getAlias().isBlank() ? apClient.getAlias() : apClient.getMyName();
            JsonObject obj = new JsonObject();
            obj.addProperty("slot", mySlot);
            obj.addProperty("name", myName);
            obj.addProperty("time", System.currentTimeMillis());

            SetPacket packet = new SetPacket(ONLINE_KEY_PREFIX + mySlot, "");
            packet.addDataStorageOperation(SetPacket.Operation.REPLACE, GSON.toJson(obj));
            packet.want_reply = false;
            apClient.dataStorageSet(packet);
        }
    }

    private static void sendOfflinePresence() {
        if (apClient != null && apClient.isConnected()) {
            int mySlot = apClient.getSlot();
            SetPacket packet = new SetPacket(ONLINE_KEY_PREFIX + mySlot, "");
            packet.addDataStorageOperation(SetPacket.Operation.REPLACE, "");
            packet.want_reply = false;
            apClient.dataStorageSet(packet);
        }
    }

    public static boolean isSlotOnline(int slotId) {
        Long time = ONLINE_PLAYERS.get(slotId);
        return time != null && (System.currentTimeMillis() - time <= ONLINE_TIMEOUT_MS);
    }

    public static List<RecipientEntry> getRecipientEntries() {
        List<RecipientEntry> list = new ArrayList<>();
        if (apClient == null || !apClient.isConnected()) {
            return list;
        }

        int mySlot = apClient.getSlot();
        Map<Integer, NetworkSlot> slotInfo = apClient.getSlotInfo();
        if (slotInfo == null) return list;

        for (Map.Entry<Integer, NetworkSlot> entry : slotInfo.entrySet()) {
            int slotId = entry.getKey();
            if (slotId == mySlot) continue;

            NetworkSlot slot = entry.getValue();
            String name = slot.name != null ? slot.name : ("Slot " + slotId);
            boolean online = isSlotOnline(slotId);
            list.add(new RecipientEntry(slotId, name, online));
        }

        // Sort: online players first, then alphabetically
        list.sort((a, b) -> {
            if (a.isOnline() != b.isOnline()) {
                return a.isOnline() ? -1 : 1;
            }
            return a.name().compareToIgnoreCase(b.name());
        });

        return list;
    }

    public static void syncRecipientsToPlayer(ServerPlayer player) {
        if (player != null) {
            PacketDistributor.sendToPlayer(player, new SyncChestRecipientsPayload(getRecipientEntries()));
        }
    }

    public static void syncRecipientsToAllOpen() {
        if (server == null) return;
        List<RecipientEntry> entries = getRecipientEntries();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.containerMenu instanceof SharedChestMenu) {
                PacketDistributor.sendToPlayer(player, new SyncChestRecipientsPayload(entries));
            }
        }
    }

    public static void openChest(ServerPlayer player, @Nullable BlockPos pos) {
        if (pos == null || !(player.level().getBlockEntity(pos) instanceof SharedChestBlockEntity be)) {
            return;
        }

        player.openMenu(be);

        ServerLevel level = (ServerLevel) player.level();
        level.playSound(null, pos, SoundEvents.ENDER_CHEST_OPEN, SoundSource.BLOCKS, 0.5F, level.getRandom().nextFloat() * 0.1F + 0.9F);
        PiglinAi.angerNearbyPiglins(level, player, true);

        syncRecipientsToPlayer(player);
    }

    public static void dropChestContents(Level level, BlockPos pos) {
        Containers.dropContents(level, pos, CONTAINER);
        CONTAINER.clearContent();
        saveWorldData();
        refreshOpenMenus();
    }

    public static void handleSendItem(ServerPlayer player, int targetSlotId) {
        if (isTransferPending()) {
            player.sendSystemMessage(Component.literal("§e[!] Предыдущая передача еще обрабатывается..."), true);
            return;
        }

        ItemStack sendStack = CONTAINER.getItem(SharedChestContainer.SEND_SLOT);
        if (sendStack.isEmpty()) {
            player.sendSystemMessage(Component.literal("§c[!] Слот передачи пуст!"), true);
            player.playSound(SoundEvents.CHEST_LOCKED, 0.8f, 1.0f);
            return;
        }

        if (apClient == null || !apClient.isConnected()) {
            player.sendSystemMessage(Component.literal("§c[!] Нет подключения к Архипелагу!"), true);
            player.playSound(SoundEvents.CHEST_LOCKED, 0.8f, 1.0f);
            return;
        }

        int mySlot = apClient.getSlot();
        if (targetSlotId == mySlot || targetSlotId <= 0) {
            player.sendSystemMessage(Component.literal("§c[!] Нельзя отправить предмет себе!"), true);
            return;
        }

        if (!isSlotOnline(targetSlotId)) {
            player.sendSystemMessage(Component.literal("§c[!] Получатель не в сети!"), true);
            player.playSound(SoundEvents.CHEST_LOCKED, 0.8f, 1.0f);
            return;
        }

        String targetName = "Игрок " + targetSlotId;
        if (apClient.getSlotInfo() != null && apClient.getSlotInfo().containsKey(targetSlotId)) {
            targetName = apClient.getSlotInfo().get(targetSlotId).name;
        }

        String txId = UUID.randomUUID().toString();
        String itemJson = serializeItemStack(sendStack);

        JsonObject tx = new JsonObject();
        tx.addProperty("txId", txId);
        tx.addProperty("fromSlot", mySlot);
        tx.addProperty("fromName", player.getScoreboardName());
        tx.addProperty("toSlot", targetSlotId);
        tx.addProperty("item", itemJson);
        tx.addProperty("timestamp", System.currentTimeMillis());

        pendingTransfer = new PendingTransfer(txId, targetSlotId, targetName, player.getUUID(), System.currentTimeMillis());

        SetPacket packet = new SetPacket(TRANSFER_KEY_PREFIX + targetSlotId, "");
        packet.addDataStorageOperation(SetPacket.Operation.REPLACE, GSON.toJson(tx));
        packet.want_reply = false;
        apClient.dataStorageSet(packet);

        player.sendSystemMessage(Component.literal("§e[...] Передача предмета игроку " + targetName + "..."), true);
    }

    public static void handleArchipelagoRetrieved(RetrievedEvent event) {
        if (event == null || event.data == null) return;
        long now = System.currentTimeMillis();

        for (Map.Entry<String, Object> entry : event.data.entrySet()) {
            String key = entry.getKey();
            Object val = entry.getValue();

            if (key.startsWith(ONLINE_KEY_PREFIX)) {
                handlePresenceData(key, val, now);
            } else if (apClient != null && key.equals(TRANSFER_KEY_PREFIX + apClient.getSlot())) {
                handleIncomingTransfer(val);
            } else if (apClient != null && key.equals(ACK_KEY_PREFIX + apClient.getSlot())) {
                handleIncomingAck(val);
            }
        }
        syncRecipientsToAllOpen();
    }

    public static void handleArchipelagoSetReply(SetReplyEvent event) {
        if (event == null || event.key == null) return;
        String key = event.key;
        Object val = event.value;
        long now = System.currentTimeMillis();

        if (key.startsWith(ONLINE_KEY_PREFIX)) {
            handlePresenceData(key, val, now);
            syncRecipientsToAllOpen();
        } else if (apClient != null && key.equals(TRANSFER_KEY_PREFIX + apClient.getSlot())) {
            handleIncomingTransfer(val);
        } else if (apClient != null && key.equals(ACK_KEY_PREFIX + apClient.getSlot())) {
            handleIncomingAck(val);
        }
    }

    private static void handlePresenceData(String key, Object val, long now) {
        String slotStr = key.substring(ONLINE_KEY_PREFIX.length());
        try {
            int slotId = Integer.parseInt(slotStr);
            String json = extractJson(val);
            if (json != null && !json.isBlank() && !json.equals("{}")) {
                JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
                long time = obj.has("time") ? obj.get("time").getAsLong() : 0L;
                if (now - time <= ONLINE_TIMEOUT_MS) {
                    ONLINE_PLAYERS.put(slotId, time);
                    return;
                }
            }
            ONLINE_PLAYERS.remove(slotId);
        } catch (Exception ignored) {}
    }

    private static void handleIncomingTransfer(Object val) {
        String json = extractJson(val);
        if (json == null || json.isBlank() || json.equals("{}")) return;

        try {
            JsonObject tx = JsonParser.parseString(json).getAsJsonObject();
            String txId = tx.get("txId").getAsString();
            int fromSlot = tx.get("fromSlot").getAsInt();
            String fromName = tx.has("fromName") ? tx.get("fromName").getAsString() : ("Игрок " + fromSlot);
            int toSlot = tx.get("toSlot").getAsInt();

            if (apClient == null || toSlot != apClient.getSlot()) return;

            if (PROCESSED_TX_IDS.contains(txId)) {
                sendAck(fromSlot, txId, "OK");
                return;
            }

            ItemStack stack = deserializeItemStack(tx.get("item"));
            if (stack.isEmpty()) return;

            if (!canFitInReceivingSlots(stack)) {
                sendAck(fromSlot, txId, "FULL");
                return;
            }

            insertIntoReceivingSlots(stack);
            PROCESSED_TX_IDS.add(txId);
            saveWorldData();
            refreshOpenMenus();
            sendAck(fromSlot, txId, "OK");

            if (server != null) {
                for (ServerPlayer sp : server.getPlayerList().getPlayers()) {
                    sp.sendSystemMessage(Component.literal("§6[Общий сундук] §aПолучен предмет §f" + stack.getCount() + "x " + stack.getHoverName().getString() + " §aот игрока §e" + fromName + "§a!"), false);
                    sp.playSound(SoundEvents.PLAYER_LEVELUP, 0.7f, 1.2f);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to process incoming shared chest transfer", e);
        }
    }

    private static void handleIncomingAck(Object val) {
        String json = extractJson(val);
        if (json == null || json.isBlank() || json.equals("{}")) return;

        try {
            JsonObject ack = JsonParser.parseString(json).getAsJsonObject();
            String txId = ack.get("txId").getAsString();
            String status = ack.get("status").getAsString();

            if (pendingTransfer != null && pendingTransfer.txId().equals(txId)) {
                PendingTransfer pt = pendingTransfer;
                pendingTransfer = null;

                ServerPlayer sender = server != null ? server.getPlayerList().getPlayer(pt.senderUuid()) : null;

                if ("OK".equals(status)) {
                    // Destroy item in sender's transmission slot
                    CONTAINER.setItem(SharedChestContainer.SEND_SLOT, ItemStack.EMPTY);
                    saveWorldData();
                    refreshOpenMenus();

                    if (sender != null) {
                        sender.sendSystemMessage(Component.literal("§6[Общий сундук] §aПредмет успешно доставлен игроку §e" + pt.targetName() + "§a!"), true);
                        sender.playSound(SoundEvents.ARROW_HIT_PLAYER, 1.0f, 1.2f);
                    }
                } else if ("FULL".equals(status)) {
                    if (sender != null) {
                        sender.sendSystemMessage(Component.literal("§6[Общий сундук] §cУ игрока §e" + pt.targetName() + " §cпереполнен инвентарь приёма!"), true);
                        sender.playSound(SoundEvents.CHEST_LOCKED, 0.8f, 1.0f);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to process incoming shared chest ACK", e);
        }
    }

    private static void sendAck(int toSlot, String txId, String status) {
        if (apClient != null && apClient.isConnected()) {
            JsonObject ack = new JsonObject();
            ack.addProperty("txId", txId);
            ack.addProperty("status", status);
            ack.addProperty("fromSlot", apClient.getSlot());
            ack.addProperty("timestamp", System.currentTimeMillis());

            SetPacket packet = new SetPacket(ACK_KEY_PREFIX + toSlot, "");
            packet.addDataStorageOperation(SetPacket.Operation.REPLACE, GSON.toJson(ack));
            packet.want_reply = false;
            apClient.dataStorageSet(packet);
        }
    }

    public static boolean canFitInReceivingSlots(ItemStack stack) {
        if (stack.isEmpty()) return true;
        int remaining = stack.getCount();

        for (int i = 0; i < SharedChestContainer.RECEIVE_SLOTS; i++) {
            ItemStack slotStack = CONTAINER.getItem(i);
            if (slotStack.isEmpty()) {
                return true;
            } else if (ItemStack.isSameItemSameComponents(slotStack, stack)) {
                int space = slotStack.getMaxStackSize() - slotStack.getCount();
                if (space > 0) {
                    remaining -= space;
                    if (remaining <= 0) return true;
                }
            }
        }
        return false;
    }

    public static void insertIntoReceivingSlots(ItemStack stack) {
        if (stack.isEmpty()) return;
        ItemStack toInsert = stack.copy();

        // 1. Try stacking into existing stacks
        for (int i = 0; i < SharedChestContainer.RECEIVE_SLOTS; i++) {
            ItemStack slotStack = CONTAINER.getItem(i);
            if (!slotStack.isEmpty() && ItemStack.isSameItemSameComponents(slotStack, toInsert)) {
                int space = slotStack.getMaxStackSize() - slotStack.getCount();
                int add = Math.min(space, toInsert.getCount());
                if (add > 0) {
                    slotStack.grow(add);
                    toInsert.shrink(add);
                    CONTAINER.setItem(i, slotStack);
                    if (toInsert.isEmpty()) return;
                }
            }
        }

        // 2. Put into empty slots
        for (int i = 0; i < SharedChestContainer.RECEIVE_SLOTS; i++) {
            ItemStack slotStack = CONTAINER.getItem(i);
            if (slotStack.isEmpty()) {
                CONTAINER.setItem(i, toInsert.copy());
                return;
            }
        }
    }

    public static String serializeItemStack(ItemStack stack) {
        if (server == null || stack.isEmpty()) return "{}";
        try {
            RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, server.registryAccess());
            return ItemStack.CODEC.encodeStart(ops, stack).result().map(GSON::toJson).orElse("{}");
        } catch (Exception e) {
            LOGGER.error("Failed to serialize ItemStack", e);
            return "{}";
        }
    }

    public static ItemStack deserializeItemStack(JsonElement element) {
        if (server == null || element == null || element.isJsonNull()) return ItemStack.EMPTY;
        try {
            if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
                element = JsonParser.parseString(element.getAsString());
            }
            RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, server.registryAccess());
            return ItemStack.CODEC.parse(ops, element).result().orElse(ItemStack.EMPTY);
        } catch (Exception e) {
            LOGGER.error("Failed to deserialize ItemStack", e);
            return ItemStack.EMPTY;
        }
    }

    public static String serializeContainerToJson() {
        if (server == null) return "[]";
        List<ItemStackWithSlot> slots = new ArrayList<>();
        for (int i = 0; i < CONTAINER.getContainerSize(); i++) {
            ItemStack stack = CONTAINER.getItem(i);
            if (!stack.isEmpty()) {
                slots.add(new ItemStackWithSlot(i, stack.copy()));
            }
        }
        if (slots.isEmpty()) return "[]";
        try {
            RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, server.registryAccess());
            return ItemStackWithSlot.CODEC.listOf().encodeStart(ops, slots).result().map(GSON::toJson).orElse("[]");
        } catch (Exception e) {
            LOGGER.error("Failed to serialize shared chest items to JSON", e);
            return "[]";
        }
    }

    public static void applyJsonToContainer(String json) {
        if (json == null || json.isBlank() || json.equals("[]")) {
            CONTAINER.clearContent();
            refreshOpenMenus();
            return;
        }
        if (server == null) return;

        try {
            JsonElement element = JsonParser.parseString(json);
            RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, server.registryAccess());
            DataResult<List<ItemStackWithSlot>> result = ItemStackWithSlot.CODEC.listOf().parse(ops, element);
            List<ItemStackWithSlot> items = result.result().orElse(Collections.emptyList());

            CONTAINER.clearContent();
            for (ItemStackWithSlot item : items) {
                if (item.isValidInContainer(CONTAINER.getContainerSize())) {
                    CONTAINER.setItem(item.slot(), item.stack());
                }
            }
            refreshOpenMenus();
        } catch (Exception e) {
            LOGGER.error("Failed to parse shared chest JSON: {}", json, e);
        }
    }

    public static void saveWorldData() {
        if (worldData != null) {
            String json = serializeContainerToJson();
            worldData.setSharedChestJson(json);
        }
    }

    public static void refreshOpenMenus() {
        if (server == null) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.containerMenu instanceof SharedChestMenu menu && menu.getContainer() == CONTAINER) {
                menu.sendAllDataToRemote();
            }
        }
    }

    private static String extractJson(Object val) {
        if (val == null) return null;
        if (val instanceof String s) return s;
        if (val instanceof JsonElement je) {
            if (je.isJsonPrimitive() && je.getAsJsonPrimitive().isString()) {
                return je.getAsString();
            }
            return je.toString();
        }
        return GSON.toJson(val);
    }

    public static int countNonEmpty() {
        int count = 0;
        for (int i = 0; i < CONTAINER.getContainerSize(); i++) {
            if (!CONTAINER.getItem(i).isEmpty()) count++;
        }
        return count;
    }

    public static LockData parseLock(Object val) {
        String json = extractJson(val);
        if (json == null || json.isBlank() || json.equals("\"\"") || json.equals("{}") || json.equals("[]")) {
            return null;
        }
        try {
            LockData lock = GSON.fromJson(json, LockData.class);
            if (lock != null && lock.token != null && !lock.token.isBlank()) {
                return lock;
            }
        } catch (Exception ignored) {}
        return null;
    }
}
