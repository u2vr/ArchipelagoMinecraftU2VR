package gg.archipelago.aprandomizer.network;

import io.netty.buffer.ByteBuf;
import gg.archipelago.aprandomizer.APRandomizer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public record SyncAbilitiesPayload(
        int miningTier,
        boolean treeFellerUnlocked,
        int jumpTier,
        boolean itemMagnetUnlocked,
        boolean stepAssistUnlocked,
        boolean trashCanUnlocked,
        boolean lightHolderUnlocked,
        int miningShapeIndex,
        boolean treeFellerActive,
        boolean itemMagnetActive,
        boolean stepAssistActive,
        ItemStack trashCanItem,
        ItemStack lightHolderItem
) implements CustomPacketPayload {
    public static final Type<SyncAbilitiesPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(APRandomizer.MODID, "sync_abilities"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncAbilitiesPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeVarInt(payload.miningTier());
                buf.writeBoolean(payload.treeFellerUnlocked());
                buf.writeVarInt(payload.jumpTier());
                buf.writeBoolean(payload.itemMagnetUnlocked());
                buf.writeBoolean(payload.stepAssistUnlocked());
                buf.writeBoolean(payload.trashCanUnlocked());
                buf.writeBoolean(payload.lightHolderUnlocked());
                buf.writeVarInt(payload.miningShapeIndex());
                buf.writeBoolean(payload.treeFellerActive());
                buf.writeBoolean(payload.itemMagnetActive());
                buf.writeBoolean(payload.stepAssistActive());
                ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, payload.trashCanItem());
                ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, payload.lightHolderItem());
            },
            buf -> new SyncAbilitiesPayload(
                    buf.readVarInt(),
                    buf.readBoolean(),
                    buf.readVarInt(),
                    buf.readBoolean(),
                    buf.readBoolean(),
                    buf.readBoolean(),
                    buf.readBoolean(),
                    buf.readVarInt(),
                    buf.readBoolean(),
                    buf.readBoolean(),
                    buf.readBoolean(),
                    ItemStack.OPTIONAL_STREAM_CODEC.decode(buf),
                    ItemStack.OPTIONAL_STREAM_CODEC.decode(buf)
            )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
