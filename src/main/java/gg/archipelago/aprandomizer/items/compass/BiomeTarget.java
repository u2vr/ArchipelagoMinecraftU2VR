package gg.archipelago.aprandomizer.items.compass;

import com.mojang.datafixers.util.Pair;
import gg.archipelago.aprandomizer.exploration.ExplorationData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.biome.Biome;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public interface BiomeTarget extends CompassTarget {
    @Override
    default Optional<BlockPos> findTarget(ServerLevel level, BlockPos start, ServerPlayer player) {
        Pair<BlockPos, Holder<Biome>> target = level.findClosestBiome3d(biome -> isTargetingBiome(biome, player), start, 6400, 32, 64);
        if (target == null) {
            return Optional.empty();
        }
        return Optional.of(target.getFirst());
    }

    @Override
    default boolean includeY() {
        return false;
    }

    @Override
    default List<String> getExtraLore(ServerLevel level, BlockPos targetPos, ServerPlayer player) {
        Holder<Biome> biome = level.getBiome(targetPos);
        return biome.unwrapKey().map(key -> {
            String biomeId = key.identifier().toString();
            ExplorationData.BiomeEntry entry = ExplorationData.getBiomeById(biomeId);
            String name = (entry != null) ? entry.displayName() : key.identifier().getPath();
            int dist = (int) Math.sqrt(player.blockPosition().distToCenterSqr(targetPos.getX(), player.getBlockY(), targetPos.getZ()));
            return List.of("Target: " + name, "Distance: ~" + dist + " blocks");
        }).orElse(Collections.emptyList());
    }

    boolean isTargetingBiome(Holder<Biome> biome, ServerPlayer player);
}
