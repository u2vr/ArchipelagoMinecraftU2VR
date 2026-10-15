package gg.archipelago.aprandomizer.managers;

import com.google.common.collect.Iterables;
import gg.archipelago.aprandomizer.APRandomizer;
import gg.archipelago.aprandomizer.attachments.APAttachmentTypes;
import gg.archipelago.aprandomizer.attachments.APPlayerAttachment;
import gg.archipelago.aprandomizer.data.WorldData;
import gg.archipelago.aprandomizer.network.SyncAbilitiesPayload;
import gg.archipelago.aprandomizer.network.ToggleAbilityPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.*;

@EventBusSubscriber(modid = APRandomizer.MODID)
public class AbilityManager {

    private static final ThreadLocal<Boolean> IS_PROCESSING_BLOCK_BREAK = ThreadLocal.withInitial(() -> false);

    public static boolean isLightEmittingBlock(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.is(Items.LAVA_BUCKET)) return true;
        Block block = Block.byItem(stack.getItem());
        if (block != Blocks.AIR) {
            return block.defaultBlockState().getLightEmission() > 0;
        }
        return false;
    }

    public static ItemStack getLightHolderItem(Player player) {
        if (player == null) return ItemStack.EMPTY;
        APPlayerAttachment attachment = player.getData(APAttachmentTypes.AP_PLAYER);
        if (attachment == null) return ItemStack.EMPTY;
        return attachment.getLightHolderItem();
    }

    public static void syncToAll(MinecraftServer server) {
        if (server == null) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            syncToPlayer(player);
        }
    }

    public static void syncToPlayer(ServerPlayer player) {
        if (player == null) return;
        WorldData data = APRandomizer.getWorldData();
        if (data == null) return;

        APPlayerAttachment attachment = player.getData(APAttachmentTypes.AP_PLAYER);
        if (attachment == null) return;

        // Clamp mining shape index to current max tier
        int maxMining = data.getMiningLevel();
        if (attachment.getMiningShapeIndex() > maxMining) {
            attachment.setMiningShapeIndex(maxMining);
        }

        applyStepAssist(player);

        SyncAbilitiesPayload payload = new SyncAbilitiesPayload(
                data.getMiningLevel(),
                data.isTreeFellerUnlocked(),
                data.getJumpLevel(),
                data.isItemMagnetUnlocked(),
                data.isStepAssistUnlocked(),
                data.isTrashCanUnlocked(),
                data.isLightHolderUnlocked(),
                attachment.getMiningShapeIndex(),
                attachment.isTreeFellerActive(),
                attachment.isItemMagnetActive(),
                attachment.isStepAssistActive(),
                attachment.getTrashCanItem(),
                attachment.getLightHolderItem()
        );

        PacketDistributor.sendToPlayer(player, payload);
    }

    public static void handleToggle(ServerPlayer player, int action) {
        WorldData data = APRandomizer.getWorldData();
        if (data == null || player == null) return;

        APPlayerAttachment attachment = player.getData(APAttachmentTypes.AP_PLAYER);
        if (attachment == null) return;

        switch (action) {
            case ToggleAbilityPayload.ACTION_CYCLE_MINING -> {
                int maxTier = data.getMiningLevel();
                if (maxTier > 0) {
                    int next = (attachment.getMiningShapeIndex() + 1) % (maxTier + 1);
                    attachment.setMiningShapeIndex(next);
                }
            }
            case ToggleAbilityPayload.ACTION_TOGGLE_TREE_FELLER -> {
                if (data.isTreeFellerUnlocked()) {
                    attachment.setTreeFellerActive(!attachment.isTreeFellerActive());
                }
            }
            case ToggleAbilityPayload.ACTION_TOGGLE_MAGNET -> {
                if (data.isItemMagnetUnlocked()) {
                    attachment.setItemMagnetActive(!attachment.isItemMagnetActive());
                }
            }
            case ToggleAbilityPayload.ACTION_TOGGLE_STEP_ASSIST -> {
                if (data.isStepAssistUnlocked()) {
                    attachment.setStepAssistActive(!attachment.isStepAssistActive());
                    applyStepAssist(player);
                }
            }
        }

        syncToPlayer(player);
    }

    public static void handleMultiJump(ServerPlayer player, int jumpNumber) {
        WorldData data = APRandomizer.getWorldData();
        if (data == null || player == null) return;

        int jumpTier = data.getJumpLevel();
        if (jumpTier <= 0 || jumpNumber > jumpTier) return;

        // Reset fall distance on server
        player.fallDistance = 0.0f;

        // Sound and particles
        Level level = player.level();
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.WIND_CHARGE_BURST.value(), SoundSource.PLAYERS, 0.5f, 1.3f);

        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 0.1, player.getZ(),
                    8, 0.25, 0.05, 0.25, 0.05);
        }
    }

    public static void handleUpdateSlot(ServerPlayer player, int slotType, ItemStack newStack) {
        WorldData data = APRandomizer.getWorldData();
        if (data == null || player == null) return;

        APPlayerAttachment attachment = player.getData(APAttachmentTypes.AP_PLAYER);
        if (attachment == null) return;

        if (slotType == 0) { // TRASH_CAN
            if (data.isTrashCanUnlocked()) {
                ItemStack oldTrash = attachment.getTrashCanItem();
                if (!newStack.isEmpty()) {
                    // Putting item into trash can: cursor carried becomes empty
                    attachment.setTrashCanItem(newStack);
                    player.containerMenu.setCarried(ItemStack.EMPTY);
                } else if (!oldTrash.isEmpty()) {
                    // Recovering item from trash can: cursor receives oldTrash
                    attachment.setTrashCanItem(ItemStack.EMPTY);
                    player.containerMenu.setCarried(oldTrash.copy());
                }
                player.containerMenu.broadcastChanges();
            }
        } else if (slotType == 1) { // LIGHT_HOLDER
            if (data.isLightHolderUnlocked()) {
                ItemStack oldLight = attachment.getLightHolderItem();
                if (!newStack.isEmpty() && isLightEmittingBlock(newStack)) {
                    // Swapping or putting item into light holder
                    attachment.setLightHolderItem(newStack);
                    player.containerMenu.setCarried(oldLight.copy());
                } else if (newStack.isEmpty() && !oldLight.isEmpty()) {
                    // Recovering item from light holder
                    attachment.setLightHolderItem(ItemStack.EMPTY);
                    player.containerMenu.setCarried(oldLight.copy());
                }
                player.containerMenu.broadcastChanges();
            }
        }

        syncToPlayer(player);
    }

    public static void handleAltTrash(ServerPlayer player, int containerSlotId) {
        WorldData data = APRandomizer.getWorldData();
        if (data == null || player == null || !data.isTrashCanUnlocked()) return;

        APPlayerAttachment attachment = player.getData(APAttachmentTypes.AP_PLAYER);
        if (attachment == null) return;

        if (containerSlotId >= 0 && containerSlotId < player.containerMenu.slots.size()) {
            var slot = player.containerMenu.getSlot(containerSlotId);
            ItemStack stack = slot.getItem();
            if (!stack.isEmpty()) {
                attachment.setTrashCanItem(stack.copy());
                slot.set(ItemStack.EMPTY);
                player.containerMenu.broadcastChanges();
                syncToPlayer(player);
            }
        }
    }

    public static void applyStepAssist(ServerPlayer player) {
        if (player == null) return;
        WorldData data = APRandomizer.getWorldData();
        boolean unlocked = data != null && data.isStepAssistUnlocked();
        APPlayerAttachment attachment = player.getData(APAttachmentTypes.AP_PLAYER);
        boolean active = attachment != null && attachment.isStepAssistActive();

        var stepAttr = player.getAttribute(Attributes.STEP_HEIGHT);
        if (stepAttr != null) {
            double target = (unlocked && active) ? 1.06 : 0.6;
            stepAttr.setBaseValue(target);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            applyStepAssist(player);
            syncToPlayer(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            applyStepAssist(player);
            syncToPlayer(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        WorldData data = APRandomizer.getWorldData();
        if (data == null) return;

        // 1. Item & XP Magnet
        if (data.isItemMagnetUnlocked()) {
            APPlayerAttachment attachment = player.getData(APAttachmentTypes.AP_PLAYER);
            if (attachment != null && attachment.isItemMagnetActive() && !player.isSpectator()) {
                AABB aabb = player.getBoundingBox().inflate(10.0);
                Level level = player.level();

                List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, aabb,
                        item -> !item.hasPickUpDelay() && item.isAlive());
                for (ItemEntity item : items) {
                    Vec3 dir = player.position().add(0, 0.4, 0).subtract(item.position());
                    double dist = dir.length();
                    if (dist > 0.2) {
                        Vec3 pull = dir.normalize().scale(Math.min(0.45, 0.8 / Math.max(1.0, dist)));
                        item.setDeltaMovement(item.getDeltaMovement().scale(0.5).add(pull));
                    }
                }

                List<ExperienceOrb> orbs = level.getEntitiesOfClass(ExperienceOrb.class, aabb,
                        ExperienceOrb::isAlive);
                for (ExperienceOrb orb : orbs) {
                    Vec3 dir = player.position().add(0, 0.4, 0).subtract(orb.position());
                    double dist = dir.length();
                    if (dist > 0.2) {
                        Vec3 pull = dir.normalize().scale(Math.min(0.45, 0.8 / Math.max(1.0, dist)));
                        orb.setDeltaMovement(orb.getDeltaMovement().scale(0.5).add(pull));
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BreakBlockEvent event) {
        if (IS_PROCESSING_BLOCK_BREAK.get()) return;
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        if (player.isCreative() || player.isSpectator()) return;

        WorldData data = APRandomizer.getWorldData();
        if (data == null) return;

        APPlayerAttachment attachment = player.getData(APAttachmentTypes.AP_PLAYER);
        if (attachment == null) return;

        BlockPos origin = event.getPos();
        Level level = player.level();
        BlockState originalState = event.getState();
        ItemStack tool = player.getMainHandItem();

        // 1. Tree Feller check
        if (data.isTreeFellerUnlocked() && attachment.isTreeFellerActive()
                && tool.is(ItemTags.AXES) && originalState.is(BlockTags.LOGS)) {
            IS_PROCESSING_BLOCK_BREAK.set(true);
            try {
                executeTreeFeller(player, (ServerLevel) level, origin, tool);
            } finally {
                IS_PROCESSING_BLOCK_BREAK.set(false);
            }
            return;
        }

        // 2. Progressive Mining check
        int miningShape = attachment.getMiningShapeIndex();
        if (data.getMiningLevel() > 0 && miningShape > 0 && isMiningTool(tool)) {
            IS_PROCESSING_BLOCK_BREAK.set(true);
            try {
                executeProgressiveMining(player, (ServerLevel) level, origin, originalState, tool, miningShape);
            } finally {
                IS_PROCESSING_BLOCK_BREAK.set(false);
            }
        }
    }

    private static boolean isMiningTool(ItemStack tool) {
        return tool.is(ItemTags.PICKAXES) || tool.is(ItemTags.SHOVELS);
    }

    private static void executeTreeFeller(ServerPlayer player, ServerLevel level, BlockPos startPos, ItemStack tool) {
        Queue<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();

        queue.add(startPos);
        visited.add(startPos);

        int brokenCount = 0;
        int maxBlocks = 1024;

        while (!queue.isEmpty() && brokenCount < maxBlocks) {
            BlockPos current = queue.poll();

            // Durability check: preserve at least 1 durability
            if (tool.isDamageableItem() && (tool.getMaxDamage() - tool.getDamageValue() <= 1)) {
                break;
            }

            if (!current.equals(startPos)) {
                BlockState state = level.getBlockState(current);
                if (state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES)) {
                    level.destroyBlock(current, true, player);
                    brokenCount++;
                    if (state.is(BlockTags.LOGS)) {
                        tool.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
                    }
                }
            }

            // Inspect 26 neighbors
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        BlockPos neighbor = current.offset(dx, dy, dz);
                        if (!visited.contains(neighbor) && startPos.distManhattan(neighbor) <= 32) {
                            visited.add(neighbor);
                            BlockState nState = level.getBlockState(neighbor);
                            if (nState.is(BlockTags.LOGS) || nState.is(BlockTags.LEAVES)) {
                                queue.add(neighbor);
                            }
                        }
                    }
                }
            }
        }
    }

    private static void executeProgressiveMining(ServerPlayer player, ServerLevel level, BlockPos origin,
                                                 BlockState originState, ItemStack tool, int shapeMode) {
        // Shapes (H x W x D):
        // 1: 2x1x1
        // 2: 2x3x1
        // 3: 3x3x1
        // 4: 3x3x2
        // 5: 3x3x3
        int height = switch (shapeMode) {
            case 1, 2 -> 2;
            case 3, 4, 5 -> 3;
            default -> 1;
        };
        int width = switch (shapeMode) {
            case 1 -> 1;
            case 2, 3, 4, 5 -> 3;
            default -> 1;
        };
        int depth = switch (shapeMode) {
            case 4 -> 2;
            case 5 -> 3;
            default -> 1;
        };

        // Determine hit face via raycast
        HitResult hit = player.pick(5.5, 0.0f, false);
        Direction face = (hit instanceof BlockHitResult bHit) ? bHit.getDirection() : player.getDirection().getOpposite();

        List<BlockPos> targets = calculateMiningPositions(origin, face, player.getDirection(), height, width, depth, player.getBlockY());

        for (BlockPos pos : targets) {
            if (pos.equals(origin)) continue;

            // Durability check: keep at least 1 durability
            if (tool.isDamageableItem() && (tool.getMaxDamage() - tool.getDamageValue() <= 1)) {
                break;
            }

            BlockState state = level.getBlockState(pos);
            if (state.isAir()) continue;
            if (state.getDestroySpeed(level, pos) < 0) continue; // Unbreakable (bedrock, etc.)

            // Only harvest if tool is effective or same block material
            if (tool.isCorrectToolForDrops(state) || state.getBlock() == originState.getBlock()) {
                level.destroyBlock(pos, true, player);
                tool.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
            }
        }
    }

    private static List<BlockPos> calculateMiningPositions(BlockPos origin, Direction face, Direction playerFacing,
                                                           int height, int width, int depth, int playerFootY) {
        List<BlockPos> list = new ArrayList<>();

        if (face == Direction.UP || face == Direction.DOWN) {
            // Looking at floor or ceiling
            // Width: perpendicular to horizontal player facing
            // Height: along horizontal player facing
            // Depth: vertical (along face normal into surface)
            Direction dirWidth = playerFacing.getClockWise();
            Direction dirHeight = playerFacing;
            Direction dirDepth = face.getOpposite();

            int halfW = width / 2;
            int halfH = height / 2;

            for (int d = 0; d < depth; d++) {
                for (int w = -halfW; w <= halfW; w++) {
                    for (int h = -halfH; h <= halfH; h++) {
                        BlockPos p = origin.relative(dirDepth, d).relative(dirWidth, w).relative(dirHeight, h);
                        list.add(p);
                    }
                }
            }
        } else {
            // Horizontal face (wall)
            // Height: along Y
            // Width: perpendicular to face normal
            // Depth: along face normal into wall
            Direction dirWidth = face.getClockWise();
            Direction dirDepth = face.getOpposite();

            int halfW = width / 2;
            int yMin, yMax;
            if (height == 2) {
                // If hitting block at or below feet level, mine current + above; else current + below
                if (origin.getY() <= playerFootY) {
                    yMin = 0;
                    yMax = 1;
                } else {
                    yMin = -1;
                    yMax = 0;
                }
            } else if (height == 3) {
                yMin = -1;
                yMax = 1;
            } else {
                yMin = 0;
                yMax = 0;
            }

            for (int d = 0; d < depth; d++) {
                for (int w = -halfW; w <= halfW; w++) {
                    for (int yOff = yMin; yOff <= yMax; yOff++) {
                        BlockPos p = origin.relative(dirDepth, d).relative(dirWidth, w).above(yOff);
                        list.add(p);
                    }
                }
            }
        }

        return list;
    }
}
