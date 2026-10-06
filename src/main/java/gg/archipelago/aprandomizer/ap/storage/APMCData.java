package gg.archipelago.aprandomizer.ap.storage;

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class APMCData {

    @SerializedName(value = "game", alternate = {"game_name"})
    public String game = "minecraft_u2vr";

    public String getGame() {
        return (game != null && !game.isBlank()) ? game : "minecraft_u2vr";
    }

    @SerializedName("world_seed")
    public long world_seed;
    @SerializedName("structures")
    public Map<String, String> structures;
    @SerializedName("seed_name")
    public String seed_name;
    @SerializedName("player_name")
    public String player_name;
    @SerializedName("player_id")
    public int player_id;
    @SerializedName("client_version")
    public int client_version;
    @SerializedName("race")
    public boolean race = false;
    @SerializedName("egg_shards_required")
    public int egg_shards_required = -1;
    @SerializedName("egg_shards_available")
    public int egg_shards_available = -1;
    @SerializedName(value = "check_goal", alternate = {"advancement_goal", "advancements_required", "checks_required"})
    public int advancements_required = -1;
    @SerializedName(value = "advancement_type", alternate = {"advancement_pack", "advancements_type"})
    public Object advancement_type = "bacap";
    @SerializedName(value = "bacap_advancement_step", alternate = {"bacap_step"})
    public int bacap_step = 10;
    @SerializedName(value = "bacap_check_count", alternate = {"bacap_milestones"})
    public int bacap_check_count = 120;
    @SerializedName(value = "selected_bacap_advancements", alternate = {"bacap_selected_advancements", "random_bacap_advancements", "selected_advancements"})
    public List<String> selected_bacap_advancements = new ArrayList<>();
    @SerializedName(value = "selected_bacap_map", alternate = {"bacap_selected_map", "random_bacap_map"})
    public Map<String, Long> selected_bacap_map = new HashMap<>();
    @SerializedName("immediate_respawn")
    public boolean respawn = true;

    @SerializedName("required_bosses")
    public Bosses required_bosses = Bosses.ENDER_DRAGON;

    @SerializedName(value = "structure_unlock_shuffle", alternate = {"structure_shuffle_order"})
    public Object structure_unlock_shuffle;

    @SerializedName(value = "hp_restriction", alternate = {"hp_shuffle", "limit_hp"})
    public Object hp_restriction;

    @SerializedName(value = "hunger_restriction", alternate = {"hunger_shuffle", "limit_hunger"})
    public Object hunger_restriction;

    @SerializedName(value = "offhand_restriction", alternate = {"offhand_shuffle", "limit_offhand"})
    public Object offhand_restriction;

    @SerializedName(value = "reach_restriction", alternate = {"reach_shuffle", "mining_distance_restriction", "mining_reach_restriction"})
    public Object reach_restriction;

    @SerializedName(value = "inventory_slot_restriction", alternate = {"inventory_slot_shuffle", "inventory_restriction", "limit_inventory"})
    public Object inventory_slot_restriction;

    @SerializedName(value = "structure_restriction", alternate = {"structure_shuffle", "restrict_structures"})
    public Object structure_restriction;

    @SerializedName(value = "world_border_restriction", alternate = {"world_border_shuffle", "level_size_restriction", "limit_world_border"})
    public Object world_border_restriction;

    @SerializedName(value = "traps_enabled", alternate = {"traps_shuffle", "include_traps", "enable_traps", "traps"})
    public Object traps_enabled;

    @SerializedName(value = "monster_trap_percentage", alternate = {"monster_traps", "monster_trap_percent", "mob_trap_percentage", "mob_traps"})
    public Object monster_trap_percentage = 20;

    @SerializedName(value = "curses_enabled", alternate = {"curses_shuffle", "permanent_debuffs", "debuffs_enabled", "enable_curses", "curses"})
    public Object curses_enabled;

    @SerializedName(value = "starting_shared_chest", alternate = {"starting_chest", "give_shared_chest", "start_with_shared_chest"})
    public Object starting_shared_chest;

    @SerializedName(value = "biome_checks", alternate = {"enable_biome_checks", "check_biomes"})
    public Object biome_checks;

    @SerializedName(value = "structure_checks", alternate = {"enable_structure_checks", "check_structures"})
    public Object structure_checks;

    @SerializedName(value = "hint_cost", alternate = {"hintCost", "location_check_points", "hint_points_per_check", "check_points", "points_per_check"})
    public Object hint_cost = 10;

    public boolean isBiomeChecks() {
        return gg.archipelago.aprandomizer.SlotData.parseBoolean(biome_checks, false);
    }

    public boolean isBiomeChecksEnabled() {
        return isBiomeChecks();
    }

    public boolean isStructureChecks() {
        return gg.archipelago.aprandomizer.SlotData.parseBoolean(structure_checks, false);
    }

    public boolean isStructureChecksEnabled() {
        return isStructureChecks();
    }


    public boolean isHpRestrictionEnabled() {
        return gg.archipelago.aprandomizer.SlotData.parseBoolean(hp_restriction, true);
    }

    public boolean isHungerRestrictionEnabled() {
        return gg.archipelago.aprandomizer.SlotData.parseBoolean(hunger_restriction, true);
    }

    public boolean isOffhandRestrictionEnabled() {
        return gg.archipelago.aprandomizer.SlotData.parseBoolean(offhand_restriction, true);
    }

    public boolean isReachRestrictionEnabled() {
        return gg.archipelago.aprandomizer.SlotData.parseBoolean(reach_restriction, true);
    }

    public boolean isInventorySlotRestrictionEnabled() {
        return gg.archipelago.aprandomizer.SlotData.parseBoolean(inventory_slot_restriction, true);
    }

    public boolean isStructureRestrictionEnabled() {
        return gg.archipelago.aprandomizer.SlotData.parseBoolean(structure_restriction, true);
    }

    public boolean isWorldBorderRestrictionEnabled() {
        return gg.archipelago.aprandomizer.SlotData.parseBoolean(world_border_restriction, true);
    }

    public boolean isTrapsEnabled() {
        return gg.archipelago.aprandomizer.SlotData.parseBoolean(traps_enabled, true);
    }

    public int getMonsterTrapPercentage() {
        if (monster_trap_percentage == null) return 20;
        if (monster_trap_percentage instanceof Number n) return Math.max(0, Math.min(100, n.intValue()));
        if (monster_trap_percentage instanceof String s) {
            try { return Math.max(0, Math.min(100, Integer.parseInt(s))); } catch (Exception ignored) {}
        }
        return 20;
    }

    public boolean isCursesEnabled() {
        return gg.archipelago.aprandomizer.SlotData.parseBoolean(curses_enabled, true);
    }

    public boolean isStructureUnlockShuffle() {
        return gg.archipelago.aprandomizer.SlotData.parseBoolean(structure_unlock_shuffle, false);
    }

    @SerializedName("recipe_tier_bias")
    public double recipe_tier_bias = 70.0;

    @SerializedName(value = "recipe_unlock_mode", alternate = {"recipe_mode", "unlock_mode"})
    public Object recipe_unlock_mode = 0;

    @SerializedName(value = "recipe_tier_unlock_percentage", alternate = {"recipe_tier_threshold", "tier_unlock_percentage", "tier_threshold"})
    public Object recipe_tier_unlock_percentage = 50;

    public boolean isTicketMode() {
        if (recipe_unlock_mode == null) return false;
        if (recipe_unlock_mode instanceof Number n) return n.intValue() == 1;
        String s = String.valueOf(recipe_unlock_mode).trim().toLowerCase(java.util.Locale.ENGLISH);
        return s.contains("ticket") || s.contains("skilltree") || s.equals("1");
    }

    public int getRecipeTierUnlockPercentage() {
        if (recipe_tier_unlock_percentage == null) return 50;
        if (recipe_tier_unlock_percentage instanceof Number n) return Math.clamp(n.intValue(), 0, 100);
        try {
            return Math.clamp(Integer.parseInt(String.valueOf(recipe_tier_unlock_percentage).trim()), 0, 100);
        } catch (Exception ignored) {
            return 50;
        }
    }

    @SerializedName(value = "wandering_trader_trades", alternate = {"trader_trades", "wandering_trader_items"})
    public Object wandering_trader_trades = 10;

    public int getWanderingTraderTrades() {
        if (wandering_trader_trades == null) return 10;
        if (wandering_trader_trades instanceof Number n) return n.intValue();
        if (wandering_trader_trades instanceof String s) {
            try { return Integer.parseInt(s); } catch (Exception ignored) {}
        }
        return 10;
    }

    public boolean isStartingSharedChestEnabled() {
        return gg.archipelago.aprandomizer.SlotData.parseBoolean(starting_shared_chest, true);
    }

    public boolean isVanillaAdvancements() {
        if (advancement_type == null) return false;
        String val = String.valueOf(advancement_type).trim().toLowerCase(java.util.Locale.ENGLISH);
        return val.equals("vanilla") || val.equals("0") || val.equals("false");
    }

    public boolean isBacapMilestones() {
        if (advancement_type == null) return true; // Default is BACAP
        String val = String.valueOf(advancement_type).trim().toLowerCase(java.util.Locale.ENGLISH);
        return val.equals("bacap") || val.equals("1") || val.equals("true") || val.equals("bacap_milestone") || val.equals("milestone");
    }

    public boolean isRandomBacap() {
        if (advancement_type == null) return false;
        String val = String.valueOf(advancement_type).trim().toLowerCase(java.util.Locale.ENGLISH);
        return val.equals("random_bacap") || val.equals("random") || val.equals("2");
    }

    public boolean isBacapAdvancements() {
        return isBacapMilestones() || isRandomBacap();
    }

    public int getBacapStep() {
        return bacap_step > 0 ? bacap_step : 10;
    }

    public int getBacapCheckCount() {
        if (bacap_check_count > 0) {
            return bacap_check_count;
        }
        int step = getBacapStep();
        return Math.min(gg.archipelago.aprandomizer.managers.advancementmanager.AdvancementManager.TOTAL_BACAP_ADVANCEMENTS / step,
                gg.archipelago.aprandomizer.managers.advancementmanager.AdvancementManager.MAX_BACAP_MILESTONES);
    }

    public int getHintCost() {
        if (hint_cost == null) return 10;
        if (hint_cost instanceof Number n) return Math.max(0, n.intValue());
        if (hint_cost instanceof String s) {
            try { return Math.max(0, Integer.parseInt(s.trim())); } catch (Exception ignored) {}
        }
        return 10;
    }

    public int getLocationCheckPoints() {
        return getHintCost();
    }

    @SerializedName("server")
    @Nullable
    public String server;

    @SerializedName("port")
    public int port;

    public State state = State.VALID;

    public boolean dragonStartSpawned() {
        //if our goal is not to kill the dragon, start with the dragon spawned.
        if (required_bosses == Bosses.NONE || required_bosses == Bosses.WITHER)
            return true;
        //if our goal is "fast" and requires no advancements or egg shards then the dragon should start spawned too;
        return advancements_required == 0 && egg_shards_required == 0;
    }

    public int getCheckGoal() {
        return advancements_required;
    }

    public int getAdvancementGoal() {
        return advancements_required;
    }

    public enum State {
        VALID, MISSING, INVALID_VERSION, INVALID_SEED
    }

    public enum Bosses {
        @SerializedName("none")
        NONE(false, false),
        @SerializedName("ender_dragon")
        ENDER_DRAGON(true, false),
        @SerializedName("wither")
        WITHER(false, true),
        @SerializedName("both")
        BOTH(true, true);

        private final boolean dragon;
        private final boolean wither;

        Bosses(boolean dragon, boolean wither) {
            this.dragon = dragon;
            this.wither = wither;
        }

        public boolean hasDragon() {
            return dragon;
        }

        public boolean hasWither() {
            return wither;
        }
    }

    public static APMCData createInvalid() {
        APMCData data = new APMCData();
        data.state = APMCData.State.MISSING;
        return data;
    }

}
