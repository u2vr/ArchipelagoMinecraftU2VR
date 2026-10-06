package gg.archipelago.aprandomizer.data.advancements;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import gg.archipelago.aprandomizer.APRandomizer;
import gg.archipelago.aprandomizer.advancements.RecipeNodeCriteria;
import gg.archipelago.aprandomizer.advancements.ReceivedItemCriteria;
import gg.archipelago.aprandomizer.items.APItem;
import gg.archipelago.aprandomizer.items.APItems;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.triggers.PlayerTrigger;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.advancements.AdvancementSubProvider;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class ReceivedAdvancementProvider extends AdvancementSubProvider {

    public ReceivedAdvancementProvider(BootstrapContext<Advancement> output) {
        super(output);
    }

    @Override
    public void generate() {
        // 1. Root advancement
        AdvancementHolder root = Advancement.Builder.recipeAdvancement()
                .rootDisplay(
                        Items.STRUCTURE_BLOCK,
                        Component.literal("Received Items & Recipes"),
                        Component.literal("Track all unlocked recipes, upgrades and items from Archipelago"),
                        Identifier.withDefaultNamespace("block/basalt_side"),
                        AdvancementType.TASK,
                        false,
                        false,
                        false)
                .addCriterion("auto", PlayerTrigger.TriggerInstance.tick())
                .save(output, Identifier.fromNamespaceAndPath(APRandomizer.MODID, "received/root"));

        Identifier rootId = root.id();

        // 2. Load recipe tree json
        JsonObject rootObj = loadRecipeTreeJson();
        if (rootObj == null) {
            throw new IllegalStateException("Failed to load recipe_tree.json for advancement generation!");
        }

        JsonArray treeArr = rootObj.getAsJsonArray("tree");
        if (treeArr == null || treeArr.isEmpty()) {
            throw new IllegalStateException("recipe_tree.json has no 'tree' element!");
        }

        JsonObject treeRoot = treeArr.get(0).getAsJsonObject();
        JsonArray rootChildren = treeRoot.has("children") ? treeRoot.getAsJsonArray("children") : new JsonArray();

        Map<String, Identifier> nodeToAdvId = new HashMap<>();
        nodeToAdvId.put("node_11", rootId);

        JsonObject settingsObj = rootObj.has("settings") && rootObj.get("settings").isJsonObject()
                ? rootObj.getAsJsonObject("settings")
                : null;
        boolean hpEnabled = isSettingEnabled(settingsObj, "hp_restriction", true);
        boolean hungerEnabled = isSettingEnabled(settingsObj, "hunger_restriction", true);
        boolean reachEnabled = isSettingEnabled(settingsObj, "reach_restriction", true);
        boolean borderEnabled = isSettingEnabled(settingsObj, "world_border_restriction", true);
        boolean structEnabled = isSettingEnabled(settingsObj, "structure_restriction", true);
        boolean offhandEnabled = isSettingEnabled(settingsObj, "offhand_restriction", true);
        boolean invEnabled = isSettingEnabled(settingsObj, "inventory_slot_restriction", true);
        boolean cursesEnabled = isSettingEnabled(settingsObj, "curses_enabled", true);

        // 3. Process all branches from root
        for (JsonElement childEl : rootChildren) {
            if (!childEl.isJsonObject()) continue;
            JsonObject childObj = childEl.getAsJsonObject();
            String name = childObj.has("name") ? childObj.get("name").getAsString() : "";
            String item = childObj.has("item") ? childObj.get("item").getAsString() : "";
            int tier = childObj.has("tier") ? childObj.get("tier").getAsInt() : 0;
            String id = childObj.has("id") ? childObj.get("id").getAsString() : "";

            if (!"minecraft:air".equals(item) && tier > 0) {
                // Recipe tree branch
                generateRecipeSubtree(childObj, rootId, nodeToAdvId);
            } else if (name.startsWith("HP")) {
                if (hpEnabled) {
                    generateLinearChain(childObj, rootId, "hp", APItems.HP_UPGRADE, 7,
                            "HP Upgrade", "+1 Heart",
                            Items.APPLE, Items.ENCHANTED_GOLDEN_APPLE, AdvancementType.GOAL, nodeToAdvId);
                }
            } else if (name.startsWith("Hunger")) {
                if (hungerEnabled) {
                    generateLinearChain(childObj, rootId, "hunger", APItems.HUNGER_UPGRADE, 6,
                            "Hunger Upgrade", "+1 Hunger Bar",
                            Items.COOKED_BEEF, Items.GOLDEN_CARROT, AdvancementType.GOAL, nodeToAdvId);
                }
            } else if (name.startsWith("Hand Length")) {
                if (reachEnabled) {
                    generateLinearChain(childObj, rootId, "reach", APItems.REACH_UPGRADE, 3,
                            "Reach Upgrade", "+1 Block Reach",
                            Items.FISHING_ROD, Items.FISHING_ROD, AdvancementType.GOAL, nodeToAdvId);
                }
            } else if (name.startsWith("World Border")) {
                if (borderEnabled) {
                    generateLinearChain(childObj, rootId, "world_border", APItems.WORLD_BORDER_UPGRADE, 5,
                            "World Border Upgrade", "Expand World Border",
                            Items.MAP, Items.MAP, AdvancementType.GOAL, nodeToAdvId);
                }
            } else if ("Village".equals(name) || "node_232".equals(id)) {
                if (structEnabled) {
                    generateStructuresChain(childObj, rootId, nodeToAdvId);
                }
            } else if ("Second Hand".equals(name) || "node_245".equals(id)) {
                if (offhandEnabled) {
                    Identifier advId = Identifier.fromNamespaceAndPath(APRandomizer.MODID, "received/second_hand");
                    Advancement.Builder.recipeAdvancement()
                            .display(
                                    Items.SHIELD,
                                    Component.literal("Second Hand Access"),
                                    Component.literal("Unlocks use of offhand slot"),
                                    AdvancementType.TASK,
                                    true,
                                    false,
                                    false)
                            .parent(rootId)
                            .addCriterion("received", ReceivedItemCriteria.TriggerInstance.receivedItem(APItems.OFFHAND_UNLOCK, 1))
                            .save(output, advId);
                    nodeToAdvId.put(id, advId);
                }
            } else if (name.startsWith("Inventory Slot") || "node_240".equals(id)) {
                if (invEnabled) {
                    generateInventorySlotChain(childObj, rootId, nodeToAdvId);
                }
            } else if ("Disable Day Monster Ignition".equals(name) || "node_261".equals(id)) {
                if (cursesEnabled) {
                    Identifier advId = Identifier.fromNamespaceAndPath(APRandomizer.MODID, "received/curse_monster_sun_burning");
                    Advancement.Builder.recipeAdvancement()
                            .display(
                                    Items.SKELETON_SKULL,
                                    Component.literal("Curse: Day Monsters"),
                                    Component.literal("Monsters no longer burn in the daylight!"),
                                    AdvancementType.CHALLENGE,
                                    true,
                                    false,
                                    false)
                            .parent(rootId)
                            .addCriterion("received", ReceivedItemCriteria.TriggerInstance.receivedItem(APItems.MONSTER_SUN_BURNING_DISABLED, 1))
                            .save(output, advId);
                    nodeToAdvId.put(id, advId);
                }
            } else if ("Disable Light Check for Monster Spawn".equals(name) || "node_260".equals(id)) {
                if (cursesEnabled) {
                    Identifier advId = Identifier.fromNamespaceAndPath(APRandomizer.MODID, "received/curse_monster_spawn_light");
                    Advancement.Builder.recipeAdvancement()
                            .display(
                                    Items.CREEPER_HEAD,
                                    Component.literal("Curse: Lightless Spawning"),
                                    Component.literal("Monsters can spawn at any light level!"),
                                    AdvancementType.CHALLENGE,
                                    true,
                                    false,
                                    false)
                            .parent(rootId)
                            .addCriterion("received", ReceivedItemCriteria.TriggerInstance.receivedItem(APItems.MONSTER_SPAWN_LIGHT_DISABLED, 1))
                            .save(output, advId);
                    nodeToAdvId.put(id, advId);
                }
            }
        }
    }

    private void generateRecipeSubtree(JsonObject nodeObj, Identifier parentAdvId, Map<String, Identifier> nodeToAdvId) {
        String id = nodeObj.has("id") ? nodeObj.get("id").getAsString() : "";
        String name = nodeObj.has("name") ? nodeObj.get("name").getAsString() : "";
        String itemStr = nodeObj.has("item") ? nodeObj.get("item").getAsString() : "";
        int tier = nodeObj.has("tier") ? nodeObj.get("tier").getAsInt() : 0;

        Identifier itemIdent = Identifier.parse(itemStr);
        Item icon = BuiltInRegistries.ITEM.get(itemIdent).map(net.minecraft.core.Holder.Reference::value).orElse(Items.CRAFTING_TABLE);
        if (icon == Items.AIR) {
            icon = Items.CRAFTING_TABLE;
        }

        String path = getRecipeAdvancementPath(id, itemIdent);
        Identifier advId = Identifier.fromNamespaceAndPath(APRandomizer.MODID, "received/" + path);
        nodeToAdvId.put(id, advId);

        AdvancementType frame = (tier >= 7) ? AdvancementType.CHALLENGE : AdvancementType.TASK;

        int recipeCount = 0;
        if (nodeObj.has("recipes")) {
            recipeCount = nodeObj.getAsJsonArray("recipes").size();
        }

        Component description = (recipeCount > 1)
                ? Component.literal(name + " (Tier " + tier + ", " + recipeCount + " recipes)")
                : Component.literal(name + " (Tier " + tier + ")");

        Advancement.Builder.recipeAdvancement()
                .display(
                        icon,
                        Component.literal(name),
                        description,
                        frame,
                        true,
                        false,
                        false)
                .parent(parentAdvId)
                .addCriterion("received", RecipeNodeCriteria.TriggerInstance.recipeNode(id))
                .save(output, advId);

        if (nodeObj.has("children")) {
            JsonArray children = nodeObj.getAsJsonArray("children");
            for (JsonElement childEl : children) {
                if (childEl.isJsonObject()) {
                    generateRecipeSubtree(childEl.getAsJsonObject(), advId, nodeToAdvId);
                }
            }
        }
    }

    private String getRecipeAdvancementPath(String nodeId, Identifier itemIdent) {
        if ("node_81".equals(nodeId)) return "boats";
        if ("node_114".equals(nodeId)) return "doors_and_trapdoors";
        return itemIdent.getPath();
    }

    private void generateLinearChain(JsonObject startNode, Identifier rootId, String prefix,
                                     ResourceKey<APItem> itemKey, int totalCount,
                                     String titlePrefix, String descTemplate,
                                     Item defaultIcon, Item maxIcon, AdvancementType maxType,
                                     Map<String, Identifier> nodeToAdvId) {
        JsonObject curr = startNode;
        Identifier parentId = rootId;

        for (int i = 1; i <= totalCount && curr != null; i++) {
            String nodeId = curr.has("id") ? curr.get("id").getAsString() : "";
            Identifier advId = Identifier.fromNamespaceAndPath(APRandomizer.MODID, "received/" + prefix + "_" + i);
            nodeToAdvId.put(nodeId, advId);

            boolean isLast = (i == totalCount);
            Item icon = (isLast && maxIcon != null) ? maxIcon : defaultIcon;
            AdvancementType frame = isLast ? maxType : AdvancementType.TASK;
            String titleText = titlePrefix + " " + i + "/" + totalCount + (isLast ? " (Max)" : "");
            String descText = isLast ? descTemplate + " (Maximum reached!)" : descTemplate;

            Advancement.Builder.recipeAdvancement()
                    .display(
                            icon,
                            Component.literal(titleText),
                            Component.literal(descText),
                            frame,
                            true,
                            false,
                            false)
                    .parent(parentId)
                    .addCriterion("received", ReceivedItemCriteria.TriggerInstance.receivedItem(itemKey, i))
                    .save(output, advId);

            parentId = advId;
            JsonArray children = curr.has("children") ? curr.getAsJsonArray("children") : null;
            if (children != null && !children.isEmpty()) {
                curr = children.get(0).getAsJsonObject();
            } else {
                curr = null;
            }
        }
    }

    private record StructureDef(String path, Item icon, String title, String description) {}

    private void generateStructuresChain(JsonObject startNode, Identifier rootId, Map<String, Identifier> nodeToAdvId) {
        List<StructureDef> structures = List.of(
                new StructureDef("structure_village", Items.EMERALD, "Village Access", "Unlocks access to Villages"),
                new StructureDef("structure_pillager_outpost", Items.CROSSBOW, "Pillager Outpost Access", "Unlocks access to Pillager Outposts"),
                new StructureDef("structure_fortress", Items.BLAZE_ROD, "Nether Fortress Access", "Unlocks access to Nether Fortresses"),
                new StructureDef("structure_bastion", Items.GILDED_BLACKSTONE, "Bastion Remnant Access", "Unlocks access to Bastion Remnants"),
                new StructureDef("structure_trial_chambers", Items.TRIAL_KEY, "Trial Chambers Access", "Unlocks access to Trial Chambers"),
                new StructureDef("structure_ancient_city", Items.ECHO_SHARD, "Ancient City Access", "Unlocks access to Ancient Cities"),
                new StructureDef("structure_ocean_monument", Items.PRISMARINE_SHARD, "Ocean Monument Access", "Unlocks access to Ocean Monuments"),
                new StructureDef("structure_end_city", Items.PURPUR_BLOCK, "End City Access", "Unlocks access to End Cities")
        );

        JsonObject curr = startNode;
        Identifier parentId = rootId;

        for (int i = 1; i <= structures.size(); i++) {
            StructureDef def = structures.get(i - 1);
            String nodeId = (curr != null && curr.has("id")) ? curr.get("id").getAsString() : "";
            Identifier advId = Identifier.fromNamespaceAndPath(APRandomizer.MODID, "received/" + def.path());
            if (!nodeId.isEmpty()) {
                nodeToAdvId.put(nodeId, advId);
            }

            boolean isLast = (i == structures.size());
            AdvancementType frame = isLast ? AdvancementType.CHALLENGE : AdvancementType.TASK;

            Advancement.Builder.recipeAdvancement()
                    .display(
                            def.icon(),
                            Component.literal(def.title()),
                            Component.literal(def.description()),
                            frame,
                            true,
                            false,
                            false)
                    .parent(parentId)
                    .addCriterion("received", ReceivedItemCriteria.TriggerInstance.receivedItem(APItems.STRUCTURE_UNLOCK, i))
                    .save(output, advId);

            parentId = advId;
            if (curr != null) {
                JsonArray children = curr.has("children") ? curr.getAsJsonArray("children") : null;
                curr = (children != null && !children.isEmpty()) ? children.get(0).getAsJsonObject() : null;
            }
        }
    }

    private void generateInventorySlotChain(JsonObject startNode, Identifier rootId, Map<String, Identifier> nodeToAdvId) {
        JsonObject curr = startNode;
        Identifier parentId = rootId;

        for (int i = 1; i <= 27; i++) {
            String nodeId = (curr != null && curr.has("id")) ? curr.get("id").getAsString() : "";
            Identifier advId = Identifier.fromNamespaceAndPath(APRandomizer.MODID, "received/inventory_slot_" + i);
            if (!nodeId.isEmpty()) {
                nodeToAdvId.put(nodeId, advId);
            }

            boolean isLast = (i == 27);
            Item icon = isLast ? Items.ENDER_CHEST : Items.CHEST;
            AdvancementType frame = isLast ? AdvancementType.CHALLENGE : AdvancementType.TASK;
            int totalSlots = 9 + i;

            String title = isLast ? "Inventory Slot 27/27 (Full Inventory)" : "Inventory Slot " + i + "/27";
            String desc = isLast
                    ? "Unlocks slot " + totalSlots + " (All " + totalSlots + "/36 inventory slots available!)"
                    : "Unlocks slot " + totalSlots + " (" + totalSlots + "/36 slots available)";

            Advancement.Builder.recipeAdvancement()
                    .display(
                            icon,
                            Component.literal(title),
                            Component.literal(desc),
                            frame,
                            true,
                            false,
                            false)
                    .parent(parentId)
                    .addCriterion("received", ReceivedItemCriteria.TriggerInstance.receivedItem(APItems.INVENTORY_SLOT_UPGRADE, i))
                    .save(output, advId);

            parentId = advId;
            if (curr != null) {
                JsonArray children = curr.has("children") ? curr.getAsJsonArray("children") : null;
                curr = (children != null && !children.isEmpty()) ? children.get(0).getAsJsonObject() : null;
            }
        }
    }

    private JsonObject loadRecipeTreeJson() {
        try {
            // 1. Try classpath resource
            InputStream is = ReceivedAdvancementProvider.class.getResourceAsStream("/recipe_tree.json");
            if (is == null) {
                is = APRandomizer.class.getResourceAsStream("/recipe_tree.json");
            }
            // 2. Try file path
            if (is == null) {
                File f = new File("recipe_tree.json");
                if (f.exists()) {
                    is = new FileInputStream(f);
                }
            }
            if (is == null) {
                File f = new File("src/main/resources/recipe_tree.json");
                if (f.exists()) {
                    is = new FileInputStream(f);
                }
            }
            if (is != null) {
                try (var reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                    return JsonParser.parseReader(reader).getAsJsonObject();
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error reading recipe_tree.json: " + e.getMessage(), e);
        }
        return null;
    }

    private static boolean isSettingEnabled(JsonObject settingsObj, String key, boolean defaultValue) {
        if (settingsObj != null && settingsObj.has(key)) {
            try {
                JsonElement el = settingsObj.get(key);
                if (el.isJsonPrimitive()) {
                    var prim = el.getAsJsonPrimitive();
                    if (prim.isBoolean()) return prim.getAsBoolean();
                    if (prim.isNumber()) return prim.getAsInt() != 0;
                    if (prim.isString()) {
                        String s = prim.getAsString();
                        if ("true".equalsIgnoreCase(s) || "1".equals(s) || "yes".equalsIgnoreCase(s)) return true;
                        if ("false".equalsIgnoreCase(s) || "0".equals(s) || "no".equalsIgnoreCase(s)) return false;
                    }
                }
            } catch (Exception ignored) {}
        }
        return defaultValue;
    }
}
