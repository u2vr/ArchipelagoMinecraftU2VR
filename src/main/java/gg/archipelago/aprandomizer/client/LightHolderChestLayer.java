package gg.archipelago.aprandomizer.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import gg.archipelago.aprandomizer.managers.AbilityManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class LightHolderChestLayer extends RenderLayer<AvatarRenderState, PlayerModel> {

    private final ItemStackRenderState itemRenderState = new ItemStackRenderState();

    public LightHolderChestLayer(RenderLayerParent<AvatarRenderState, PlayerModel> renderer) {
        super(renderer);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, AvatarRenderState state, float yRot, float xRot) {
        Minecraft mc = Minecraft.getInstance();

        Player player = null;
        if (mc.player != null && (state.id == mc.player.getId() || state.id <= 0)) {
            player = mc.player;
        } else if (mc.level != null) {
            Entity entity = mc.level.getEntity(state.id);
            if (entity instanceof Player p) {
                player = p;
            }
        }
        if (player == null) {
            player = mc.player;
        }
        if (player == null) return;

        ItemStack lightItem = ItemStack.EMPTY;
        if (player == mc.player) {
            lightItem = ClientAbilityManager.getLightHolderItem();
        }
        if (lightItem.isEmpty()) {
            lightItem = AbilityManager.getLightHolderItem(player);
        }

        if (lightItem.isEmpty() || !AbilityManager.isLightEmittingBlock(lightItem)) return;

        poseStack.pushPose();

        // Attach to body
        this.getParentModel().root().translateAndRotate(poseStack);
        this.getParentModel().body.translateAndRotate(poseStack);

        // Position on chest (front of chest)
        // Offset forward so it stays visible over chestplate armor
        boolean hasChestArmor = !player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST).isEmpty();
        float zOffset = hasChestArmor ? -0.25F : -0.19F;
        poseStack.translate(0.0F, 0.28F, zOffset);
        poseStack.rotateDegrees(Axis.YP, 180.0F);
        poseStack.scale(0.35F, -0.35F, -0.35F);

        mc.getItemModelResolver().updateForLiving(this.itemRenderState, lightItem, ItemDisplayContext.FIXED, player);
        this.itemRenderState.submit(poseStack, submitNodeCollector, 0xF000F0, OverlayTexture.NO_OVERLAY, state.outlineColor);

        poseStack.popPose();
    }
}
