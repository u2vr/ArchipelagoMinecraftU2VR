package gg.archipelago.aprandomizer.data.tags;

import gg.archipelago.aprandomizer.APRandomizer;
import gg.archipelago.aprandomizer.APStructures;
import gg.archipelago.aprandomizer.tags.APStructureTags;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.StructureTagsProvider;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;

import java.util.concurrent.CompletableFuture;

public class APStructureTagsProvider extends StructureTagsProvider {

    public APStructureTagsProvider(PackOutput output, CompletableFuture<Provider> provider) {
        super(output, provider, APRandomizer.MODID);
    }

    @Override
    protected void addTags(Provider p_256087_) {
        tag(APStructureTags.BASTION_REMNANT)
                .add(BuiltinStructures.BASTION_REMNANT);

        tag(APStructureTags.END_CITY)
                .add(BuiltinStructures.END_CITY)
                .add(APStructures.END_CITY_NETHER_STRUCTURE);

        tag(APStructureTags.FORTRESS)
                .add(BuiltinStructures.FORTRESS);

        tag(APStructureTags.PILLAGER_OUTPOST)
                .add(BuiltinStructures.PILLAGER_OUTPOST)
                .add(APStructures.PILLAGER_OUTPOST_NETHER_STRUCTURE);

        tag(APStructureTags.VILLAGE)
                .add(BuiltinStructures.VILLAGE_DESERT)
                .add(BuiltinStructures.VILLAGE_PLAINS)
                .add(BuiltinStructures.VILLAGE_SAVANNA)
                .add(BuiltinStructures.VILLAGE_SNOWY)
                .add(BuiltinStructures.VILLAGE_TAIGA)
                .add(APStructures.VILLAGE_NETHER_STRUCTURE);

        tag(APStructureTags.ANCIENT_CITY)
                .add(BuiltinStructures.ANCIENT_CITY);

        tag(APStructureTags.WOODLAND_MANSION)
                .add(BuiltinStructures.WOODLAND_MANSION);

        tag(APStructureTags.OCEAN_MONUMENT)
                .add(BuiltinStructures.OCEAN_MONUMENT);

        tag(APStructureTags.TRAIL_RUINS)
                .add(BuiltinStructures.TRAIL_RUINS);

        tag(APStructureTags.TRIAL_CHAMBERS)
                .add(BuiltinStructures.TRIAL_CHAMBERS);

        tag(APStructureTags.STRONGHOLD)
                .add(BuiltinStructures.STRONGHOLD);

        tag(APStructureTags.MINESHAFT)
                .add(BuiltinStructures.MINESHAFT)
                .add(BuiltinStructures.MINESHAFT_MESA);

        tag(APStructureTags.DESERT_PYRAMID)
                .add(BuiltinStructures.DESERT_PYRAMID);

        tag(APStructureTags.JUNGLE_TEMPLE)
                .add(BuiltinStructures.JUNGLE_TEMPLE);

        tag(APStructureTags.SWAMP_HUT)
                .add(BuiltinStructures.SWAMP_HUT);

        tag(APStructureTags.IGLOO)
                .add(BuiltinStructures.IGLOO);

        tag(APStructureTags.SHIPWRECK)
                .add(BuiltinStructures.SHIPWRECK)
                .add(BuiltinStructures.SHIPWRECK_BEACHED);

        tag(APStructureTags.OCEAN_RUIN)
                .add(BuiltinStructures.OCEAN_RUIN_COLD)
                .add(BuiltinStructures.OCEAN_RUIN_WARM);

        tag(APStructureTags.BURIED_TREASURE)
                .add(BuiltinStructures.BURIED_TREASURE);

        tag(APStructureTags.NETHER_FOSSIL)
                .add(BuiltinStructures.NETHER_FOSSIL);

        tag(APStructureTags.RUINED_PORTAL)
                .add(BuiltinStructures.RUINED_PORTAL_STANDARD)
                .add(BuiltinStructures.RUINED_PORTAL_DESERT)
                .add(BuiltinStructures.RUINED_PORTAL_JUNGLE)
                .add(BuiltinStructures.RUINED_PORTAL_SWAMP)
                .add(BuiltinStructures.RUINED_PORTAL_MOUNTAIN)
                .add(BuiltinStructures.RUINED_PORTAL_OCEAN)
                .add(BuiltinStructures.RUINED_PORTAL_NETHER);

        tag(APStructureTags.ABANDONED_CAMP)
                .add(BuiltinStructures.ABANDONED_CAMP_BAMBOO_JUNGLE)
                .add(BuiltinStructures.ABANDONED_CAMP_BIRCH_FOREST)
                .add(BuiltinStructures.ABANDONED_CAMP_CHERRY_GROVE)
                .add(BuiltinStructures.ABANDONED_CAMP_DAPPLED_FOREST)
                .add(BuiltinStructures.ABANDONED_CAMP_FLOWER_FOREST)
                .add(BuiltinStructures.ABANDONED_CAMP_FOREST)
                .add(BuiltinStructures.ABANDONED_CAMP_MEADOW)
                .add(BuiltinStructures.ABANDONED_CAMP_OLD_GROWTH_BIRCH_FOREST)
                .add(BuiltinStructures.ABANDONED_CAMP_OLD_GROWTH_PINE_TAIGA)
                .add(BuiltinStructures.ABANDONED_CAMP_OLD_GROWTH_SPRUCE_TAIGA)
                .add(BuiltinStructures.ABANDONED_CAMP_PALE_GARDEN)
                .add(BuiltinStructures.ABANDONED_CAMP_SAVANNA)
                .add(BuiltinStructures.ABANDONED_CAMP_SNOWY_TAIGA)
                .add(BuiltinStructures.ABANDONED_CAMP_SPARSE_JUNGLE)
                .add(BuiltinStructures.ABANDONED_CAMP_SWAMP)
                .add(BuiltinStructures.ABANDONED_CAMP_TAIGA)
                .add(BuiltinStructures.ABANDONED_CAMP_WINDSWEPT_FOREST)
                .add(BuiltinStructures.ABANDONED_CAMP_WOODED_BADLANDS);
    }

}
