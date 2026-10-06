package gg.archipelago.aprandomizer.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import gg.archipelago.aprandomizer.APRandomizer;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

public class WorldData extends SavedData {

    private String seedName = "";
    private int dragonState = ASLEEP;
    private int witherState = ASLEEP;
    private boolean jailPlayers = true;
    private LongSet locations = new LongOpenHashSet();
    private int index = 0;
    private int dragonEggShards = 0;
    private List<ResourceKey<Recipe<?>>> unlockedRecipes = new ArrayList<>();
    private RestrictionsData restrictions = RestrictionsData.DEFAULT;
    private String sharedChestJson = "[]";
    private HashSet<String> earnedBacapAdvancements = new HashSet<>();
    private HashSet<String> visitedBiomes = new HashSet<>();
    private HashSet<String> visitedStructures = new HashSet<>();

    public static final int KILLED = 30;
    public static final int SPAWNED = 20;
    public static final int WAITING = 15;
    public static final int ASLEEP = 10;

    public static final Codec<WorldData> CODEC = RecordCodecBuilder.create(instance -> instance
            .group(
                    Codec.STRING.fieldOf("seedName").forGetter(WorldData::getSeedName),
                    Codec.INT.fieldOf("dragonState").forGetter(WorldData::getDragonState),
                    Codec.INT.optionalFieldOf("witherState", ASLEEP).forGetter(WorldData::getWitherState),
                    Codec.BOOL.fieldOf("jailPlayers").forGetter(WorldData::getJailPlayers),
                    Codec.LONG_STREAM.<LongSet>xmap(stream -> new LongOpenHashSet(stream.toArray()), LongSet::longStream).fieldOf("locations").forGetter(WorldData::getLocations),
                    Codec.INT.fieldOf("index").forGetter(WorldData::getItemIndex),
                    Codec.INT.optionalFieldOf("dragonEggShards", 0).forGetter(WorldData::getDragonEggShards),
                    ResourceKey.codec(Registries.RECIPE).listOf().xmap(list -> (List<ResourceKey<Recipe<?>>>) new ArrayList<>(list), Function.identity()).fieldOf("unlockedRecipes").forGetter(WorldData::getUnlockedRecipes),
                    RestrictionsData.CODEC.optionalFieldOf("restrictions", RestrictionsData.DEFAULT).forGetter(WorldData::getRestrictions),
                    Codec.STRING.optionalFieldOf("sharedChestJson", "[]").forGetter(WorldData::getSharedChestJson),
                    Codec.STRING.listOf().xmap(HashSet::new, ArrayList::new).optionalFieldOf("earnedBacapAdvancements", new HashSet<>()).forGetter(WorldData::getEarnedBacapAdvancements),
                    Codec.STRING.listOf().xmap(HashSet::new, ArrayList::new).optionalFieldOf("visitedBiomes", new HashSet<>()).forGetter(WorldData::getVisitedBiomes),
                    Codec.STRING.listOf().xmap(HashSet::new, ArrayList::new).optionalFieldOf("visitedStructures", new HashSet<>()).forGetter(WorldData::getVisitedStructures))
            .apply(instance, WorldData::new));


    public void setSeedName(String seedName) {
        this.seedName = seedName;
        this.setDirty();
    }

    public String getSeedName() {
        return seedName;
    }

    public void setDragonState(int dragonState) {
        this.dragonState = dragonState;
        this.setDirty();
    }

    public void setDragonKilled() {
        setDragonState(KILLED);
    }

    public int getDragonState() {
        return dragonState;
    }

    public boolean isDragonKilled() {
        return dragonState == KILLED;
    }

    public boolean getJailPlayers() {
        return jailPlayers;
    }

    public void setJailPlayers(boolean jailPlayers) {
        this.jailPlayers = jailPlayers;
        this.setDirty();
    }

    public void addLocation(long location) {
        this.locations.add(location);
        this.setDirty();
    }

    public void addLocations(long[] locations) {
        this.locations.addAll(new LongOpenHashSet(locations));
        this.setDirty();
    }

    public LongSet getLocations() {
        return locations;
    }

    public int getItemIndex() {
        return this.index;
    }

    public void setItemIndex(int index) {
        this.index = index;
        this.setDirty();
    }

    public static SavedDataType<WorldData> getFactory() {
        return new SavedDataType<>(Identifier.fromNamespaceAndPath(APRandomizer.MODID, "aprandomizer/worlddata"), WorldData::new, WorldData.CODEC);
    }

    public WorldData() {
    }

    private WorldData(String seedName, int dragonState, int witherState, boolean jailPlayers, LongSet locations, int itemIndex, int dragonEggShards, List<ResourceKey<Recipe<?>>> unlockedRecipes, RestrictionsData restrictions, String sharedChestJson, HashSet<String> earnedBacapAdvancements, HashSet<String> visitedBiomes, HashSet<String> visitedStructures) {
        this.seedName = seedName;
        this.dragonState = dragonState;
        this.witherState = witherState;
        this.jailPlayers = jailPlayers;
        this.locations = locations;
        this.index = itemIndex;
        this.dragonEggShards = dragonEggShards;
        this.unlockedRecipes = unlockedRecipes;
        this.restrictions = restrictions != null ? restrictions : RestrictionsData.DEFAULT;
        this.sharedChestJson = (sharedChestJson != null && !sharedChestJson.isBlank()) ? sharedChestJson : "[]";
        this.earnedBacapAdvancements = earnedBacapAdvancements != null ? earnedBacapAdvancements : new HashSet<>();
        this.visitedBiomes = visitedBiomes != null ? visitedBiomes : new HashSet<>();
        this.visitedStructures = visitedStructures != null ? visitedStructures : new HashSet<>();
    }

    public HashSet<String> getEarnedBacapAdvancements() {
        return earnedBacapAdvancements;
    }

    public boolean addBacapAdvancement(String advId) {
        boolean added = this.earnedBacapAdvancements.add(advId);
        if (added) {
            this.setDirty();
        }
        return added;
    }

    public int getEarnedBacapAdvancementsCount() {
        return this.earnedBacapAdvancements.size();
    }

    public HashSet<String> getVisitedBiomes() {
        return visitedBiomes;
    }

    public boolean addVisitedBiome(String biomeId) {
        boolean added = this.visitedBiomes.add(biomeId);
        if (added) {
            this.setDirty();
        }
        return added;
    }

    public boolean hasVisitedBiome(String biomeId) {
        return this.visitedBiomes.contains(biomeId);
    }

    public HashSet<String> getVisitedStructures() {
        return visitedStructures;
    }

    public boolean addVisitedStructure(String structId) {
        boolean added = this.visitedStructures.add(structId);
        if (added) {
            this.setDirty();
        }
        return added;
    }

    public boolean hasVisitedStructure(String structId) {
        return this.visitedStructures.contains(structId);
    }


    public String getSharedChestJson() {
        return sharedChestJson;
    }

    public void setSharedChestJson(String sharedChestJson) {
        this.sharedChestJson = (sharedChestJson != null && !sharedChestJson.isBlank()) ? sharedChestJson : "[]";
        this.setDirty();
    }

    public RestrictionsData getRestrictions() {
        return restrictions;
    }

    public int getHpUpgrades() {
        return restrictions.hpUpgrades();
    }

    public void incrementHpUpgrades() {
        this.restrictions = restrictions.withHpUpgrades(restrictions.hpUpgrades() + 1);
        this.setDirty();
    }

    public int getHungerUpgrades() {
        return restrictions.hungerUpgrades();
    }

    public void incrementHungerUpgrades() {
        this.restrictions = restrictions.withHungerUpgrades(restrictions.hungerUpgrades() + 1);
        this.setDirty();
    }

    public int getReachUpgrades() {
        return restrictions.reachUpgrades();
    }

    public void incrementReachUpgrades() {
        this.restrictions = restrictions.withReachUpgrades(restrictions.reachUpgrades() + 1);
        this.setDirty();
    }

    public int getWorldBorderUpgrades() {
        return restrictions.worldBorderUpgrades();
    }

    public void incrementWorldBorderUpgrades() {
        this.restrictions = restrictions.withWorldBorderUpgrades(restrictions.worldBorderUpgrades() + 1);
        this.setDirty();
    }

    public int getStructureUpgrades() {
        return restrictions.structureUpgrades();
    }

    public void incrementStructureUpgrades() {
        this.restrictions = restrictions.withStructureUpgrades(restrictions.structureUpgrades() + 1);
        this.setDirty();
    }

    public boolean isOffhandUnlocked() {
        return restrictions.offhandUnlocked();
    }

    public void setOffhandUnlocked(boolean unlocked) {
        this.restrictions = restrictions.withOffhandUnlocked(unlocked);
        this.setDirty();
    }

    public int getInventorySlotUpgrades() {
        return restrictions.inventorySlotUpgrades();
    }

    public void incrementInventorySlotUpgrades() {
        this.restrictions = restrictions.withInventorySlotUpgrades(restrictions.inventorySlotUpgrades() + 1);
        this.setDirty();
    }

    public boolean isMonsterSunBurningDisabled() {
        return restrictions.monsterSunBurningDisabled();
    }

    public void setMonsterSunBurningDisabled(boolean disabled) {
        this.restrictions = restrictions.withMonsterSunBurningDisabled(disabled);
        this.setDirty();
    }

    public boolean isIgnoreLightSpawningEnabled() {
        return restrictions.ignoreLightSpawningEnabled();
    }

    public void setIgnoreLightSpawningEnabled(boolean enabled) {
        this.restrictions = restrictions.withIgnoreLightSpawningEnabled(enabled);
        this.setDirty();
    }

    public int getRecipeUnlockTier() {
        return restrictions.recipeUnlockTier();
    }

    public void incrementRecipeUnlockTier() {
        this.restrictions = restrictions.withRecipeUnlockTier(restrictions.recipeUnlockTier() + 1);
        this.setDirty();
    }

    public int getRecipeTickets() {
        return restrictions.recipeTickets();
    }

    public void addRecipeTickets(int count) {
        this.restrictions = restrictions.withRecipeTickets(Math.max(0, restrictions.recipeTickets() + count));
        this.setDirty();
    }

    public void setRecipeTickets(int count) {
        this.restrictions = restrictions.withRecipeTickets(Math.max(0, count));
        this.setDirty();
    }

    public int getWitherState() {
        return witherState;
    }

    public void setWitherState(int waiting) {
        this.witherState = waiting;
        this.setDirty();
    }

    public void setWitherKilled() {
        setWitherState(KILLED);
    }

    public boolean isWitherKilled() {
        return witherState == KILLED;
    }

    public void incrementDragonEggShards() {
        this.dragonEggShards++;
        this.setDirty();
    }

    public int getDragonEggShards() {
        return dragonEggShards;
    }

    public List<ResourceKey<Recipe<?>>> getUnlockedRecipes() {
        return unlockedRecipes;
    }

    public void addUnlockedRecipe(ResourceKey<Recipe<?>> recipe) {
        unlockedRecipes.add(recipe);
        this.setDirty();
    }

    public List<String> getUnlockedStructureIds() {
        return restrictions.unlockedStructureIds();
    }

    public void unlockStructureId(String structureId) {
        List<String> list = new ArrayList<>(restrictions.unlockedStructureIds());
        if (!list.contains(structureId)) {
            list.add(structureId);
            this.restrictions = restrictions.withStructureUpgrades(restrictions.structureUpgrades() + 1).withUnlockedStructureIds(list);
            this.setDirty();
        }
    }

    public boolean isStructureUnlocked(String structureId) {
        return restrictions.unlockedStructureIds().contains(structureId);
    }
}