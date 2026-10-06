package gg.archipelago.aprandomizer.common.events;

import gg.archipelago.aprandomizer.APRandomizer;
import gg.archipelago.aprandomizer.APRegistries;
import gg.archipelago.aprandomizer.ap.storage.APMCData;
import gg.archipelago.aprandomizer.data.WorldData;
import gg.archipelago.aprandomizer.locations.APLocation;
import gg.archipelago.aprandomizer.locations.AdvancementLocation;
import gg.archipelago.aprandomizer.managers.advancementmanager.AdvancementManager;
import gg.archipelago.aprandomizer.common.Utils.Utils;
import net.minecraft.advancements.Advancement;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AdvancementEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

@EventBusSubscriber
public class OnAdvancement {
    // Directly reference a log4j logger.
    private static final Logger LOGGER = LogManager.getLogger();

    @SubscribeEvent
    static void onAdvancementEvent(AdvancementEvent.AdvancementProgressEvent event) {
        MinecraftServer server = APRandomizer.getServer();
        if (server == null) return;

        server.execute(() -> {
            for (String progress : event.getAdvancementProgress().getCompletedCriteria()) {
                for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                    p.getAdvancements().award(event.getAdvancement(), progress);
                }
            }
        });
    }

    @SubscribeEvent
    static void onAdvancementEvent(AdvancementEvent.AdvancementEarnEvent event) {
        MinecraftServer server = APRandomizer.getServer();
        if (server == null) return;

        // dont do any checking if the apmcdata file is not valid.
        if (APRandomizer.getApmcData().state != APMCData.State.VALID) return;

        ServerPlayer player = (ServerPlayer) event.getEntity();
        AdvancementManager am = APRandomizer.getAdvancementManager();
        if (am == null) return;
        WorldData worldData = APRandomizer.getWorldData();

        if (APRandomizer.getApmcData().isRandomBacap()) {
            handleRandomBacapAdvancement(server, player, event.getAdvancement(), am, worldData);
        } else if (APRandomizer.getApmcData().isBacapMilestones()) {
            if (worldData == null) return;
            handleBacapAdvancement(server, player, event.getAdvancement(), am, worldData, true);
        } else {
            handleVanillaAdvancement(server, player, event.getAdvancement(), am);
        }
    }

    private static void handleRandomBacapAdvancement(
            MinecraftServer server,
            ServerPlayer player,
            net.minecraft.advancements.AdvancementHolder holder,
            AdvancementManager am,
            WorldData worldData
    ) {
        Identifier id = holder.id();
        Advancement adv = holder.value();

        if (!am.isRandomBacapTarget(id)) return;

        long locationId = am.getRandomBacapLocationId(id);
        if (locationId == 0L || am.hasAdvancement(locationId)) return;

        LOGGER.info("{} earned Random BACAP Check {} (locationId: {})",
                player.getDisplayName().getString(), id, locationId);

        am.addAdvancement(locationId);

        net.minecraft.network.chat.Component advTitle = adv.display()
                .map(net.minecraft.advancements.DisplayInfo::title)
                .orElse(net.minecraft.network.chat.Component.literal(id.getPath()));

        server.getPlayerList().broadcastSystemMessage(
                net.minecraft.network.chat.Component.literal("§6[Archipelago] §aЧек выполнен: §e").append(advTitle),
                false
        );

        Utils.sendActionBarToAll(
                net.minecraft.network.chat.Component.literal("§6[Archipelago] §aЧек выполнен: §e").append(advTitle)
        );

        gg.archipelago.aprandomizer.managers.GoalManager gm = APRandomizer.getGoalManager();
        if (gm != null) {
            gm.updateGoal(true);
        }
    }

    private static void handleBacapAdvancement(
            MinecraftServer server,
            ServerPlayer player,
            net.minecraft.advancements.AdvancementHolder holder,
            AdvancementManager am,
            WorldData worldData,
            boolean notifyActionbar
    ) {
        Identifier id = holder.id();
        Advancement adv = holder.value();

        // 1. Must have a display (exclude technical trigger advancements)
        if (adv.display().isEmpty()) return;

        // 2. Exclude recipes
        if (id.getPath().startsWith("recipes/")) return;

        // 3. Must be blazeandcave or minecraft namespace
        String ns = id.getNamespace();
        if (!ns.equals("blazeandcave") && !ns.equals("minecraft")) return;

        // 4. Exclude the starter welcome root advancement
        if (ns.equals("blazeandcave") && id.getPath().equals("bacap/root")) return;

        // 5. Add to world data; if already had it, do nothing
        if (!worldData.addBacapAdvancement(id.toString())) return;

        int totalEarned = worldData.getEarnedBacapAdvancementsCount();
        LOGGER.info("{} earned BACAP advancement {}. Total BACAP earned: {}",
                player.getDisplayName().getString(), id, totalEarned);

        int step = APRandomizer.getApmcData().getBacapStep();
        int maxChecks = APRandomizer.getApmcData().getBacapCheckCount();
        int milestonesEarned = totalEarned / step;

        boolean reachedMilestone = false;
        for (int m = 1; m <= milestonesEarned && m <= maxChecks && m <= AdvancementManager.MAX_BACAP_MILESTONES; m++) {
            long locationId = AdvancementManager.getBacapMilestoneLocationID(m);
            if (!am.hasAdvancement(locationId)) {
                reachedMilestone = true;
                LOGGER.info("Unlocking BACAP Milestone {} (locationId: {}) at {} advancements", m, locationId, totalEarned);
                am.addAdvancement(locationId);
                server.getPlayerList().broadcastSystemMessage(
                        net.minecraft.network.chat.Component.literal("§6[Archipelago] §aBACAP Milestone " + m + " reached! (" + (m * step) + " advancements)"),
                        false
                );
                int nextM = m + 1;
                net.minecraft.network.chat.Component abMsg;
                if (nextM <= maxChecks && nextM <= AdvancementManager.MAX_BACAP_MILESTONES) {
                    abMsg = net.minecraft.network.chat.Component.literal("§6Майлстоун " + m + " достигнут! §a(" + (m * step) + " ачивок) §7| До след. майлстоуна: §e" + Utils.pluralizeAdvancements(step));
                } else {
                    abMsg = net.minecraft.network.chat.Component.literal("§6Майлстоун " + m + " достигнут! §aВсе майлстоуны выполнены! (" + (m * step) + " ачивок)");
                }
                Utils.sendActionBarToAll(abMsg);
            }
        }

        if (!reachedMilestone && notifyActionbar) {
            net.minecraft.network.chat.Component advTitle = adv.display().map(net.minecraft.advancements.DisplayInfo::title).orElse(net.minecraft.network.chat.Component.literal(id.getPath()));
            int nextM = milestonesEarned + 1;
            net.minecraft.network.chat.Component abMsg;
            if (nextM <= maxChecks && nextM <= AdvancementManager.MAX_BACAP_MILESTONES) {
                int remaining = step - (totalEarned % step);
                if (remaining == 0) remaining = step;
                abMsg = net.minecraft.network.chat.Component.literal("§a").append(advTitle).append(net.minecraft.network.chat.Component.literal(" §7| До след. майлстоуна: §e" + Utils.pluralizeAdvancements(remaining)));
            } else {
                abMsg = net.minecraft.network.chat.Component.literal("§a").append(advTitle).append(net.minecraft.network.chat.Component.literal(" §7| Все майлстоуны выполнены! (§f" + totalEarned + "§7)"));
            }
            Utils.sendActionBarToAll(abMsg);
        }

        gg.archipelago.aprandomizer.managers.GoalManager gm = APRandomizer.getGoalManager();
        if (gm != null) {
            gm.updateGoal(true);
        }
    }

    private static void handleVanillaAdvancement(
            MinecraftServer server,
            ServerPlayer player,
            net.minecraft.advancements.AdvancementHolder holder,
            AdvancementManager am
    ) {
        Advancement advancement = holder.value();
        Identifier id = holder.id();

        Registry<APLocation> locations = server.registryAccess().lookupOrThrow(APRegistries.ARCHIPELAGO_LOCATION);
        // don't do anything if this advancement has already been had, or is not on our list of tracked advancements.
        for (Map.Entry<ResourceKey<APLocation>, APLocation> entry : locations.entrySet()) {
            if (!(entry.getValue() instanceof AdvancementLocation(Identifier advKey) && advKey.equals(id) && !am.hasAdvancement(entry.getKey()) && am.getAdvancementID(entry.getKey()) != 0))
                continue;
            LOGGER.debug("{} has gotten the advancement {}", player.getDisplayName().getString(), id);
            am.addAdvancement(entry.getKey());
            am.syncAdvancement(entry.getKey(), entry.getValue());
            advancement.display().ifPresent(it -> server.getPlayerList().broadcastSystemMessage(
                    advancement.display().get().type().createAnnouncement(holder, player),
                    false
            ));
        }
    }

    @SubscribeEvent
    static void onPlayerLoggedIn(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) {
        if (!APRandomizer.getApmcData().isBacapAdvancements()) return;
        if (event.getEntity() instanceof ServerPlayer player) {
            MinecraftServer server = player.level().getServer();
            if (server == null) return;
            AdvancementManager am = APRandomizer.getAdvancementManager();
            WorldData worldData = APRandomizer.getWorldData();
            if (am == null) return;

            if (APRandomizer.getApmcData().isRandomBacap()) {
                for (net.minecraft.advancements.AdvancementHolder holder : server.getAdvancements().getAllAdvancements()) {
                    if (player.getAdvancements().getOrStartProgress(holder).isDone()) {
                        handleRandomBacapAdvancement(server, player, holder, am, worldData);
                    }
                }
                return;
            }

            if (worldData == null) return;
            int prevEarned = worldData.getEarnedBacapAdvancementsCount();
            for (net.minecraft.advancements.AdvancementHolder holder : server.getAdvancements().getAllAdvancements()) {
                if (player.getAdvancements().getOrStartProgress(holder).isDone()) {
                    handleBacapAdvancement(server, player, holder, am, worldData, false);
                }
            }
            int newEarned = worldData.getEarnedBacapAdvancementsCount();
            if (newEarned > prevEarned) {
                int step = APRandomizer.getApmcData().getBacapStep();
                int maxChecks = APRandomizer.getApmcData().getBacapCheckCount();
                int milestones = newEarned / step;
                if (milestones < maxChecks && milestones < AdvancementManager.MAX_BACAP_MILESTONES) {
                    int rem = step - (newEarned % step);
                    if (rem == 0) rem = step;
                    Utils.sendActionBarToPlayer(player, net.minecraft.network.chat.Component.literal("§6[BACAP] §fСинхронизировано ачивок: §a" + newEarned + " §7| До след. майлстоуна: §e" + Utils.pluralizeAdvancements(rem)));
                } else {
                    Utils.sendActionBarToPlayer(player, net.minecraft.network.chat.Component.literal("§6[BACAP] §fВсе майлстоуны выполнены! (§a" + newEarned + " §fачивок)"));
                }
            }
        }
    }
}
