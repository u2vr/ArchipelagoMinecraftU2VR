package gg.archipelago.aprandomizer.mixin;

import gg.archipelago.aprandomizer.client.ClientRecipeManager;
import gg.archipelago.aprandomizer.client.RecipeSkillTreeScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PauseScreen.class)
public abstract class MixinPauseScreen extends Screen {

    protected MixinPauseScreen(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void aprandomizer$addRecipeTreeButton(CallbackInfo ci) {
        int tickets = ClientRecipeManager.getTickets();
        boolean ticketMode = ClientRecipeManager.isTicketMode();

        Component buttonText;
        if (ticketMode) {
            buttonText = Component.literal("✦ Рецепты [🎟 " + tickets + "]");
        } else {
            buttonText = Component.literal("✦ Древо рецептов");
        }

        int btnWidth = 124;
        int btnHeight = 20;
        int btnX = this.width - btnWidth - 8;
        int btnY = 8;

        this.addRenderableWidget(Button.builder(buttonText, button -> {
            if (this.minecraft != null) {
                this.minecraft.gui.setScreen(new RecipeSkillTreeScreen(this));
            }
        }).bounds(btnX, btnY, btnWidth, btnHeight).build());
    }
}

