package gg.archipelago.aprandomizer.managers.chest;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import gg.archipelago.aprandomizer.APRandomizer;
import gg.archipelago.aprandomizer.ap.APClient;
import gg.archipelago.aprandomizer.data.WorldData;
import io.github.archipelagomw.events.SetReplyEvent;
import io.github.archipelagomw.network.client.SetPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ItemStackWithSlot;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@EventBusSubscriber(modid = APRandomizer.MODID)
public class SharedChestManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final String STORAGE_KEY = "mc_shared_chest";
    public static final String LOCK_KEY = "mc_shared_chest_lock";
    public static final long LOCK_EXPIRY_MS = 45_000L;
    public static final long HEARTBEAT_INTERVAL_MS = 15_000L;
    public static final long AFK_TIMEOUT_MS = 120_000L;
    public static final long PENDING_TIMEOUT_MS = 4_000L;

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

    private static final SharedChestContainer CONTAINER = new SharedChestContainer();
    private static final Gson GSON = new Gson();

    private static MinecraftServer server;
    private static WorldData worldData;
    private static APClient apClient;

    private static boolean isUpdatingFromArchipelago = false;
    private static int pendingSyncTicks = 0;
    private static String lastKnownArchipelagoJson = "[]";

    private static UUID currentLockPlayer = null;
    private static String currentLockPlayerName = null;
    private static String currentLockToken = null;
    private static BlockPos currentOpenPos = null;
    private static long currentLockTime = 0L;
    private static long lastHeartbeatTime = 0L;
    private static long lastInteractionTime = 0L;

    private static UUID pendingLockPlayer = null;
    private static String pendingLockPlayerName = null;
    private static String pendingLockToken = null;
    private static BlockPos pendingLockPos = null;
    private static long pendingLockStartTime = 0L;

    private static LockData remoteLock = null;

    public static SharedChestContainer getContainer() {
        return CONTAINER;
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
                lastKnownArchipelagoJson = savedJson;
                applyJsonToContainer(savedJson);
                LOGGER.info("Loaded shared chest from local world data");
            }
        }
    }

    public static void onConnected(APClient client) {
        apClient = client;
        if (apClient != null && apClient.isConnected()) {
            LOGGER.info("Registering Archipelago data storage notify & fetch for keys: {}, {}", STORAGE_KEY, LOCK_KEY);
            apClient.dataStorageSetNotify(List.of(STORAGE_KEY, LOCK_KEY));
            apClient.dataStorageGet(List.of(STORAGE_KEY, LOCK_KEY));
        }
    }

    public static void onContainerChanged() {
        if (isUpdatingFromArchipelago) {
            return;
        }
        lastInteractionTime = System.currentTimeMillis();
        pendingSyncTicks = 2;
    }

    public static void onPlayerClosed(ContainerUser user) {
        LivingEntity entity = user != null ? user.getLivingEntity() : null;
        if (entity instanceof ServerPlayer player) {
            if (currentLockPlayer != null && currentLockPlayer.equals(player.getUUID())) {
                releaseLock();
            }
        } else {
            if (currentLockPlayer != null) {
                ServerPlayer holder = server != null ? server.getPlayerList().getPlayer(currentLockPlayer) : null;
                if (holder == null || !isChestMenuOpen(holder)) {
                    releaseLock();
                }
            }
        }
    }

    public static void onPlayerClosed() {
        releaseLock();
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (currentLockPlayer != null && currentLockPlayer.equals(player.getUUID())) {
                releaseLock();
            }
            if (pendingLockPlayer != null && pendingLockPlayer.equals(player.getUUID())) {
                pendingLockPlayer = null;
                pendingLockPlayerName = null;
                pendingLockToken = null;
                pendingLockPos = null;
            }
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        tick();
    }

    public static void tick() {
        long now = System.currentTimeMillis();

        // Pending lock timeout check
        if (pendingLockPlayer != null && (now - pendingLockStartTime > PENDING_TIMEOUT_MS)) {
            ServerPlayer pending = server != null ? server.getPlayerList().getPlayer(pendingLockPlayer) : null;
            if (pending != null) {
                pending.sendSystemMessage(Component.literal("§c[!] Таймаут ответа от Архипелага при открытии сундука."), true);
                pending.playSound(SoundEvents.CHEST_LOCKED, 0.8f, 1.0f);
            }
            pendingLockPlayer = null;
            pendingLockPlayerName = null;
            pendingLockToken = null;
            pendingLockPos = null;
        }

        // Active lock holder validation, heartbeat & AFK check
        if (currentLockPlayer != null) {
            ServerPlayer holder = server != null ? server.getPlayerList().getPlayer(currentLockPlayer) : null;
            if (holder == null || !holder.isAlive() || !isChestMenuOpen(holder)) {
                releaseLock();
            } else if (now - lastInteractionTime > AFK_TIMEOUT_MS) {
                holder.closeContainer();
                holder.sendSystemMessage(Component.literal("§c[!] Общий сундук закрыт из-за бездействия (2 мин)."), true);
                releaseLock();
            } else if (now - lastHeartbeatTime > HEARTBEAT_INTERVAL_MS) {
                lastHeartbeatTime = now;
                sendHeartbeatToArchipelago();
            }
        }

        if (pendingSyncTicks > 0) {
            pendingSyncTicks--;
            if (pendingSyncTicks == 0) {
                syncToArchipelago();
            }
        }
    }

    public static String serializeContainerToJson() {
        if (server == null) {
            return "[]";
        }
        List<ItemStackWithSlot> slots = new ArrayList<>();
        for (int i = 0; i < CONTAINER.getContainerSize(); i++) {
            ItemStack stack = CONTAINER.getItem(i);
            if (!stack.isEmpty()) {
                slots.add(new ItemStackWithSlot(i, stack.copy()));
            }
        }
        if (slots.isEmpty()) {
            return "[]";
        }
        try {
            RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, server.registryAccess());
            DataResult<JsonElement> result = ItemStackWithSlot.CODEC.listOf().encodeStart(ops, slots);
            return result.result().map(GSON::toJson).orElse("[]");
        } catch (Exception e) {
            LOGGER.error("Failed to serialize shared chest items to JSON", e);
            return "[]";
        }
    }

    public static void syncToArchipelago() {
        String json = serializeContainerToJson();
        if (worldData != null) {
            worldData.setSharedChestJson(json);
        }
        if (json.equals(lastKnownArchipelagoJson)) {
            return;
        }
        lastKnownArchipelagoJson = json;

        if (apClient != null && apClient.isConnected()) {
            try {
                SetPacket packet = new SetPacket(STORAGE_KEY, "[]");
                packet.addDataStorageOperation(SetPacket.Operation.REPLACE, json);
                packet.want_reply = true;
                apClient.dataStorageSet(packet);
                LOGGER.info("Sent updated shared chest contents to Archipelago: {} items", countNonEmpty());
            } catch (Exception e) {
                LOGGER.error("Failed to send shared chest SetPacket to Archipelago", e);
            }
        }
    }

    public static void handleArchipelagoUpdate(Object val) {
        String json = extractJson(val);
        if (json == null) {
            return;
        }

        if ((json.isBlank() || json.equals("[]")) && !isContainerEmpty()) {
            LOGGER.info("Archipelago shared chest is empty, initializing it with local items");
            syncToArchipelago();
            return;
        }

        if (json.equals(lastKnownArchipelagoJson)) {
            return;
        }
        lastKnownArchipelagoJson = json;

        if (worldData != null) {
            worldData.setSharedChestJson(json);
        }

        applyJsonToContainer(json);
        LOGGER.info("Updated shared chest from Archipelago ({} items)", countNonEmpty());
    }

    private static void applyJsonToContainer(String json) {
        if (json == null || json.isBlank() || json.equals("[]")) {
            isUpdatingFromArchipelago = true;
            try {
                CONTAINER.clearContent();
            } finally {
                isUpdatingFromArchipelago = false;
            }
            refreshOpenMenus();
            return;
        }

        if (server == null) {
            return;
        }

        try {
            JsonElement element = JsonParser.parseString(json);
            RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, server.registryAccess());
            DataResult<List<ItemStackWithSlot>> result = ItemStackWithSlot.CODEC.listOf().parse(ops, element);
            List<ItemStackWithSlot> items = result.result().orElse(Collections.emptyList());

            isUpdatingFromArchipelago = true;
            try {
                CONTAINER.clearContent();
                for (ItemStackWithSlot item : items) {
                    if (item.isValidInContainer(CONTAINER.getContainerSize())) {
                        CONTAINER.setItem(item.slot(), item.stack());
                    }
                }
            } finally {
                isUpdatingFromArchipelago = false;
            }

            refreshOpenMenus();
        } catch (Exception e) {
            LOGGER.error("Failed to parse shared chest JSON from Archipelago: {}", json, e);
        }
    }

    private static void refreshOpenMenus() {
        if (server == null) {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.containerMenu instanceof SharedChestMenu menu && menu.getContainer() == CONTAINER) {
                menu.sendAllDataToRemote();
            }
        }
    }

    private static String extractJson(Object val) {
        if (val == null) {
            return null;
        }
        if (val instanceof String s) {
            return s;
        }
        if (val instanceof JsonElement je) {
            if (je.isJsonPrimitive() && je.getAsJsonPrimitive().isString()) {
                return je.getAsString();
            }
            return je.toString();
        }
        return GSON.toJson(val);
    }

    public static boolean isContainerEmpty() {
        return CONTAINER.isEmpty();
    }

    public static int countNonEmpty() {
        int count = 0;
        for (int i = 0; i < CONTAINER.getContainerSize(); i++) {
            if (!CONTAINER.getItem(i).isEmpty()) {
                count++;
            }
        }
        return count;
    }

    public static boolean isPlayerLockHolder(Player player) {
        if (!APRandomizer.isConnected()) {
            return false;
        }
        if (player == null || currentLockPlayer == null) {
            return false;
        }
        return currentLockPlayer.equals(player.getUUID());
    }

    public static boolean isChestMenuOpen(ServerPlayer player) {
        if (player == null) return false;
        if (player.containerMenu instanceof SharedChestMenu menu) {
            return menu.getContainer() == CONTAINER;
        }
        return false;
    }

    public static void tryOpenChest(ServerPlayer player, @Nullable BlockPos pos) {
        if (!APRandomizer.isConnected()) {
            player.sendSystemMessage(Component.literal("§c[!] Общий сундук недоступен: нет подключения к Архипелагу!"), true);
            player.playSound(SoundEvents.CHEST_LOCKED, 0.8f, 1.0f);
            return;
        }

        long now = System.currentTimeMillis();

        // 1. Check if locally locked
        if (currentLockPlayer != null) {
            if (currentLockPlayer.equals(player.getUUID())) {
                actuallyOpenChest(player, pos);
                return;
            }
            ServerPlayer holder = server != null ? server.getPlayerList().getPlayer(currentLockPlayer) : null;
            if (holder != null && holder.isAlive() && isChestMenuOpen(holder)) {
                player.sendSystemMessage(Component.literal("§c[!] Этот сундук уже открыт игроком " + currentLockPlayerName + "!"), true);
                player.playSound(SoundEvents.CHEST_LOCKED, 0.8f, 1.0f);
                return;
            } else {
                forceReleaseLock();
            }
        }

        // 2. Check if another opening is pending
        if (pendingLockPlayer != null) {
            if (pendingLockPlayer.equals(player.getUUID())) {
                return;
            }
            if (now - pendingLockStartTime < PENDING_TIMEOUT_MS) {
                player.sendSystemMessage(Component.literal("§c[!] Общий сундук сейчас открывается другим игроком..."), true);
                player.playSound(SoundEvents.CHEST_LOCKED, 0.8f, 1.0f);
                return;
            } else {
                pendingLockPlayer = null;
            }
        }

        // 3. Check if remotely locked by another client
        if (remoteLock != null && !remoteLock.isExpired(now)) {
            player.sendSystemMessage(Component.literal("§c[!] Этот сундук уже открыт игроком " + remoteLock.player + "!"), true);
            player.playSound(SoundEvents.CHEST_LOCKED, 0.8f, 1.0f);
            return;
        }

        // 4. Request lock from Archipelago
        String token = UUID.randomUUID().toString();
        pendingLockPlayer = player.getUUID();
        pendingLockPlayerName = player.getScoreboardName();
        pendingLockToken = token;
        pendingLockStartTime = now;
        pendingLockPos = pos;

        player.sendSystemMessage(Component.literal("§e[...] Подключение к общему сундуку..."), true);

        if (apClient != null && apClient.isConnected()) {
            apClient.dataStorageGet(List.of(STORAGE_KEY));

            LockData myLock = new LockData(player.getScoreboardName(), player.getUUID().toString(), token, now);
            String lockJson = GSON.toJson(myLock);
            SetPacket packet = new SetPacket(LOCK_KEY, "");
            packet.addDataStorageOperation(SetPacket.Operation.REPLACE, lockJson);
            packet.want_reply = true;
            apClient.dataStorageSet(packet);
        } else {
            pendingLockPlayer = null;
            player.sendSystemMessage(Component.literal("§c[!] Общий сундук недоступен: нет подключения к Архипелагу!"), true);
        }
    }

    public static void handleLockSetReply(SetReplyEvent event) {
        if (!LOCK_KEY.equals(event.key)) {
            return;
        }
        long now = System.currentTimeMillis();
        LockData replyVal = parseLock(event.value);
        LockData origVal = parseLock(event.original_value);

        if (pendingLockPlayer != null && pendingLockToken != null) {
            boolean isOurToken = replyVal != null && pendingLockToken.equals(replyVal.token);

            if (isOurToken) {
                boolean collision = origVal != null
                        && !pendingLockToken.equals(origVal.token)
                        && !origVal.isExpired(now);

                if (collision) {
                    LOGGER.warn("Shared chest lock collision! Lock already held by {}", origVal.player);
                    if (apClient != null && apClient.isConnected()) {
                        SetPacket revertPacket = new SetPacket(LOCK_KEY, "");
                        revertPacket.addDataStorageOperation(SetPacket.Operation.REPLACE, GSON.toJson(origVal));
                        revertPacket.want_reply = false;
                        apClient.dataStorageSet(revertPacket);
                    }

                    ServerPlayer pending = server != null ? server.getPlayerList().getPlayer(pendingLockPlayer) : null;
                    if (pending != null) {
                        pending.sendSystemMessage(Component.literal("§c[!] Этот сундук уже открыт игроком " + origVal.player + "!"), true);
                        pending.playSound(SoundEvents.CHEST_LOCKED, 0.8f, 1.0f);
                    }
                    pendingLockPlayer = null;
                    pendingLockPlayerName = null;
                    pendingLockToken = null;
                    pendingLockPos = null;
                    return;
                }

                currentLockPlayer = pendingLockPlayer;
                currentLockPlayerName = pendingLockPlayerName;
                currentLockToken = pendingLockToken;
                currentLockTime = now;
                currentOpenPos = pendingLockPos;
                lastHeartbeatTime = now;
                lastInteractionTime = now;
                remoteLock = replyVal;

                UUID playerId = pendingLockPlayer;
                BlockPos pos = pendingLockPos;

                pendingLockPlayer = null;
                pendingLockPlayerName = null;
                pendingLockToken = null;
                pendingLockPos = null;

                ServerPlayer player = server != null ? server.getPlayerList().getPlayer(playerId) : null;
                if (player == null || !player.isAlive()) {
                    releaseLock();
                    return;
                }

                actuallyOpenChest(player, pos);
                return;
            }
        }

        remoteLock = replyVal;
    }

    public static void handleLockRetrieved(Object value) {
        LockData lock = parseLock(value);
        remoteLock = lock;
        long now = System.currentTimeMillis();

        if (lock != null && !lock.isExpired(now)) {
            if (currentLockPlayer != null && !lock.token.equals(currentLockToken)) {
                LOGGER.warn("Remote lock acquired by {}, force closing local chest", lock.player);
                forceCloseLocalChest();
            }
        }
    }

    private static void actuallyOpenChest(ServerPlayer player, @Nullable BlockPos pos) {
        MenuProvider provider = null;
        if (pos != null && player.level().getBlockEntity(pos) instanceof SharedChestBlockEntity be) {
            provider = be;
        } else {
            provider = new SimpleMenuProvider(
                    (containerId, inv, p) -> new SharedChestMenu(containerId, inv, CONTAINER),
                    Component.translatable("container.aprandomizer.shared_chest")
            );
        }

        player.openMenu(provider);

        if (pos != null) {
            ServerLevel level = (ServerLevel) player.level();
            level.playSound(null, pos, SoundEvents.ENDER_CHEST_OPEN, SoundSource.BLOCKS, 0.5F, level.getRandom().nextFloat() * 0.1F + 0.9F);
            PiglinAi.angerNearbyPiglins(level, player, true);
        } else {
            player.playSound(SoundEvents.ENDER_CHEST_OPEN, 0.5F, 1.0F);
        }
    }

    private static void sendHeartbeatToArchipelago() {
        if (apClient != null && apClient.isConnected() && currentLockPlayer != null && currentLockToken != null) {
            LockData heartbeatLock = new LockData(currentLockPlayerName, currentLockPlayer.toString(), currentLockToken, System.currentTimeMillis());
            SetPacket packet = new SetPacket(LOCK_KEY, "");
            packet.addDataStorageOperation(SetPacket.Operation.REPLACE, GSON.toJson(heartbeatLock));
            packet.want_reply = false;
            apClient.dataStorageSet(packet);
        }
    }

    public static void releaseLock() {
        if (currentLockPlayer == null && pendingLockPlayer == null) {
            return;
        }

        syncToArchipelago();

        if (apClient != null && apClient.isConnected()) {
            SetPacket packet = new SetPacket(LOCK_KEY, "");
            packet.addDataStorageOperation(SetPacket.Operation.REPLACE, "");
            packet.want_reply = false;
            apClient.dataStorageSet(packet);
        }

        currentLockPlayer = null;
        currentLockPlayerName = null;
        currentLockToken = null;
        currentOpenPos = null;
        pendingLockPlayer = null;
        pendingLockPlayerName = null;
        pendingLockToken = null;
        pendingLockPos = null;
        remoteLock = null;
    }

    public static void forceReleaseLock() {
        releaseLock();
    }

    private static void forceCloseLocalChest() {
        if (currentLockPlayer != null && server != null) {
            ServerPlayer player = server.getPlayerList().getPlayer(currentLockPlayer);
            if (player != null && isChestMenuOpen(player)) {
                player.closeContainer();
                player.sendSystemMessage(Component.literal("§c[!] Этот сундук был закрыт, так как занят другим игроком."), true);
            }
        }
        currentLockPlayer = null;
        currentLockPlayerName = null;
        currentLockToken = null;
        currentOpenPos = null;
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
        } catch (Exception ignored) {
        }
        return null;
    }

    public static void openChest(ServerPlayer player) {
        tryOpenChest(player, null);
    }
}
