package gg.archipelago.aprandomizer.mixin;

import gg.archipelago.aprandomizer.managers.RestrictionManager;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Inventory.class)
public abstract class MixinInventory {

    @Shadow @Final public NonNullList<ItemStack> items;
    @Shadow public int selected;

    @Shadow public abstract ItemStack getItem(int index);
    @Shadow abstract boolean hasRemainingSpaceForItem(ItemStack destination, ItemStack origin);

    @Inject(method = "getFreeSlot", at = @At("HEAD"), cancellable = true)
    private void aprandomizer$onGetFreeSlot(CallbackInfoReturnable<Integer> cir) {
        if (RestrictionManager.isInventorySlotRestrictionEnabled()) {
            int allowed = RestrictionManager.getAllowedSlots();
            if (allowed < 36) {
                for (int i = 0; i < allowed && i < this.items.size(); i++) {
                    if (this.items.get(i).isEmpty()) {
                        cir.setReturnValue(i);
                        return;
                    }
                }
                cir.setReturnValue(-1);
            }
        }
    }

    @Inject(method = "getSlotWithRemainingSpace", at = @At("HEAD"), cancellable = true)
    private void aprandomizer$onGetSlotWithRemainingSpace(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        if (RestrictionManager.isInventorySlotRestrictionEnabled()) {
            int allowed = RestrictionManager.getAllowedSlots();
            if (allowed < 36) {
                if (this.hasRemainingSpaceForItem(this.getItem(this.selected), stack)) {
                    cir.setReturnValue(this.selected);
                    return;
                }
                if ((!RestrictionManager.isOffhandRestrictionEnabled() || RestrictionManager.isOffhandUnlocked()) && this.hasRemainingSpaceForItem(this.getItem(40), stack)) {
                    cir.setReturnValue(40);
                    return;
                }
                for (int i = 0; i < allowed && i < this.items.size(); i++) {
                    if (this.hasRemainingSpaceForItem(this.items.get(i), stack)) {
                        cir.setReturnValue(i);
                        return;
                    }
                }
                cir.setReturnValue(-1);
            }
        }
    }

    @Inject(method = "add(ILnet/minecraft/world/item/ItemStack;)Z", at = @At("HEAD"), cancellable = true)
    private void aprandomizer$onAdd(int slot, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (RestrictionManager.isInventorySlotRestrictionEnabled() && slot >= 0 && RestrictionManager.isSlotRestricted(slot)) {
            cir.setReturnValue(false);
        }
    }
}
