package gg.archipelago.aprandomizer.client;

import gg.archipelago.aprandomizer.APRandomizer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = APRandomizer.MODID, value = Dist.CLIENT)
public class ClientItemTooltipHandler {

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        if (ClientRecipeManager.isItemLocked(event.getItemStack())) {
            event.getToolTip().add(Component.literal("§c🔒 Рецепт заблокирован в Архипелаге").withStyle(ChatFormatting.RED));
        }
    }
}
