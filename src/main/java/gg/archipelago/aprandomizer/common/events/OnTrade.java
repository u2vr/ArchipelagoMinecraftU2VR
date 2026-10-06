package gg.archipelago.aprandomizer.common.events;

import gg.archipelago.aprandomizer.managers.trademanager.WanderingTraderManager;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.TradeWithVillagerEvent;

@EventBusSubscriber
public class OnTrade {

    @SubscribeEvent
    public static void onTradeWithVillager(TradeWithVillagerEvent event) {
        if (event.getEntity() == null || event.getEntity().level().isClientSide()) {
            return;
        }

        if (event.getAbstractVillager() instanceof WanderingTrader) {
            WanderingTraderManager.handleTrade(event.getMerchantOffer(), event.getEntity());
        }
    }
}
