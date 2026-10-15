package gg.archipelago.aprandomizer.network;

import io.netty.buffer.ByteBuf;
import gg.archipelago.aprandomizer.APRandomizer;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record AltTrashItemPayload(int containerSlotId) implements CustomPacketPayload {
    public static final Type<AltTrashItemPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(APRandomizer.MODID, "alt_trash_item"));

    public static final StreamCodec<ByteBuf, AltTrashItemPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            AltTrashItemPayload::containerSlotId,
            AltTrashItemPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
