package gg.archipelago.aprandomizer.managers.chest;

import gg.archipelago.aprandomizer.APMenus;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class SharedChestMenu extends AbstractContainerMenu {
    public static final int RECEIVE_SLOTS = SharedChestContainer.RECEIVE_SLOTS; // 18
    public static final int SEND_SLOT = SharedChestContainer.SEND_SLOT;         // 18
    public static final int TOTAL_CHEST_SLOTS = SharedChestContainer.CONTAINER_SIZE; // 19

    private final Container container;

    public static class SendSlot extends Slot {
        public SendSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPickup(Player player) {
            return !SharedChestManager.isTransferPending();
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return !SharedChestManager.isTransferPending();
        }
    }

    public SharedChestMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(TOTAL_CHEST_SLOTS));
    }

    public SharedChestMenu(int containerId, Inventory playerInventory, Container container) {
        super(APMenus.SHARED_CHEST.get(), containerId);
        checkContainerSize(container, TOTAL_CHEST_SLOTS);
        this.container = container;
        container.startOpen(playerInventory.player);

        // 1. Receiving Inventory (2 rows x 9 columns = 18 slots, slots 0..17)
        for (int row = 0; row < 2; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(container, col + row * 9, 8 + col * 18, 20 + row * 18));
            }
        }

        // 2. Send Slot (slot 18, 1 slot at x=8, y=73)
        this.addSlot(new SendSlot(container, SEND_SLOT, 8, 73));

        // 3. Player Inventory (3 rows x 9 columns, slots 19..45)
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 111 + row * 18));
            }
        }

        // 4. Player Hotbar (9 columns, slots 46..54)
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 169));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            itemstack = stackInSlot.copy();

            // Total container slots: 0..18 (19 slots). Player slots: 19..54 (36 slots).
            if (index < TOTAL_CHEST_SLOTS) {
                // From chest (receiving or send slot) -> move to player inventory (19..55)
                if (!this.moveItemStackTo(stackInSlot, TOTAL_CHEST_SLOTS, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // From player inventory -> try send slot (18..19) first
                if (!this.moveItemStackTo(stackInSlot, SEND_SLOT, SEND_SLOT + 1, false)) {
                    // Then try receiving inventory (0..18)
                    if (!this.moveItemStackTo(stackInSlot, 0, RECEIVE_SLOTS, false)) {
                        // Between player inv and hotbar
                        if (index < TOTAL_CHEST_SLOTS + 27) {
                            if (!this.moveItemStackTo(stackInSlot, TOTAL_CHEST_SLOTS + 27, this.slots.size(), false)) {
                                return ItemStack.EMPTY;
                            }
                        } else if (!this.moveItemStackTo(stackInSlot, TOTAL_CHEST_SLOTS, TOTAL_CHEST_SLOTS + 27, false)) {
                            return ItemStack.EMPTY;
                        }
                    }
                }
            }

            if (stackInSlot.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (stackInSlot.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, stackInSlot);
        }

        return itemstack;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.container.stopOpen(player);
    }

    public Container getContainer() {
        return this.container;
    }
}
