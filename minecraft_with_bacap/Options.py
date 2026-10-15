import typing
from dataclasses import dataclass
from Options import (
    Choice,
    Toggle,
    DefaultOnToggle,
    Range,
    OptionList,
    DeathLink,
    PlandoConnections,
    PerGameCommonOptions,
    ProgressionBalancing,
    Accessibility,
)
from .Constants import region_info

try:
    from Options import OptionGroup
except ImportError:
    class OptionGroup:
        def __init__(self, name: str, options: typing.Iterable[type], start_collapsed: bool = False, description: str = ""):
            self.name = name
            self.options = list(options)
            self.start_collapsed = start_collapsed
            self.description = description

# === 1. MAIN OPTIONS ===

class CheckGoal(Range):
    """Number of checks required to complete the goal.
    Simultaneously counts structure checks, biome checks, and milestone/advancement checks."""
    display_name = "Check Goal (Checks to Complete)"
    range_start = 0
    range_end = 1000
    default = 40

# Backward compatibility alias
AdvancementGoal = CheckGoal


class BossGoal(Choice):
    """Bosses which must be defeated to finish the game."""
    display_name = "Required Bosses"
    option_none = 0
    option_ender_dragon = 1
    option_wither = 2
    option_both = 3
    default = 1

    @property
    def dragon(self):
        return self.value % 2 == 1

    @property
    def wither(self):
        return self.value > 1


class RecipeUnlockMode(Choice):
    """Method for unlocking recipes when Recipe Unlock items are obtained (78 items in pool):
    random_by_bias: Recipes are randomly unlocked based on the tier bias.
    ticket_mode_skilltree: Recipe Unlocks grant tickets to manually unlock recipes in the Skill Tree GUI."""
    display_name = "Recipe Unlock Mode (78 recipe items)"
    option_random_by_bias = 0
    option_ticket_mode_skilltree = 1
    default = 0


class RecipeTierBias(Range):
    """Slider controlling recipe tier unlocking bias for the 78 Recipe Unlock items:
    0 = completely uniform random across all remaining recipe tiers.
    100 = strictly unlock earlier tiers first.
    1-99 = weighted bias favoring earlier recipe tiers (75% = strongly favors earlier tiers)."""
    display_name = "Craft Recipe Tier Bias (78 recipe items)"
    range_start = 0
    range_end = 100
    default = 75


class RecipeTierThreshold(Range):
    """Percentage of recipes in the current tier that must be unlocked before recipes in the next tier can be unlocked (Ticket Mode).
    Default is 50%."""
    display_name = "Recipe Tier Unlock Percentage"
    range_start = 0
    range_end = 100
    default = 50

# Backward compatibility alias
RecipeTierUnlockPercentage = RecipeTierThreshold


class StartingSharedChest(DefaultOnToggle):
    """Start the game with an Archipelago Shared Chest in your inventory.
    The shared chest synchronizes items with all other players in the room in real time."""
    display_name = "Starting Shared Chest"


class EggShardsRequired(Range):
    """Number of dragon egg shards to collect to spawn bosses."""
    display_name = "Egg Shards Required"
    range_start = 0
    range_end = 50
    default = 0


class EggShardsAvailable(Range):
    """Number of dragon egg shards available in the item pool (adds 0 to 50 items)."""
    display_name = "Egg Shards Available (0-50 items)"
    range_start = 0
    range_end = 50
    default = 0


# === 2. CHECKS OPTIONS ===

class WanderingTraderTrades(Range):
    """Number of Archipelago item trades available from the Wandering Trader (adds 0 to 20 checks).
    The Wandering Trader sells items from across all games in the multiworld.
    Setting this to 0 disables Wandering Trader Archipelago trades."""
    display_name = "Wandering Trader Trades (0-20 checks)"
    range_start = 0
    range_end = 20
    default = 10


class HintCost(Range):
    """The percentage of total locations that need to be checked to receive a hint.
    If 0, hints are free. If 100, hints cost the same number of points as there are locations.
    Default is 10%."""
    display_name = "Hint Cost (%)"
    range_start = 0
    range_end = 100
    default = 10

# Backward compatibility alias
LocationCheckPoints = HintCost
HintPointsPerCheck = HintCost


class _DisabledOption:
    @property
    def value(self) -> int:
        return 0

    @value.setter
    def value(self, val: typing.Any) -> None:
        pass

    def __bool__(self) -> bool:
        return False

    def __int__(self) -> int:
        return 0

    def __eq__(self, other: object) -> bool:
        if isinstance(other, bool):
            return not other
        if isinstance(other, int):
            return other == 0
        return False


_DISABLED_OPTION = _DisabledOption()


class AdvancementType(Choice):
    """Choose the advancement checks mode:
    vanilla: standard individual vanilla advancements (max = 138 checks).
    bacap: milestone checks granted every milestoneStep completed BACAP advancements (max = 1112/milestoneStep checks, up to 300 checks).
    random_bacap: randomly selected individual BACAP achievements from a balanced pool (max = 524 checks pool, excluding structures, biomes, endgame, and extreme grind)."""
    display_name = "Advancement Checks Type"
    option_vanilla = 0
    option_bacap = 1
    option_random_bacap = 2
    default = 1


class BACAPAdvancementStep(Range):
    """In BACAP milestone mode, number of completed advancements required for each Archipelago check.
    The number of milestone checks generated will be 1112 divided by this step (max = 1112/milestoneStep, up to 300 checks)."""
    display_name = "BACAP Advancement Step"
    range_start = 1
    range_end = 20
    default = 5


class MajorBiomeChecks(DefaultOnToggle):
    """Enables Archipelago checks for discovering major surface and nether biomes (31 Overworld, 5 Nether = 36 checks)."""
    display_name = "Major Biome Checks (36 checks)"


class SmallBiomeChecks(DefaultOnToggle):
    """Enables Archipelago checks for discovering small biomes, rivers, beaches, caves, peaks, and micro-variations (25 checks)."""
    display_name = "Small Biome Checks (25 checks)"


class EndGameBiomeChecks(Toggle):
    """Enables Archipelago checks for discovering the 5 The End dimension biomes (5 checks). Off by default."""
    display_name = "EndGame Biome Checks (5 checks)"


# Backward compatibility alias
BiomeChecks = MajorBiomeChecks


class StructureChecks(DefaultOnToggle):
    """Enables Archipelago checks for discovering unique structures (22 structures = 22 checks)."""
    display_name = "Structure Checks (22 checks)"


# === 3. UNLOCKABLES OPTIONS ===

class UnvisitedStructuresCompass(DefaultOnToggle):
    """Adds an unvisited structure compass to the item pool (1 item), which dynamically points to the closest unvisited structure."""
    display_name = "Unvisited Structures Compass (1 item)"


class StructureCompasses(Toggle):
    """Adds structure compasses for all structures to the item pool (22 items), each pointing to its specific structure."""
    display_name = "All Structure Compasses (22 items)"


class UnvisitedBiomesCompass(DefaultOnToggle):
    """Adds an unvisited biome compass to the item pool (1 item), which dynamically points to the closest unvisited biome."""
    display_name = "Unvisited Biomes Compass (1 item)"


class MajorBiomesCompasses(Toggle):
    """Adds biome compasses for all major biomes to the item pool (36 items)."""
    display_name = "Major Biome Compasses (36 items)"


class MinorBiomesCompasses(Toggle):
    """Adds biome compasses for small/minor biomes to the item pool (25 items), matching the small biome checks class."""
    display_name = "Minor Biome Compasses (25 items)"


class ExtraAbilities(Toggle):
    """Enables extra QoL abilities and adds 12 ability items to the item pool:
    - Progressive Mining (5 items: 2x1x1, 2x3x1, 3x3x1, 3x3x2, 3x3x3)
    - Tree Feller (1 item)
    - Progressive Jump (2 items: Double Jump, Triple Jump)
    - Item Magnet (1 item)
    - Step Assist (1 item)
    - Trash Can (1 item)
    - Light Holder (1 item)
    """
    display_name = "Extra Abilities (12 items)"


class ReachRestriction(DefaultOnToggle):
    """Restricts player block mining and entity interaction distance until Reach Upgrades are obtained (adds 3 items)."""
    display_name = "Reach Distance Restriction (3 items)"


class HPRestriction(DefaultOnToggle):
    """Restricts maximum health to 3 hearts (6 HP) initially. Each HP Upgrade adds +1 heart (+2 HP) up to 10 hearts (adds 7 items)."""
    display_name = "HP Restriction (7 items)"


class HungerRestriction(DefaultOnToggle):
    """Restricts maximum food capacity to 4 drumsticks (8 hunger) initially. Each Hunger Upgrade adds +1 drumstick (+2 hunger) (adds 6 items)."""
    display_name = "Hunger Restriction (6 items)"


class WorldBorderRestriction(DefaultOnToggle):
    """Restricts world size with an expanding world border (start 1000, 1800, 3000, 5000, 8000, and max), requiring World Border Upgrades (adds 5 items)."""
    display_name = "World Border Restriction (5 items)"


class StructureRestriction(DefaultOnToggle):
    """Blocks entry to major structures (Villages, Fortresses, Bastions, Ancient Cities, Trial Chambers, etc.) until unlocked (adds 9 items)."""
    display_name = "Structure Restrictions (9 items)"


class StructureUnlockShuffle(Toggle):
    """If enabled, structure unlocks occur in a randomized order instead of standard sequential order."""
    display_name = "Shuffle Structure Unlock Order"


class InventorySlotRestriction(DefaultOnToggle):
    """Restricts player inventory to only 9 hotbar slots initially. Each Inventory Slot Upgrade unlocks one extra slot (adds 27 items)."""
    display_name = "Inventory Slot Restriction (27 items)"


class OffhandRestriction(DefaultOnToggle):
    """Locks the offhand slot (preventing shield and offhand item usage) until the Offhand Unlock upgrade is obtained (adds 1 item)."""
    display_name = "Offhand Restriction (1 item)"


# === 4. TRAPS & CURSES ===

class BeeTraps(Range):
    """Replaces a percentage of filler items with random traps."""
    display_name = "Trap Percentage (Converts filler items to traps)"
    range_start = 0
    range_end = 100
    default = 5


class MonsterTrapPercentage(Range):
    """Percentage of triggered traps that will spawn monsters (bees, skeletons, zombies) instead of non-monster traps (junk items, falling sand, blindness)."""
    display_name = "Monster Trap Percentage"
    range_start = 0
    range_end = 100
    default = 20

# Backward compatibility alias
MobTrapPercentage = MonsterTrapPercentage


class CursesEnabled(DefaultOnToggle):
    """Adds 2 permanent curse items to the item pool. When received, they permanently curse the world: monsters will no longer burn under daylight and can spawn at any light level."""
    display_name = "Permanent Curses (2 items)"


# === 5. GAME OPTIONS ===

class CombatDifficulty(Choice):
    """Modifies the level of items logically required for exploring dangerous areas and fighting bosses."""
    display_name = "Combat Difficulty"
    option_easy = 0
    option_normal = 1
    option_hard = 2
    default = 1


class ImmediateRespawn(DefaultOnToggle):
    """Choose whether to respawn immediately on death, or to be put into the game over screen."""
    display_name = "Immediate Respawn"


# === 6. OTHER / ADVANCED OPTIONS ===

class ShuffleStructures(DefaultOnToggle):
    """Enables shuffling of villages, outposts, fortresses, bastions, and end cities."""
    display_name = "Shuffle Structures"


class HardAdvancements(Toggle):
    """Enables certain RNG-reliant or tedious advancements."""
    display_name = "Include Hard Advancements"


class UnreasonableAdvancements(Toggle):
    """Enables the extremely difficult advancements "How Did We Get Here?" and "Adventuring Time.\""""
    display_name = "Include Unreasonable Advancements"


class PostgameAdvancements(Toggle):
    """Enables advancements that require spawning and defeating the required bosses."""
    display_name = "Include Postgame Advancements"


class SendDefeatedMobs(Toggle):
    """Send killed mobs to other Minecraft worlds which have this option enabled."""
    display_name = "Send Defeated Mobs"


class StartingItems(OptionList):
    """Start with these items. Each entry should be of this format: {item: "item_name", amount: #}
    `item` can include components, and should be in an identical format to a `/give` command with
    `"` escaped for json reasons.

    `amount` is optional and will default to 1 if omitted.

    example:
    ```
    starting_items: [
        { "item": "minecraft:stick[minecraft:custom_name=\"{'text':'pointy stick'}\"]" },
        { "item": "minecraft:arrow[minecraft:rarity=epic]", amount: 64 }
    ]
    ```
    """
    display_name = "Starting Items"


class MCPlandoConnections(PlandoConnections):
    entrances = set(connection[0] for connection in region_info["default_connections"])
    exits = set(connection[1] for connection in region_info["default_connections"])

    @classmethod
    def can_connect(cls, entrance, exit):
        if exit in region_info["illegal_connections"] and entrance in region_info["illegal_connections"][exit]:
            return False
        return True


# === OPTION GROUPS ===

option_groups: list[OptionGroup] = [
    OptionGroup("Game Options", [
        ProgressionBalancing,
        Accessibility,
        CombatDifficulty,
        ImmediateRespawn,
        DeathLink,
        StartingItems,
    ]),
    OptionGroup("Main", [
        CheckGoal,
        BossGoal,
        RecipeUnlockMode,
        RecipeTierBias,
        RecipeTierThreshold,
        StartingSharedChest,
        EggShardsRequired,
        EggShardsAvailable,
    ]),
    OptionGroup("Checks", [
        AdvancementType,
        BACAPAdvancementStep,
        MajorBiomeChecks,
        SmallBiomeChecks,
        EndGameBiomeChecks,
        StructureChecks,
        WanderingTraderTrades,
    ]),
    OptionGroup("Unlockables", [
        UnvisitedStructuresCompass,
        StructureCompasses,
        UnvisitedBiomesCompass,
        MajorBiomesCompasses,
        MinorBiomesCompasses,
        ExtraAbilities,
        ReachRestriction,
        HPRestriction,
        HungerRestriction,
        WorldBorderRestriction,
        StructureRestriction,
        StructureUnlockShuffle,
        InventorySlotRestriction,
        OffhandRestriction,
    ]),
    OptionGroup("Traps", [
        BeeTraps,
        MonsterTrapPercentage,
        CursesEnabled,
    ]),
]


@dataclass
class MinecraftOptions(PerGameCommonOptions):
    # Game Options
    combat_difficulty: CombatDifficulty
    immediate_respawn: ImmediateRespawn
    death_link: DeathLink
    starting_items: StartingItems

    # Main
    check_goal: CheckGoal
    required_bosses: BossGoal
    recipe_unlock_mode: RecipeUnlockMode
    recipe_tier_bias: RecipeTierBias
    recipe_tier_threshold: RecipeTierThreshold
    starting_shared_chest: StartingSharedChest
    egg_shards_required: EggShardsRequired
    egg_shards_available: EggShardsAvailable

    # Checks
    advancement_type: AdvancementType
    bacap_advancement_step: BACAPAdvancementStep
    major_biome_checks: MajorBiomeChecks
    small_biome_checks: SmallBiomeChecks
    endgame_biome_checks: EndGameBiomeChecks
    structure_checks: StructureChecks
    wandering_trader_trades: WanderingTraderTrades

    # Unlockables
    unvisited_structures_compass: UnvisitedStructuresCompass
    structure_compasses: StructureCompasses
    unvisited_biomes_compass: UnvisitedBiomesCompass
    major_biomes_compasses: MajorBiomesCompasses
    minor_biomes_compasses: MinorBiomesCompasses
    extra_abilities: ExtraAbilities
    reach_restriction: ReachRestriction
    hp_restriction: HPRestriction
    hunger_restriction: HungerRestriction
    world_border_restriction: WorldBorderRestriction
    structure_restriction: StructureRestriction
    structure_unlock_shuffle: StructureUnlockShuffle
    inventory_slot_restriction: InventorySlotRestriction
    offhand_restriction: OffhandRestriction

    # Traps
    bee_traps: BeeTraps
    monster_trap_percentage: MonsterTrapPercentage
    curses_enabled: CursesEnabled

    # Other options
    plando_connections: MCPlandoConnections

    @property
    def biome_checks(self) -> MajorBiomeChecks:
        return self.major_biome_checks

    @property
    def advancement_goal(self) -> CheckGoal:
        return self.check_goal

    @property
    def hint_cost(self) -> _DisabledOption:
        return _DISABLED_OPTION

    @property
    def location_check_points(self) -> _DisabledOption:
        return _DISABLED_OPTION

    # Compatibility properties for removed options
    @property
    def shuffle_structures(self) -> _DisabledOption:
        return _DISABLED_OPTION

    @property
    def include_hard_advancements(self) -> _DisabledOption:
        return _DISABLED_OPTION

    @property
    def include_unreasonable_advancements(self) -> _DisabledOption:
        return _DISABLED_OPTION

    @property
    def include_postgame_advancements(self) -> _DisabledOption:
        return _DISABLED_OPTION

    @property
    def send_defeated_mobs(self) -> _DisabledOption:
        return _DISABLED_OPTION
