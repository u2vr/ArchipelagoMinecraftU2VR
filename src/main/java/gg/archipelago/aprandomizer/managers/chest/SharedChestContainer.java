package gg.archipelago.aprandomizer.managers.chest;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public class SharedChestContainer extends SimpleContainer {
    public static final int CONTAINER_SIZE = 1;

    public SharedChestContainer() {
        super(CONTAINER_SIZE);
    }

    @Override
    public void setChanged() {
        super.setChanged();
        SharedChestManager.onContainerChanged();
    }

    @Override
    public void stopOpen(ContainerUser user) {
        super.stopOpen(user);
        LivingEntity entity = user.getLivingEntity();
        if (entity != null && entity.level() != null && !entity.level().isClientSide()) {
            entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                    SoundEvents.ENDER_CHEST_CLOSE, SoundSource.BLOCKS, 0.5F,
                    entity.level().getRandom().nextFloat() * 0.1F + 0.9F);
        }
        SharedChestManager.onPlayerClosed(user);
    }

    @Override
    public boolean stillValid(Player player) {
        if (player == null) {
            return true;
        }
        return SharedChestManager.isPlayerLockHolder(player);
    }
}
