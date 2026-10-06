package gg.archipelago.aprandomizer.items;

import com.mojang.serialization.MapCodec;
import gg.archipelago.aprandomizer.managers.recipemanager.RecipeTreeManager;
import net.minecraft.server.MinecraftServer;

public record RecipeUnlockReward() implements APReward {
    public static final MapCodec<RecipeUnlockReward> MAP_CODEC = MapCodec.unit(RecipeUnlockReward::new);

    @Override
    public MapCodec<? extends APReward> codec() {
        return MAP_CODEC;
    }

    @Override
    public void give(MinecraftServer server) {
        if (RecipeTreeManager.isTicketMode()) {
            RecipeTreeManager.addRecipeTicket(server);
        } else {
            RecipeTreeManager.unlockNextRecipeNode(server);
        }
    }
}
