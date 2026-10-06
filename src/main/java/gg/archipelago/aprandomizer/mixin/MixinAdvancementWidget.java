package gg.archipelago.aprandomizer.mixin;

import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import gg.archipelago.aprandomizer.client.ClientRecipeManager;
import gg.archipelago.aprandomizer.managers.advancementmanager.AdvancementManager;
import gg.archipelago.aprandomizer.managers.recipemanager.RecipeTreeManager;
import gg.archipelago.aprandomizer.managers.recipemanager.RecipeTreeNode;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.advancements.AdvancementWidget;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AdvancementWidget.class)
public abstract class MixinAdvancementWidget {

    @Shadow @Final private AdvancementNode advancementNode;
    @Shadow @Final private DisplayInfo display;
    @Shadow @Final private int x;
    @Shadow @Final private int y;
    @Shadow private AdvancementProgress progress;

    @Redirect(
            method = "extractRenderState",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V"
            )
    )
    private void aprandomizer$blitSpriteWithTierTint(GuiGraphicsExtractor graphics, RenderPipeline pipeline, Identifier sprite, int x, int y, int width, int height) {
        int tier = RecipeTreeManager.getAdvancementTier(this.advancementNode.holder().id());
        int color = RecipeTreeManager.getTierColor(tier);
        if (color == 0xFFFFFFFF) {
            graphics.blitSprite(pipeline, sprite, x, y, width, height);
        } else {
            graphics.blitSprite(pipeline, sprite, x, y, width, height, color);
        }
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void aprandomizer$drawAdvancementExtras(GuiGraphicsExtractor graphics, int x, int y, CallbackInfo ci) {
        if (this.display.hidden() && (this.progress == null || !this.progress.isDone())) {
            return;
        }

        Identifier advId = this.advancementNode.holder().id();

        // 1. Draw 2px recipe tier stripe and emerald frame for unlocked recipe advancements
        int tier = RecipeTreeManager.getAdvancementTier(advId);
        RecipeTreeNode recipeNode = RecipeTreeManager.getNodeByAdvancement(advId);
        boolean isRecipe = (tier > 0 || recipeNode != null);

        if (isRecipe) {
            int stripeTier = (tier > 0) ? tier : (recipeNode != null ? recipeNode.tier() : 1);
            int color = RecipeTreeManager.getTierStripeColor(stripeTier);
            int iconX = x + this.x + 8;
            int iconY = y + this.y + 5;
            graphics.fill(iconX, iconY + 14, iconX + 16, iconY + 16, color);

            boolean isDone = (this.progress != null && this.progress.isDone())
                    || (recipeNode != null && ClientRecipeManager.isNodeUnlocked(recipeNode));

            if (isDone) {
                int frameX = x + this.x + 3;
                int frameY = y + this.y;
                int frameW = 26;
                int frameH = 26;
                int emeraldOutline = 0xFF00E676;

                // Subtle emerald background tint behind icon
                graphics.fill(frameX + 1, frameY + 1, frameX + frameW - 1, frameY + frameH - 1, 0x4400E676);

                // 2px outer emerald border
                graphics.fill(frameX - 1, frameY - 1, frameX + frameW + 1, frameY + 1, emeraldOutline); // Top
                graphics.fill(frameX - 1, frameY + frameH - 1, frameX + frameW + 1, frameY + frameH + 1, emeraldOutline); // Bottom
                graphics.fill(frameX - 1, frameY + 1, frameX + 1, frameY + frameH - 1, emeraldOutline); // Left
                graphics.fill(frameX + frameW - 1, frameY + 1, frameX + frameW + 1, frameY + frameH - 1, emeraldOutline); // Right

                // Outer subtle glow
                graphics.fill(frameX - 2, frameY - 2, frameX + frameW + 2, frameY - 1, 0x6600E676);
                graphics.fill(frameX - 2, frameY + frameH + 1, frameX + frameW + 2, frameY + frameH + 2, 0x6600E676);
                graphics.fill(frameX - 2, frameY - 1, frameX - 1, frameY + frameH + 1, 0x6600E676);
                graphics.fill(frameX + frameW + 1, frameY - 1, frameX + frameW + 2, frameY + frameH + 1, 0x6600E676);

                // Emerald check badge in top-right corner
                int badgeX = frameX + frameW - 4;
                int badgeY = frameY - 3;
                graphics.fill(badgeX - 1, badgeY - 1, badgeX + 7, badgeY + 7, 0xFF000000);
                graphics.fill(badgeX, badgeY, badgeX + 6, badgeY + 6, 0xFF00E676);
                graphics.fill(badgeX + 2, badgeY + 2, badgeX + 4, badgeY + 4, 0xFFFFFFFF);
            }
        }

        // 2. Draw Archipelago Check Target Highlight
        if (AdvancementManager.isAdvancementCheckTarget(advId)) {
            boolean isDone = (this.progress != null && this.progress.isDone()) || AdvancementManager.isAdvancementCheckCompleted(advId);
            int frameX = x + this.x + 3;
            int frameY = y + this.y;
            int frameW = 26;
            int frameH = 26;

            if (!isDone) {
                // Active check target: pulsing gold border + gold star badge
                long time = System.currentTimeMillis();
                float pulse = (float) (Math.sin(time / 220.0) * 0.35 + 0.65);
                int alpha = (int) (pulse * 255);
                int goldOutline = (alpha << 24) | 0xFFD700;
                int goldGlow = ((int) (alpha * 0.30f) << 24) | 0xFFD700;

                // Subtle glow behind frame
                graphics.fill(frameX - 1, frameY - 1, frameX + frameW + 1, frameY + frameH + 1, goldGlow);

                // 2px outer gold border
                graphics.fill(frameX - 2, frameY - 2, frameX + frameW + 2, frameY, goldOutline); // Top
                graphics.fill(frameX - 2, frameY + frameH, frameX + frameW + 2, frameY + frameH + 2, goldOutline); // Bottom
                graphics.fill(frameX - 2, frameY, frameX, frameY + frameH, goldOutline); // Left
                graphics.fill(frameX + frameW, frameY, frameX + frameW + 2, frameY + frameH, goldOutline); // Right

                // Gold target badge in top-right corner
                int badgeX = frameX + frameW - 4;
                int badgeY = frameY - 3;
                graphics.fill(badgeX - 1, badgeY - 1, badgeX + 7, badgeY + 7, 0xFF000000);
                graphics.fill(badgeX, badgeY, badgeX + 6, badgeY + 6, 0xFFFFD700);
                graphics.fill(badgeX + 1, badgeY + 1, badgeX + 5, badgeY + 5, 0xFFFFFFFF);
            } else {
                // Completed check target: emerald border + green completed badge
                int greenOutline = 0xFF4CAF50;
                graphics.fill(frameX - 1, frameY - 1, frameX + frameW + 1, frameY, greenOutline); // Top
                graphics.fill(frameX - 1, frameY + frameH, frameX + frameW + 1, frameY + frameH + 1, greenOutline); // Bottom
                graphics.fill(frameX - 1, frameY, frameX, frameY + frameH, greenOutline); // Left
                graphics.fill(frameX + frameW, frameY, frameX + frameW + 1, frameY + frameH, greenOutline); // Right

                // Green check badge in top-right corner
                int badgeX = frameX + frameW - 4;
                int badgeY = frameY - 3;
                graphics.fill(badgeX - 1, badgeY - 1, badgeX + 7, badgeY + 7, 0xFF000000);
                graphics.fill(badgeX, badgeY, badgeX + 6, badgeY + 6, 0xFF4CAF50);
                graphics.fill(badgeX + 2, badgeY + 2, badgeX + 4, badgeY + 4, 0xFFE8F5E9);
            }
        }
    }
}
