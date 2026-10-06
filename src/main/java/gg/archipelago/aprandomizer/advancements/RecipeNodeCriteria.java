package gg.archipelago.aprandomizer.advancements;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.triggers.Criterion;
import net.minecraft.advancements.triggers.SimpleCriterionTrigger;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

import java.util.Optional;

public class RecipeNodeCriteria extends SimpleCriterionTrigger<RecipeNodeCriteria.TriggerInstance> {

    @Override
    public Codec<TriggerInstance> codec() {
        return TriggerInstance.CODEC;
    }

    public void trigger(ServerPlayer player, String nodeId) {
        this.trigger(player, instance -> instance.nodeId().equals(nodeId));
    }

    public record TriggerInstance(Optional<Holder<LootItemCondition>> player, String nodeId) implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(instance -> instance
                .group(
                        LootItemCondition.CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
                        Codec.STRING.fieldOf("node_id").forGetter(TriggerInstance::nodeId))
                .apply(instance, TriggerInstance::new));

        public static Criterion<TriggerInstance> recipeNode(String nodeId) {
            return APCriteriaTriggers.RECIPE_NODE.get().createCriterion(new TriggerInstance(Optional.empty(), nodeId));
        }
    }
}
