package gg.archipelago.aprandomizer.structures.level;

import com.mojang.serialization.MapCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public record CurrentLevel() implements StructureLevelReference {
    public static final MapCodec<CurrentLevel> CODEC = MapCodec.unit(CurrentLevel::new);

    @Override
    public MapCodec<? extends StructureLevelReference> codec() {
        return CODEC;
    }

    @Override
    public ResourceKey<Level> level() {
        return Level.OVERWORLD;
    }
}
