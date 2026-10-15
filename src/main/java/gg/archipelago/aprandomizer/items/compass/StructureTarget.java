package gg.archipelago.aprandomizer.items.compass;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import gg.archipelago.aprandomizer.structures.level.StructureLevelReference;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.levelgen.structure.Structure;

import gg.archipelago.aprandomizer.structures.level.CurrentLevel;
import net.minecraft.world.level.Level;
import java.util.List;
import java.util.Optional;

public record StructureTarget(TagKey<Structure> structures, StructureLevelReference level) implements CompassTarget {

    public static final MapCodec<StructureTarget> CODEC = RecordCodecBuilder.mapCodec(instance -> instance
            .group(
                    TagKey.hashedCodec(Registries.STRUCTURE).fieldOf("structures").forGetter(StructureTarget::structures),
                    StructureLevelReference.CODEC.fieldOf("level").forGetter(StructureTarget::level))
            .apply(instance, StructureTarget::new));

    @Override
    public Optional<BlockPos> findTarget(ServerLevel currentLevel, BlockPos start, ServerPlayer player) {
        if (level instanceof CurrentLevel || currentLevel.dimension() == level.level()) {
            return Optional.ofNullable(currentLevel.findNearestMapStructure(structures, start, 75, false));
        }
        return Optional.empty();
    }

    @Override
    public List<String> getNotFoundLore(ServerLevel currentLevel, ServerPlayer player) {
        if (!(level instanceof CurrentLevel) && currentLevel.dimension() != level.level()) {
            if (level.level() == Level.NETHER) {
                return List.of("This structure is in the Nether!", "Travel to the Nether to locate it.");
            } else if (level.level() == Level.END) {
                return List.of("This structure is in The End!", "Travel to The End to locate it.");
            } else {
                return List.of("This structure is in the Overworld!", "Travel to the Overworld to locate it.");
            }
        }
        return List.of("Structure not found within search radius.", "Try searching from a different location.");
    }

    @Override
    public boolean includeY() {
        return false;
    }

    @Override
    public MapCodec<? extends CompassTarget> codec() {
        return CODEC;
    }

}
