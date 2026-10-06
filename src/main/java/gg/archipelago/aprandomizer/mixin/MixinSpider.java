package gg.archipelago.aprandomizer.mixin;

import gg.archipelago.aprandomizer.managers.RestrictionManager;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Spider.class)
public abstract class MixinSpider extends Monster {

    protected MixinSpider(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public float getLightLevelDependentMagicValue() {
        if (!this.level().isClientSide() && RestrictionManager.isIgnoreLightSpawningEnabled()) {
            return 0.0F;
        }
        return super.getLightLevelDependentMagicValue();
    }
}
