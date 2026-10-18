package gg.archipelago.aprandomizer.mixin;

import gg.archipelago.aprandomizer.client.AbilitySidebarRenderer;
import gg.archipelago.aprandomizer.client.ClientAbilityManager;
import gg.archipelago.aprandomizer.client.ClientRecipeManager;
import gg.archipelago.aprandomizer.client.RecipeSkillTreeScreen;
import gg.archipelago.aprandomizer.managers.RestrictionManager;
import gg.archipelago.aprandomizer.managers.recipemanager.RecipeTreeManager;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerScreen.class)
public abstract class MixinAbstractContainerScreen extends Screen {

    @Shadow protected int leftPos;
    @Shadow protected int topPos;
    @Shadow protected int imageWidth;
    @Shadow protected int imageHeight;

    @Shadow protected abstract Slot getHoveredSlot(double mouseX, double mouseY);

    @Unique
    private static final Identifier APRANDOMIZER$DISABLED_SLOT_SPRITE =
            Identifier.withDefaultNamespace("container/crafter/disabled_slot");

    @Unique
    private Button aprandomizer$recipeTreeButton;

    protected MixinAbstractContainerScreen(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void aprandomizer$addRecipeTreeButtonToInventory(CallbackInfo ci) {
        if ((Object) this instanceof InventoryScreen) {
            int btnX = this.leftPos;
            int btnY = Math.max(2, this.topPos - 22);
            int btnWidth = 114;
            int btnHeight = 20;

            int tickets = ClientRecipeManager.getTickets();
            boolean ticketMode = ClientRecipeManager.isTicketMode();
            Component text = ticketMode
                    ? Component.translatable("aprandomizer.button.recipes_tickets", tickets)
                    : Component.translatable("aprandomizer.button.recipe_tree");

            this.aprandomizer$recipeTreeButton = Button.builder(text, b -> {
                if (this.minecraft != null) {
                    this.minecraft.gui.setScreen(new RecipeSkillTreeScreen(this));
                }
            }).bounds(btnX, btnY, btnWidth, btnHeight).build();

            this.addRenderableWidget(this.aprandomizer$recipeTreeButton);
        }
    }

    @Inject(method = "containerTick", at = @At("TAIL"))
    private void aprandomizer$updateRecipeTreeButton(CallbackInfo ci) {
        if (this.aprandomizer$recipeTreeButton != null) {
            int tickets = ClientRecipeManager.getTickets();
            boolean ticketMode = ClientRecipeManager.isTicketMode();
            Component text = ticketMode
                    ? Component.translatable("aprandomizer.button.recipes_tickets", tickets)
                    : Component.translatable("aprandomizer.button.recipe_tree");
            this.aprandomizer$recipeTreeButton.setMessage(text);
        }
    }

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

    @Inject(method = "extractContents", at = @At("TAIL"))
    private void aprandomizer$renderAbilitiesSidebar(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if ((Object) this instanceof InventoryScreen invScreen) {
            AbilitySidebarRenderer.render(graphics, invScreen, this.leftPos, this.topPos, this.imageWidth, this.imageHeight, mouseX, mouseY);
        }
    }

    @Inject(method = "hasClickedOutside", at = @At("HEAD"), cancellable = true)
    private void aprandomizer$onHasClickedOutside(double mouseX, double mouseY, int guiLeft, int guiTop, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof InventoryScreen) {
            if (AbilitySidebarRenderer.isMouseOverSidebar(this.leftPos, this.topPos, this.imageWidth, mouseX, mouseY)) {
                cir.setReturnValue(false);
                return;
            }
            if (this.aprandomizer$recipeTreeButton != null && this.aprandomizer$recipeTreeButton.isMouseOver(mouseX, mouseY)) {
                cir.setReturnValue(false);
            }
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void aprandomizer$onContainerMouseClicked(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof InventoryScreen invScreen) {
            if (AbilitySidebarRenderer.isMouseOverSidebar(this.leftPos, this.topPos, this.imageWidth, event.x(), event.y())) {
                AbilitySidebarRenderer.mouseClicked(invScreen, event, this.leftPos, this.topPos, this.imageWidth, this.imageHeight);
                cir.setReturnValue(true);
                return;
            }
        }

        // Alt + Left Click on slot to trash item
        boolean isAlt = event.hasAltDown()
                || InputConstants.isKeyDown(InputConstants.KEY_LALT)
                || InputConstants.isKeyDown(InputConstants.KEY_RALT);
        if (isAlt && (event.button() == InputConstants.MOUSE_BUTTON_LEFT || event.button() == 0) && ClientAbilityManager.isTrashCanUnlocked()) {
            Slot slot = this.getHoveredSlot(event.x(), event.y());
            if (slot != null && slot.hasItem()) {
                ItemStack stack = slot.getItem();
                ClientAbilityManager.setTrashCanItem(stack.copy());
                ClientAbilityManager.sendAltTrash(slot.index);
                slot.set(ItemStack.EMPTY);
                Minecraft mc = Minecraft.getInstance();
                if (mc.player != null) {
                    mc.player.playSound(SoundEvents.ITEM_PICKUP, 0.4f, 0.8f);
                }
                cir.setReturnValue(true);
            }
        }
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
    private void aprandomizer$onContainerMouseReleased(MouseButtonEvent event, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof InventoryScreen) {
            if (AbilitySidebarRenderer.isMouseOverSidebar(this.leftPos, this.topPos, this.imageWidth, event.x(), event.y())) {
                cir.setReturnValue(true);
            }
        }
    }
}
