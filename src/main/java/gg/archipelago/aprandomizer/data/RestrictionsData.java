package gg.archipelago.aprandomizer.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

public record RestrictionsData(
        int hpUpgrades,
        int hungerUpgrades,
        int reachUpgrades,
        int worldBorderUpgrades,
        int structureUpgrades,
        boolean offhandUnlocked,
        int inventorySlotUpgrades,
        boolean monsterSunBurningDisabled,
        boolean ignoreLightSpawningEnabled,
        int recipeUnlockTier,
        List<String> unlockedStructureIds,
        int recipeTickets
) {
    public static final RestrictionsData DEFAULT = new RestrictionsData(0, 0, 0, 0, 0, false, 0, false, false, 0, List.of(), 0);

    public RestrictionsData(int hpUpgrades, int hungerUpgrades, int reachUpgrades, int worldBorderUpgrades, int structureUpgrades, boolean offhandUnlocked, int inventorySlotUpgrades, boolean monsterSunBurningDisabled, boolean ignoreLightSpawningEnabled, int recipeUnlockTier, List<String> unlockedStructureIds) {
        this(hpUpgrades, hungerUpgrades, reachUpgrades, worldBorderUpgrades, structureUpgrades, offhandUnlocked, inventorySlotUpgrades, monsterSunBurningDisabled, ignoreLightSpawningEnabled, recipeUnlockTier, unlockedStructureIds, 0);
    }

    public static final Codec<RestrictionsData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("hpUpgrades", 0).forGetter(RestrictionsData::hpUpgrades),
            Codec.INT.optionalFieldOf("hungerUpgrades", 0).forGetter(RestrictionsData::hungerUpgrades),
            Codec.INT.optionalFieldOf("reachUpgrades", 0).forGetter(RestrictionsData::reachUpgrades),
            Codec.INT.optionalFieldOf("worldBorderUpgrades", 0).forGetter(RestrictionsData::worldBorderUpgrades),
            Codec.INT.optionalFieldOf("structureUpgrades", 0).forGetter(RestrictionsData::structureUpgrades),
            Codec.BOOL.optionalFieldOf("offhandUnlocked", false).forGetter(RestrictionsData::offhandUnlocked),
            Codec.INT.optionalFieldOf("inventorySlotUpgrades", 0).forGetter(RestrictionsData::inventorySlotUpgrades),
            Codec.BOOL.optionalFieldOf("monsterSunBurningDisabled", false).forGetter(RestrictionsData::monsterSunBurningDisabled),
            Codec.BOOL.optionalFieldOf("ignoreLightSpawningEnabled", false).forGetter(RestrictionsData::ignoreLightSpawningEnabled),
            Codec.INT.optionalFieldOf("recipeUnlockTier", 0).forGetter(RestrictionsData::recipeUnlockTier),
            Codec.STRING.listOf().optionalFieldOf("unlockedStructureIds", List.of()).forGetter(RestrictionsData::unlockedStructureIds),
            Codec.INT.optionalFieldOf("recipeTickets", 0).forGetter(RestrictionsData::recipeTickets)
    ).apply(instance, RestrictionsData::new));

    public RestrictionsData withHpUpgrades(int hpUpgrades) {
        return new RestrictionsData(hpUpgrades, hungerUpgrades, reachUpgrades, worldBorderUpgrades, structureUpgrades, offhandUnlocked, inventorySlotUpgrades, monsterSunBurningDisabled, ignoreLightSpawningEnabled, recipeUnlockTier, unlockedStructureIds, recipeTickets);
    }

    public RestrictionsData withHungerUpgrades(int hungerUpgrades) {
        return new RestrictionsData(hpUpgrades, hungerUpgrades, reachUpgrades, worldBorderUpgrades, structureUpgrades, offhandUnlocked, inventorySlotUpgrades, monsterSunBurningDisabled, ignoreLightSpawningEnabled, recipeUnlockTier, unlockedStructureIds, recipeTickets);
    }

    public RestrictionsData withReachUpgrades(int reachUpgrades) {
        return new RestrictionsData(hpUpgrades, hungerUpgrades, reachUpgrades, worldBorderUpgrades, structureUpgrades, offhandUnlocked, inventorySlotUpgrades, monsterSunBurningDisabled, ignoreLightSpawningEnabled, recipeUnlockTier, unlockedStructureIds, recipeTickets);
    }

    public RestrictionsData withWorldBorderUpgrades(int worldBorderUpgrades) {
        return new RestrictionsData(hpUpgrades, hungerUpgrades, reachUpgrades, worldBorderUpgrades, structureUpgrades, offhandUnlocked, inventorySlotUpgrades, monsterSunBurningDisabled, ignoreLightSpawningEnabled, recipeUnlockTier, unlockedStructureIds, recipeTickets);
    }

    public RestrictionsData withStructureUpgrades(int structureUpgrades) {
        return new RestrictionsData(hpUpgrades, hungerUpgrades, reachUpgrades, worldBorderUpgrades, structureUpgrades, offhandUnlocked, inventorySlotUpgrades, monsterSunBurningDisabled, ignoreLightSpawningEnabled, recipeUnlockTier, unlockedStructureIds, recipeTickets);
    }

    public RestrictionsData withOffhandUnlocked(boolean offhandUnlocked) {
        return new RestrictionsData(hpUpgrades, hungerUpgrades, reachUpgrades, worldBorderUpgrades, structureUpgrades, offhandUnlocked, inventorySlotUpgrades, monsterSunBurningDisabled, ignoreLightSpawningEnabled, recipeUnlockTier, unlockedStructureIds, recipeTickets);
    }

    public RestrictionsData withInventorySlotUpgrades(int inventorySlotUpgrades) {
        return new RestrictionsData(hpUpgrades, hungerUpgrades, reachUpgrades, worldBorderUpgrades, structureUpgrades, offhandUnlocked, inventorySlotUpgrades, monsterSunBurningDisabled, ignoreLightSpawningEnabled, recipeUnlockTier, unlockedStructureIds, recipeTickets);
    }

    public RestrictionsData withMonsterSunBurningDisabled(boolean disabled) {
        return new RestrictionsData(hpUpgrades, hungerUpgrades, reachUpgrades, worldBorderUpgrades, structureUpgrades, offhandUnlocked, inventorySlotUpgrades, disabled, ignoreLightSpawningEnabled, recipeUnlockTier, unlockedStructureIds, recipeTickets);
    }

    public RestrictionsData withIgnoreLightSpawningEnabled(boolean enabled) {
        return new RestrictionsData(hpUpgrades, hungerUpgrades, reachUpgrades, worldBorderUpgrades, structureUpgrades, offhandUnlocked, inventorySlotUpgrades, monsterSunBurningDisabled, enabled, recipeUnlockTier, unlockedStructureIds, recipeTickets);
    }

    public RestrictionsData withRecipeUnlockTier(int tier) {
        return new RestrictionsData(hpUpgrades, hungerUpgrades, reachUpgrades, worldBorderUpgrades, structureUpgrades, offhandUnlocked, inventorySlotUpgrades, monsterSunBurningDisabled, ignoreLightSpawningEnabled, tier, unlockedStructureIds, recipeTickets);
    }

    public RestrictionsData withUnlockedStructureIds(List<String> list) {
        return new RestrictionsData(hpUpgrades, hungerUpgrades, reachUpgrades, worldBorderUpgrades, structureUpgrades, offhandUnlocked, inventorySlotUpgrades, monsterSunBurningDisabled, ignoreLightSpawningEnabled, recipeUnlockTier, list, recipeTickets);
    }

    public RestrictionsData withRecipeTickets(int tickets) {
        return new RestrictionsData(hpUpgrades, hungerUpgrades, reachUpgrades, worldBorderUpgrades, structureUpgrades, offhandUnlocked, inventorySlotUpgrades, monsterSunBurningDisabled, ignoreLightSpawningEnabled, recipeUnlockTier, unlockedStructureIds, tickets);
    }
}
