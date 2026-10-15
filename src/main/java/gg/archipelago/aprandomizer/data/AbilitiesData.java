package gg.archipelago.aprandomizer.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record AbilitiesData(
        int progressiveMiningTier,
        boolean treeFellerUnlocked,
        int progressiveJumpTier,
        boolean itemMagnetUnlocked,
        boolean stepAssistUnlocked,
        boolean trashCanUnlocked,
        boolean lightHolderUnlocked
) {
    public static final AbilitiesData DEFAULT = new AbilitiesData(0, false, 0, false, false, false, false);

    public static final Codec<AbilitiesData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("progressiveMiningTier", 0).forGetter(AbilitiesData::progressiveMiningTier),
            Codec.BOOL.optionalFieldOf("treeFellerUnlocked", false).forGetter(AbilitiesData::treeFellerUnlocked),
            Codec.INT.optionalFieldOf("progressiveJumpTier", 0).forGetter(AbilitiesData::progressiveJumpTier),
            Codec.BOOL.optionalFieldOf("itemMagnetUnlocked", false).forGetter(AbilitiesData::itemMagnetUnlocked),
            Codec.BOOL.optionalFieldOf("stepAssistUnlocked", false).forGetter(AbilitiesData::stepAssistUnlocked),
            Codec.BOOL.optionalFieldOf("trashCanUnlocked", false).forGetter(AbilitiesData::trashCanUnlocked),
            Codec.BOOL.optionalFieldOf("lightHolderUnlocked", false).forGetter(AbilitiesData::lightHolderUnlocked)
    ).apply(instance, AbilitiesData::new));

    public AbilitiesData withProgressiveMiningTier(int tier) {
        return new AbilitiesData(tier, treeFellerUnlocked, progressiveJumpTier, itemMagnetUnlocked, stepAssistUnlocked, trashCanUnlocked, lightHolderUnlocked);
    }

    public AbilitiesData withTreeFellerUnlocked(boolean unlocked) {
        return new AbilitiesData(progressiveMiningTier, unlocked, progressiveJumpTier, itemMagnetUnlocked, stepAssistUnlocked, trashCanUnlocked, lightHolderUnlocked);
    }

    public AbilitiesData withProgressiveJumpTier(int tier) {
        return new AbilitiesData(progressiveMiningTier, treeFellerUnlocked, tier, itemMagnetUnlocked, stepAssistUnlocked, trashCanUnlocked, lightHolderUnlocked);
    }

    public AbilitiesData withItemMagnetUnlocked(boolean unlocked) {
        return new AbilitiesData(progressiveMiningTier, treeFellerUnlocked, progressiveJumpTier, unlocked, stepAssistUnlocked, trashCanUnlocked, lightHolderUnlocked);
    }

    public AbilitiesData withStepAssistUnlocked(boolean unlocked) {
        return new AbilitiesData(progressiveMiningTier, treeFellerUnlocked, progressiveJumpTier, itemMagnetUnlocked, unlocked, trashCanUnlocked, lightHolderUnlocked);
    }

    public AbilitiesData withTrashCanUnlocked(boolean unlocked) {
        return new AbilitiesData(progressiveMiningTier, treeFellerUnlocked, progressiveJumpTier, itemMagnetUnlocked, stepAssistUnlocked, unlocked, lightHolderUnlocked);
    }

    public AbilitiesData withLightHolderUnlocked(boolean unlocked) {
        return new AbilitiesData(progressiveMiningTier, treeFellerUnlocked, progressiveJumpTier, itemMagnetUnlocked, stepAssistUnlocked, trashCanUnlocked, unlocked);
    }
}
