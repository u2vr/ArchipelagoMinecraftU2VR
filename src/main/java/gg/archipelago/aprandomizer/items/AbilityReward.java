package gg.archipelago.aprandomizer.items;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import gg.archipelago.aprandomizer.APRandomizer;
import gg.archipelago.aprandomizer.common.Utils.Utils;
import gg.archipelago.aprandomizer.data.WorldData;
import gg.archipelago.aprandomizer.managers.AbilityManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.StringRepresentable;

import java.util.Locale;

public record AbilityReward(AbilityType type) implements APReward {
    public static final MapCodec<AbilityReward> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance
            .group(AbilityType.CODEC.fieldOf("ability_type").forGetter(AbilityReward::type))
            .apply(instance, AbilityReward::new));

    public enum AbilityType implements StringRepresentable {
        PROGRESSIVE_MINING,
        TREE_FELLER,
        PROGRESSIVE_JUMP,
        ITEM_MAGNET,
        STEP_ASSIST,
        TRASH_CAN,
        LIGHT_HOLDER;

        public static final Codec<AbilityType> CODEC = StringRepresentable.fromEnum(AbilityType::values);

        @Override
        public String getSerializedName() {
            return this.name().toLowerCase(Locale.ROOT);
        }
    }

    @Override
    public MapCodec<? extends APReward> codec() {
        return MAP_CODEC;
    }

    @Override
    public void give(MinecraftServer server) {
        WorldData worldData = APRandomizer.getWorldData();
        if (worldData == null) return;

        switch (type) {
            case PROGRESSIVE_MINING -> {
                worldData.incrementMiningLevel();
                int level = worldData.getMiningLevel();
                String shapeDesc = switch (level) {
                    case 1 -> "2×1×1";
                    case 2 -> "2×3×1";
                    case 3 -> "3×3×1";
                    case 4 -> "3×3×2";
                    case 5 -> "3×3×3";
                    default -> "1×1×1";
                };
                Utils.sendTitleToAll(Component.translatable("aprandomizer.reward.mining.title", level, 5),
                        Component.translatable("aprandomizer.reward.mining.subtitle", shapeDesc), 10, 40, 10);
                Utils.sendMessageToAll(Component.translatable("aprandomizer.reward.mining.chat", level, 5, shapeDesc));
            }
            case TREE_FELLER -> {
                worldData.unlockTreeFeller();
                Utils.sendTitleToAll(Component.translatable("aprandomizer.reward.tree_feller.title"),
                        Component.translatable("aprandomizer.reward.tree_feller.subtitle"), 10, 40, 10);
                Utils.sendMessageToAll(Component.translatable("aprandomizer.reward.tree_feller.chat"));
            }
            case PROGRESSIVE_JUMP -> {
                worldData.incrementJumpLevel();
                int level = worldData.getJumpLevel();
                Component jumpName = (level >= 2)
                        ? Component.translatable("aprandomizer.reward.triple_jump.title")
                        : Component.translatable("aprandomizer.reward.double_jump.title");
                Utils.sendTitleToAll(Component.empty().append(jumpName).append(" (" + level + "/2)"),
                        Component.translatable("aprandomizer.reward.jump.subtitle"), 10, 40, 10);
                Utils.sendMessageToAll(Component.translatable("aprandomizer.reward.jump.chat", jumpName, level, 2));
            }
            case ITEM_MAGNET -> {
                worldData.unlockItemMagnet();
                Utils.sendTitleToAll(Component.translatable("aprandomizer.reward.magnet.title"),
                        Component.translatable("aprandomizer.reward.magnet.subtitle"), 10, 40, 10);
                Utils.sendMessageToAll(Component.translatable("aprandomizer.reward.magnet.chat"));
            }
            case STEP_ASSIST -> {
                worldData.unlockStepAssist();
                Utils.sendTitleToAll(Component.translatable("aprandomizer.reward.step_assist.title"),
                        Component.translatable("aprandomizer.reward.step_assist.subtitle"), 10, 40, 10);
                Utils.sendMessageToAll(Component.translatable("aprandomizer.reward.step_assist.chat"));
            }
            case TRASH_CAN -> {
                worldData.unlockTrashCan();
                Utils.sendTitleToAll(Component.translatable("aprandomizer.reward.trash_can.title"),
                        Component.translatable("aprandomizer.reward.trash_can.subtitle"), 10, 40, 10);
                Utils.sendMessageToAll(Component.translatable("aprandomizer.reward.trash_can.chat"));
            }
            case LIGHT_HOLDER -> {
                worldData.unlockLightHolder();
                Utils.sendTitleToAll(Component.translatable("aprandomizer.reward.light_holder.title"),
                        Component.translatable("aprandomizer.reward.light_holder.subtitle"), 10, 40, 10);
                Utils.sendMessageToAll(Component.translatable("aprandomizer.reward.light_holder.chat"));
            }
        }

        AbilityManager.syncToAll(server);
    }
}
