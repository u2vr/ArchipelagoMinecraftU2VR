from ..test.bases import MCTestBase
from ..Constants import region_info
from .. import Options

from BaseClasses import ItemClassification


class AdvancementTestBase(MCTestBase):
    options = {
        "advancement_goal": Options.AdvancementGoal.range_end
    }
    # beatability test implicit


class ShardTestBase(MCTestBase):
    options = {
        "egg_shards_required": Options.EggShardsRequired.range_end,
        "egg_shards_available": Options.EggShardsAvailable.range_end
    }

    # check that itempool is not overfilled with shards
    def test_itempool(self):
        assert len(self.multiworld.get_unfilled_locations()) == len(self.multiworld.itempool)


class StructureRestrictionTestBase(MCTestBase):
    options = {
        "structure_restriction": True
    }

    def test_structures_in_pool(self):
        item_names = [item.name for item in self.multiworld.itempool]
        assert item_names.count("Structure Unlock") == 8


class RestrictionsTestBase(MCTestBase):
    options = {
        "hp_restriction": True,
        "hunger_restriction": True,
        "reach_restriction": True,
        "world_border_restriction": True,
        "structure_restriction": True,
        "inventory_slot_restriction": True,
        "offhand_restriction": True,
        "curses_enabled": True
    }

    def test_all_restrictions_in_pool(self):
        item_names = [item.name for item in self.multiworld.itempool]
        assert item_names.count("HP Upgrade") == 7
        assert item_names.count("Hunger Upgrade") == 6
        assert item_names.count("Reach Upgrade") == 3
        assert item_names.count("World Border Upgrade") == 5
        assert item_names.count("Structure Unlock") == 8
        assert item_names.count("Inventory Slot Upgrade") == 27
        assert item_names.count("Offhand Unlock") == 1
        assert item_names.count("Disable Day Monster Ignition") == 1
        assert item_names.count("Disable Light Check for Monster Spawn") == 1
        assert len(self.multiworld.get_unfilled_locations()) == len(self.multiworld.itempool)


class NoTrapTestBase(MCTestBase):
    options = {
        "bee_traps": Options.BeeTraps.range_start
    }

    # With no traps, there are no trap items in the pool
    def test_traps(self):
        for item in self.multiworld.itempool:
            assert item.name != "Random Trap"


class AllTrapTestBase(MCTestBase):
    options = {
        "bee_traps": Options.BeeTraps.range_end
    }

    # With max traps, all filler items are traps
    def test_traps(self):
        for item in self.multiworld.itempool:
            assert item.classification != ItemClassification.filler


class WanderingTraderTradesTestBase(MCTestBase):
    options = {
        "wandering_trader_trades": 10
    }

    def test_trader_locations(self):
        locations = [loc.name for loc in self.multiworld.get_locations()]
        for i in range(1, 11):
            assert f"Wandering Trader Trade {i}" in locations
        assert "Wandering Trader Trade 11" not in locations


class RandomBACAPTestBase(MCTestBase):
    options = {
        "advancement_type": Options.AdvancementType.option_random_bacap,
        "world_border_restriction": True,
        "structure_checks": True,
        "biome_checks": True,
    }

    def test_random_bacap_locations(self):
        locations = [loc.name for loc in self.multiworld.get_locations()]
        bacap_locs = [l for l in locations if l.startswith("BACAP: ")]
        assert len(bacap_locs) > 0
        for name in bacap_locs:
            info = Constants.BACAP_ADVANCEMENT_BY_NAME.get(name)
            if info:
                assert "biome_or_distance" not in info.get("tags", [])
        assert len(self.multiworld.get_unfilled_locations()) == len(self.multiworld.itempool)


class RandomBACAPTrapsTestBase(MCTestBase):
    options = {
        "advancement_type": Options.AdvancementType.option_random_bacap,
        "bee_traps": 10,
    }

    def test_random_bacap_traps(self):
        traps = [item for item in self.multiworld.itempool if item.name == "Random Trap"]
        assert len(traps) > 0
        assert len(self.multiworld.get_unfilled_locations()) == len(self.multiworld.itempool)
