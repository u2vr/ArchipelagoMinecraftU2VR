package gg.archipelago.aprandomizer.managers.trademanager;

import gg.archipelago.aprandomizer.APRandomizer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.equine.TraderLlama;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.level.CustomSpawner;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.world.entity.npc.wanderingtrader.WanderingTraderSpawner;

public class ArchipelagoTraderSpawner implements CustomSpawner {

    private static final Logger LOGGER = LogManager.getLogger();

    // Check timer every 10 seconds (200 ticks)
    private static final int TICK_INTERVAL = 200;
    // Delay between spawns when uncompleted AP trades exist (10 minutes = 12000 ticks)
    private static final int ACTIVE_SPAWN_INTERVAL = 12000;

    private int tickCounter = TICK_INTERVAL;
    private int spawnTimer = ACTIVE_SPAWN_INTERVAL;
    private WanderingTraderSpawner vanillaSpawner;

    @Override
    public void tick(ServerLevel level, boolean spawnEnemies) {
        if (!level.getGameRules().get(GameRules.SPAWN_WANDERING_TRADERS)) {
            return;
        }
        if (APRandomizer.isJailPlayers()) {
            return;
        }

        boolean hasUncompleted = WanderingTraderManager.hasUncompletedTrades();

        // When all AP trades are bought, revert completely to vanilla spawning mechanics in Overworld
        if (!hasUncompleted) {
            if (level.dimension() == Level.OVERWORLD) {
                if (vanillaSpawner == null && level.getServer() != null) {
                    vanillaSpawner = new WanderingTraderSpawner(level.getServer().overworld().getDataStorage());
                }
                if (vanillaSpawner != null) {
                    vanillaSpawner.tick(level, spawnEnemies);
                }
            }
            return;
        }

        if (level.dimension() != Level.OVERWORLD && level.dimension() != Level.NETHER) {
            return;
        }

        tickCounter--;
        if (tickCounter > 0) {
            return;
        }
        tickCounter = TICK_INTERVAL;

        List<ServerPlayer> players = level.players();
        if (players.isEmpty()) {
            return;
        }

        spawnTimer -= TICK_INTERVAL;
        if (spawnTimer > 0) {
            return;
        }
        spawnTimer = ACTIVE_SPAWN_INTERVAL;

        // Try to spawn near a player who doesn't have a trader nearby
        List<ServerPlayer> shuffledPlayers = new ArrayList<>(players);
        Collections.shuffle(shuffledPlayers);

        for (ServerPlayer player : shuffledPlayers) {
            if (player.isSpectator()) {
                continue;
            }

            // Check if there is already an alive Wandering Trader within 64 blocks
            AABB checkArea = new AABB(player.blockPosition()).inflate(64.0);
            List<WanderingTrader> existing = level.getEntitiesOfClass(WanderingTrader.class, checkArea, Entity::isAlive);
            if (!existing.isEmpty()) {
                continue;
            }

            if (trySpawnTrader(level, player)) {
                break;
            }
        }
    }

    public static boolean trySpawnTrader(ServerLevel level, ServerPlayer player) {
        if (level == null || player == null) return false;

        BlockPos playerPos = player.blockPosition();
        BlockPos spawnPos = findSpawnPosition(level, playerPos, 10, 24);
        if (spawnPos == null) {
            spawnPos = findSpawnPosition(level, playerPos, 6, 32);
        }
        if (spawnPos == null) {
            LOGGER.debug("Could not find suitable spawn position for Wandering Trader near player {}", player.getScoreboardName());
            return false;
        }

        WanderingTrader trader = EntityTypes.WANDERING_TRADER.spawn(level, spawnPos, EntitySpawnReason.EVENT);
        if (trader == null) {
            return false;
        }

        // Attach AP trades immediately
        WanderingTraderManager.attachArchipelagoTrades(trader, player);

        // Despawn delay: 40 minutes (48,000 ticks)
        trader.setDespawnDelay(48000);
        trader.setWanderTarget(playerPos);
        trader.setHomeTo(playerPos, 16);

        // Visual and defensive buffs:
        // Glowing for 60 seconds (1200 ticks) so player can easily locate him through walls/trees
        trader.addEffect(new MobEffectInstance(MobEffects.GLOWING, 1200, 0, false, false));
        // Resistance II for 5 minutes (6000 ticks) to protect against mob ambush
        trader.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 6000, 1, false, false));

        if (level.dimension() == Level.NETHER) {
            trader.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 72000, 0, false, false));
        }

        // Try spawning 2 trader llamas
        for (int i = 0; i < 2; i++) {
            trySpawnLlama(level, trader);
        }

        // Notify player via sound, action bar, and chat
        player.playSound(SoundEvents.WANDERING_TRADER_YES, 1.0f, 1.0f);
        player.sendSystemMessage(Component.literal("§6[Archipelago] §eA Wandering Trader has arrived nearby!"), true);
        player.sendSystemMessage(Component.literal("§6[Archipelago] §eA Wandering Trader has arrived nearby!"), false);

        LOGGER.info("Archipelago Wandering Trader spawned at {} in {} near player {}",
                spawnPos, level.dimension().identifier(), player.getScoreboardName());
        return true;
    }

    public static BlockPos findSpawnPosition(ServerLevel level, BlockPos center, int minRadius, int maxRadius) {
        RandomSource random = level.getRandom();
        for (int attempt = 0; attempt < 30; attempt++) {
            double angle = random.nextDouble() * 2.0 * Math.PI;
            double distance = minRadius + random.nextDouble() * (maxRadius - minRadius);
            int x = center.getX() + (int) (distance * Math.cos(angle));
            int z = center.getZ() + (int) (distance * Math.sin(angle));

            // 1. Search near center Y (from center.getY() + 4 down to center.getY() - 8)
            for (int dy = 4; dy >= -8; dy--) {
                int y = center.getY() + dy;
                BlockPos candidate = new BlockPos(x, y, z);
                if (isValidSpawnPos(level, candidate)) {
                    return candidate;
                }
            }

            // 2. In Overworld, check surface heightmap
            if (level.dimension() == Level.OVERWORLD) {
                int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                BlockPos candidate = new BlockPos(x, surfaceY, z);
                if (isValidSpawnPos(level, candidate)) {
                    return candidate;
                }
            }
        }
        return null;
    }

    public static boolean isValidSpawnPos(ServerLevel level, BlockPos pos) {
        if (!level.getWorldBorder().isWithinBounds(pos)) {
            return false;
        }
        if (pos.getY() <= level.getMinY() || pos.getY() >= level.getMaxY() - 1) {
            return false;
        }
        if (level.getBiome(pos).is(BiomeTags.WITHOUT_WANDERING_TRADER_SPAWNS)) {
            return false;
        }

        BlockPos floor = pos.below();
        BlockState floorState = level.getBlockState(floor);

        // Floor must be solid/sturdy
        if (!floorState.isFaceSturdy(level, floor, Direction.UP) && !floorState.isSolid()) {
            return false;
        }
        if (floorState.is(BlockTags.FIRE) || floorState.is(Blocks.MAGMA_BLOCK)
                || floorState.is(Blocks.CACTUS) || floorState.is(Blocks.SWEET_BERRY_BUSH)) {
            return false;
        }
        if (!level.getFluidState(floor).isEmpty()) {
            return false;
        }

        // Pos (feet) and pos.above() (head) must have empty collision shape
        BlockState feetState = level.getBlockState(pos);
        BlockState headState = level.getBlockState(pos.above());
        if (!feetState.getCollisionShape(level, pos).isEmpty() || !headState.getCollisionShape(level, pos.above()).isEmpty()) {
            return false;
        }

        // Must not be in lava or fire
        if (!level.getFluidState(pos).isEmpty() && level.getFluidState(pos).is(FluidTags.LAVA)) {
            return false;
        }
        if (feetState.is(BlockTags.FIRE) || headState.is(BlockTags.FIRE)) {
            return false;
        }

        return true;
    }

    private static void trySpawnLlama(ServerLevel level, WanderingTrader trader) {
        BlockPos traderPos = trader.blockPosition();
        RandomSource random = level.getRandom();
        for (int i = 0; i < 10; i++) {
            int x = traderPos.getX() + random.nextInt(7) - 3;
            int z = traderPos.getZ() + random.nextInt(7) - 3;
            for (int dy = 2; dy >= -2; dy--) {
                BlockPos candidate = new BlockPos(x, traderPos.getY() + dy, z);
                if (isValidSpawnPos(level, candidate)) {
                    TraderLlama llama = EntityTypes.TRADER_LLAMA.spawn(level, candidate, EntitySpawnReason.EVENT);
                    if (llama != null) {
                        llama.setLeashedTo(trader, true);
                        if (level.dimension() == Level.NETHER) {
                            llama.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 72000, 0, false, false));
                        }
                        return;
                    }
                }
            }
        }
    }
}
