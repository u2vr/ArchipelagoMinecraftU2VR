package gg.archipelago.aprandomizer.common.events;

import gg.archipelago.aprandomizer.APBlocks;
import gg.archipelago.aprandomizer.APRandomizer;
import gg.archipelago.aprandomizer.ap.storage.APMCData;
import gg.archipelago.aprandomizer.common.Utils.Utils;
import gg.archipelago.aprandomizer.data.WorldData;
import gg.archipelago.aprandomizer.managers.GoalManager;
import gg.archipelago.aprandomizer.managers.RestrictionManager;
import gg.archipelago.aprandomizer.managers.advancementmanager.AdvancementManager;
import gg.archipelago.aprandomizer.managers.itemmanager.ItemManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber
public class OnJoin {
    @SubscribeEvent
    static void onPlayerLoginEvent(PlayerEvent.PlayerLoggedInEvent event) {
        ServerPlayer player = (ServerPlayer) event.getEntity();
        GoalManager goalManager = APRandomizer.getGoalManager();
        if (goalManager == null){
            Utils.sendMessageToAll("Goal Manager did not initialize");
            return;
        }
        ItemManager itemManager = APRandomizer.getItemManager();
        if (itemManager == null){
            Utils.sendMessageToAll("Item Manager did not initialize");
            return;
        }
        AdvancementManager advancementManager = APRandomizer.getAdvancementManager();
        if (advancementManager == null){
            Utils.sendMessageToAll("Advancement Manager did not initialize");
            return;
        }

        if (APRandomizer.isRace())
            player.setGameMode(GameType.SURVIVAL);

        APMCData data = APRandomizer.getApmcData();
        if (data.state == APMCData.State.MISSING) {
            Utils.sendMessageToAll("No APMC file found, please only start the server via the APMC file.");
            return;
        }
        else if (data.state == APMCData.State.INVALID_VERSION) {
            Utils.sendMessageToAll("This Seed was generated using an incompatible randomizer version.");
            return;
        }
        else if (data.state == APMCData.State.INVALID_SEED) {
            Utils.sendMessageToAll("Supplied APMC file does not match world loaded. something went very wrong here.");
            return;
        }
        advancementManager.syncAllAdvancements();
        goalManager.updateInfoBar();
        itemManager.catchUpPlayer(player);

        if (APRandomizer.getApmcData().isBacapMilestones()) {
            WorldData wd = APRandomizer.getWorldData();
            if (wd != null) {
                int total = wd.getEarnedBacapAdvancementsCount();
                int step = APRandomizer.getApmcData().getBacapStep();
                int maxChecks = APRandomizer.getApmcData().getBacapCheckCount();
                int milestones = total / step;
                if (milestones < maxChecks && milestones < AdvancementManager.MAX_BACAP_MILESTONES) {
                    int rem = step - (total % step);
                    if (rem == 0) rem = step;
                    Utils.sendActionBarToPlayer(player, net.minecraft.network.chat.Component.literal("§6[BACAP] §fАчивок: §a" + total + " §7| До след. майлстоуна: §e" + Utils.pluralizeAdvancements(rem)));
                } else {
                    Utils.sendActionBarToPlayer(player, net.minecraft.network.chat.Component.literal("§6[BACAP] §fВсе майлстоуны выполнены! (§a" + total + " §fачивок)"));
                }
            }
        }

        player.awardRecipesByKey(java.util.List.of(
                net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE, net.minecraft.resources.Identifier.fromNamespaceAndPath(APRandomizer.MODID, "shared_chest")),
                net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE, net.minecraft.resources.Identifier.fromNamespaceAndPath(APRandomizer.MODID, "shared_chest_from_ender_chest"))
        ));

        if (APRandomizer.isJailPlayers()) {
            BlockPos jail = APRandomizer.getJailPosition();
            player.teleportTo(jail.getX(), jail.getY(), jail.getZ());
            player.setGameMode(GameType.SURVIVAL);
        } else if (RestrictionManager.isStartingSharedChestEnabled()) {
            if (player.getStats().getValue(Stats.CUSTOM.get(Stats.PLAY_TIME)) <= 1) {
                Utils.giveItemToPlayer(player, new ItemStack(APBlocks.SHARED_CHEST_ITEM.get()));
            }
        }
    }
}
