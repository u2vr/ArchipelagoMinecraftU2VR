package gg.archipelago.aprandomizer.data.advancements;

import gg.archipelago.aprandomizer.advancements.ExplorationCriteria;
import gg.archipelago.aprandomizer.exploration.ExplorationData;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.triggers.PlayerTrigger;
import net.minecraft.data.advancements.AdvancementSubProvider;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Items;

import java.util.HashMap;
import java.util.Map;

public class ExplorationAdvancementProvider extends AdvancementSubProvider {

    public ExplorationAdvancementProvider(BootstrapContext<Advancement> output) {
        super(output);
    }

    @Override
    public void generate() {
        // 1. Root advancement
        AdvancementHolder root = Advancement.Builder.recipeAdvancement()
                .rootDisplay(
                        Items.COMPASS,
                        Component.literal("World Exploration"),
                        Component.literal("Discover unique biomes and structures across the dimensions"),
                        Identifier.withDefaultNamespace("gui/advancements/backgrounds/adventure"),
                        AdvancementType.TASK,
                        false,
                        false,
                        false)
                .addCriterion("auto", PlayerTrigger.TriggerInstance.tick())
                .save(output, ExplorationData.ROOT_ADVANCEMENT_ID);

        // 2. Categories
        Map<ExplorationData.BiomeCategory, AdvancementHolder> catHolders = new HashMap<>();
        for (ExplorationData.BiomeCategory cat : ExplorationData.BiomeCategory.values()) {
            AdvancementHolder catAdv = Advancement.Builder.recipeAdvancement()
                    .parent(root)
                    .display(
                            cat.getIcon(),
                            Component.literal(cat.getDisplayName()),
                            Component.literal("Explore biomes in " + cat.getDisplayName()),
                            AdvancementType.TASK,
                            true,
                            false,
                            false)
                    .addCriterion("visited", ExplorationCriteria.TriggerInstance.explored(cat.getPath()))
                    .save(output, cat.getAdvancementId());
            catHolders.put(cat, catAdv);
        }

        AdvancementHolder structCat = Advancement.Builder.recipeAdvancement()
                .parent(root)
                .display(
                        Items.CHISELED_STONE_BRICKS,
                        Component.literal("Structures"),
                        Component.literal("Discover unique structures across the world"),
                        AdvancementType.TASK,
                        true,
                        false,
                        false)
                .addCriterion("visited", ExplorationCriteria.TriggerInstance.explored("structures"))
                .save(output, ExplorationData.STRUCTURES_CATEGORY_ID);

        // 3. Child biomes
        for (ExplorationData.BiomeEntry biome : ExplorationData.BIOMES) {
            AdvancementHolder parent = catHolders.get(biome.category());
            Advancement.Builder.recipeAdvancement()
                    .parent(parent)
                    .display(
                            biome.icon(),
                            Component.literal(biome.displayName()),
                            Component.literal("Visit the " + biome.displayName() + " biome"),
                            AdvancementType.TASK,
                            true,
                            true,
                            false)
                    .addCriterion("visited", ExplorationCriteria.TriggerInstance.explored(biome.id()))
                    .save(output, biome.getAdvancementId());
        }

        // 4. Child structures
        for (ExplorationData.StructureCheckEntry structure : ExplorationData.STRUCTURES) {
            Advancement.Builder.recipeAdvancement()
                    .parent(structCat)
                    .display(
                            structure.icon(),
                            Component.literal(structure.displayName()),
                            Component.literal("Discover the " + structure.displayName() + " structure"),
                            AdvancementType.TASK,
                            true,
                            true,
                            false)
                    .addCriterion("visited", ExplorationCriteria.TriggerInstance.explored(structure.id()))
                    .save(output, structure.getAdvancementId());
        }
    }
}
