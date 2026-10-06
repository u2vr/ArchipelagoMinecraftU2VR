package gg.archipelago.aprandomizer.items;

import com.mojang.serialization.MapCodec;
import gg.archipelago.aprandomizer.APRandomizer;
import gg.archipelago.aprandomizer.APRegistries;
import gg.archipelago.aprandomizer.items.traps.MobTrap;
import net.minecraft.core.Registry;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class APRewardTypes {
    public static final DeferredRegister<MapCodec<? extends APReward>> REGISTER = DeferredRegister.create(APRegistries.ARCHIPELAGO_REWARD_TYPE, APRandomizer.MODID);

    public static final Registry<MapCodec<? extends APReward>> REGISTRY = REGISTER.makeRegistry(builder -> {});

    public static final DeferredHolder<MapCodec<? extends APReward>, MapCodec<RecipeReward>> RECIPE = REGISTER.register("recipe", () -> RecipeReward.CODEC);
    public static final DeferredHolder<MapCodec<? extends APReward>, MapCodec<ItemReward>> ITEM = REGISTER.register("item", () -> ItemReward.CODEC);
    public static final DeferredHolder<MapCodec<? extends APReward>, MapCodec<ExperienceReward>> EXPERIENCE = REGISTER.register("experience", () -> ExperienceReward.CODEC);
    public static final DeferredHolder<MapCodec<? extends APReward>, MapCodec<CompassReward>> COMPASS = REGISTER.register("compass", () -> CompassReward.MAP_CODEC);
    public static final DeferredHolder<MapCodec<? extends APReward>, MapCodec<MobTrap>> MOB_TRAP = REGISTER.register("mob_trap", () -> MobTrap.MAP_CODEC);
    public static final DeferredHolder<MapCodec<? extends APReward>, MapCodec<DragonEggShardReward>> DRAGON_EGG_SHARD = REGISTER.register("dragon_egg_shard", () -> DragonEggShardReward.MAP_CODEC);

    public static final DeferredHolder<MapCodec<? extends APReward>, MapCodec<RecipeUnlockReward>> RECIPE_UNLOCK = REGISTER.register("recipe_unlock", () -> RecipeUnlockReward.MAP_CODEC);
    public static final DeferredHolder<MapCodec<? extends APReward>, MapCodec<HpReward>> HP = REGISTER.register("hp", () -> HpReward.MAP_CODEC);
    public static final DeferredHolder<MapCodec<? extends APReward>, MapCodec<HungerReward>> HUNGER = REGISTER.register("hunger", () -> HungerReward.MAP_CODEC);
    public static final DeferredHolder<MapCodec<? extends APReward>, MapCodec<ReachReward>> REACH = REGISTER.register("reach", () -> ReachReward.MAP_CODEC);
    public static final DeferredHolder<MapCodec<? extends APReward>, MapCodec<WorldBorderReward>> WORLD_BORDER = REGISTER.register("world_border", () -> WorldBorderReward.MAP_CODEC);
    public static final DeferredHolder<MapCodec<? extends APReward>, MapCodec<StructureUnlockReward>> STRUCTURE_UNLOCK = REGISTER.register("structure_unlock", () -> StructureUnlockReward.MAP_CODEC);
    public static final DeferredHolder<MapCodec<? extends APReward>, MapCodec<OffhandReward>> OFFHAND = REGISTER.register("offhand", () -> OffhandReward.MAP_CODEC);
    public static final DeferredHolder<MapCodec<? extends APReward>, MapCodec<InventorySlotReward>> INVENTORY_SLOT = REGISTER.register("inventory_slot", () -> InventorySlotReward.MAP_CODEC);
    public static final DeferredHolder<MapCodec<? extends APReward>, MapCodec<RandomTrapReward>> RANDOM_TRAP = REGISTER.register("random_trap", () -> RandomTrapReward.MAP_CODEC);
    public static final DeferredHolder<MapCodec<? extends APReward>, MapCodec<MonsterSunBurningReward>> MONSTER_SUN_BURNING = REGISTER.register("monster_sun_burning", () -> MonsterSunBurningReward.MAP_CODEC);
    public static final DeferredHolder<MapCodec<? extends APReward>, MapCodec<MonsterSpawnLightReward>> MONSTER_SPAWN_LIGHT = REGISTER.register("monster_spawn_light", () -> MonsterSpawnLightReward.MAP_CODEC);
}
