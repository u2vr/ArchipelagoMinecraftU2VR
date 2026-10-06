package gg.archipelago.aprandomizer.data.advancements;

import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.triggers.PlayerTrigger;
import net.minecraft.core.Holder.Reference;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.advancements.AdvancementSubProvider;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

public class AfterAdvancementProvider extends AdvancementSubProvider {

    private final List<AdvancementSubProvider.Factory> baseAdvancements;
    private final Function<Identifier, Identifier> idConverter;

    public AfterAdvancementProvider(BootstrapContext<Advancement> output, List<AdvancementSubProvider.Factory> baseAdvancements, Function<Identifier, Identifier> idConverter) {
        super(output);
        this.baseAdvancements = baseAdvancements;
        this.idConverter = idConverter;
    }

    @Override
    public void generate() {
        Object2BooleanMap<Identifier> noChildren = new Object2BooleanOpenHashMap<>();
        for (AdvancementSubProvider.Factory factory : baseAdvancements) {
            BootstrapContext<Advancement> subOutput = new BootstrapContext<Advancement>() {
                @Override
                public <S> HolderGetter<S> lookup(ResourceKey<? extends Registry<? extends S>> key) {
                    return output.lookup(key);
                }

                @Override
                public <S> Stream<Reference<S>> listContextElements(ResourceKey<? extends Registry<? extends S>> key) {
                    return output.listContextElements(key);
                }

                @Override
                public Reference<Advancement> register(ResourceKey<Advancement> key, Advancement value) {
                    if (value.display().isPresent() && !value.display().get().hidden()) {
                        if (!noChildren.containsKey(key.identifier())) {
                            noChildren.put(key.identifier(), true);
                        }
                        if (value.parent().isPresent()) {
                            noChildren.put(value.parent().get(), false);
                        }
                    }
                    return lookup(Registries.ADVANCEMENT).getOrThrow(key);
                }
            };
            AdvancementSubProvider provider = factory.create(subOutput);
            provider.generate();
        }

        for (Object2BooleanMap.Entry<Identifier> entry : noChildren.object2BooleanEntrySet()) {
            if (entry.getBooleanValue()) {
                Advancement.Builder.recipeAdvancement()
                        .parent(entry.getKey())
                        .addCriterion("auto", PlayerTrigger.TriggerInstance.tick())
                        .save(output, this.idConverter.apply(entry.getKey()));
            }
        }
    }

}
