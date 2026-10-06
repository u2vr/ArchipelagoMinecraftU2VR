package gg.archipelago.aprandomizer.items;

import com.mojang.serialization.MapCodec;
import gg.archipelago.aprandomizer.APRandomizer;
import gg.archipelago.aprandomizer.common.Utils.Utils;
import gg.archipelago.aprandomizer.data.WorldData;
import gg.archipelago.aprandomizer.managers.RestrictionManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public record HpReward() implements APReward {
    public static final MapCodec<HpReward> MAP_CODEC = MapCodec.unit(HpReward::new);

    @Override
    public MapCodec<? extends APReward> codec() {
        return MAP_CODEC;
    }

    @Override
    public void give(MinecraftServer server) {
        WorldData worldData = APRandomizer.getWorldData();
        if (worldData != null) {
            worldData.incrementHpUpgrades();
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            RestrictionManager.applyHealth(player);
            player.heal(2.0f);
        }
        int hp = worldData.getHpUpgrades();
        int hearts = 3 + hp;
        Utils.sendTitleToAll(Component.literal("§c+1 Сердце (HP)"), Component.literal("Максимальное здоровье: " + hearts + " сердец (" + hp + "/7)"), 10, 40, 10);
        Utils.sendMessageToAll("§a[Archipelago] §fРазблокировано улучшение здоровья: §c+1 Сердце §7(Всего: " + hearts + " сердец, " + hp + "/7)");
    }
}
