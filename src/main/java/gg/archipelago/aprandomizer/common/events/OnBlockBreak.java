package gg.archipelago.aprandomizer.common.events;

import gg.archipelago.aprandomizer.APRandomizer;
import gg.archipelago.aprandomizer.managers.RestrictionManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

@EventBusSubscriber
public class OnBlockBreak {
    @SubscribeEvent
    static void onPlayerBlockInteract(BreakBlockEvent event) {
        if (APRandomizer.isJailPlayers()) {
            event.setCanceled(true);
            if (event.getPlayer() instanceof ServerPlayer player)
                player.sendSystemMessage(Component.literal("No!"), true);
            return;
        }

        if (event.getPlayer() != null && !event.getPlayer().isSpectator()
                && RestrictionManager.isItemLocked(event.getPlayer().getMainHandItem())) {
            event.setCanceled(true);
            if (event.getPlayer() instanceof ServerPlayer player) {
                RestrictionManager.warnItemLocked(player, player.getMainHandItem());
            }
        }
    }
}
