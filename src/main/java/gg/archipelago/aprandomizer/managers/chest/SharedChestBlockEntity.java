package gg.archipelago.aprandomizer.managers.chest;

import gg.archipelago.aprandomizer.APBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class SharedChestBlockEntity extends BlockEntity implements MenuProvider {

    public SharedChestBlockEntity(BlockPos pos, BlockState state) {
        super(APBlocks.SHARED_CHEST_BLOCK_ENTITY.get(), pos, state);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.aprandomizer.shared_chest");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        if (!SharedChestManager.isPlayerLockHolder(player)) {
            return null;
        }
        return new SharedChestMenu(containerId, playerInventory, SharedChestManager.getContainer());
    }
}
