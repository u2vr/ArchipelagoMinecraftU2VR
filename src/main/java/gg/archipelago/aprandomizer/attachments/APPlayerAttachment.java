package gg.archipelago.aprandomizer.attachments;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import gg.archipelago.aprandomizer.APRegistries;
import gg.archipelago.aprandomizer.items.APItem;
import gg.archipelago.aprandomizer.items.CompassReward;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.neoforge.common.util.NeoForgeExtraCodecs;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class APPlayerAttachment {

    public static final MapCodec<APPlayerAttachment> CODEC = RecordCodecBuilder.mapCodec(instance -> instance
            .group(
                    Codec.INT
                            .fieldOf("index").forGetter(APPlayerAttachment::getIndex),
                    Codec.unboundedMap(Identifier.CODEC, CompassReward.CODEC)
                            .fieldOf("compass_rewards").forGetter(APPlayerAttachment::getUnlockedCompassRewards),
                    Codec.unboundedMap(ResourceKey.codec(APRegistries.ARCHIPELAGO_ITEM), Codec.INT)
                            .fieldOf("tiers").forGetter(APPlayerAttachment::getTiers),
                    NeoForgeExtraCodecs.setOf(ResourceKey.codec(Registries.BIOME))
                            .fieldOf("visited_biomes").forGetter(APPlayerAttachment::getVisitedBiomes),
                    Codec.INT.optionalFieldOf("mining_shape", 0).forGetter(APPlayerAttachment::getMiningShapeIndex),
                    Codec.BOOL.optionalFieldOf("tree_feller_active", true).forGetter(APPlayerAttachment::isTreeFellerActive),
                    Codec.BOOL.optionalFieldOf("magnet_active", true).forGetter(APPlayerAttachment::isItemMagnetActive),
                    Codec.BOOL.optionalFieldOf("step_assist_active", true).forGetter(APPlayerAttachment::isStepAssistActive),
                    ItemStack.OPTIONAL_CODEC.optionalFieldOf("trash_item", ItemStack.EMPTY).forGetter(APPlayerAttachment::getTrashCanItem),
                    ItemStack.OPTIONAL_CODEC.optionalFieldOf("light_item", ItemStack.EMPTY).forGetter(APPlayerAttachment::getLightHolderItem)
            )
            .apply(instance, APPlayerAttachment::new));

    private int index = 0;
    private Map<Identifier, CompassReward> compassRewards = new HashMap<>();
    private Object2IntMap<ResourceKey<APItem>> tiers = new Object2IntOpenHashMap<>();
    private Set<ResourceKey<Biome>> visitedBiomes = new HashSet<>();

    // Ability player toggles and slots
    private int miningShapeIndex = 0;
    private boolean treeFellerActive = true;
    private boolean itemMagnetActive = true;
    private boolean stepAssistActive = true;
    private ItemStack trashCanItem = ItemStack.EMPTY;
    private ItemStack lightHolderItem = ItemStack.EMPTY;

    public APPlayerAttachment() {
    }

    public APPlayerAttachment(int index, Map<Identifier, CompassReward> compassRewards, Map<ResourceKey<APItem>, Integer> tiers, Set<ResourceKey<Biome>> visitedBiomes) {
        this(index, compassRewards, tiers, visitedBiomes, 0, true, true, true, ItemStack.EMPTY, ItemStack.EMPTY);
    }

    public APPlayerAttachment(int index, Map<Identifier, CompassReward> compassRewards, Map<ResourceKey<APItem>, Integer> tiers, Set<ResourceKey<Biome>> visitedBiomes,
                              int miningShapeIndex, boolean treeFellerActive, boolean itemMagnetActive, boolean stepAssistActive,
                              ItemStack trashCanItem, ItemStack lightHolderItem) {
        this.index = index;
        this.compassRewards = new HashMap<>(compassRewards);
        this.tiers = new Object2IntOpenHashMap<>(tiers);
        this.visitedBiomes = new HashSet<>(visitedBiomes);
        this.miningShapeIndex = miningShapeIndex;
        this.treeFellerActive = treeFellerActive;
        this.itemMagnetActive = itemMagnetActive;
        this.stepAssistActive = stepAssistActive;
        this.trashCanItem = (trashCanItem != null) ? trashCanItem.copy() : ItemStack.EMPTY;
        this.lightHolderItem = (lightHolderItem != null) ? lightHolderItem.copy() : ItemStack.EMPTY;
    }

    public int getIndex() {
        return this.index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public Map<Identifier, CompassReward> getUnlockedCompassRewards() {
        return compassRewards;
    }

    public Object2IntMap<ResourceKey<APItem>> getTiers() {
        return tiers;
    }

    public Set<ResourceKey<Biome>> getVisitedBiomes() {
        return visitedBiomes;
    }

    public int getMiningShapeIndex() {
        return miningShapeIndex;
    }

    public void setMiningShapeIndex(int miningShapeIndex) {
        this.miningShapeIndex = miningShapeIndex;
    }

    public boolean isTreeFellerActive() {
        return treeFellerActive;
    }

    public void setTreeFellerActive(boolean treeFellerActive) {
        this.treeFellerActive = treeFellerActive;
    }

    public boolean isItemMagnetActive() {
        return itemMagnetActive;
    }

    public void setItemMagnetActive(boolean itemMagnetActive) {
        this.itemMagnetActive = itemMagnetActive;
    }

    public boolean isStepAssistActive() {
        return stepAssistActive;
    }

    public void setStepAssistActive(boolean stepAssistActive) {
        this.stepAssistActive = stepAssistActive;
    }

    public ItemStack getTrashCanItem() {
        return trashCanItem;
    }

    public void setTrashCanItem(ItemStack trashCanItem) {
        this.trashCanItem = (trashCanItem != null) ? trashCanItem : ItemStack.EMPTY;
    }

    public ItemStack getLightHolderItem() {
        return lightHolderItem;
    }

    public void setLightHolderItem(ItemStack lightHolderItem) {
        this.lightHolderItem = (lightHolderItem != null) ? lightHolderItem : ItemStack.EMPTY;
    }
}
