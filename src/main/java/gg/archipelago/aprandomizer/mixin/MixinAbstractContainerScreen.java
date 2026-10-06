package gg.archipelago.aprandomizer.mixin;

import gg.archipelago.aprandomizer.managers.RestrictionManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
public abstract class MixinAbstractContainerScreen {

    @Unique
    private static final Identifier APRANDOMIZER$DISABLED_SLOT_SPRITE =
            Identifier.withDefaultNamespace("container/crafter/disabled_slot");

    @Inject(method = "extractSlot", at = @At("TAIL"))
    private void aprandomizer$renderLockedSlotOverlay(GuiGraphicsExtractor graphics, Slot slot, int mouseX, int mouseY, CallbackInfo ci) {
        if (slot.container instanceof Inventory inv) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && inv.player == mc.player) {
                if (RestrictionManager.isSlotRestricted(slot.getSlotIndex())) {
                    graphics.blitSprite(
                            RenderPipelines.GUI_TEXTURED,
                            APRANDOMIZER$DISABLED_SLOT_SPRITE,
                            slot.x - 1,
                            slot.y - 1,
                            18,
                            18
                    );
                }
            }
        }
    }
}
