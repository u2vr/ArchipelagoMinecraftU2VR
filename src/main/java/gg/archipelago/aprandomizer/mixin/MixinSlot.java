package gg.archipelago.aprandomizer.mixin;

import gg.archipelago.aprandomizer.managers.RestrictionManager;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Slot.class)
public abstract class MixinSlot {

    @Shadow @Final public Container container;
    @Shadow @Final private int slot;

    @Inject(method = "mayPlace", at = @At("HEAD"), cancellable = true)
    private void aprandomizer$onMayPlace(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (this.container instanceof Inventory) {
            if (RestrictionManager.isSlotRestricted(this.slot)) {
                cir.setReturnValue(false);
            }
        }
    }

    @Inject(method = "mayPickup", at = @At("HEAD"), cancellable = true)
    private void aprandomizer$onMayPickup(Player player, CallbackInfoReturnable<Boolean> cir) {
        // If an item somehow was in a restricted slot, allow picking it up so it can be moved out
    }

    @Inject(method = "getMaxStackSize()I", at = @At("HEAD"), cancellable = true)
    private void aprandomizer$onGetMaxStackSize(CallbackInfoReturnable<Integer> cir) {
        if (this.container instanceof Inventory && RestrictionManager.isSlotRestricted(this.slot)) {
            cir.setReturnValue(0);
        }
    }

    @Inject(method = "getMaxStackSize(Lnet/minecraft/world/item/ItemStack;)I", at = @At("HEAD"), cancellable = true)
    private void aprandomizer$onGetMaxStackSizeStack(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        if (this.container instanceof Inventory && RestrictionManager.isSlotRestricted(this.slot)) {
            cir.setReturnValue(0);
        }
    }
}
