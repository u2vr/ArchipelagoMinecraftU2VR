package gg.archipelago.aprandomizer;

import gg.archipelago.aprandomizer.managers.chest.SharedChestBlock;
import gg.archipelago.aprandomizer.managers.chest.SharedChestBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Set;

public class APBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(APRandomizer.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(APRandomizer.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, APRandomizer.MODID);

    public static final DeferredBlock<SharedChestBlock> SHARED_CHEST = BLOCKS.registerBlock(
            "shared_chest",
            SharedChestBlock::new,
            () -> BlockBehaviour.Properties.of()
                    .setId(net.minecraft.resources.ResourceKey.create(Registries.BLOCK, net.minecraft.resources.Identifier.fromNamespaceAndPath(APRandomizer.MODID, "shared_chest")))
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(2.5F, 1200.0F)
                    .sound(SoundType.STONE)
                    .lightLevel(state -> 4)
                    .noOcclusion()
    );

    public static final DeferredItem<BlockItem> SHARED_CHEST_ITEM = ITEMS.registerSimpleBlockItem("shared_chest", SHARED_CHEST);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SharedChestBlockEntity>> SHARED_CHEST_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register("shared_chest", () -> new BlockEntityType<>(SharedChestBlockEntity::new, Set.of(SHARED_CHEST.get())));

    public static void buildCreativeTabContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS || event.getTabKey() == CreativeModeTabs.OP_BLOCKS) {
            event.accept(SHARED_CHEST_ITEM.get());
        }
    }
}
