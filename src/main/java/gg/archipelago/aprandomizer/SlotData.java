package gg.archipelago.aprandomizer;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.annotations.SerializedName;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import gg.archipelago.aprandomizer.common.Utils.Utils;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.commands.arguments.item.ItemParser;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SlotData {

    public int include_hard_advancements;
    public int include_insane_advancements;
    public int include_postgame_advancements;
    @SerializedName(value = "check_goal", alternate = {"advancement_goal", "advancements_required", "checks_required"})
    public int advancement_goal = 40;
    public long minecraft_world_seed;
    public int client_version;

    //public boolean connected = false; //TODO: See if we can find out if we're connected through here

    @SerializedName("MC35")
    public boolean MC35 = false;

    @SerializedName("death_link")
    public boolean deathlink = false;

    @SerializedName(value = "structure_unlock_shuffle", alternate = {"structure_shuffle_order"})
    public Object structureUnlockShuffle;

    @SerializedName(value = "hp_restriction", alternate = {"hp_shuffle", "limit_hp"})
    public Object hpRestriction;

    @SerializedName(value = "hunger_restriction", alternate = {"hunger_shuffle", "limit_hunger"})
    public Object hungerRestriction;

    @SerializedName(value = "offhand_restriction", alternate = {"offhand_shuffle", "limit_offhand"})
    public Object offhandRestriction;

    @SerializedName(value = "reach_restriction", alternate = {"reach_shuffle", "mining_distance_restriction", "mining_reach_restriction"})
    public Object reachRestriction;

    @SerializedName(value = "inventory_slot_restriction", alternate = {"inventory_slot_shuffle", "inventory_restriction", "limit_inventory"})
    public Object inventorySlotRestriction;

    @SerializedName(value = "structure_restriction", alternate = {"structure_shuffle", "restrict_structures"})
    public Object structureRestriction;

    @SerializedName(value = "world_border_restriction", alternate = {"world_border_shuffle", "level_size_restriction", "limit_world_border"})
    public Object worldBorderRestriction;

    @SerializedName(value = "traps_enabled", alternate = {"traps_shuffle", "include_traps", "enable_traps", "traps"})
    public Object trapsEnabled;

    @SerializedName(value = "monster_trap_percentage", alternate = {"monster_traps", "monster_trap_percent", "mob_trap_percentage", "mob_traps"})
    public Object monsterTrapPercentage = 20;

    @SerializedName(value = "curses_enabled", alternate = {"curses_shuffle", "permanent_debuffs", "debuffs_enabled", "enable_curses", "curses"})
    public Object cursesEnabled;

    @SerializedName("recipe_tier_bias")
    public double recipeTierBias = 70.0;

    @SerializedName(value = "recipe_unlock_mode", alternate = {"recipe_mode", "unlock_mode"})
    public Object recipeUnlockMode = 0;

    @SerializedName(value = "recipe_tier_unlock_percentage", alternate = {"recipe_tier_threshold", "tier_unlock_percentage", "tier_threshold"})
    public Object recipeTierUnlockPercentage = 50;

    public boolean isTicketMode() {
        if (recipeUnlockMode == null) return false;
        if (recipeUnlockMode instanceof Number n) return n.intValue() == 1;
        String s = String.valueOf(recipeUnlockMode).trim().toLowerCase(java.util.Locale.ENGLISH);
        return s.contains("ticket") || s.contains("skilltree") || s.equals("1");
    }

    public int getRecipeTierUnlockPercentage() {
        if (recipeTierUnlockPercentage == null) return 50;
        if (recipeTierUnlockPercentage instanceof Number n) return Math.clamp(n.intValue(), 0, 100);
        try {
            return Math.clamp(Integer.parseInt(String.valueOf(recipeTierUnlockPercentage).trim()), 0, 100);
        } catch (Exception ignored) {
            return 50;
        }
    }

    @SerializedName(value = "wandering_trader_trades", alternate = {"trader_trades", "wandering_trader_items"})
    public Object wanderingTraderTrades = 10;

    @SerializedName(value = "starting_shared_chest", alternate = {"starting_chest", "give_shared_chest", "start_with_shared_chest"})
    public Object startingSharedChest;

    @SerializedName(value = "biome_checks", alternate = {"enable_biome_checks", "check_biomes"})
    public Object biomeChecks;

    @SerializedName(value = "structure_checks", alternate = {"enable_structure_checks", "check_structures"})
    public Object structureChecks;


    @SerializedName(value = "advancement_type", alternate = {"advancement_pack", "advancements_type"})
    public Object advancementType = "bacap";

    @SerializedName(value = "bacap_advancement_step", alternate = {"bacap_step"})
    public int bacapStep = 10;

    @SerializedName(value = "bacap_check_count", alternate = {"bacap_milestones"})
    public int bacapCheckCount = 120;

    @SerializedName(value = "selected_bacap_advancements", alternate = {"bacap_selected_advancements", "random_bacap_advancements", "selected_advancements"})
    public List<String> selectedBacapAdvancements = new ArrayList<>();

    @SerializedName(value = "selected_bacap_map", alternate = {"bacap_selected_map", "random_bacap_map"})
    public Map<String, Long> selectedBacapMap = new HashMap<>();

    @SerializedName(value = "hint_cost", alternate = {"hintCost", "location_check_points", "hint_points_per_check", "check_points", "points_per_check"})
    public Object hintCost = 10;

    @SerializedName("starting_items")
    public String startingItems;

    public static boolean parseBoolean(Object val, boolean defaultVal) {
        if (val == null) return defaultVal;
        if (val instanceof Boolean b) return b;
        if (val instanceof Number n) return n.intValue() != 0;
        if (val instanceof String s) {
            if ("true".equalsIgnoreCase(s) || "1".equals(s) || "yes".equalsIgnoreCase(s) || "on".equalsIgnoreCase(s)) return true;
            if ("false".equalsIgnoreCase(s) || "0".equals(s) || "no".equalsIgnoreCase(s) || "off".equalsIgnoreCase(s)) return false;
        }
        return defaultVal;
    }

    public boolean isHpRestrictionEnabled() {
        return parseBoolean(hpRestriction, true);
    }

    public boolean isHungerRestrictionEnabled() {
        return parseBoolean(hungerRestriction, true);
    }

    public boolean isOffhandRestrictionEnabled() {
        return parseBoolean(offhandRestriction, true);
    }

    public boolean isReachRestrictionEnabled() {
        return parseBoolean(reachRestriction, true);
    }

    public boolean isInventorySlotRestrictionEnabled() {
        return parseBoolean(inventorySlotRestriction, true);
    }

    public boolean isStructureRestrictionEnabled() {
        return parseBoolean(structureRestriction, true);
    }

    public boolean isWorldBorderRestrictionEnabled() {
        return parseBoolean(worldBorderRestriction, true);
    }

    public boolean isTrapsEnabled() {
        return parseBoolean(trapsEnabled, true);
    }

    public int getMonsterTrapPercentage() {
        if (monsterTrapPercentage == null) return 20;
        if (monsterTrapPercentage instanceof Number n) return Math.max(0, Math.min(100, n.intValue()));
        if (monsterTrapPercentage instanceof String s) {
            try { return Math.max(0, Math.min(100, Integer.parseInt(s))); } catch (Exception ignored) {}
        }
        return 20;
    }

    public boolean isCursesEnabled() {
        return parseBoolean(cursesEnabled, true);
    }

    public boolean isStructureUnlockShuffle() {
        return parseBoolean(structureUnlockShuffle, false);
    }

    public int getWanderingTraderTrades() {
        if (wanderingTraderTrades == null) return 10;
        if (wanderingTraderTrades instanceof Number n) return n.intValue();
        if (wanderingTraderTrades instanceof String s) {
            try { return Integer.parseInt(s); } catch (Exception ignored) {}
        }
        return 10;
    }

    public boolean isStartingSharedChestEnabled() {
        return parseBoolean(startingSharedChest, true);
    }

    public boolean isBiomeChecks() {
        return parseBoolean(biomeChecks, false);
    }

    public boolean isBiomeChecksEnabled() {
        return isBiomeChecks();
    }

    public boolean isStructureChecks() {
        return parseBoolean(structureChecks, false);
    }

    public boolean isStructureChecksEnabled() {
        return isStructureChecks();
    }

    public boolean isVanillaAdvancements() {
        if (advancementType == null) return false;
        String val = String.valueOf(advancementType).trim().toLowerCase(java.util.Locale.ENGLISH);
        return val.equals("vanilla") || val.equals("0") || val.equals("false");
    }

    public boolean isBacapMilestones() {
        if (advancementType == null) return true; // Default is BACAP
        String val = String.valueOf(advancementType).trim().toLowerCase(java.util.Locale.ENGLISH);
        return val.equals("bacap") || val.equals("1") || val.equals("true") || val.equals("bacap_milestone") || val.equals("milestone");
    }

    public boolean isRandomBacap() {
        if (advancementType == null) return false;
        String val = String.valueOf(advancementType).trim().toLowerCase(java.util.Locale.ENGLISH);
        return val.equals("random_bacap") || val.equals("random") || val.equals("2");
    }

    public boolean isBacapAdvancements() {
        return isBacapMilestones() || isRandomBacap();
    }

    public int getBacapStep() {
        return bacapStep > 0 ? bacapStep : 10;
    }

    public int getBacapCheckCount() {
        if (bacapCheckCount > 0) {
            return bacapCheckCount;
        }
        int step = getBacapStep();
        return Math.min(gg.archipelago.aprandomizer.managers.advancementmanager.AdvancementManager.TOTAL_BACAP_ADVANCEMENTS / step,
                gg.archipelago.aprandomizer.managers.advancementmanager.AdvancementManager.MAX_BACAP_MILESTONES);
    }

    public int getHintCost() {
        if (hintCost == null) return 10;
        if (hintCost instanceof Number n) return Math.max(0, n.intValue());
        if (hintCost instanceof String s) {
            try { return Math.max(0, Integer.parseInt(s.trim())); } catch (Exception ignored) {}
        }
        return 10;
    }

    public int getLocationCheckPoints() {
        return getHintCost();
    }

    transient public final List<ItemStack> startingItemStacks = new ArrayList<>();

    //public boolean isConnected(){
    //    return connected;
    //}
    public boolean getMC35() {
        return MC35;
    }

    public boolean getDeath_link() {
        return deathlink;
    }

    public int getInclude_hard_advancements() {
        return include_hard_advancements;
    }

    public int getClient_version() {
        return client_version;
    }

    public long getMinecraft_world_seed() {
        return minecraft_world_seed;
    }

    public int getAdvancement_goal() {
        return advancement_goal;
    }

    public int getAdvancementGoal() {
        return advancement_goal;
    }

    public int getCheckGoal() {
        return advancement_goal;
    }

    public int getInclude_postgame_advancements() {
        return include_postgame_advancements;
    }

    public int getInclude_insane_advancements() {
        return include_insane_advancements;
    }

    public void parseStartingItems(HolderLookup.Provider registries) {
        JsonArray si = JsonParser.parseString(startingItems).getAsJsonArray();
        ItemParser itemParser = new ItemParser(registries);
        for (JsonElement jsonItem : si) {
            JsonObject object = jsonItem.getAsJsonObject();
            String itemName = object.getAsJsonObject().get("item").getAsString();

            int amount = object.has("amount") ? object.get("amount").getAsInt() : 1;

            try {
                ItemInput item = itemParser.parse(new StringReader(itemName));

                ItemStack iStack = new ItemStack(item.item(), amount, item.components());
                // TODO: figure out how to parse a string of components into actual components
//                if(object.has("nbt"))
//                    iStack.set(TagParser.parseTag(object.get("nbt").getAsString()));

                startingItemStacks.add(iStack);

            } catch (CommandSyntaxException e) {
                Utils.sendMessageToAll("No such item \"" + itemName + "\"");
            }
        }
    }
}
