package gg.archipelago.aprandomizer.items;

import com.mojang.serialization.MapCodec;
import gg.archipelago.aprandomizer.APRandomizer;
import gg.archipelago.aprandomizer.common.Utils.Utils;
import gg.archipelago.aprandomizer.data.WorldData;
import gg.archipelago.aprandomizer.managers.RestrictionManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

public record HungerReward() implements APReward {
    public static final MapCodec<HungerReward> MAP_CODEC = MapCodec.unit(HungerReward::new);

    @Override
    public MapCodec<? extends APReward> codec() {
        return MAP_CODEC;
    }

    @Override
    public void give(MinecraftServer server) {
        WorldData worldData = APRandomizer.getWorldData();
        if (worldData != null) {
            worldData.incrementHungerUpgrades();
        }
        int maxFood = RestrictionManager.getMaxFoodLevel();
        int hunger = worldData.getHungerUpgrades();
        Utils.sendTitleToAll(Component.literal("§6+Уровень Сытости"), Component.literal("Макс. уровень голода: " + (maxFood / 2) + " окорочков! (" + hunger + "/6)"), 10, 40, 10);
        Utils.sendMessageToAll("§a[Archipelago] §fРазблокировано улучшение сытости: §6+1 Окорочок §7(Всего: " + (maxFood / 2) + " окорочков, " + hunger + "/6)");
    }
}
