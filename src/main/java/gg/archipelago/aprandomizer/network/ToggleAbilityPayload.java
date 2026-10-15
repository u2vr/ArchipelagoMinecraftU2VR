package gg.archipelago.aprandomizer.network;

import io.netty.buffer.ByteBuf;
import gg.archipelago.aprandomizer.APRandomizer;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ToggleAbilityPayload(int action) implements CustomPacketPayload {
    public static final Type<ToggleAbilityPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(APRandomizer.MODID, "toggle_ability"));

    public static final int ACTION_CYCLE_MINING = 0;
    public static final int ACTION_TOGGLE_TREE_FELLER = 1;
    public static final int ACTION_TOGGLE_MAGNET = 2;
    public static final int ACTION_TOGGLE_STEP_ASSIST = 3;

    public static final StreamCodec<ByteBuf, ToggleAbilityPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            ToggleAbilityPayload::action,
            ToggleAbilityPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
