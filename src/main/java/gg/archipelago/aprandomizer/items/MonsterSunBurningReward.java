package gg.archipelago.aprandomizer.items;

import com.mojang.serialization.MapCodec;
import gg.archipelago.aprandomizer.APRandomizer;
import gg.archipelago.aprandomizer.common.Utils.Utils;
import gg.archipelago.aprandomizer.data.WorldData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

public record MonsterSunBurningReward() implements APReward {
    public static final MapCodec<MonsterSunBurningReward> MAP_CODEC = MapCodec.unit(MonsterSunBurningReward::new);

    @Override
    public MapCodec<? extends APReward> codec() {
        return MAP_CODEC;
    }

    @Override
    public void give(MinecraftServer server) {
        WorldData worldData = APRandomizer.getWorldData();
        if (worldData != null) {
            worldData.setMonsterSunBurningDisabled(true);
        }
        Utils.sendTitleToAll(Component.literal("§4Затмение"), Component.literal("Монстры больше не сгорают на солнце!"), 10, 50, 10);
        Utils.sendMessageToAll("§4[Наказание] Постоянное проклятие: Монстры больше не горят под лучами солнца!");
    }
}
