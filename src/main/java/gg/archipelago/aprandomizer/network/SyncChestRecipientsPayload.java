package gg.archipelago.aprandomizer.network;

import gg.archipelago.aprandomizer.APRandomizer;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.List;

public record SyncChestRecipientsPayload(List<RecipientEntry> recipients) implements CustomPacketPayload {
    public static final Type<SyncChestRecipientsPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(APRandomizer.MODID, "sync_chest_recipients"));
    public static final StreamCodec<ByteBuf, SyncChestRecipientsPayload> STREAM_CODEC = StreamCodec.composite(
            RecipientEntry.STREAM_CODEC.apply(ByteBufCodecs.list()),
            SyncChestRecipientsPayload::recipients,
            SyncChestRecipientsPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
