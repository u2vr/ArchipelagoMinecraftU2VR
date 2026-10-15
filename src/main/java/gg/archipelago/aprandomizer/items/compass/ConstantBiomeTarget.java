package gg.archipelago.aprandomizer.items.compass;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.codec.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.biome.Biome;

import java.util.List;

public record ConstantBiomeTarget(HolderSet<Biome> biomes) implements BiomeTarget {
    public static final MapCodec<ConstantBiomeTarget> CODEC = RecordCodecBuilder.mapCodec(instance -> instance
            .group(
                    RegistryCodecs.holderSet(Registries.BIOME).fieldOf("biomes").forGetter(ConstantBiomeTarget::biomes))
            .apply(instance, ConstantBiomeTarget::new));

    @Override
    public boolean isTargetingBiome(Holder<Biome> biome, ServerPlayer player) {
        return biomes.contains(biome);
    }

    @Override
    public List<String> getNotFoundLore(net.minecraft.server.level.ServerLevel level, ServerPlayer player) {
        boolean hasNether = biomes.stream().anyMatch(h -> h.is(net.minecraft.world.level.biome.Biomes.NETHER_WASTES) || h.is(net.minecraft.world.level.biome.Biomes.WARPED_FOREST) || h.is(net.minecraft.world.level.biome.Biomes.CRIMSON_FOREST) || h.is(net.minecraft.world.level.biome.Biomes.SOUL_SAND_VALLEY) || h.is(net.minecraft.world.level.biome.Biomes.BASALT_DELTAS));
        boolean hasEnd = biomes.stream().anyMatch(h -> h.is(net.minecraft.world.level.biome.Biomes.THE_END) || h.is(net.minecraft.world.level.biome.Biomes.END_HIGHLANDS) || h.is(net.minecraft.world.level.biome.Biomes.END_MIDLANDS) || h.is(net.minecraft.world.level.biome.Biomes.SMALL_END_ISLANDS) || h.is(net.minecraft.world.level.biome.Biomes.END_BARRENS));
        if (hasNether && level.dimension() != net.minecraft.world.level.Level.NETHER) {
            return java.util.List.of("This biome is in the Nether!", "Travel to the Nether to locate it.");
        }
        if (hasEnd && level.dimension() != net.minecraft.world.level.Level.END) {
            return java.util.List.of("This biome is in The End!", "Travel to The End to locate it.");
        }
        if (!hasNether && !hasEnd && level.dimension() != net.minecraft.world.level.Level.OVERWORLD) {
            return java.util.List.of("This biome is in the Overworld!", "Travel to the Overworld to locate it.");
        }
        return java.util.List.of("Biome not found within 6400 blocks.", "Try searching from a different location.");
    }

    @Override
    public java.util.Optional<net.minecraft.core.BlockPos> findTarget(net.minecraft.server.level.ServerLevel level, net.minecraft.core.BlockPos start, ServerPlayer player) {
        boolean hasNether = biomes.stream().anyMatch(h -> h.is(net.minecraft.world.level.biome.Biomes.NETHER_WASTES) || h.is(net.minecraft.world.level.biome.Biomes.WARPED_FOREST) || h.is(net.minecraft.world.level.biome.Biomes.CRIMSON_FOREST) || h.is(net.minecraft.world.level.biome.Biomes.SOUL_SAND_VALLEY) || h.is(net.minecraft.world.level.biome.Biomes.BASALT_DELTAS));
        boolean hasEnd = biomes.stream().anyMatch(h -> h.is(net.minecraft.world.level.biome.Biomes.THE_END) || h.is(net.minecraft.world.level.biome.Biomes.END_HIGHLANDS) || h.is(net.minecraft.world.level.biome.Biomes.END_MIDLANDS) || h.is(net.minecraft.world.level.biome.Biomes.SMALL_END_ISLANDS) || h.is(net.minecraft.world.level.biome.Biomes.END_BARRENS));
        if (hasNether && level.dimension() != net.minecraft.world.level.Level.NETHER) {
            return java.util.Optional.empty();
        }
        if (hasEnd && level.dimension() != net.minecraft.world.level.Level.END) {
            return java.util.Optional.empty();
        }
        if (!hasNether && !hasEnd && level.dimension() != net.minecraft.world.level.Level.OVERWORLD) {
            return java.util.Optional.empty();
        }
        return BiomeTarget.super.findTarget(level, start, player);
    }

    @Override
    public MapCodec<? extends CompassTarget> codec() {
        return CODEC;
    }

}
