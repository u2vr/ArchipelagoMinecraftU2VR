package gg.archipelago.aprandomizer.network;

import io.netty.buffer.ByteBuf;
import gg.archipelago.aprandomizer.APRandomizer;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.List;

public record SyncRecipeStatePayload(
        int tickets,
        int tierThreshold,
        boolean ticketMode,
        List<String> unlockedNodeIds
) implements CustomPacketPayload {
    public static final Type<SyncRecipeStatePayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(APRandomizer.MODID, "sync_recipe_state"));
    public static final StreamCodec<ByteBuf, SyncRecipeStatePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            SyncRecipeStatePayload::tickets,
            ByteBufCodecs.VAR_INT,
            SyncRecipeStatePayload::tierThreshold,
            ByteBufCodecs.BOOL,
            SyncRecipeStatePayload::ticketMode,
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()),
            SyncRecipeStatePayload::unlockedNodeIds,
            SyncRecipeStatePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
