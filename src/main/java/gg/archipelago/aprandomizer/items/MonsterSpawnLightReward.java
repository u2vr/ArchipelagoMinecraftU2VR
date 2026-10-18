package gg.archipelago.aprandomizer.items;

import com.mojang.serialization.MapCodec;
import gg.archipelago.aprandomizer.APRandomizer;
import gg.archipelago.aprandomizer.common.Utils.Utils;
import gg.archipelago.aprandomizer.data.WorldData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

public record MonsterSpawnLightReward() implements APReward {
    public static final MapCodec<MonsterSpawnLightReward> MAP_CODEC = MapCodec.unit(MonsterSpawnLightReward::new);

    @Override
    public MapCodec<? extends APReward> codec() {
        return MAP_CODEC;
    }

    @Override
    public void give(MinecraftServer server) {
        WorldData worldData = APRandomizer.getWorldData();
        if (worldData != null) {
            worldData.setIgnoreLightSpawningEnabled(true);
        }
        Utils.sendTitleToAll(Component.translatable("aprandomizer.curse.monster_light.title"), Component.translatable("aprandomizer.curse.monster_light.subtitle"), 10, 50, 10);
        Utils.sendMessageToAll(Component.translatable("aprandomizer.curse.monster_light.chat"));
    }
}
