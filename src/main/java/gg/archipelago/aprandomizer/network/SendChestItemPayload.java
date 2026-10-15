package gg.archipelago.aprandomizer.network;

import gg.archipelago.aprandomizer.APRandomizer;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record SendChestItemPayload(int targetSlotId) implements CustomPacketPayload {
    public static final Type<SendChestItemPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(APRandomizer.MODID, "send_chest_item"));
    public static final StreamCodec<ByteBuf, SendChestItemPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            SendChestItemPayload::targetSlotId,
            SendChestItemPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
