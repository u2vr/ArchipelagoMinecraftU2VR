package gg.archipelago.aprandomizer.items.compass;

import com.mojang.serialization.MapCodec;
import gg.archipelago.aprandomizer.APRandomizer;
import gg.archipelago.aprandomizer.data.WorldData;
import gg.archipelago.aprandomizer.exploration.ExplorationData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public class UnvisitedStructureTarget implements CompassTarget {

    public static final MapCodec<UnvisitedStructureTarget> CODEC = MapCodec.unit(UnvisitedStructureTarget::new);

    private final ThreadLocal<String> lastFoundName = new ThreadLocal<>();

    @Override
    public Optional<BlockPos> findTarget(ServerLevel currentLevel, BlockPos start, ServerPlayer player) {
        WorldData worldData = APRandomizer.getWorldData();
        if (worldData == null) return Optional.empty();

        Set<String> visited = worldData.getVisitedStructures();
        BlockPos closestPos = null;
        double closestDistSq = Double.MAX_VALUE;
        String closestName = null;

        for (ExplorationData.StructureCheckEntry entry : ExplorationData.STRUCTURES) {
            if (visited.contains(entry.id())) {
                continue; // Already visited
            }

            TagKey<Structure> tag = entry.tag();
            if (tag == null) continue;

            if (!isStructurePossibleInDimension(entry.id(), currentLevel.dimension())) {
                continue;
            }

            try {
                int searchRadius = (closestPos != null)
                        ? Math.min(100, (int) (Math.sqrt(closestDistSq) / 16.0) + 1)
                        : 100;
                BlockPos pos = currentLevel.findNearestMapStructure(tag, start, searchRadius, false);
                if (pos != null) {
                    double distSq = start.distSqr(pos);
                    if (distSq < closestDistSq) {
                        closestDistSq = distSq;
                        closestPos = pos;
                        closestName = entry.displayName();
                    }
                }
            } catch (Exception ignored) {}
        }

        this.lastFoundName.set(closestName);
        return Optional.ofNullable(closestPos);
    }

    private static boolean isStructurePossibleInDimension(String structureId, net.minecraft.resources.ResourceKey<Level> dim) {
        if (dim == Level.NETHER) {
            return structureId.equals("nether_fortress") ||
                    structureId.equals("bastion_remnant") ||
                    structureId.equals("nether_fossil") ||
                    structureId.equals("ruined_portal");
        }
        if (dim == Level.END) {
            return structureId.equals("end_city");
        }
        // Overworld
        return !structureId.equals("nether_fortress") &&
                !structureId.equals("bastion_remnant") &&
                !structureId.equals("nether_fossil") &&
                !structureId.equals("end_city");
    }

    @Override
    public boolean includeY() {
        return false;
    }

    @Override
    public List<String> getExtraLore(ServerLevel level, BlockPos targetPos, ServerPlayer player) {
        int dist = (int) Math.sqrt(player.blockPosition().distToCenterSqr(targetPos.getX(), player.getBlockY(), targetPos.getZ()));
        String name = lastFoundName.get();
        if (name == null || name.isBlank()) {
            name = "Structure";
        }
        return List.of("Target: " + name, "Distance: ~" + dist + " blocks");
    }

    @Override
    public List<String> getNotFoundLore(ServerLevel level, ServerPlayer player) {
        return List.of("No unvisited structures found within 1600 blocks.", "Try exploring in another direction.");
    }

    @Override
    public MapCodec<? extends CompassTarget> codec() {
        return CODEC;
    }
}
