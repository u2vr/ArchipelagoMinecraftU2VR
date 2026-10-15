package gg.archipelago.aprandomizer.mixin;

import gg.archipelago.aprandomizer.client.AbilitySidebarRenderer;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractRecipeBookScreen.class)
public abstract class MixinAbstractRecipeBookScreen {

    @Inject(method = "hasClickedOutside", at = @At("HEAD"), cancellable = true)
    private void aprandomizer$onRecipeBookHasClickedOutside(double mouseX, double mouseY, int guiLeft, int guiTop, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof InventoryScreen) {
            // InventoryScreen default imageWidth is 176
            if (AbilitySidebarRenderer.isMouseOverSidebar(guiLeft, guiTop, 176, mouseX, mouseY)) {
                cir.setReturnValue(false);
            }
        }
    }
}
