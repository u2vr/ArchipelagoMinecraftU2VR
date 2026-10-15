package gg.archipelago.aprandomizer.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record RecipientEntry(int slotId, String name, boolean isOnline) {
    public static final StreamCodec<ByteBuf, RecipientEntry> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            RecipientEntry::slotId,
            ByteBufCodecs.STRING_UTF8,
            RecipientEntry::name,
            ByteBufCodecs.BOOL,
            RecipientEntry::isOnline,
            RecipientEntry::new
    );
}
