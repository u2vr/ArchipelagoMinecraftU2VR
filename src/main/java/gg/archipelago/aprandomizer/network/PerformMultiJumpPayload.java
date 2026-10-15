package gg.archipelago.aprandomizer.network;

import io.netty.buffer.ByteBuf;
import gg.archipelago.aprandomizer.APRandomizer;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record PerformMultiJumpPayload(int jumpNumber) implements CustomPacketPayload {
    public static final Type<PerformMultiJumpPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(APRandomizer.MODID, "perform_multi_jump"));

    public static final StreamCodec<ByteBuf, PerformMultiJumpPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            PerformMultiJumpPayload::jumpNumber,
            PerformMultiJumpPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
