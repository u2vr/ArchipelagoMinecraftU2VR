package gg.archipelago.aprandomizer.items;

import com.mojang.serialization.MapCodec;
import gg.archipelago.aprandomizer.APRandomizer;
import gg.archipelago.aprandomizer.common.Utils.Utils;
import gg.archipelago.aprandomizer.data.WorldData;
import gg.archipelago.aprandomizer.managers.RestrictionManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

public record InventorySlotReward() implements APReward {
    public static final MapCodec<InventorySlotReward> MAP_CODEC = MapCodec.unit(InventorySlotReward::new);

    @Override
    public MapCodec<? extends APReward> codec() {
        return MAP_CODEC;
    }

    @Override
    public void give(MinecraftServer server) {
        WorldData worldData = APRandomizer.getWorldData();
        if (worldData != null) {
            worldData.incrementInventorySlotUpgrades();
        }
        int allowed = RestrictionManager.getAllowedSlots();
        Utils.sendTitleToAll(Component.translatable("aprandomizer.reward.inventory_slot.title"), Component.translatable("aprandomizer.reward.inventory_slot.subtitle", allowed, 36), 10, 50, 10);
        Utils.sendMessageToAll(Component.translatable("aprandomizer.reward.inventory_slot.chat", allowed, 36));
    }
}
