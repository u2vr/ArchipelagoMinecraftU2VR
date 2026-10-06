package gg.archipelago.aprandomizer.network;

import io.netty.buffer.ByteBuf;
import gg.archipelago.aprandomizer.APRandomizer;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record UnlockRecipeNodePayload(String nodeId) implements CustomPacketPayload {
    public static final Type<UnlockRecipeNodePayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(APRandomizer.MODID, "unlock_recipe_node"));
    public static final StreamCodec<ByteBuf, UnlockRecipeNodePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            UnlockRecipeNodePayload::nodeId,
            UnlockRecipeNodePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
