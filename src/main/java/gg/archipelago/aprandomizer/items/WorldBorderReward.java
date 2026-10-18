package gg.archipelago.aprandomizer.items;

import com.mojang.serialization.MapCodec;
import gg.archipelago.aprandomizer.APRandomizer;
import gg.archipelago.aprandomizer.common.Utils.Utils;
import gg.archipelago.aprandomizer.data.WorldData;
import gg.archipelago.aprandomizer.managers.RestrictionManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

public record WorldBorderReward() implements APReward {
    public static final MapCodec<WorldBorderReward> MAP_CODEC = MapCodec.unit(WorldBorderReward::new);

    @Override
    public MapCodec<? extends APReward> codec() {
        return MAP_CODEC;
    }

    @Override
    public void give(MinecraftServer server) {
        WorldData worldData = APRandomizer.getWorldData();
        if (worldData != null) {
            worldData.incrementWorldBorderUpgrades();
        }
        RestrictionManager.applyWorldBorder(server);
        double size = RestrictionManager.getWorldBorderSize();
        Component sizeText = size >= 60000000.0
                ? Component.translatable("aprandomizer.reward.world_border.max")
                : Component.translatable("aprandomizer.reward.world_border.blocks", (int) size);
        int borderUpgrades = (worldData != null) ? worldData.getWorldBorderUpgrades() : 0;
        Utils.sendTitleToAll(Component.translatable("aprandomizer.reward.world_border.title"), Component.translatable("aprandomizer.reward.world_border.subtitle", sizeText), 10, 40, 10);
        Utils.sendMessageToAll(Component.translatable("aprandomizer.reward.world_border.chat", sizeText, borderUpgrades, 5));
    }
}
