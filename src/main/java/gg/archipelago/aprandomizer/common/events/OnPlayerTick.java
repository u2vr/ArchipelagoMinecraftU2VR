package gg.archipelago.aprandomizer.common.events;

import gg.archipelago.aprandomizer.attachments.APAttachmentTypes;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber
public class OnPlayerTick {

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        // Biome tracking is handled centrally by ExplorationManager
    }

}
