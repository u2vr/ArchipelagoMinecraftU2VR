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
                Utils.sendTitleToAll(Component.literal("§6Прогрессивное копание (" + level + "/5)"),
                        Component.literal("Макс. сетка: " + shapeDesc), 10, 40, 10);
                Utils.sendMessageToAll("§a[Archipelago] §fРазблокировано улучшение копания: §6Уровень " + level + "/5 §7(Сетка: " + shapeDesc + ")");
            }
            case TREE_FELLER -> {
                worldData.unlockTreeFeller();
                Utils.sendTitleToAll(Component.literal("§aРубка деревьев"),
                        Component.literal("Срубает всё дерево целиком!"), 10, 40, 10);
                Utils.sendMessageToAll("§a[Archipelago] §fРазблокирована способность: §aРубка деревьев §7(Срубает всё дерево и листву)");
            }
            case PROGRESSIVE_JUMP -> {
                worldData.incrementJumpLevel();
                int level = worldData.getJumpLevel();
                String name = (level >= 2) ? "Тройной прыжок" : "Двойной прыжок";
                Utils.sendTitleToAll(Component.literal("§b" + name + " (" + level + "/2)"),
                        Component.literal("Дополнительный прыжок в воздухе!"), 10, 40, 10);
                Utils.sendMessageToAll("§a[Archipelago] §fРазблокирована способность: §b" + name + " §7(" + level + "/2)");
            }
            case ITEM_MAGNET -> {
                worldData.unlockItemMagnet();
                Utils.sendTitleToAll(Component.literal("§cМагнит предметов"),
                        Component.literal("Притягивает дроп и сферы опыта"), 10, 40, 10);
                Utils.sendMessageToAll("§a[Archipelago] §fРазблокирована способность: §cМагнит предметов §7(Радиус 10 блоков)");
            }
            case STEP_ASSIST -> {
                worldData.unlockStepAssist();
                Utils.sendTitleToAll(Component.literal("§eАвтоподъем на 1 блок"),
                        Component.literal("Плавный шаг без прыжков"), 10, 40, 10);
                Utils.sendMessageToAll("§a[Archipelago] §fРазблокирована способность: §eАвтоподъем на 1 блок §7(Step Assist)");
            }
            case TRASH_CAN -> {
                worldData.unlockTrashCan();
                Utils.sendTitleToAll(Component.literal("§8Мусорка в инвентаре"),
                        Component.literal("Ячейка для удаления ненужных предметов"), 10, 40, 10);
                Utils.sendMessageToAll("§a[Archipelago] §fРазблокирована ячейка: §8Мусорка §7(Поместите предмет или Alt+Клик)");
            }
            case LIGHT_HOLDER -> {
                worldData.unlockLightHolder();
                Utils.sendTitleToAll(Component.literal("§eДержатель света"),
                        Component.literal("Светящийся блок на груди освещает путь"), 10, 40, 10);
                Utils.sendMessageToAll("§a[Archipelago] §fРазблокирована ячейка: §eДержатель света §7(Светящиеся блоки освещают мир)");
            }
        }

        AbilityManager.syncToAll(server);
    }
}
