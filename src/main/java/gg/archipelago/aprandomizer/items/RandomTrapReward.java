package gg.archipelago.aprandomizer.items;

import com.mojang.serialization.MapCodec;
import gg.archipelago.aprandomizer.common.Utils.Utils;
import gg.archipelago.aprandomizer.managers.RestrictionManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public record RandomTrapReward() implements APReward {
    public static final MapCodec<RandomTrapReward> MAP_CODEC = MapCodec.unit(RandomTrapReward::new);

    @Override
    public MapCodec<? extends APReward> codec() {
        return MAP_CODEC;
    }

    @Override
    public void give(ServerPlayer player) {
        RestrictionManager.triggerRandomTrap(player.level().getServer(), player);
    }

    @Override
    public void give(MinecraftServer server) {
        // Handled per-player via give(ServerPlayer) to prevent double execution
    }
}
