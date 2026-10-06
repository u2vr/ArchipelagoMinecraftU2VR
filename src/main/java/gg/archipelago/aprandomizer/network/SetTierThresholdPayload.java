package gg.archipelago.aprandomizer.network;

import io.netty.buffer.ByteBuf;
import gg.archipelago.aprandomizer.APRandomizer;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record SetTierThresholdPayload(int threshold) implements CustomPacketPayload {
    public static final Type<SetTierThresholdPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(APRandomizer.MODID, "set_tier_threshold"));
    public static final StreamCodec<ByteBuf, SetTierThresholdPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            SetTierThresholdPayload::threshold,
            SetTierThresholdPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
