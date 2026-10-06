package gg.archipelago.aprandomizer.common.events;

import gg.archipelago.aprandomizer.managers.trademanager.ArchipelagoTraderSpawner;
import net.minecraft.world.entity.npc.CatSpawner;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTraderSpawner;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ModifyCustomSpawnersEvent;

@EventBusSubscriber
public class OnModifyCustomSpawners {
    @SubscribeEvent
    public static void onModifyCustomSpawners(ModifyCustomSpawnersEvent event) {
        if (event.getLevel().dimension() != Level.OVERWORLD && event.getCustomSpawners().stream().noneMatch(spawner -> spawner instanceof CatSpawner)) {
            event.addCustomSpawner(new CatSpawner());
        }

        // Replace vanilla WanderingTraderSpawner with accelerated ArchipelagoTraderSpawner in Overworld and Nether
        if (event.getLevel().dimension() == Level.OVERWORLD || event.getLevel().dimension() == Level.NETHER) {
            event.getCustomSpawners().removeIf(spawner -> spawner instanceof WanderingTraderSpawner || spawner instanceof ArchipelagoTraderSpawner);
            event.addCustomSpawner(new ArchipelagoTraderSpawner());
        }
    }
}
