package gg.archipelago.aprandomizer.data;

import gg.archipelago.aprandomizer.APRandomizer;
import gg.archipelago.aprandomizer.APRegistries;
import gg.archipelago.aprandomizer.APStructures;
import gg.archipelago.aprandomizer.data.advancements.AfterAdvancementProvider;
import gg.archipelago.aprandomizer.data.advancements.ReceivedAdvancementProvider;
import gg.archipelago.aprandomizer.data.advancements.VanillaOverrideAdvancementProvider;
import gg.archipelago.aprandomizer.data.datamaps.APDataMapProvider;
import gg.archipelago.aprandomizer.data.loot.APAddedLootTableProvider;
import gg.archipelago.aprandomizer.data.loot.APGlobalLootModifierProvider;
import gg.archipelago.aprandomizer.data.recipes.APRecipeProvider;
import gg.archipelago.aprandomizer.data.tags.APBiomeTagsProvider;
import gg.archipelago.aprandomizer.data.tags.APDamageTypeTagsProvider;
import gg.archipelago.aprandomizer.data.tags.APStructureTagsProvider;
import gg.archipelago.aprandomizer.data.tags.APTimelineTagsProvider;
import gg.archipelago.aprandomizer.items.APItems;
import gg.archipelago.aprandomizer.locations.APLocations;
import gg.archipelago.aprandomizer.modifiers.APStructureModifiers;
import gg.archipelago.aprandomizer.structures.APStructureSets;
import gg.archipelago.aprandomizer.structures.APTemplatePools;
import gg.archipelago.aprandomizer.timelines.APTimelines;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.advancements.AdvancementProvider;
import net.minecraft.data.advancements.packs.*;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@EventBusSubscriber(modid = APRandomizer.MODID)
public class APDataGenerator {

    @SubscribeEvent
    public static void onDataGen(GatherDataEvent.Client event) {
        event.createWorldRegistryObjects(
                new RegistrySetBuilder()
                        .add(NeoForgeRegistries.Keys.STRUCTURE_MODIFIERS, APStructureModifiers::bootstrap)
                        .add(Registries.STRUCTURE, APStructures::bootstrap)
                        .add(Registries.TEMPLATE_POOL, APTemplatePools::bootstrap)
                        .add(Registries.STRUCTURE_SET, APStructureSets::bootstrap)
                        .add(APRegistries.ARCHIPELAGO_LOCATION, APLocations::bootstrap)
                        .add(APRegistries.ARCHIPELAGO_ITEM, APItems::bootstrap)
                        .add(Registries.TIMELINE, APTimelines::bootstrap),
                Set.of(APRandomizer.MODID, "minecraft"));

        event.createReloadableRegistryObjects(
                new RegistrySetBuilder()
                        .add(Registries.ADVANCEMENT, new AdvancementProvider(List.of(
                                ReceivedAdvancementProvider::new,
                                gg.archipelago.aprandomizer.data.advancements.ExplorationAdvancementProvider::new,
                                output -> new AfterAdvancementProvider(output, List.of(

                                        VanillaStoryAdvancements::new,
                                        VanillaNetherAdvancements::new,
                                        VanillaTheEndAdvancements::new,
                                        VanillaHusbandryAdvancements::new,
                                        VanillaAdventureAdvancements::new),
                                        id -> Identifier.fromNamespaceAndPath(APRandomizer.MODID, "vanilla/" + id.getPath() + "_after")),
                                VanillaOverrideAdvancementProvider::new)))
                        .add(RecipeProvider.asBootstrap(APRecipeProvider::new))
                        .add(Registries.LOOT_TABLE, new LootTableProvider(Set.of(), List.of(
                                new LootTableProvider.SubProviderEntry(APAddedLootTableProvider::new, LootContextParamSets.ALL_PARAMS)))),
                Set.of(APRandomizer.MODID, "minecraft"));

        CompletableFuture<HolderLookup.Provider> worldLookup = event.getWorldLookupProvider();

        event.addProvider(new APDamageTypeTagsProvider(event.getGenerator().getPackOutput(), worldLookup));
        event.addProvider(new APDataMapProvider(event.getGenerator().getPackOutput(), worldLookup));
        event.addProvider(new APBiomeTagsProvider(event.getGenerator().getPackOutput(), worldLookup));
        event.addProvider(new APStructureTagsProvider(event.getGenerator().getPackOutput(), worldLookup));
        event.addProvider(new APGlobalLootModifierProvider(event.getGenerator().getPackOutput(), worldLookup));
        event.addProvider(new APTimelineTagsProvider(event.getGenerator().getPackOutput(), worldLookup));
    }
}
