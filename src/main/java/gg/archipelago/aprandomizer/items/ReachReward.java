package gg.archipelago.aprandomizer.items;

import com.mojang.serialization.MapCodec;
import gg.archipelago.aprandomizer.APRandomizer;
import gg.archipelago.aprandomizer.common.Utils.Utils;
import gg.archipelago.aprandomizer.data.WorldData;
import gg.archipelago.aprandomizer.managers.RestrictionManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public record ReachReward() implements APReward {
    public static final MapCodec<ReachReward> MAP_CODEC = MapCodec.unit(ReachReward::new);

    @Override
    public MapCodec<? extends APReward> codec() {
        return MAP_CODEC;
    }

    @Override
    public void give(MinecraftServer server) {
        WorldData worldData = APRandomizer.getWorldData();
        if (worldData != null) {
            worldData.incrementReachUpgrades();
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            RestrictionManager.applyReach(player);
        }
        double reach = RestrictionManager.getBlockReach();
        int reachUpgrades = (worldData != null) ? worldData.getReachUpgrades() : 0;
        Utils.sendTitleToAll(Component.translatable("aprandomizer.reward.reach.title"), Component.translatable("aprandomizer.reward.reach.subtitle", (int) reach), 10, 40, 10);
        Utils.sendMessageToAll(Component.translatable("aprandomizer.reward.reach.chat", (int) reach, reachUpgrades, 3));
    }
}
