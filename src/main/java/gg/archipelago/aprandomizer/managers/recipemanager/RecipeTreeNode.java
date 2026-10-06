package gg.archipelago.aprandomizer.managers.recipemanager;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.Recipe;

import java.util.List;
import java.util.Set;

public record RecipeTreeNode(
        String id,
        String parentId,
        int tier,
        String item,
        String name,
        List<ResourceKey<Recipe<?>>> recipes
) {
    public RecipeTreeNode(String id, int tier, String item, String name, List<ResourceKey<Recipe<?>>> recipes) {
        this(id, null, tier, item, name, recipes);
    }

    public boolean isUnlocked(Set<ResourceKey<Recipe<?>>> unlocked) {
        if (recipes.isEmpty()) return true;
        for (ResourceKey<Recipe<?>> r : recipes) {
            if (!unlocked.contains(r)) {
                return false;
            }
        }
        return true;
    }
}
