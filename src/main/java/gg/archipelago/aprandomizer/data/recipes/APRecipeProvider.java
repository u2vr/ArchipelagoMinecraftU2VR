package gg.archipelago.aprandomizer.data.recipes;

import gg.archipelago.aprandomizer.APRandomizer;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.advancements.triggers.ImpossibleTrigger;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;

public class APRecipeProvider extends RecipeProvider {

    public APRecipeProvider(BootstrapContext<Recipe<?>> recipeOutput, BootstrapContext<Advancement> advancementOutput) {
        super(recipeOutput, advancementOutput);
    }

    @Override
    protected void buildRecipes() {
        RecipeOutput output = new NoAdvancementsRecipeOutput(this.output);
        this.shaped(RecipeCategory.FOOD, Items.ENCHANTED_GOLDEN_APPLE)
                .define('#', Items.GOLD_BLOCK)
                .define('A', Items.APPLE)
                .pattern("###")
                .pattern("#A#")
                .pattern("###")
                .unlockedBy("impossible", CriteriaTriggers.IMPOSSIBLE.createCriterion(new ImpossibleTrigger.TriggerInstance()))
                .save(output, ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath(APRandomizer.MODID, "enchanted_apple")));

    }

}
