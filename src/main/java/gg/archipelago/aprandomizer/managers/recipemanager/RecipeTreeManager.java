package gg.archipelago.aprandomizer.managers.recipemanager;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import gg.archipelago.aprandomizer.APRandomizer;
import gg.archipelago.aprandomizer.advancements.APCriteriaTriggers;
import gg.archipelago.aprandomizer.common.Utils.Utils;
import gg.archipelago.aprandomizer.data.WorldData;
import gg.archipelago.aprandomizer.network.SyncRecipeStatePayload;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.crafting.Recipe;
import net.neoforged.neoforge.network.PacketDistributor;
import org.slf4j.Logger;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class RecipeTreeManager {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final List<RecipeTreeNode> ALL_NODES = new ArrayList<>();
    private static final Set<ResourceKey<Recipe<?>>> ALL_RECIPES = new HashSet<>();
    private static final Map<String, RecipeTreeNode> NODES_BY_ID = new HashMap<>();
    private static final Map<Integer, List<RecipeTreeNode>> NODES_BY_TIER = new HashMap<>();
    private static final Map<Identifier, Integer> ADVANCEMENT_TIER_MAP = new HashMap<>();
    private static final Map<Identifier, RecipeTreeNode> ADVANCEMENT_NODE_MAP = new HashMap<>();
    private static JsonObject settingsObj = null;
    private static boolean loaded = false;

    public static synchronized void ensureLoaded() {
        if (loaded) return;
        loadRecipeTree();
        loaded = true;
    }

    public static synchronized void reload() {
        ALL_NODES.clear();
        ALL_RECIPES.clear();
        NODES_BY_ID.clear();
        NODES_BY_TIER.clear();
        ADVANCEMENT_TIER_MAP.clear();
        ADVANCEMENT_NODE_MAP.clear();
        settingsObj = null;
        loadRecipeTree();
        loaded = true;
    }

    private static void loadRecipeTree() {
        try {
            JsonObject rootObj = null;

            // 1. Try local file ./recipe_tree.json first (allows runtime customization)
            File localFile = new File("recipe_tree.json");
            if (localFile.exists() && localFile.isFile()) {
                try (var reader = Files.newBufferedReader(localFile.toPath(), StandardCharsets.UTF_8)) {
                    rootObj = JsonParser.parseReader(reader).getAsJsonObject();
                    LOGGER.info("Loaded recipe_tree.json from local file: {}", localFile.getAbsolutePath());
                } catch (Exception e) {
                    LOGGER.warn("Failed to parse local recipe_tree.json, falling back to classpath: {}", e.getMessage());
                }
            }

            // 2. Fall back to classpath resource
            if (rootObj == null) {
                InputStream is = RecipeTreeManager.class.getResourceAsStream("/recipe_tree.json");
                if (is == null) {
                    is = APRandomizer.class.getResourceAsStream("/recipe_tree.json");
                }
                if (is != null) {
                    try (var reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                        rootObj = JsonParser.parseReader(reader).getAsJsonObject();
                        LOGGER.info("Loaded recipe_tree.json from classpath");
                    }
                }
            }

            if (rootObj == null) {
                LOGGER.error("Could not find or load recipe_tree.json anywhere!");
                return;
            }

            if (rootObj.has("tree")) {
                JsonArray treeArr = rootObj.getAsJsonArray("tree");
                for (JsonElement el : treeArr) {
                    if (el.isJsonObject()) {
                        parseNode(el.getAsJsonObject(), null);
                    }
                }
            }

            if (rootObj.has("settings") && rootObj.get("settings").isJsonObject()) {
                settingsObj = rootObj.getAsJsonObject("settings");
                LOGGER.info("Loaded custom settings from recipe_tree.json: {}", settingsObj);
            }

            LOGGER.info("Successfully loaded {} recipe nodes with {} total recipe keys from recipe_tree.json",
                    ALL_NODES.size(), ALL_RECIPES.size());

        } catch (Exception e) {
            LOGGER.error("Error loading recipe tree: ", e);
        }
    }

    public static boolean isSettingEnabled(String key, boolean defaultValue) {
        ensureLoaded();
        if (settingsObj != null && settingsObj.has(key)) {
            try {
                JsonElement el = settingsObj.get(key);
                if (el.isJsonPrimitive()) {
                    var prim = el.getAsJsonPrimitive();
                    if (prim.isBoolean()) return prim.getAsBoolean();
                    if (prim.isNumber()) return prim.getAsInt() != 0;
                    if (prim.isString()) {
                        String s = prim.getAsString().toLowerCase(Locale.ENGLISH);
                        if ("true".equals(s) || "1".equals(s) || "yes".equals(s) || s.contains("ticket") || s.contains("skilltree")) return true;
                        if ("false".equals(s) || "0".equals(s) || "no".equals(s) || "random_by_bias".equals(s)) return false;
                    }
                }
            } catch (Exception ignored) {}
        }
        return defaultValue;
    }

    public static String getRecipeAdvancementPath(String nodeId, Identifier itemIdent) {
        if ("node_81".equals(nodeId)) return "boats";
        if ("node_114".equals(nodeId)) return "doors_and_trapdoors";
        return itemIdent.getPath();
    }

    private static void parseNode(JsonObject obj, String parentId) {
        String id = obj.has("id") ? obj.get("id").getAsString() : "";
        int tier = obj.has("tier") ? obj.get("tier").getAsInt() : 0;
        String item = obj.has("item") ? obj.get("item").getAsString() : "";
        String name = obj.has("name") ? obj.get("name").getAsString() : "";

        List<ResourceKey<Recipe<?>>> recipes = new ArrayList<>();
        if (obj.has("recipes")) {
            JsonArray recArr = obj.getAsJsonArray("recipes");
            for (JsonElement rEl : recArr) {
                String rStr = rEl.getAsString();
                if (rStr != null && !rStr.isBlank() && !rStr.equals("minecraft:air")) {
                    try {
                        Identifier idKey = Identifier.parse(rStr);
                        ResourceKey<Recipe<?>> key = ResourceKey.create(Registries.RECIPE, idKey);
                        recipes.add(key);
                        ALL_RECIPES.add(key);
                    } catch (Exception e) {
                        LOGGER.warn("Invalid recipe identifier '{}' in node {}: {}", rStr, id, e.getMessage());
                    }
                }
            }
        }

        // Only register nodes that have actual recipes
        if (!recipes.isEmpty()) {
            RecipeTreeNode node = new RecipeTreeNode(id, parentId, tier, item, name, recipes);
            ALL_NODES.add(node);
            NODES_BY_ID.put(id, node);
            if (tier > 0) {
                NODES_BY_TIER.computeIfAbsent(tier, k -> new ArrayList<>()).add(node);
                if (!"minecraft:air".equals(item)) {
                    try {
                        Identifier itemIdent = Identifier.parse(item);
                        String path = getRecipeAdvancementPath(id, itemIdent);
                        Identifier advId = Identifier.fromNamespaceAndPath(APRandomizer.MODID, "received/" + path);
                        ADVANCEMENT_TIER_MAP.put(advId, tier);
                        ADVANCEMENT_NODE_MAP.put(advId, node);
                    } catch (Exception ignored) {}
                }
            }
        }

        if (obj.has("children")) {
            JsonArray children = obj.getAsJsonArray("children");
            for (JsonElement childEl : children) {
                if (childEl.isJsonObject()) {
                    parseNode(childEl.getAsJsonObject(), id);
                }
            }
        }
    }

    public static List<RecipeTreeNode> getAllNodes() {
        ensureLoaded();
        return Collections.unmodifiableList(ALL_NODES);
    }

    public static Set<ResourceKey<Recipe<?>>> getAllTreeRecipes() {
        ensureLoaded();
        return Collections.unmodifiableSet(ALL_RECIPES);
    }

    /**
     * Initial recipes that are considered unlocked from the start (e.g. root node tier 0 or non-item nodes).
     */
    public static Set<ResourceKey<Recipe<?>>> getInitialUnlockedRecipes() {
        ensureLoaded();
        Set<ResourceKey<Recipe<?>>> initial = new HashSet<>();
        for (RecipeTreeNode node : ALL_NODES) {
            if (node.tier() == 0 || "minecraft:air".equals(node.item())) {
                initial.addAll(node.recipes());
            }
        }
        return initial;
    }

    /**
     * Nodes eligible for progression unlocking (tier >= 1 with actual items).
     */
    public static List<RecipeTreeNode> getUnlockableNodes() {
        ensureLoaded();
        List<RecipeTreeNode> list = new ArrayList<>();
        for (RecipeTreeNode node : ALL_NODES) {
            if (node.tier() > 0 && !"minecraft:air".equals(node.item())) {
                list.add(node);
            }
        }
        return list;
    }

    /**
     * Percentage bias (0..100) towards earlier tiers.
     * 100: only the current minimum tier can drop.
     * 0: completely uniform random across all remaining tiers.
     * 1..99: exponential decay gradient where each higher tier is progressively less likely.
     */
    public static double getRecipeTierBias() {
        if (APRandomizer.isConnected() && APRandomizer.getAP() != null && APRandomizer.getAP().getSlotData() != null) {
            return APRandomizer.getAP().getSlotData().recipeTierBias;
        }
        if (APRandomizer.getApmcData() != null) {
            return APRandomizer.getApmcData().recipe_tier_bias;
        }
        return 0.0;
    }

    public static void setRecipeTierBias(double bias) {
        double clamped = Math.clamp(bias, 0.0, 100.0);
        if (APRandomizer.isConnected() && APRandomizer.getAP() != null && APRandomizer.getAP().getSlotData() != null) {
            APRandomizer.getAP().getSlotData().recipeTierBias = clamped;
        }
        if (APRandomizer.getApmcData() != null) {
            APRandomizer.getApmcData().recipe_tier_bias = clamped;
        }
    }

    public static RecipeTreeNode unlockNextRecipeNode(MinecraftServer server) {
        ensureLoaded();
        WorldData worldData = APRandomizer.getWorldData();
        if (worldData == null) return null;

        Set<ResourceKey<Recipe<?>>> unlocked = new HashSet<>(worldData.getUnlockedRecipes());

        // Ensure any initial tier 0 recipes are recorded as unlocked
        for (ResourceKey<Recipe<?>> initKey : getInitialUnlockedRecipes()) {
            if (!unlocked.contains(initKey)) {
                worldData.addUnlockedRecipe(initKey);
                unlocked.add(initKey);
            }
        }

        List<RecipeTreeNode> lockedNodes = new ArrayList<>();
        for (RecipeTreeNode node : getUnlockableNodes()) {
            if (!node.isUnlocked(unlocked)) {
                lockedNodes.add(node);
            }
        }

        if (lockedNodes.isEmpty()) {
            Utils.sendMessageToAll("§a[Archipelago] Все известные рецепты уже разблокированы!");
            return null;
        }

        int minTier = Integer.MAX_VALUE;
        for (RecipeTreeNode node : lockedNodes) {
            if (node.tier() < minTier) {
                minTier = node.tier();
            }
        }

        double bias = Math.clamp(getRecipeTierBias(), 0.0, 100.0);
        RecipeTreeNode chosen = null;

        if (bias >= 100.0) {
            // Strictly minimal remaining tier only!
            List<RecipeTreeNode> minTierNodes = new ArrayList<>();
            for (RecipeTreeNode node : lockedNodes) {
                if (node.tier() == minTier) {
                    minTierNodes.add(node);
                }
            }
            int randomIndex = ThreadLocalRandom.current().nextInt(minTierNodes.size());
            chosen = minTierNodes.get(randomIndex);
        } else if (bias <= 0.0) {
            // Completely uniform random across all remaining locked nodes
            int randomIndex = ThreadLocalRandom.current().nextInt(lockedNodes.size());
            chosen = lockedNodes.get(randomIndex);
        } else {
            // Exponential decay gradient: weight = (1 - bias/100)^(tier - minTier)
            double decay = 1.0 - (bias / 100.0);
            double[] weights = new double[lockedNodes.size()];
            double totalWeight = 0.0;

            for (int i = 0; i < lockedNodes.size(); i++) {
                RecipeTreeNode node = lockedNodes.get(i);
                int deltaTier = node.tier() - minTier;
                double w = Math.pow(decay, deltaTier);
                weights[i] = w;
                totalWeight += w;
            }

            double randomValue = ThreadLocalRandom.current().nextDouble() * totalWeight;
            double cumulative = 0.0;
            for (int i = 0; i < lockedNodes.size(); i++) {
                cumulative += weights[i];
                if (randomValue <= cumulative) {
                    chosen = lockedNodes.get(i);
                    break;
                }
            }
            if (chosen == null) {
                chosen = lockedNodes.getLast();
            }
        }

        // Apply unlock
        List<ResourceKey<Recipe<?>>> toAward = new ArrayList<>();
        for (ResourceKey<Recipe<?>> key : chosen.recipes()) {
            if (!unlocked.contains(key)) {
                worldData.addUnlockedRecipe(key);
                toAward.add(key);
            }
        }

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.awardRecipesByKey(toAward);
            APCriteriaTriggers.RECIPE_NODE.get().trigger(player, chosen.id());
            try {
                player.getAdvancements().save();
            } catch (Exception ignored) {}
        }
        worldData.incrementRecipeUnlockTier();

        if (chosen.recipes().size() > 1) {
            Utils.sendMessageToAll("§a[Archipelago] Открыт рецепт: §e" + chosen.name() + " §7(Тир " + chosen.tier() + ", рецептов: " + chosen.recipes().size() + ")!");
        } else {
            Utils.sendMessageToAll("§a[Archipelago] Открыт рецепт: §e" + chosen.name() + " §7(Тир " + chosen.tier() + ")!");
        }
        Utils.sendTitleToAll(
                Component.literal("§6" + chosen.name()),
                Component.literal("§aРецепт разблокирован (Тир " + chosen.tier() + ")"),
                10, 40, 10
        );

        syncStateToAll(server);
        return chosen;
    }

    public static RecipeTreeNode getNodeById(String id) {
        ensureLoaded();
        return NODES_BY_ID.get(id);
    }

    public static List<RecipeTreeNode> getNodesByTier(int tier) {
        ensureLoaded();
        return NODES_BY_TIER.getOrDefault(tier, Collections.emptyList());
    }

    public static int getAdvancementTier(Identifier id) {
        ensureLoaded();
        return ADVANCEMENT_TIER_MAP.getOrDefault(id, 0);
    }

    public static RecipeTreeNode getNodeByAdvancement(Identifier id) {
        ensureLoaded();
        return ADVANCEMENT_NODE_MAP.get(id);
    }

    public static int getTierColor(int tier) {
        return switch (tier) {
            case 1 -> 0xFF8D5524; // коричневый (Brown)
            case 2 -> 0xFFA0A0A0; // серый (Gray)
            case 3 -> 0xFF4CAF50; // зелёный (Green)
            case 4 -> 0xFF2196F3; // синий (Blue)
            case 5 -> 0xFF9C27B0; // фиолетовый (Purple)
            case 6 -> 0xFFFF9800; // оранжевый (Orange)
            case 7 -> 0xFFF44336; // красный (Red)
            default -> 0xFFFFFFFF; // белый (White for all non-recipe achievements)
        };
    }

    public static int getTierStripeColor(int tier) {
        return getTierColor(tier);
    }

    public static boolean isTicketMode() {
        ensureLoaded();
        if (APRandomizer.isConnected() && APRandomizer.getAP() != null && APRandomizer.getAP().getSlotData() != null) {
            return APRandomizer.getAP().getSlotData().isTicketMode();
        }
        if (APRandomizer.getApmcData() != null) {
            return APRandomizer.getApmcData().isTicketMode();
        }
        return isSettingEnabled("recipe_unlock_mode", false);
    }

    public static void setTicketMode(boolean ticketMode) {
        Object val = ticketMode ? 1 : 0;
        if (APRandomizer.isConnected() && APRandomizer.getAP() != null && APRandomizer.getAP().getSlotData() != null) {
            APRandomizer.getAP().getSlotData().recipeUnlockMode = val;
        }
        if (APRandomizer.getApmcData() != null) {
            APRandomizer.getApmcData().recipe_unlock_mode = val;
        }
    }

    public static int getRecipeTierUnlockPercentage() {
        ensureLoaded();
        if (APRandomizer.isConnected() && APRandomizer.getAP() != null && APRandomizer.getAP().getSlotData() != null) {
            return APRandomizer.getAP().getSlotData().getRecipeTierUnlockPercentage();
        }
        if (APRandomizer.getApmcData() != null) {
            return APRandomizer.getApmcData().getRecipeTierUnlockPercentage();
        }
        if (settingsObj != null && settingsObj.has("recipe_tier_unlock_percentage")) {
            try {
                return Math.clamp(settingsObj.get("recipe_tier_unlock_percentage").getAsInt(), 0, 100);
            } catch (Exception ignored) {}
        }
        return 50;
    }

    public static void setRecipeTierUnlockPercentage(int pct) {
        int clamped = Math.clamp(pct, 0, 100);
        if (APRandomizer.isConnected() && APRandomizer.getAP() != null && APRandomizer.getAP().getSlotData() != null) {
            APRandomizer.getAP().getSlotData().recipeTierUnlockPercentage = clamped;
        }
        if (APRandomizer.getApmcData() != null) {
            APRandomizer.getApmcData().recipe_tier_unlock_percentage = clamped;
        }
    }

    public static boolean canUnlockInTree(RecipeTreeNode node, Set<ResourceKey<Recipe<?>>> unlocked) {
        String parentId = node.parentId();
        if (parentId == null || parentId.isBlank() || "node_11".equals(parentId)) {
            return true;
        }
        RecipeTreeNode parent = getNodeById(parentId);
        if (parent == null || parent.tier() == 0 || "minecraft:air".equals(parent.item())) {
            return true;
        }
        return parent.isUnlocked(unlocked);
    }

    public static boolean canUnlockInTierProgression(RecipeTreeNode node, Set<ResourceKey<Recipe<?>>> unlocked, int thresholdPct) {
        if (thresholdPct <= 0) {
            return true;
        }
        int tier = node.tier();
        if (tier <= 1) {
            return true;
        }

        for (int t = 1; t < tier; t++) {
            List<RecipeTreeNode> tierNodes = getNodesByTier(t);
            if (tierNodes.isEmpty()) continue;
            int unlockedCount = 0;
            for (RecipeTreeNode tn : tierNodes) {
                if (tn.isUnlocked(unlocked)) {
                    unlockedCount++;
                }
            }
            int requiredCount = (int) Math.ceil(tierNodes.size() * (thresholdPct / 100.0));
            if (unlockedCount < requiredCount) {
                return false;
            }
        }
        return true;
    }

    public enum UnlockStatus {
        UNLOCKED,
        AVAILABLE,
        LOCKED_TREE,
        LOCKED_TIER,
        NO_TICKETS
    }

    public static UnlockStatus getUnlockStatus(RecipeTreeNode node, Set<ResourceKey<Recipe<?>>> unlocked, int tickets, int thresholdPct) {
        if (node.isUnlocked(unlocked)) {
            return UnlockStatus.UNLOCKED;
        }
        if (!canUnlockInTree(node, unlocked)) {
            return UnlockStatus.LOCKED_TREE;
        }
        if (!canUnlockInTierProgression(node, unlocked, thresholdPct)) {
            return UnlockStatus.LOCKED_TIER;
        }
        if (tickets <= 0) {
            return UnlockStatus.NO_TICKETS;
        }
        return UnlockStatus.AVAILABLE;
    }

    public static void addRecipeTicket(MinecraftServer server) {
        WorldData worldData = APRandomizer.getWorldData();
        if (worldData == null) return;
        worldData.addRecipeTickets(1);
        int tickets = worldData.getRecipeTickets();

        Utils.sendMessageToAll("§a[Archipelago] Получен талон на разблокировку рецепта! §7(Всего талонов: §e" + tickets + "§7)");
        Utils.sendMessageToAll("§7Откройте §eМеню паузы -> Древо рецептов§7, чтобы выбрать и открыть рецепт.");
        Utils.sendTitleToAll(
                Component.literal("§6+1 Талон на рецепт"),
                Component.literal("§eДоступно талонов: " + tickets + " §7(Меню паузы -> Рецепты)"),
                10, 40, 10
        );

        syncStateToAll(server);
    }

    public static boolean unlockRecipeNode(MinecraftServer server, String nodeId) {
        ensureLoaded();
        WorldData worldData = APRandomizer.getWorldData();
        if (worldData == null) return false;

        RecipeTreeNode node = getNodeById(nodeId);
        if (node == null) {
            LOGGER.warn("Attempted to unlock non-existent recipe node: {}", nodeId);
            return false;
        }

        Set<ResourceKey<Recipe<?>>> unlocked = new HashSet<>(worldData.getUnlockedRecipes());
        if (node.isUnlocked(unlocked)) {
            return false;
        }

        if (worldData.getRecipeTickets() <= 0) {
            Utils.sendMessageToAll("§c[Archipelago] Недостаточно талонов для разблокировки рецепта!");
            return false;
        }

        if (!canUnlockInTree(node, unlocked)) {
            RecipeTreeNode parent = getNodeById(node.parentId());
            String parentName = parent != null ? parent.name() : node.parentId();
            Utils.sendMessageToAll("§c[Archipelago] Нельзя разблокировать " + node.name() + ": сначала разблокируйте «" + parentName + "»!");
            return false;
        }

        int threshold = getRecipeTierUnlockPercentage();
        if (!canUnlockInTierProgression(node, unlocked, threshold)) {
            Utils.sendMessageToAll("§c[Archipelago] Нельзя разблокировать " + node.name() + ": откройте больше рецептов предыдущих тиров (требуется " + threshold + "%)!");
            return false;
        }

        // Deduct ticket
        worldData.addRecipeTickets(-1);

        // Apply unlock
        List<ResourceKey<Recipe<?>>> toAward = new ArrayList<>();
        for (ResourceKey<Recipe<?>> key : node.recipes()) {
            if (!unlocked.contains(key)) {
                worldData.addUnlockedRecipe(key);
                toAward.add(key);
            }
        }

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.awardRecipesByKey(toAward);
            APCriteriaTriggers.RECIPE_NODE.get().trigger(player, node.id());
            try {
                player.getAdvancements().save();
            } catch (Exception ignored) {}
        }
        worldData.incrementRecipeUnlockTier();

        int remainingTickets = worldData.getRecipeTickets();
        if (node.recipes().size() > 1) {
            Utils.sendMessageToAll("§a[Archipelago] Открыт рецепт: §e" + node.name() + " §7(Тир " + node.tier() + ", рецептов: " + node.recipes().size() + ")! §7Осталось талонов: §e" + remainingTickets);
        } else {
            Utils.sendMessageToAll("§a[Archipelago] Открыт рецепт: §e" + node.name() + " §7(Тир " + node.tier() + ")! §7Осталось талонов: §e" + remainingTickets);
        }
        Utils.sendTitleToAll(
                Component.literal("§6" + node.name()),
                Component.literal("§aРецепт разблокирован! §7(Осталось талонов: " + remainingTickets + ")"),
                10, 40, 10
        );

        syncStateToAll(server);
        return true;
    }

    public static SyncRecipeStatePayload createSyncPayload() {
        ensureLoaded();
        WorldData worldData = APRandomizer.getWorldData();
        int tickets = worldData != null ? worldData.getRecipeTickets() : 0;
        int threshold = getRecipeTierUnlockPercentage();
        boolean ticketMode = isTicketMode();

        List<String> unlockedNodeIds = new ArrayList<>();
        if (worldData != null) {
            Set<ResourceKey<Recipe<?>>> unlocked = new HashSet<>(worldData.getUnlockedRecipes());
            for (RecipeTreeNode node : ALL_NODES) {
                if (node.isUnlocked(unlocked)) {
                    unlockedNodeIds.add(node.id());
                }
            }
        }

        return new SyncRecipeStatePayload(tickets, threshold, ticketMode, unlockedNodeIds);
    }

    public static void syncStateToPlayer(ServerPlayer player) {
        if (player == null) return;
        try {
            PacketDistributor.sendToPlayer(player, createSyncPayload());
        } catch (Exception e) {
            LOGGER.warn("Failed to sync recipe state to player {}: {}", player.getName().getString(), e.getMessage());
        }
    }

    public static void syncStateToAll(MinecraftServer server) {
        if (server == null) return;
        try {
            PacketDistributor.sendToAllPlayers(createSyncPayload());
        } catch (Exception e) {
            LOGGER.warn("Failed to sync recipe state to all players: {}", e.getMessage());
        }
    }
}
