package gg.archipelago.aprandomizer.data.advancements;

import gg.archipelago.aprandomizer.APStructures;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.predicates.LocationPredicate;
import net.minecraft.advancements.triggers.PlayerTrigger;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.advancements.AdvancementSubProvider;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.levelgen.structure.Structure;

public class VanillaOverrideAdvancementProvider extends AdvancementSubProvider {

    public VanillaOverrideAdvancementProvider(BootstrapContext<Advancement> output) {
        super(output);
    }

    @Override
    public void generate() {
        HolderGetter<Structure> structures = output.lookup(Registries.STRUCTURE);

        Advancement.Builder.advancement()
                .parent(Identifier.withDefaultNamespace("end/enter_end_gateway"))
                .addCriterion("in_city", PlayerTrigger.TriggerInstance.located(
                        LocationPredicate.Builder.inStructure(structures.getOrThrow(BuiltinStructures.END_CITY))))
                .addCriterion("in_city_nether", PlayerTrigger.TriggerInstance.located(
                        LocationPredicate.Builder.inStructure(structures.getOrThrow(APStructures.END_CITY_NETHER_STRUCTURE))))
                .display(
                        Items.PURPUR_BLOCK,
                        Component.translatable("advancements.end.find_end_city.title"),
                        Component.translatable("advancements.end.find_end_city.description"),
                        AdvancementType.TASK,
                        true,
                        true,
                        false)
                .requirements(AdvancementRequirements.Strategy.OR)
                .save(output, Identifier.withDefaultNamespace("end/find_end_city"));
    }

}
