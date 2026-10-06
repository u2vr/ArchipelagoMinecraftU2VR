package gg.archipelago.aprandomizer.data.loot;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.LootItemKilledByPlayerCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceWithEnchantedBonusCondition;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;

public record APAddedLootTableProvider(Context output) implements LootTableSubProvider {

    @Override
    public void run() {
        HolderGetter<Enchantment> enchantments = output.lookup(Registries.ENCHANTMENT);

        output.accept(APLootTables.ENTITIES_DROWNED_ADD_TRIDENT,
                LootTable.lootTable()
                        .withPool(
                                LootPool.lootPool()
                                        .setRolls(ContextIntProviders.exactly(1))
                                        .add(
                                                LootItem.lootTableItem(Items.TRIDENT)
                                                        .when(LootItemRandomChanceCondition.randomChance(0.25f)))));

        output.accept(APLootTables.ENTITIES_WITHER_SKELETON_ADD_WITHER_SKELETON_SKULL,
                LootTable.lootTable()
                        .withPool(
                                LootPool.lootPool()
                                        .setRolls(ContextIntProviders.exactly(1))
                                        .when(LootItemKilledByPlayerCondition.killedByPlayer())
                                        .when(LootItemRandomChanceWithEnchantedBonusCondition.randomChanceAndLootingBoost(enchantments, 1f / 3f, 1f / 9f))
                                        .add(
                                                LootItem.lootTableItem(Items.WITHER_SKELETON_SKULL))));
    }

}
