package gg.archipelago.aprandomizer.modifiers;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import gg.archipelago.aprandomizer.APRandomizer;
import gg.archipelago.aprandomizer.ap.storage.APMCData;
import gg.archipelago.aprandomizer.datamaps.APDataMaps;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.codec.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.neoforged.neoforge.common.world.ModifiableStructureInfo;
import net.neoforged.neoforge.common.world.StructureModifier;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// TODO: null safety

public record APStructureModifier(Map<ResourceKey<Level>, LevelReplacements> levels, ResourceKey<Structure> defaultStructure, Identifier name) implements StructureModifier {

    public static final MapCodec<APStructureModifier> CODEC = RecordCodecBuilder.mapCodec(instance -> instance
            .group(
                    Codec.unboundedMap(ResourceKey.codec(Registries.DIMENSION), LevelReplacements.CODEC).optionalFieldOf("levels", Map.of()).forGetter(APStructureModifier::levels),
                    ResourceKey.codec(Registries.STRUCTURE).fieldOf("default_structure").forGetter(APStructureModifier::defaultStructure),
                    Identifier.CODEC.fieldOf("name").forGetter(APStructureModifier::name))
            .apply(instance, APStructureModifier::new));

    public static final DeferredRegister<MapCodec<? extends StructureModifier>> STRUCTURE_MODIFIERS = DeferredRegister.create(NeoForgeRegistries.STRUCTURE_MODIFIER_SERIALIZERS, APRandomizer.MODID);
    private static final DeferredHolder<MapCodec<? extends StructureModifier>, MapCodec<APStructureModifier>> AP_STRUCTURE_MODIFIER = STRUCTURE_MODIFIERS.register("ap_structure_modifier", () -> CODEC);

    public static Map<Identifier, ResourceKey<Level>> structures = new HashMap<>();

    public static void loadTags() {
        if (!structures.isEmpty()) return;
        if (APRandomizer.getApmcData().state == APMCData.State.MISSING) {
            APRandomizer.LOGGER.error("APMCData is missing, cannot load tags.");
            return;
        }
        APRandomizer.LOGGER.info("Loading Biome info.");

        APMCData data = APRandomizer.getApmcData();
        for (Map.Entry<String, String> entry : data.structures.entrySet()) {
            switch (entry.getKey()) {
                case "Overworld Structure 1", "Overworld Structure 2" ->
                    structures.put(getIdentifier(entry.getValue()), Level.OVERWORLD);
                case "Nether Structure 1", "Nether Structure 2" ->
                    structures.put(getIdentifier(entry.getValue()), Level.NETHER);
                case "The End Structure" ->
                    structures.put(getIdentifier(entry.getValue()), Level.END);
            }
        }
    }

    @Override
    public void modify(RegistryAccess registries, Holder<Structure> structure, Phase phase, ModifiableStructureInfo.StructureInfo.Builder builder) {
        // Disabled: Structure shuffle alters structure biomes and breaks world generation.
    }

    private static Identifier getIdentifier(String name) {
        return switch (name) {
            case "Village" -> Identifier.fromNamespaceAndPath(APRandomizer.MODID, "village");
            case "Pillager Outpost" -> Identifier.fromNamespaceAndPath(APRandomizer.MODID, "pillager_outpost");
            case "Nether Fortress" -> Identifier.fromNamespaceAndPath(APRandomizer.MODID, "fortress");
            case "Bastion Remnant" -> Identifier.fromNamespaceAndPath(APRandomizer.MODID, "bastion_remnant");
            case "End City" -> Identifier.fromNamespaceAndPath(APRandomizer.MODID, "end_city");
            default -> Identifier.fromNamespaceAndPath(APRandomizer.MODID, "unknown");
        };
    }

    @Override
    public MapCodec<APStructureModifier> codec() {
        return CODEC;
    }

    public record LevelReplacements(Map<ResourceKey<Structure>, HolderSet<Biome>> replacements) {
        public static final Codec<LevelReplacements> CODEC = RecordCodecBuilder.create(instance -> instance
                .group(
                        Codec.unboundedMap(ResourceKey.codec(Registries.STRUCTURE), RegistryCodecs.holderSet(Registries.BIOME)).fieldOf("replacements").forGetter(LevelReplacements::replacements))
                .apply(instance, LevelReplacements::new));
    }
}

