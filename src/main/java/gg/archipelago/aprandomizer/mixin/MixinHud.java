package gg.archipelago.aprandomizer.mixin;

import gg.archipelago.aprandomizer.managers.RestrictionManager;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(Hud.class)
public abstract class MixinHud {

    @ModifyConstant(method = "extractFood", constant = @Constant(intValue = 10))
    private int aprandomizer$modifyMaxFoodIcons(int original) {
        if (!RestrictionManager.isHungerRestrictionEnabled()) {
            return original;
        }
        return Math.min(10, Math.max(1, (RestrictionManager.getMaxFoodLevel() + 1) / 2));
    }
}
