package gg.archipelago.aprandomizer.mixin;

import gg.archipelago.aprandomizer.exploration.ExplorationManager;
import it.unimi.dsi.fastutil.Stack;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.resources.Identifier;
import net.minecraft.server.advancements.AdvancementVisibilityEvaluator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Predicate;

@Mixin(AdvancementVisibilityEvaluator.class)
public abstract class MixinAdvancementVisibilityEvaluator {

    @Inject(
            method = "evaluateVisibility(Lnet/minecraft/advancements/AdvancementNode;Lit/unimi/dsi/fastutil/Stack;Ljava/util/function/Predicate;Lnet/minecraft/server/advancements/AdvancementVisibilityEvaluator$Output;)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void aprandomizer$filterExplorationAdvancements(
            AdvancementNode node,
            Stack<?> ascendants,
            Predicate<AdvancementNode> isDoneTest,
            AdvancementVisibilityEvaluator.Output output,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (node != null && node.holder() != null) {
            Identifier id = node.holder().id();
            if (!ExplorationManager.isExplorationAdvancementEnabled(id)) {
                output.accept(node, false);
                cir.setReturnValue(false);
            }
        }
    }
}
