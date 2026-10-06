package gg.archipelago.aprandomizer.managers;

import gg.archipelago.aprandomizer.APRandomizer;
import gg.archipelago.aprandomizer.APRegistries;
import gg.archipelago.aprandomizer.common.Utils.Utils;
import gg.archipelago.aprandomizer.data.WorldData;
import gg.archipelago.aprandomizer.items.APItem;
import gg.archipelago.aprandomizer.items.APReward;
import gg.archipelago.aprandomizer.items.APTier;
import gg.archipelago.aprandomizer.items.RecipeReward;
import gg.archipelago.aprandomizer.managers.itemmanager.ItemManager;
import gg.archipelago.aprandomizer.managers.recipemanager.RecipeTreeManager;
import gg.archipelago.aprandomizer.tags.APStructureTags;
import net.minecraft.core.BlockPos;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetHealthPacket;
import net.minecraft.resources.ResourceKey;
import org.jetbrains.annotations.Nullable;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.TriState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@EventBusSubscriber(modid = APRandomizer.MODID)
public class RestrictionManager {

    private static final List<StructureEntry> STRUCTURES = List.of(
            new StructureEntry("Village", APStructureTags.VILLAGE, "Деревня"),
            new StructureEntry("Pillager Outpost", APStructureTags.PILLAGER_OUTPOST, "Аванпост разбойников"),
            new StructureEntry("Nether Fortress", APStructureTags.FORTRESS, "Крепость Незера"),
            new StructureEntry("Bastion Remnant", APStructureTags.BASTION_REMNANT, "Бастион"),
            new StructureEntry("Trial Chambers", APStructureTags.TRIAL_CHAMBERS, "Испытательные палаты"),
            new StructureEntry("Ancient City", APStructureTags.ANCIENT_CITY, "Древний город"),
            new StructureEntry("Ocean Monument", APStructureTags.OCEAN_MONUMENT, "Подводный монумент"),
            new StructureEntry("End City", APStructureTags.END_CITY, "Город Края")
    );

    public record StructureEntry(String id, TagKey<Structure> tag, String displayName) {}

    public static List<StructureEntry> getStructures() {
        return STRUCTURES;
    }

    // --- Settings Checkers ---

    public static boolean isHpRestrictionEnabled() {
        if (APRandomizer.isConnected() && APRandomizer.getAP() != null && APRandomizer.getAP().getSlotData() != null) {
            var sd = APRandomizer.getAP().getSlotData();
            if (sd.hpRestriction != null) return sd.isHpRestrictionEnabled();
        }
        if (APRandomizer.getApmcData() != null && APRandomizer.getApmcData().hp_restriction != null) {
            return APRandomizer.getApmcData().isHpRestrictionEnabled();
        }
        return RecipeTreeManager.isSettingEnabled("hp_restriction", true);
    }

    public static boolean isHungerRestrictionEnabled() {
        if (APRandomizer.isConnected() && APRandomizer.getAP() != null && APRandomizer.getAP().getSlotData() != null) {
            var sd = APRandomizer.getAP().getSlotData();
            if (sd.hungerRestriction != null) return sd.isHungerRestrictionEnabled();
        }
        if (APRandomizer.getApmcData() != null && APRandomizer.getApmcData().hunger_restriction != null) {
            return APRandomizer.getApmcData().isHungerRestrictionEnabled();
        }
        return RecipeTreeManager.isSettingEnabled("hunger_restriction", true);
    }

    public static boolean isOffhandRestrictionEnabled() {
        if (APRandomizer.isConnected() && APRandomizer.getAP() != null && APRandomizer.getAP().getSlotData() != null) {
            var sd = APRandomizer.getAP().getSlotData();
            if (sd.offhandRestriction != null) return sd.isOffhandRestrictionEnabled();
        }
        if (APRandomizer.getApmcData() != null && APRandomizer.getApmcData().offhand_restriction != null) {
            return APRandomizer.getApmcData().isOffhandRestrictionEnabled();
        }
        return RecipeTreeManager.isSettingEnabled("offhand_restriction", true);
    }

    public static boolean isReachRestrictionEnabled() {
        if (APRandomizer.isConnected() && APRandomizer.getAP() != null && APRandomizer.getAP().getSlotData() != null) {
            var sd = APRandomizer.getAP().getSlotData();
            if (sd.reachRestriction != null) return sd.isReachRestrictionEnabled();
        }
        if (APRandomizer.getApmcData() != null && APRandomizer.getApmcData().reach_restriction != null) {
            return APRandomizer.getApmcData().isReachRestrictionEnabled();
        }
        return RecipeTreeManager.isSettingEnabled("reach_restriction", true);
    }

    public static boolean isInventorySlotRestrictionEnabled() {
        if (APRandomizer.isConnected() && APRandomizer.getAP() != null && APRandomizer.getAP().getSlotData() != null) {
            var sd = APRandomizer.getAP().getSlotData();
            if (sd.inventorySlotRestriction != null) return sd.isInventorySlotRestrictionEnabled();
        }
        if (APRandomizer.getApmcData() != null && APRandomizer.getApmcData().inventory_slot_restriction != null) {
            return APRandomizer.getApmcData().isInventorySlotRestrictionEnabled();
        }
        return RecipeTreeManager.isSettingEnabled("inventory_slot_restriction", true);
    }

    public static boolean isStructureRestrictionEnabled() {
        if (APRandomizer.isConnected() && APRandomizer.getAP() != null && APRandomizer.getAP().getSlotData() != null) {
            var sd = APRandomizer.getAP().getSlotData();
            if (sd.structureRestriction != null) return sd.isStructureRestrictionEnabled();
        }
        if (APRandomizer.getApmcData() != null && APRandomizer.getApmcData().structure_restriction != null) {
            return APRandomizer.getApmcData().isStructureRestrictionEnabled();
        }
        return RecipeTreeManager.isSettingEnabled("structure_restriction", true);
    }

    public static boolean isWorldBorderRestrictionEnabled() {
        if (APRandomizer.isConnected() && APRandomizer.getAP() != null && APRandomizer.getAP().getSlotData() != null) {
            var sd = APRandomizer.getAP().getSlotData();
            if (sd.worldBorderRestriction != null) return sd.isWorldBorderRestrictionEnabled();
        }
        if (APRandomizer.getApmcData() != null && APRandomizer.getApmcData().world_border_restriction != null) {
            return APRandomizer.getApmcData().isWorldBorderRestrictionEnabled();
        }
        return RecipeTreeManager.isSettingEnabled("world_border_restriction", true);
    }

    public static boolean isTrapsEnabled() {
        if (APRandomizer.isConnected() && APRandomizer.getAP() != null && APRandomizer.getAP().getSlotData() != null) {
            var sd = APRandomizer.getAP().getSlotData();
            if (sd.trapsEnabled != null) return sd.isTrapsEnabled();
        }
        if (APRandomizer.getApmcData() != null && APRandomizer.getApmcData().traps_enabled != null) {
            return APRandomizer.getApmcData().isTrapsEnabled();
        }
        return RecipeTreeManager.isSettingEnabled("traps_enabled", true);
    }

    public static boolean isCursesEnabled() {
        if (APRandomizer.isConnected() && APRandomizer.getAP() != null && APRandomizer.getAP().getSlotData() != null) {
            var sd = APRandomizer.getAP().getSlotData();
            if (sd.cursesEnabled != null) return sd.isCursesEnabled();
        }
        if (APRandomizer.getApmcData() != null && APRandomizer.getApmcData().curses_enabled != null) {
            return APRandomizer.getApmcData().isCursesEnabled();
        }
        return RecipeTreeManager.isSettingEnabled("curses_enabled", true);
    }

    public static boolean isIgnoreLightSpawningEnabled() {
        if (!isCursesEnabled()) return false;
        WorldData worldData = APRandomizer.getWorldData();
        return worldData != null && worldData.isIgnoreLightSpawningEnabled();
    }

    public static boolean isStructureUnlockShuffle() {
        if (APRandomizer.isConnected() && APRandomizer.getAP() != null && APRandomizer.getAP().getSlotData() != null) {
            var sd = APRandomizer.getAP().getSlotData();
            if (sd.structureUnlockShuffle != null) return sd.isStructureUnlockShuffle();
        }
        if (APRandomizer.getApmcData() != null && APRandomizer.getApmcData().structure_unlock_shuffle != null) {
            return APRandomizer.getApmcData().isStructureUnlockShuffle();
        }
        return false;
    }

    public static boolean isStartingSharedChestEnabled() {
        return gg.archipelago.aprandomizer.managers.chest.SharedChestManager.isStartingSharedChestEnabled();
    }

    public static boolean isStructureUnlocked(StructureEntry entry) {
        if (!isStructureRestrictionEnabled()) {
            return true;
        }
        WorldData worldData = APRandomizer.getWorldData();
        if (worldData == null) return false;
        // If explicit list is tracked:
        if (!worldData.getUnlockedStructureIds().isEmpty()) {
            return worldData.isStructureUnlocked(entry.id());
        }
        // Fallback or sequential before explicit list:
        int index = STRUCTURES.indexOf(entry);
        return index != -1 && index < worldData.getStructureUpgrades();
    }

    public static StructureEntry unlockNextStructure() {
        if (!isStructureRestrictionEnabled()) {
            return null;
        }
        WorldData worldData = APRandomizer.getWorldData();
        if (worldData == null) return null;

        // Ensure previously sequential upgrades are represented if any
        if (worldData.getUnlockedStructureIds().isEmpty() && worldData.getStructureUpgrades() > 0) {
            for (int i = 0; i < Math.min(worldData.getStructureUpgrades(), STRUCTURES.size()); i++) {
                worldData.unlockStructureId(STRUCTURES.get(i).id());
            }
        }

        List<StructureEntry> locked = new ArrayList<>();
        for (StructureEntry entry : STRUCTURES) {
            if (!isStructureUnlocked(entry)) {
                locked.add(entry);
            }
        }

        if (locked.isEmpty()) {
            worldData.incrementStructureUpgrades();
            return null;
        }

        StructureEntry chosen;
        if (isStructureUnlockShuffle()) {
            // Random unlock order
            int randomIndex = ThreadLocalRandom.current().nextInt(locked.size());
            chosen = locked.get(randomIndex);
        } else {
            // Sequential unlock order (as in recipe_tree.json)
            chosen = locked.get(0);
        }

        worldData.unlockStructureId(chosen.id());
        return chosen;
    }

    // --- HP ---
    public static void applyHealth(ServerPlayer player) {
        var attr = player.getAttribute(Attributes.MAX_HEALTH);
        if (attr == null) return;
        if (!isHpRestrictionEnabled()) {
            attr.setBaseValue(20.0);
            return;
        }
        WorldData worldData = APRandomizer.getWorldData();
        int upgrades = worldData != null ? worldData.getHpUpgrades() : 0;
        // Base: 6.0 HP (3 hearts). Each upgrade: +2.0 HP (1 heart). Max: 40.0 (20 hearts)
        double maxHp = Math.min(40.0, 6.0 + upgrades * 2.0);
        attr.setBaseValue(maxHp);
        if (player.getHealth() > maxHp) {
            player.setHealth((float) maxHp);
        }
    }

    // --- Hunger ---
    private static final Map<UUID, Integer> hungerRegenTimers = new HashMap<>();

    public static int getMaxFoodLevel() {
        if (!isHungerRestrictionEnabled()) return 20;
        WorldData worldData = APRandomizer.getWorldData();
        int upgrades = worldData != null ? worldData.getHungerUpgrades() : 0;
        // Base: 8 food (4 drumsticks). Each upgrade: +2 food (1 drumstick) up to 20 (10 drumsticks)
        return Math.min(20, 8 + upgrades * 2);
    }

    public static boolean isEatingBlocked(Player player, ItemStack stack) {
        if (player == null || stack.isEmpty()) return false;
        if (!isHungerRestrictionEnabled()) return false;
        FoodProperties food = stack.get(DataComponents.FOOD);
        if (food == null) return false;
        if (food.canAlwaysEat()) return false;
        return player.getFoodData().getFoodLevel() >= getMaxFoodLevel();
    }

    public static float getRespawnSaturation(int maxFood) {
        return Math.min((float) maxFood, 5.0F * ((float) maxFood / 20.0F));
    }

    public static void applyHunger(ServerPlayer player) {
        if (!isHungerRestrictionEnabled()) return;
        int maxFood = getMaxFoodLevel();
        var foodData = player.getFoodData();
        int currentFood = foodData.getFoodLevel();
        float currentSat = foodData.getSaturationLevel();

        boolean clamped = false;
        if (currentFood > maxFood) {
            foodData.setFoodLevel(maxFood);
            clamped = true;
            if (currentSat >= 5.0F && currentFood >= 20) {
                float targetSat = getRespawnSaturation(maxFood);
                foodData.setSaturation(targetSat);
            }
        }
        if (currentSat > (float) maxFood) {
            foodData.setSaturation((float) maxFood);
            clamped = true;
        }
        if (clamped) {
            player.connection.send(new ClientboundSetHealthPacket(
                    player.getHealth(),
                    foodData.getFoodLevel(),
                    foodData.getSaturationLevel()
            ));
        }
    }

    public static void applyHungerOnRespawn(ServerPlayer player) {
        if (!isHungerRestrictionEnabled()) return;
        int maxFood = getMaxFoodLevel();
        var foodData = player.getFoodData();
        foodData.setFoodLevel(maxFood);
        float targetSat = getRespawnSaturation(maxFood);
        foodData.setSaturation(targetSat);
        player.connection.send(new ClientboundSetHealthPacket(
                player.getHealth(),
                foodData.getFoodLevel(),
                foodData.getSaturationLevel()
        ));
    }

    public static void tickHungerRegen(ServerPlayer player) {
        if (!isHungerRestrictionEnabled()) {
            hungerRegenTimers.remove(player.getUUID());
            return;
        }

        int maxFood = getMaxFoodLevel();
        if (maxFood >= 20) {
            hungerRegenTimers.remove(player.getUUID());
            return;
        }

        if (!player.isHurt()) {
            hungerRegenTimers.put(player.getUUID(), 0);
            return;
        }

        Boolean naturalRegen = player.level().getGameRules().get(GameRules.NATURAL_HEALTH_REGENERATION);
        if (Boolean.FALSE.equals(naturalRegen)) {
            hungerRegenTimers.put(player.getUUID(), 0);
            return;
        }

        var foodData = player.getFoodData();
        int currentFood = foodData.getFoodLevel();
        float currentSat = foodData.getSaturationLevel();

        int timer = hungerRegenTimers.getOrDefault(player.getUUID(), 0) + 1;

        // Fast heal: requires foodLevel at maxFood and saturation > 0
        if (currentFood >= maxFood && currentSat > 0.0F) {
            if (timer >= 10) {
                float f = Math.min(currentSat, 6.0F);
                player.heal(f / 6.0F);
                foodData.addExhaustion(f);
                timer = 0;
            }
            hungerRegenTimers.put(player.getUUID(), timer);
        }
        // Slow heal: requires foodLevel >= maxFood - 2, and currentFood < 18 (at 18+, vanilla slow heal handles it)
        else if (currentFood >= Math.max(1, maxFood - 2) && currentFood < 18) {
            if (timer >= 80) {
                player.heal(1.0F);
                foodData.addExhaustion(6.0F);
                timer = 0;
            }
            hungerRegenTimers.put(player.getUUID(), timer);
        } else {
            hungerRegenTimers.put(player.getUUID(), 0);
        }
    }

    // --- Reach ---
    private static final double[] BLOCK_REACH_STAGES = { 2.5, 3.0, 3.5, 4.5 };
    private static final double[] ENTITY_REACH_STAGES = { 2.0, 2.5, 3.0, 3.5 };

    public static double getBlockReach() {
        if (!isReachRestrictionEnabled()) return 4.5;
        WorldData worldData = APRandomizer.getWorldData();
        int upgrades = worldData != null ? worldData.getReachUpgrades() : 0;
        int index = Math.min(upgrades, BLOCK_REACH_STAGES.length - 1);
        return BLOCK_REACH_STAGES[index];
    }

    public static double getEntityReach() {
        if (!isReachRestrictionEnabled()) return 3.0;
        WorldData worldData = APRandomizer.getWorldData();
        int upgrades = worldData != null ? worldData.getReachUpgrades() : 0;
        int index = Math.min(upgrades, ENTITY_REACH_STAGES.length - 1);
        return ENTITY_REACH_STAGES[index];
    }

    public static void applyReach(ServerPlayer player) {
        double blockReach = getBlockReach();
        double entityReach = getEntityReach();
        var blockAttr = player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE);
        if (blockAttr != null) blockAttr.setBaseValue(blockReach);
        var entityAttr = player.getAttribute(Attributes.ENTITY_INTERACTION_RANGE);
        if (entityAttr != null) entityAttr.setBaseValue(entityReach);
    }

    // --- World Border ---
    private static final double[] WORLD_BORDER_SIZES = { 1000.0, 1800.0, 3000.0, 6000.0, 60000000.0 };

    public static double getWorldBorderSize() {
        if (!isWorldBorderRestrictionEnabled()) return 60000000.0;
        WorldData worldData = APRandomizer.getWorldData();
        int upgrades = worldData != null ? worldData.getWorldBorderUpgrades() : 0;
        int index = Math.min(upgrades, WORLD_BORDER_SIZES.length - 1);
        return WORLD_BORDER_SIZES[index];
    }

    public static void applyWorldBorder(MinecraftServer server) {
        if (server == null) return;
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (overworld == null) return;

        var respawnData = overworld.getRespawnData();
        if (respawnData != null && respawnData.pos() != null) {
            BlockPos spawn = respawnData.pos();
            overworld.getWorldBorder().setCenter(spawn.getX() + 0.5, spawn.getZ() + 0.5);
        }

        if (APRandomizer.isJailPlayers() || !isWorldBorderRestrictionEnabled()) {
            overworld.getWorldBorder().setSize(60000000.0);
            return;
        }

        double size = getWorldBorderSize();
        overworld.getWorldBorder().setSize(size);
    }

    // --- Offhand ---
    public static boolean isOffhandUnlocked() {
        if (!isOffhandRestrictionEnabled()) return true;
        WorldData worldData = APRandomizer.getWorldData();
        return worldData != null && worldData.isOffhandUnlocked();
    }

    // --- Inventory Slots ---
    public static int getAllowedSlots() {
        if (!isInventorySlotRestrictionEnabled()) return 36;
        WorldData worldData = APRandomizer.getWorldData();
        int upgrades = worldData != null ? worldData.getInventorySlotUpgrades() : 0;
        // Base: 9 slots (hotbar 0-8). Each upgrade: +1 slot (9-35). Max: 36
        return Math.min(36, 9 + upgrades);
    }

    public static boolean isSlotRestricted(int slotIndex) {
        if (!isInventorySlotRestrictionEnabled()) return false;
        if (slotIndex < 0 || slotIndex >= 36) return false;
        return slotIndex >= getAllowedSlots();
    }

    public static boolean canPlayerFitItemInAllowedSlots(net.minecraft.world.entity.player.Inventory inv, ItemStack stack) {
        if (!isInventorySlotRestrictionEnabled()) return true;
        int allowed = getAllowedSlots();
        if (allowed >= 36) return true;
        for (int i = 0; i < allowed; i++) {
            ItemStack existing = inv.getItem(i);
            if (existing.isEmpty()) {
                return true;
            }
            if (ItemStack.isSameItemSameComponents(existing, stack) && existing.getCount() < existing.getMaxStackSize()) {
                return true;
            }
        }
        return false;
    }

    public static void enforceInventory(ServerPlayer player) {
        if (!isInventorySlotRestrictionEnabled()) return;
        int allowed = getAllowedSlots();
        if (allowed >= 36) return;

        var inv = player.getInventory();
        boolean anyDroppedOrMoved = false;
        // Main inventory slots 0-35. Slots >= allowed are restricted.
        for (int i = allowed; i < 36; i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty()) {
                // Try moving to free allowed slot or merging into existing stacks
                boolean placed = false;
                for (int free = 0; free < allowed; free++) {
                    ItemStack freeStack = inv.getItem(free);
                    if (freeStack.isEmpty()) {
                        inv.setItem(free, stack.copy());
                        inv.setItem(i, ItemStack.EMPTY);
                        placed = true;
                        anyDroppedOrMoved = true;
                        break;
                    } else if (ItemStack.isSameItemSameComponents(freeStack, stack) && freeStack.getCount() < freeStack.getMaxStackSize()) {
                        int toAdd = Math.min(stack.getCount(), freeStack.getMaxStackSize() - freeStack.getCount());
                        freeStack.grow(toAdd);
                        stack.shrink(toAdd);
                        if (stack.isEmpty()) {
                            inv.setItem(i, ItemStack.EMPTY);
                            placed = true;
                            anyDroppedOrMoved = true;
                            break;
                        }
                    }
                }
                if (!placed) {
                    inv.setItem(i, ItemStack.EMPTY);
                    net.minecraft.world.entity.item.ItemEntity dropped = player.drop(stack, false, net.minecraft.util.Prediction.PREDICTED);
                    if (dropped != null) {
                        dropped.setPickUpDelay(40);
                    }
                    anyDroppedOrMoved = true;
                }
            }
        }
        if (anyDroppedOrMoved) {
            player.sendSystemMessage(Component.literal("§cСлот инвентаря заблокирован! Разблокировано: " + allowed + "/36"), true);
            player.containerMenu.broadcastChanges();
        }
    }

    // --- Recipe Unlock ---
    public static void unlockNextRecipes(MinecraftServer server) {
        gg.archipelago.aprandomizer.managers.recipemanager.RecipeTreeManager.unlockNextRecipeNode(server);
    }

    // --- Traps ---
    private static final List<net.minecraft.world.item.Item> JUNK_ITEMS = List.of(
            Items.DIRT,
            Items.COBBLESTONE,
            Items.ROTTEN_FLESH,
            Items.POISONOUS_POTATO,
            Items.GRAVEL,
            Items.STICK,
            Items.DEAD_BUSH,
            Items.PUMPKIN_SEEDS,
            Items.WHEAT_SEEDS,
            Items.BEETROOT_SEEDS,
            Items.DRIED_KELP,
            Items.WOODEN_SHOVEL,
            Items.BOWL,
            Items.STRING,
            Items.CLAY_BALL,
            Items.FLINT
    );

    public static int getMonsterTrapPercentage() {
        if (APRandomizer.isConnected() && APRandomizer.getAP() != null && APRandomizer.getAP().getSlotData() != null) {
            var sd = APRandomizer.getAP().getSlotData();
            if (sd.monsterTrapPercentage != null) return sd.getMonsterTrapPercentage();
        }
        if (APRandomizer.getApmcData() != null && APRandomizer.getApmcData().monster_trap_percentage != null) {
            return APRandomizer.getApmcData().getMonsterTrapPercentage();
        }
        return 20;
    }

    private static String currentTrapSender = null;

    public static void setCurrentTrapSender(@Nullable String sender) {
        currentTrapSender = sender;
    }

    public static String getCurrentTrapSender() {
        return (currentTrapSender != null && !currentTrapSender.isBlank()) ? currentTrapSender : "Archipelago";
    }

    public static void triggerRandomTrap(MinecraftServer server, ServerPlayer player) {
        triggerRandomTrap(server, player, getCurrentTrapSender());
    }

    public static void triggerRandomTrap(MinecraftServer server, ServerPlayer player, @Nullable String senderSlotName) {
        if (!isTrapsEnabled()) return;
        String sender = (senderSlotName != null && !senderSlotName.isBlank()) ? senderSlotName : getCurrentTrapSender();
        ServerLevel level = (ServerLevel) player.level();
        Vec3 pos = player.position();

        int monsterPct = Math.max(0, Math.min(100, getMonsterTrapPercentage()));
        boolean isMonsterTrap = ThreadLocalRandom.current().nextInt(100) < monsterPct;

        if (isMonsterTrap) {
            // Спавн мобов: 0 - злые пчелы, 1 - скелеты, 2 - зомби
            int mobType = ThreadLocalRandom.current().nextInt(3);
            switch (mobType) {
                case 0 -> {
                    // Спавн злых пчёл
                    for (int i = 0; i < 4; i++) {
                        Entity entity = EntityTypes.BEE.create(level, EntitySpawnReason.MOB_SUMMONED);
                        if (entity instanceof Mob mob) {
                            Vec3 offset = Utils.getRandomPosition(pos, 4);
                            mob.snapTo(offset);
                            mob.setTarget(player);
                            mob.setCustomName(Component.literal(sender).withStyle(ChatFormatting.RED));
                            mob.setCustomNameVisible(true);
                            if (mob instanceof NeutralMob neutralMob) {
                                neutralMob.setTimeToRemainAngry(1200);
                            }
                            level.addFreshEntity(mob);
                        }
                    }
                    Utils.sendTitleToPlayer(player, Component.literal("§eЗлые пчёлы!"), Component.literal("От: §c" + sender), 10, 40, 10);
                    Utils.sendMessageToAll(Component.literal("§c[Ловушка] §e" + player.getName().getString() + " §fполучил ловушку §eЗлые пчёлы §fот §c" + sender + "§f!"));
                }
                case 1 -> {
                    // Спавн скелетов
                    for (int i = 0; i < 3; i++) {
                        Entity entity = EntityTypes.SKELETON.create(level, EntitySpawnReason.MOB_SUMMONED);
                        if (entity instanceof Mob mob) {
                            Vec3 offset = Utils.getRandomPosition(pos, 4);
                            mob.snapTo(offset);
                            mob.setTarget(player);
                            mob.setCustomName(Component.literal(sender).withStyle(ChatFormatting.RED));
                            mob.setCustomNameVisible(true);
                            level.addFreshEntity(mob);
                        }
                    }
                    Utils.sendTitleToPlayer(player, Component.literal("§7Засада скелетов!"), Component.literal("От: §c" + sender), 10, 40, 10);
                    Utils.sendMessageToAll(Component.literal("§c[Ловушка] §e" + player.getName().getString() + " §fполучил ловушку §7Засада скелетов §fот §c" + sender + "§f!"));
                }
                case 2 -> {
                    // Спавн зомби
                    for (int i = 0; i < 4; i++) {
                        Entity entity = EntityTypes.ZOMBIE.create(level, EntitySpawnReason.MOB_SUMMONED);
                        if (entity instanceof Mob mob) {
                            Vec3 offset = Utils.getRandomPosition(pos, 4);
                            mob.snapTo(offset);
                            mob.setTarget(player);
                            mob.setCustomName(Component.literal(sender).withStyle(ChatFormatting.RED));
                            mob.setCustomNameVisible(true);
                            level.addFreshEntity(mob);
                        }
                    }
                    Utils.sendTitleToPlayer(player, Component.literal("§2Орда зомби!"), Component.literal("От: §c" + sender), 10, 40, 10);
                    Utils.sendMessageToAll(Component.literal("§c[Ловушка] §e" + player.getName().getString() + " §fполучил ловушку §2Орда зомби §fот §c" + sender + "§f!"));
                }
            }
        } else {
            // Не-мобы: Мусор в карманах (70%), Песок 3х9х9 (15%), Слепота (15%)
            int nonMobRoll = ThreadLocalRandom.current().nextInt(100);
            if (nonMobRoll < 70) {
                // Мусор в инвентарь
                var inv = player.getInventory();
                int allowed = getAllowedSlots();
                int junkCount = 0;
                int targetSlots = ThreadLocalRandom.current().nextInt(2, 5);
                for (int i = 0; i < Math.min(36, allowed); i++) {
                    if (inv.getItem(i).isEmpty()) {
                        var junkItem = JUNK_ITEMS.get(ThreadLocalRandom.current().nextInt(JUNK_ITEMS.size()));
                        int amount = ThreadLocalRandom.current().nextInt(4, 24);
                        inv.setItem(i, new ItemStack(junkItem, amount));
                        junkCount++;
                        if (junkCount >= targetSlots) break;
                    }
                }
                if (junkCount == 0) {
                    // Если свободных слотов нет, дропнем мусор под ноги
                    var junkItem = JUNK_ITEMS.get(ThreadLocalRandom.current().nextInt(JUNK_ITEMS.size()));
                    Utils.giveItemToPlayer(player, new ItemStack(junkItem, 16));
                }
                player.containerMenu.broadcastChanges();
                Utils.sendTitleToPlayer(player, Component.literal("§6Мусор в карманах!"), Component.literal("От: §c" + sender), 10, 40, 10);
                Utils.sendMessageToAll(Component.literal("§c[Ловушка] §e" + player.getName().getString() + " §fполучил ловушку §6Мусор в карманах §fот §c" + sender + "§f!"));
            } else if (nonMobRoll < 85) {
                // Падающий песок 3х9х9 (высота 3, ширина 9, длина 9) над игроком
                int startX = player.getBlockX() - 4;
                int startZ = player.getBlockZ() - 4;
                int baseY = player.getBlockY() + 6;

                for (int dy = 0; dy < 3; dy++) {
                    for (int dx = 0; dx < 9; dx++) {
                        for (int dz = 0; dz < 9; dz++) {
                            BlockPos p = new BlockPos(startX + dx, baseY + dy, startZ + dz);
                            if (level.isEmptyBlock(p)) {
                                level.setBlock(p, Blocks.SAND.defaultBlockState(), 3);
                            }
                        }
                    }
                }
                Utils.sendTitleToPlayer(player, Component.literal("§6Падающий песок!"), Component.literal("Песчаный обвал 3х9х9 от: §c" + sender), 10, 40, 10);
                Utils.sendMessageToAll(Component.literal("§c[Ловушка] §e" + player.getName().getString() + " §fполучил ловушку §6Песчаный обвал §fот §c" + sender + "§f!"));
            } else {
                // Слепота
                player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 200, 0, false, false));
                Utils.sendTitleToPlayer(player, Component.literal("§8Слепота!"), Component.literal("От: §c" + sender), 10, 40, 10);
                Utils.sendMessageToAll(Component.literal("§c[Ловушка] §e" + player.getName().getString() + " §fполучил ловушку §8Слепота §fот §c" + sender + "§f!"));
            }
        }
    }

    public static final int STRUCTURE_BUFFER_BLOCKS = 5;
    private static final Map<UUID, Long> LAST_STRUCTURE_WARN_TIME = new HashMap<>();

    public static boolean isStructureLockedById(String explorationStructureId) {
        if (!isStructureRestrictionEnabled()) return false;
        for (StructureEntry entry : STRUCTURES) {
            String normalizedEntry = entry.id().toLowerCase().replace(" ", "_");
            String normalizedId = explorationStructureId.toLowerCase().replace(" ", "_");
            if (normalizedEntry.equals(normalizedId)) {
                return !isStructureUnlocked(entry);
            }
        }
        return false;
    }

    // --- Structure Checks ---
    public static void checkStructureRestrictions(ServerPlayer player) {
        if (!isStructureRestrictionEnabled()) return;
        if (APRandomizer.isJailPlayers()) return;
        if (player.isSpectator()) return;

        ServerLevel level = (ServerLevel) player.level();
        BlockPos playerPos = player.blockPosition();
        int cx = SectionPos.blockToSectionCoord(playerPos.getX());
        int cz = SectionPos.blockToSectionCoord(playerPos.getZ());

        Set<StructureStart> checkedStarts = new HashSet<>();

        // Check 3x3 chunks around player for any structure piece buffer collisions
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                int scx = cx + dx;
                int scz = cz + dz;
                LevelChunk chunk = level.getChunkSource().getChunkNow(scx, scz);
                if (chunk == null) continue;

                Map<Structure, LongSet> allReferences = chunk.getAllReferences();
                if (allReferences.isEmpty()) continue;

                for (Map.Entry<Structure, LongSet> refEntry : allReferences.entrySet()) {
                    Structure structure = refEntry.getKey();
                    Holder<Structure> holder = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).wrapAsHolder(structure);

                    for (StructureEntry entry : STRUCTURES) {
                        if (isStructureUnlocked(entry)) continue;
                        if (!holder.is(entry.tag())) continue;

                        level.structureManager().fillStartsForStructure(structure, refEntry.getValue(), start -> {
                            if (start != null && start.isValid() && checkedStarts.add(start)) {
                                checkStructureStartBarrier(player, entry, start);
                            }
                        });
                    }
                }
            }
        }

        // Also check getStructureWithPieceAt directly in case piece bounds are already reached
        for (StructureEntry entry : STRUCTURES) {
            if (isStructureUnlocked(entry)) continue;
            StructureStart start = level.structureManager().getStructureWithPieceAt(playerPos, entry.tag());
            if (start != null && start.isValid() && checkedStarts.add(start)) {
                checkStructureStartBarrier(player, entry, start);
            }
        }
    }

    private static void checkStructureStartBarrier(ServerPlayer player, StructureEntry entry, StructureStart start) {
        BoundingBox startBox = start.getBoundingBox();
        if (startBox == null) return;

        BlockPos playerPos = player.blockPosition();
        // Fast bounding box check with 5-block inflation
        if (!startBox.inflatedBy(STRUCTURE_BUFFER_BLOCKS).isInside(playerPos)) {
            return;
        }

        List<StructurePiece> pieces = start.getPieces();
        if (pieces.isEmpty()) {
            if (startBox.inflatedBy(STRUCTURE_BUFFER_BLOCKS).isInside(playerPos)) {
                ejectPlayerFromStructure(player, entry, startBox);
            }
            return;
        }

        StructurePiece closestPiece = null;
        double closestDistSq = Double.MAX_VALUE;
        double px = player.getX();
        double pz = player.getZ();

        for (StructurePiece piece : pieces) {
            BoundingBox pieceBox = piece.getBoundingBox();
            if (pieceBox.inflatedBy(STRUCTURE_BUFFER_BLOCKS).isInside(playerPos)) {
                double centerX = (pieceBox.minX() + pieceBox.maxX()) / 2.0;
                double centerZ = (pieceBox.minZ() + pieceBox.maxZ()) / 2.0;
                double d = (px - centerX) * (px - centerX) + (pz - centerZ) * (pz - centerZ);
                if (d < closestDistSq) {
                    closestDistSq = d;
                    closestPiece = piece;
                }
            }
        }

        if (closestPiece != null) {
            ejectPlayerFromStructure(player, entry, closestPiece.getBoundingBox());
        }
    }

    private static void ejectPlayerFromStructure(ServerPlayer player, StructureEntry entry, BoundingBox pieceBox) {
        double px = player.getX();
        double py = player.getY();
        double pz = player.getZ();

        double minX = pieceBox.minX();
        double maxX = pieceBox.maxX();
        double minZ = pieceBox.minZ();
        double maxZ = pieceBox.maxZ();

        // Calculate outward direction vector away from structure piece
        double dirX;
        if (px < minX) {
            dirX = px - minX; // negative, pointing West
        } else if (px > maxX) {
            dirX = px - maxX; // positive, pointing East
        } else {
            // Player is horizontally inside the piece X bounds: push to closest edge
            double distMin = px - minX;
            double distMax = maxX - px;
            dirX = (distMin < distMax) ? -1.0 : 1.0;
        }

        double dirZ;
        if (pz < minZ) {
            dirZ = pz - minZ; // negative, pointing North
        } else if (pz > maxZ) {
            dirZ = pz - maxZ; // positive, pointing South
        } else {
            // Player is horizontally inside the piece Z bounds: push to closest edge
            double distMin = pz - minZ;
            double distMax = maxZ - pz;
            dirZ = (distMin < distMax) ? -1.0 : 1.0;
        }

        double len = Math.sqrt(dirX * dirX + dirZ * dirZ);
        if (len > 1.0E-4) {
            dirX /= len;
            dirZ /= len;
        } else {
            Vec3 look = player.getLookAngle();
            double lookLen = Math.sqrt(look.x * look.x + look.z * look.z);
            if (lookLen > 1.0E-4) {
                dirX = -look.x / lookLen;
                dirZ = -look.z / lookLen;
            } else {
                dirX = 1.0;
                dirZ = 0.0;
            }
        }

        // If player is inside the actual piece (not just the 5-block buffer), teleport them safely outside
        BlockPos playerPos = player.blockPosition();
        if (pieceBox.isInside(playerPos)) {
            double edgeX = (dirX > 0) ? maxX : (dirX < 0 ? minX : px);
            double edgeZ = (dirZ > 0) ? maxZ : (dirZ < 0 ? minZ : pz);
            double targetX = edgeX + dirX * (STRUCTURE_BUFFER_BLOCKS + 2.0);
            double targetZ = edgeZ + dirZ * (STRUCTURE_BUFFER_BLOCKS + 2.0);
            int safeX = Mth.floor(targetX);
            int safeZ = Mth.floor(targetZ);
            int safeY = findSafeTeleportY((ServerLevel) player.level(), safeX, (int) py, safeZ);
            if (player.getVehicle() != null) {
                player.stopRiding();
            }
            player.teleportTo(targetX + 0.5, safeY, targetZ + 0.5);
        }

        // Apply strong repulsive velocity throwing the player away from the structure
        double speed = 1.6;
        double lift = 0.42;
        Vec3 motion = new Vec3(dirX * speed, lift, dirZ * speed);

        if (player.getVehicle() != null) {
            Entity vehicle = player.getVehicle();
            player.stopRiding();
            vehicle.setDeltaMovement(motion);
        }

        player.setDeltaMovement(motion);
        player.connection.send(new ClientboundSetEntityMotionPacket(player.getId(), motion));

        // Feedback: sound, particles, notification message
        ServerLevel level = (ServerLevel) player.level();
        long now = level.getGameTime();
        Long lastWarn = LAST_STRUCTURE_WARN_TIME.get(player.getUUID());
        if (lastWarn == null || now - lastWarn > 20) {
            LAST_STRUCTURE_WARN_TIME.put(player.getUUID(), now);
            player.sendSystemMessage(Component.literal("§c[!] Структура \"" + entry.displayName() + "\" заблокирована в Архипелаге!"), true);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WIND_CHARGE_BURST.value(), SoundSource.PLAYERS, 1.2f, 0.8f);
            level.sendParticles(ParticleTypes.GUST, player.getX(), player.getY() + 0.8, player.getZ(), 6, 0.4, 0.4, 0.4, 0.05);
        }
    }

    private static int findSafeTeleportY(ServerLevel level, int x, int startY, int z) {
        if (level.dimension() == Level.OVERWORLD || level.dimension() == Level.END) {
            int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
            if (surfaceY > level.getMinY()) {
                return surfaceY;
            }
        }
        BlockPos.MutableBlockPos mpos = new BlockPos.MutableBlockPos(x, startY, z);
        for (int dy = 0; dy <= 20; dy++) {
            for (int sign : new int[]{1, -1}) {
                int y = startY + dy * sign;
                if (y <= level.getMinY() || y >= level.getMaxY() - 2) continue;
                mpos.set(x, y, z);
                if (level.getBlockState(mpos).isSolid() &&
                    !level.getBlockState(mpos.above()).isSolid() &&
                    !level.getBlockState(mpos.above(2)).isSolid()) {
                    return y + 1;
                }
            }
        }
        return startY;
    }

    // --- Subscribed Events ---

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (APRandomizer.isJailPlayers()) return;

        // 1. Enforce hunger cap & natural regen
        if (isHungerRestrictionEnabled()) {
            applyHunger(player);
            tickHungerRegen(player);
        }

        // 2. Enforce offhand lock
        if (isOffhandRestrictionEnabled() && !isOffhandUnlocked() && !player.getOffhandItem().isEmpty()) {
            ItemStack offhand = player.getOffhandItem();
            player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
            Utils.giveItemToPlayer(player, offhand);
            player.sendSystemMessage(Component.literal("§cВторая рука заблокирована до получения улучшения!"), true);
            player.containerMenu.broadcastChanges();
        }

        // 3. Enforce structure barrier every tick
        if (isStructureRestrictionEnabled()) {
            checkStructureRestrictions(player);
        }

        // 4. Enforce inventory slot limits every 20 ticks
        if (player.tickCount % 20 == 0) {
            if (isInventorySlotRestrictionEnabled()) {
                enforceInventory(player);
            }
        }
    }

    @SubscribeEvent
    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        if (!isCursesEnabled()) return;
        WorldData worldData = APRandomizer.getWorldData();
        if (worldData == null || !worldData.isMonsterSunBurningDisabled()) return;

        if (event.getEntity() instanceof Monster monster) {
            var src = event.getSource();
            if (src.is(DamageTypes.ON_FIRE) || src.is(DamageTypes.IN_FIRE)) {
                if (monster.level().canSeeSky(monster.blockPosition())) {
                    monster.clearFire();
                    event.setCanceled(true);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onMobSpawnPlacementCheck(MobSpawnEvent.SpawnPlacementCheck event) {
        if (!isIgnoreLightSpawningEnabled()) return;
        if (event.getLevel().getDifficulty() == net.minecraft.world.Difficulty.PEACEFUL) {
            event.setResult(MobSpawnEvent.SpawnPlacementCheck.Result.FAIL);
            return;
        }

        EntityType<?> type = event.getEntityType();

        if (type == EntityTypes.SLIME) {
            // Slimes on the surface should only spawn in swamp biomes (ALLOWS_SURFACE_SLIME_SPAWNS)
            if (event.getLevel().getBiome(event.getPos()).is(net.minecraft.tags.BiomeTags.ALLOWS_SURFACE_SLIME_SPAWNS)
                    && event.getPos().getY() > 50 && event.getPos().getY() < 70) {
                @SuppressWarnings("unchecked")
                EntityType<? extends Mob> mobType = (EntityType<? extends Mob>) type;
                if (Mob.checkMobSpawnRules(mobType, event.getLevel(), event.getSpawnType(), event.getPos(), event.getRandom())) {
                    event.setResult(MobSpawnEvent.SpawnPlacementCheck.Result.SUCCEED);
                } else {
                    event.setResult(MobSpawnEvent.SpawnPlacementCheck.Result.FAIL);
                }
            }
            // For other locations (e.g. non-swamp surface or underground slime chunks),
            // leave DEFAULT so vanilla handles slime chunks underground and rejects non-swamp surface.
            return;
        }

        if (type == EntityTypes.DROWNED) {
            if (event.getLevel().getFluidState(event.getPos()).is(net.minecraft.tags.FluidTags.WATER)) {
                event.setResult(MobSpawnEvent.SpawnPlacementCheck.Result.SUCCEED);
            }
            return;
        }

        if (type.getCategory() == net.minecraft.world.entity.MobCategory.MONSTER) {
            @SuppressWarnings("unchecked")
            EntityType<? extends Mob> mobType = (EntityType<? extends Mob>) type;
            if (Mob.checkMobSpawnRules(mobType, event.getLevel(), event.getSpawnType(), event.getPos(), event.getRandom())) {
                event.setResult(MobSpawnEvent.SpawnPlacementCheck.Result.SUCCEED);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerInteract(PlayerInteractEvent.RightClickItem event) {
        if (isEatingBlocked(event.getEntity(), event.getItemStack())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
            return;
        }
        if (event.getSide().isClient()) return;
        if (event.getHand() == InteractionHand.OFF_HAND && !isOffhandUnlocked()) {
            event.setCanceled(true);
            if (event.getEntity() instanceof ServerPlayer player) {
                player.sendSystemMessage(Component.literal("§cВторая рука заблокирована!"), true);
            }
        }
    }

    @SubscribeEvent
    public static void onUseItemStart(LivingEntityUseItemEvent.Start event) {
        if (event.getEntity() instanceof Player player) {
            if (isEatingBlocked(player, event.getItem())) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onUseItemTick(LivingEntityUseItemEvent.Tick event) {
        if (event.getEntity() instanceof Player player) {
            if (isEatingBlocked(player, event.getItem())) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onUseItemFinish(LivingEntityUseItemEvent.Finish event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            applyHunger(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        applyHealth(player);
        applyReach(player);
        applyHunger(player);
        gg.archipelago.aprandomizer.managers.recipemanager.RecipeTreeManager.syncStateToPlayer(player);
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!isHungerRestrictionEnabled()) return;
        int maxFood = getMaxFoodLevel();
        var foodData = player.getFoodData();
        foodData.setFoodLevel(maxFood);
        foodData.setSaturation(getRespawnSaturation(maxFood));
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        applyHealth(player);
        applyReach(player);
        applyHungerOnRespawn(player);
        hungerRegenTimers.remove(player.getUUID());
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        hungerRegenTimers.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onItemPickupPre(ItemEntityPickupEvent.Pre event) {
        if (isInventorySlotRestrictionEnabled() && !canPlayerFitItemInAllowedSlots(event.getPlayer().getInventory(), event.getItemEntity().getItem())) {
            event.setCanPickup(TriState.FALSE);
        }
    }

    public static void updateAdvancementsTree(MinecraftServer server) {
        if (server == null) return;
        Set<net.minecraft.resources.Identifier> toRemove = new HashSet<>();

        if (!isHpRestrictionEnabled()) {
            toRemove.add(net.minecraft.resources.Identifier.fromNamespaceAndPath(APRandomizer.MODID, "received/hp_1"));
        }
        if (!isHungerRestrictionEnabled()) {
            toRemove.add(net.minecraft.resources.Identifier.fromNamespaceAndPath(APRandomizer.MODID, "received/hunger_1"));
        }
        if (!isReachRestrictionEnabled()) {
            toRemove.add(net.minecraft.resources.Identifier.fromNamespaceAndPath(APRandomizer.MODID, "received/reach_1"));
        }
        if (!isWorldBorderRestrictionEnabled()) {
            toRemove.add(net.minecraft.resources.Identifier.fromNamespaceAndPath(APRandomizer.MODID, "received/world_border_1"));
        }
        if (!isStructureRestrictionEnabled()) {
            toRemove.add(net.minecraft.resources.Identifier.fromNamespaceAndPath(APRandomizer.MODID, "received/structure_village"));
        }
        if (!isOffhandRestrictionEnabled()) {
            toRemove.add(net.minecraft.resources.Identifier.fromNamespaceAndPath(APRandomizer.MODID, "received/second_hand"));
        }
        if (!isInventorySlotRestrictionEnabled()) {
            toRemove.add(net.minecraft.resources.Identifier.fromNamespaceAndPath(APRandomizer.MODID, "received/inventory_slot_1"));
        }
        if (!isCursesEnabled()) {
            toRemove.add(net.minecraft.resources.Identifier.fromNamespaceAndPath(APRandomizer.MODID, "received/curse_monster_sun_burning"));
            toRemove.add(net.minecraft.resources.Identifier.fromNamespaceAndPath(APRandomizer.MODID, "received/curse_monster_spawn_light"));
        }

        if (!toRemove.isEmpty()) {
            var advManager = server.getAdvancements();
            if (advManager != null && advManager.tree() != null) {
                advManager.tree().remove(toRemove);
            }
        }
    }

    public static void applyAllRestrictions(MinecraftServer server) {
        if (server == null) return;
        applyWorldBorder(server);
        updateAdvancementsTree(server);
        ItemManager itemManager = APRandomizer.getItemManager();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            applyHealth(player);
            applyReach(player);
            applyHunger(player);
            player.getAdvancements().reload(server.getAdvancements());
            if (itemManager != null) {
                itemManager.syncPlayerAdvancements(player);
            }
        }
    }

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        applyAllRestrictions(event.getServer());
    }
}
