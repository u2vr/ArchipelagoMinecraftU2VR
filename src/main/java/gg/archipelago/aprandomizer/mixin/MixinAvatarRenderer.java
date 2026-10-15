package gg.archipelago.aprandomizer.mixin;

import gg.archipelago.aprandomizer.client.LightHolderChestLayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AvatarRenderer.class)
public abstract class MixinAvatarRenderer {

    @Inject(method = "<init>", at = @At("TAIL"))
    private void aprandomizer$addLightHolderLayer(EntityRendererProvider.Context context, boolean slimSteve, CallbackInfo ci) {
        AvatarRenderer<?> renderer = (AvatarRenderer<?>) (Object) this;
        renderer.addLayer(new LightHolderChestLayer(renderer));
    }
}
