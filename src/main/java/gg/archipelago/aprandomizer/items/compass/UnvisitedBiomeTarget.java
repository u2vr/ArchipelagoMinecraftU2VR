package gg.archipelago.aprandomizer.items.compass;

import com.mojang.serialization.MapCodec;
import gg.archipelago.aprandomizer.APRandomizer;
import gg.archipelago.aprandomizer.data.WorldData;
import gg.archipelago.aprandomizer.exploration.ExplorationData;
import gg.archipelago.aprandomizer.exploration.ExplorationManager;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.biome.Biome;

import java.util.List;

public class UnvisitedBiomeTarget implements BiomeTarget {

    public static final MapCodec<UnvisitedBiomeTarget> CODEC = MapCodec.unit(UnvisitedBiomeTarget::new);

    @Override
    public boolean isTargetingBiome(Holder<Biome> biome, ServerPlayer player) {
        ResourceKey<Biome> key = biome.unwrapKey().orElse(null);
        if (key == null) return false;
        String biomeId = key.identifier().toString();

        ExplorationData.BiomeEntry entry = ExplorationData.getBiomeById(biomeId);
        if (entry == null) {
            return false;
        }

        if (!ExplorationManager.isBiomeCheckEnabled(entry)) {
            return false;
        }

        WorldData worldData = APRandomizer.getWorldData();
        if (worldData == null) {
            return false;
        }

        return !worldData.getVisitedBiomes().contains(biomeId);
    }

    @Override
    public List<String> getNotFoundLore(ServerLevel level, ServerPlayer player) {
        return List.of("No unvisited biomes found within 6400 blocks.", "Try exploring in another direction.");
    }

    @Override
    public MapCodec<UnvisitedBiomeTarget> codec() {
        return CODEC;
    }

}
