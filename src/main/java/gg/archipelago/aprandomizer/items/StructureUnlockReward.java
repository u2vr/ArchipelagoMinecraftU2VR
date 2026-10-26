package gg.archipelago.aprandomizer.items;

import com.mojang.serialization.MapCodec;
import gg.archipelago.aprandomizer.APRandomizer;
import gg.archipelago.aprandomizer.common.Utils.Utils;
import gg.archipelago.aprandomizer.data.WorldData;
import gg.archipelago.aprandomizer.managers.RestrictionManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public record StructureUnlockReward() implements APReward {
    public static final MapCodec<StructureUnlockReward> MAP_CODEC = MapCodec.unit(StructureUnlockReward::new);

    @Override
    public MapCodec<? extends APReward> codec() {
        return MAP_CODEC;
    }

    @Override
    public void give(MinecraftServer server) {
        RestrictionManager.StructureEntry unlocked = RestrictionManager.unlockNextStructure();
        if (unlocked != null) {
            Utils.sendTitleToAll(Component.translatable("aprandomizer.reward.structure.title"), unlocked.displayNameComponent(), 10, 50, 10);
            Utils.sendMessageToAll(Component.translatable("aprandomizer.reward.structure.chat", unlocked.displayNameComponent()));
            if (server != null) {
                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                    gg.archipelago.aprandomizer.advancements.APCriteriaTriggers.STRUCTURE_UNLOCK.get().trigger(player, unlocked.id());
                }
            }
        } else {
            Utils.sendMessageToAll(Component.translatable("aprandomizer.reward.structure.all_unlocked"));
        }
    }
}
