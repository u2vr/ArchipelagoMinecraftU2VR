package gg.archipelago.aprandomizer.items;

import com.mojang.serialization.MapCodec;
import gg.archipelago.aprandomizer.APRandomizer;
import gg.archipelago.aprandomizer.common.Utils.Utils;
import gg.archipelago.aprandomizer.data.WorldData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

public record OffhandReward() implements APReward {
    public static final MapCodec<OffhandReward> MAP_CODEC = MapCodec.unit(OffhandReward::new);

    @Override
    public MapCodec<? extends APReward> codec() {
        return MAP_CODEC;
    }

    @Override
    public void give(MinecraftServer server) {
        WorldData worldData = APRandomizer.getWorldData();
        if (worldData != null) {
            worldData.setOffhandUnlocked(true);
        }
        Utils.sendTitleToAll(Component.literal("§eВторая рука разблокирована!"), Component.literal("Теперь доступен слот второй руки!"), 10, 50, 10);
        Utils.sendMessageToAll("§e[Archipelago] Слот второй руки (Offhand) теперь разблокирован!");
    }
}
