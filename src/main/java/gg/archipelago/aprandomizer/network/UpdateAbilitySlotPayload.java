package gg.archipelago.aprandomizer.network;

import gg.archipelago.aprandomizer.APRandomizer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public record UpdateAbilitySlotPayload(int slotType, ItemStack stack) implements CustomPacketPayload {
    public static final Type<UpdateAbilitySlotPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(APRandomizer.MODID, "update_ability_slot"));

    public static final int SLOT_TRASH_CAN = 0;
    public static final int SLOT_LIGHT_HOLDER = 1;

    public static final StreamCodec<RegistryFriendlyByteBuf, UpdateAbilitySlotPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeVarInt(payload.slotType());
                ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, payload.stack());
            },
            buf -> new UpdateAbilitySlotPayload(
                    buf.readVarInt(),
                    ItemStack.OPTIONAL_STREAM_CODEC.decode(buf)
            )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
