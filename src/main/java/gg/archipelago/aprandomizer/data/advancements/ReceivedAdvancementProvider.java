package gg.archipelago.aprandomizer.data.advancements;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import gg.archipelago.aprandomizer.APRandomizer;
import gg.archipelago.aprandomizer.advancements.RecipeNodeCriteria;
import gg.archipelago.aprandomizer.advancements.ReceivedItemCriteria;
import gg.archipelago.aprandomizer.advancements.StructureUnlockCriteria;
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
                        gg.archipelago.aprandomizer.APBlocks.ARCHIPELAGO_ICON.get(),
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
        boolean extraAbilitiesEnabled = isSettingEnabled(settingsObj, "extra_abilities", true);
        if (APRandomizer.getApmcData() != null && APRandomizer.getApmcData().extra_abilities != null) {
            extraAbilitiesEnabled = APRandomizer.getApmcData().isExtraAbilitiesEnabled();
        }

        // Crafts Group Root (Crafting Table)
        Identifier craftsRootId = Identifier.fromNamespaceAndPath(APRandomizer.MODID, "received/crafts_root");
        Advancement.Builder.recipeAdvancement()
                .display(
                        Items.CRAFTING_TABLE,
                        Component.literal("Crafting Recipes"),
                        Component.literal("Crafting recipes and equipment unlocked from Archipelago"),
                        AdvancementType.GOAL,
                        false,
                        false,
                        false)
                .parent(rootId)
                .addCriterion("auto", PlayerTrigger.TriggerInstance.tick())
                .save(output, craftsRootId);

        // Vanilla Abilities / Upgrades Group Root (Heart)
        Identifier vanillaAbilitiesRootId = Identifier.fromNamespaceAndPath(APRandomizer.MODID, "received/vanilla_abilities");
        Advancement.Builder.recipeAdvancement()
                .display(
                        Items.HEART_OF_THE_SEA,
                        Component.literal("Vanilla Upgrades"),
                        Component.literal("Core player attributes and mechanics: Health, Hunger, Reach, Offhand, and Inventory"),
                        AdvancementType.GOAL,
                        false,
                        false,
                        false)
                .parent(rootId)
                .addCriterion("auto", PlayerTrigger.TriggerInstance.tick())
                .save(output, vanillaAbilitiesRootId);

        // 3. Process all branches from root
        for (JsonElement childEl : rootChildren) {
            if (!childEl.isJsonObject()) continue;
            JsonObject childObj = childEl.getAsJsonObject();
            String name = childObj.has("name") ? childObj.get("name").getAsString() : "";
            String item = childObj.has("item") ? childObj.get("item").getAsString() : "";
            int tier = childObj.has("tier") ? childObj.get("tier").getAsInt() : 0;
            String id = childObj.has("id") ? childObj.get("id").getAsString() : "";

            if (!"minecraft:air".equals(item) && tier > 0) {
                // Recipe tree branch -> groups under craftsRootId
                generateRecipeSubtree(childObj, craftsRootId, nodeToAdvId);
            } else if (name.startsWith("HP")) {
                if (hpEnabled) {
                    generateLinearChain(childObj, vanillaAbilitiesRootId, "hp", APItems.HP_UPGRADE, 7,
                            "HP Upgrade", "+1 Heart",
                            Items.APPLE, Items.ENCHANTED_GOLDEN_APPLE, AdvancementType.GOAL, nodeToAdvId);
                }
            } else if (name.startsWith("Hunger")) {
                if (hungerEnabled) {
                    generateLinearChain(childObj, vanillaAbilitiesRootId, "hunger", APItems.HUNGER_UPGRADE, 6,
                            "Hunger Upgrade", "+1 Hunger Bar",
                            Items.COOKED_BEEF, Items.GOLDEN_CARROT, AdvancementType.GOAL, nodeToAdvId);
                }
            } else if (name.startsWith("Hand Length")) {
                if (reachEnabled) {
                    generateLinearChain(childObj, vanillaAbilitiesRootId, "reach", APItems.REACH_UPGRADE, 3,
                            "Reach Upgrade", "+1 Block Reach",
                            Items.FISHING_ROD, Items.FISHING_ROD, AdvancementType.GOAL, nodeToAdvId);
                }
            } else if (name.startsWith("World Border")) {
                if (borderEnabled) {
                    generateWorldBorderChain(childObj, vanillaAbilitiesRootId, nodeToAdvId);
                }
            } else if ("Village".equals(name) || "node_232".equals(id)) {
                if (structEnabled) {
                    generateStructuresChain(childObj, vanillaAbilitiesRootId, nodeToAdvId);
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
                                    false,
                                    false,
                                    false)
                            .parent(vanillaAbilitiesRootId)
                            .addCriterion("received", ReceivedItemCriteria.TriggerInstance.receivedItem(APItems.OFFHAND_UNLOCK, 1))
                            .save(output, advId);
                    nodeToAdvId.put(id, advId);
                }
            } else if (name.startsWith("Inventory Slot") || "node_240".equals(id)) {
                if (invEnabled) {
                    generateInventorySlotChain(childObj, vanillaAbilitiesRootId, nodeToAdvId);
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
                                    false,
                                    false,
                                    false)
                            .parent(vanillaAbilitiesRootId)
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
                                    false,
                                    false,
                                    false)
                            .parent(vanillaAbilitiesRootId)
                            .addCriterion("received", ReceivedItemCriteria.TriggerInstance.receivedItem(APItems.MONSTER_SPAWN_LIGHT_DISABLED, 1))
                            .save(output, advId);
                    nodeToAdvId.put(id, advId);
                }
            }
        }

        // 4. Extra Abilities branch (only generated if enabled)
        if (extraAbilitiesEnabled) {
            generateExtraAbilities(rootId, nodeToAdvId);
        }
    }

    private void generateExtraAbilities(Identifier rootId, Map<String, Identifier> nodeToAdvId) {
        Identifier branchRoot = Identifier.fromNamespaceAndPath(APRandomizer.MODID, "received/extra_abilities");
        Advancement.Builder.recipeAdvancement()
                .display(
                        Items.NETHER_STAR,
                        Component.literal("Extra Abilities"),
                        Component.literal("Special gameplay bonuses and convenience upgrades"),
                        AdvancementType.GOAL,
                        false,
                        false,
                        false)
                .parent(rootId)
                .addCriterion("auto", PlayerTrigger.TriggerInstance.tick())
                .save(output, branchRoot);

        // 1. Progressive Mining (5 tiers)
        Identifier parentMining = branchRoot;
        Item[] pickaxes = { Items.IRON_PICKAXE, Items.GOLDEN_PICKAXE, Items.DIAMOND_PICKAXE, Items.NETHERITE_PICKAXE, Items.NETHERITE_PICKAXE };
        String[] miningShapes = { "2×1×1", "2×3×1", "3×3×1", "3×3×2", "3×3×3" };
        for (int i = 1; i <= 5; i++) {
            Identifier advId = Identifier.fromNamespaceAndPath(APRandomizer.MODID, "received/ability_mining_" + i);
            AdvancementType frame = (i == 5) ? AdvancementType.CHALLENGE : AdvancementType.TASK;
            Advancement.Builder.recipeAdvancement()
                    .display(
                            pickaxes[i - 1],
                            Component.literal("Progressive Mining (" + miningShapes[i - 1] + ")"),
                            Component.literal("Breaks blocks in a " + miningShapes[i - 1] + " shape without breaking tools"),
                            frame,
                            false,
                            false,
                            false)
                    .parent(parentMining)
                    .addCriterion("received", ReceivedItemCriteria.TriggerInstance.receivedItem(APItems.PROGRESSIVE_MINING, i))
                    .save(output, advId);
            parentMining = advId;
        }

        // 2. Tree Feller
        Identifier fellerId = Identifier.fromNamespaceAndPath(APRandomizer.MODID, "received/ability_tree_feller");
        Advancement.Builder.recipeAdvancement()
                .display(
                        Items.DIAMOND_AXE,
                        Component.literal("Tree Feller"),
                        Component.literal("Chops down whole trees and leaves with an axe"),
                        AdvancementType.TASK,
                        false,
                        false,
                        false)
                .parent(branchRoot)
                .addCriterion("received", ReceivedItemCriteria.TriggerInstance.receivedItem(APItems.TREE_FELLER, 1))
                .save(output, fellerId);

        // 3. Progressive Jump (2 tiers)
        Identifier parentJump = branchRoot;
        Item[] jumpIcons = { Items.FEATHER, Items.RABBIT_FOOT };
        String[] jumpNames = { "Double Jump", "Triple Jump" };
        String[] jumpDescs = { "Perform a mid-air jump canceling fall momentum", "Perform a third mid-air jump" };
        for (int i = 1; i <= 2; i++) {
            Identifier advId = Identifier.fromNamespaceAndPath(APRandomizer.MODID, "received/ability_jump_" + i);
            Advancement.Builder.recipeAdvancement()
                    .display(
                            jumpIcons[i - 1],
                            Component.literal(jumpNames[i - 1]),
                            Component.literal(jumpDescs[i - 1]),
                            AdvancementType.TASK,
                            false,
                            false,
                            false)
                    .parent(parentJump)
                    .addCriterion("received", ReceivedItemCriteria.TriggerInstance.receivedItem(APItems.PROGRESSIVE_JUMP, i))
                    .save(output, advId);
            parentJump = advId;
        }

        // 4. Item Magnet
        Identifier magnetId = Identifier.fromNamespaceAndPath(APRandomizer.MODID, "received/ability_item_magnet");
        Advancement.Builder.recipeAdvancement()
                .display(
                        Items.IRON_INGOT,
                        Component.literal("Item Magnet"),
                        Component.literal("Attracts dropped items and experience within 10 blocks"),
                        AdvancementType.TASK,
                        false,
                        false,
                        false)
                .parent(branchRoot)
                .addCriterion("received", ReceivedItemCriteria.TriggerInstance.receivedItem(APItems.ITEM_MAGNET, 1))
                .save(output, magnetId);

        // 5. Step Assist
        Identifier stepId = Identifier.fromNamespaceAndPath(APRandomizer.MODID, "received/ability_step_assist");
        Advancement.Builder.recipeAdvancement()
                .display(
                        Items.LADDER,
                        Component.literal("Step Assist"),
                        Component.literal("Smoothly step up 1 block without jumping"),
                        AdvancementType.TASK,
                        false,
                        false,
                        false)
                .parent(branchRoot)
                .addCriterion("received", ReceivedItemCriteria.TriggerInstance.receivedItem(APItems.STEP_ASSIST, 1))
                .save(output, stepId);

        // 6. Trash Can
        Identifier trashId = Identifier.fromNamespaceAndPath(APRandomizer.MODID, "received/ability_trash_can");
        Advancement.Builder.recipeAdvancement()
                .display(
                        Items.CAULDRON,
                        Component.literal("Trash Can"),
                        Component.literal("Extra inventory slot to destroy unwanted items (Alt+Click)"),
                        AdvancementType.TASK,
                        false,
                        false,
                        false)
                .parent(branchRoot)
                .addCriterion("received", ReceivedItemCriteria.TriggerInstance.receivedItem(APItems.TRASH_CAN, 1))
                .save(output, trashId);

        // 7. Light Holder
        Identifier lightId = Identifier.fromNamespaceAndPath(APRandomizer.MODID, "received/ability_light_holder");
        Advancement.Builder.recipeAdvancement()
                .display(
                        Items.LANTERN,
                        Component.literal("Light Holder"),
                        Component.literal("Slot for light-emitting blocks attached to chest (LambDynamicLights)"),
                        AdvancementType.TASK,
                        false,
                        false,
                        false)
                .parent(branchRoot)
                .addCriterion("received", ReceivedItemCriteria.TriggerInstance.receivedItem(APItems.LIGHT_HOLDER, 1))
                .save(output, lightId);
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
                        false,
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
        if ("node_318".equals(nodeId)) return "beds";
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
                            false,
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

    private record WorldBorderDef(int size, String title, String description) {}

    private void generateWorldBorderChain(JsonObject startNode, Identifier rootId, Map<String, Identifier> nodeToAdvId) {
        List<WorldBorderDef> defs = List.of(
                new WorldBorderDef(1800, "World Border 1/5 (1800 blocks)", "Expands the world border to 1800 blocks (1800×1800, ±900 from spawn)"),
                new WorldBorderDef(3000, "World Border 2/5 (3000 blocks)", "Expands the world border to 3000 blocks (3000×3000, ±1500 from spawn)"),
                new WorldBorderDef(5000, "World Border 3/5 (5000 blocks)", "Expands the world border to 5000 blocks (5000×5000, ±2500 from spawn)"),
                new WorldBorderDef(8000, "World Border 4/5 (8000 blocks)", "Expands the world border to 8000 blocks (8000×8000, ±4000 from spawn)"),
                new WorldBorderDef(60000000, "World Border 5/5 (Max / Unrestricted)", "Removes world border restrictions (maximum size, 60M×60M blocks)")
        );

        JsonObject curr = startNode;
        Identifier parentId = rootId;

        for (int i = 1; i <= defs.size() && curr != null; i++) {
            WorldBorderDef def = defs.get(i - 1);
            String nodeId = curr.has("id") ? curr.get("id").getAsString() : "";
            Identifier advId = Identifier.fromNamespaceAndPath(APRandomizer.MODID, "received/world_border_" + i);
            if (!nodeId.isEmpty()) {
                nodeToAdvId.put(nodeId, advId);
            }

            boolean isLast = (i == defs.size());
            AdvancementType frame = isLast ? AdvancementType.CHALLENGE : AdvancementType.GOAL;

            Advancement.Builder.recipeAdvancement()
                    .display(
                            Items.MAP,
                            Component.literal(def.title()),
                            Component.literal(def.description()),
                            frame,
                            false,
                            false,
                            false)
                    .parent(parentId)
                    .addCriterion("received", ReceivedItemCriteria.TriggerInstance.receivedItem(APItems.WORLD_BORDER_UPGRADE, i))
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

    private record StructureDef(String id, String path, Item icon, String title, String description) {}

    private void generateStructuresChain(JsonObject startNode, Identifier rootId, Map<String, Identifier> nodeToAdvId) {
        Identifier structuresRootId = Identifier.fromNamespaceAndPath(APRandomizer.MODID, "received/structures_root");
        Advancement.Builder.recipeAdvancement()
                .display(
                        Items.CHISELED_STONE_BRICKS,
                        Component.literal("Structure Access"),
                        Component.literal("Structures unlocked across the world"),
                        AdvancementType.GOAL,
                        false,
                        false,
                        false)
                .parent(rootId)
                .addCriterion("auto", PlayerTrigger.TriggerInstance.tick())
                .save(output, structuresRootId);

        List<StructureDef> structures = List.of(
                new StructureDef("Village", "structure_village", Items.EMERALD, "Village Access", "Unlocks access to Villages"),
                new StructureDef("Pillager Outpost", "structure_pillager_outpost", Items.CROSSBOW, "Pillager Outpost Access", "Unlocks access to Pillager Outposts"),
                new StructureDef("Trial Chambers", "structure_trial_chambers", Items.TRIAL_KEY, "Trial Chambers Access", "Unlocks access to Trial Chambers"),
                new StructureDef("Nether Fortress", "structure_fortress", Items.BLAZE_ROD, "Nether Fortress Access", "Unlocks access to Nether Fortresses"),
                new StructureDef("Bastion Remnant", "structure_bastion", Items.GILDED_BLACKSTONE, "Bastion Remnant Access", "Unlocks access to Bastion Remnants"),
                new StructureDef("Stronghold", "structure_stronghold", Items.ENDER_EYE, "Stronghold Access", "Unlocks access to Strongholds"),
                new StructureDef("Ancient City", "structure_ancient_city", Items.ECHO_SHARD, "Ancient City Access", "Unlocks access to Ancient Cities"),
                new StructureDef("Ocean Monument", "structure_ocean_monument", Items.PRISMARINE_SHARD, "Ocean Monument Access", "Unlocks access to Ocean Monuments"),
                new StructureDef("End City", "structure_end_city", Items.PURPUR_BLOCK, "End City Access", "Unlocks access to End Cities")
        );

        JsonObject curr = startNode;
        for (int i = 0; i < structures.size(); i++) {
            StructureDef def = structures.get(i);
            String nodeId = (curr != null && curr.has("id")) ? curr.get("id").getAsString() : "";
            Identifier advId = Identifier.fromNamespaceAndPath(APRandomizer.MODID, "received/" + def.path());
            if (!nodeId.isEmpty()) {
                nodeToAdvId.put(nodeId, advId);
            }

            boolean isLast = (i == structures.size() - 1);
            AdvancementType frame = isLast ? AdvancementType.CHALLENGE : AdvancementType.TASK;

            Advancement.Builder.recipeAdvancement()
                    .display(
                            def.icon(),
                            Component.literal(def.title()),
                            Component.literal(def.description()),
                            frame,
                            false,
                            false,
                            false)
                    .parent(structuresRootId)
                    .addCriterion("unlocked", StructureUnlockCriteria.TriggerInstance.unlocked(def.id()))
                    .save(output, advId);

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
                            false,
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
