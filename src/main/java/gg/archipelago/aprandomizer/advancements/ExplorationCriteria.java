package gg.archipelago.aprandomizer.advancements;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.triggers.Criterion;
import net.minecraft.advancements.triggers.SimpleCriterionTrigger;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

import java.util.Optional;

public class ExplorationCriteria extends SimpleCriterionTrigger<ExplorationCriteria.TriggerInstance> {

    @Override
    public Codec<TriggerInstance> codec() {
        return TriggerInstance.CODEC;
    }

    public void trigger(ServerPlayer player, String id) {
        this.trigger(player, instance -> instance.id().equals(id));
    }

    public record TriggerInstance(Optional<Holder<LootItemCondition>> player, String id) implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(instance -> instance
                .group(
                        LootItemCondition.CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
                        Codec.STRING.fieldOf("id").forGetter(TriggerInstance::id))
                .apply(instance, TriggerInstance::new));

        public static Criterion<TriggerInstance> explored(String id) {
            return APCriteriaTriggers.EXPLORATION.get().createCriterion(new TriggerInstance(Optional.empty(), id));
        }
    }
}
