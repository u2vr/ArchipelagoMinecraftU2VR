package gg.archipelago.aprandomizer.items;

import com.mojang.serialization.MapCodec;
import gg.archipelago.aprandomizer.APRandomizer;
import gg.archipelago.aprandomizer.common.Utils.Utils;
import gg.archipelago.aprandomizer.data.WorldData;
import gg.archipelago.aprandomizer.managers.RestrictionManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

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
            Utils.sendTitleToAll(Component.literal("§eСтруктура разблокирована!"), Component.literal(unlocked.displayName()), 10, 50, 10);
            Utils.sendMessageToAll("§e[Archipelago] Разблокирован доступ к структуре: " + unlocked.displayName());
        } else {
            Utils.sendMessageToAll("§e[Archipelago] Все известные структуры уже разблокированы!");
        }
    }
}
