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


# === 1. CHECKS OPTIONS ===

class CheckGoal(Range):
    """Number of checks required to complete the goal.
    Simultaneously counts structure checks, biome checks, and milestone/advancement checks."""
    display_name = "Check Goal"
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
    """Choose whether to use vanilla advancements, BlazeandCave's Advancements Pack (BACAP) milestone checks, or randomly selected BACAP achievements.
    vanilla: standard individual vanilla advancements.
    bacap: checks granted every N completed BACAP advancements (milestones).
    random_bacap: randomly selects individual BACAP achievements based on required slot count."""
    display_name = "Advancement Type"
    option_vanilla = 0
    option_bacap = 1
    option_random_bacap = 2
    default = 1


class BACAPAdvancementStep(Range):
    """In BACAP mode, number of completed advancements required for each Archipelago check.
    The number of milestone checks generated will be 1200 divided by this step."""
    display_name = "BACAP Step (Advancements per Check)"
    range_start = 4
    range_end = 100
    default = 10


class BiomeChecks(DefaultOnToggle):
    """Enables Archipelago checks for discovering unique biomes."""
    display_name = "Biome Checks"


class StructureChecks(DefaultOnToggle):
    """Enables Archipelago checks for discovering unique structures."""
    display_name = "Structure Checks"


# === 2. MAIN OPTIONS ===

class RecipeTierBias(Range):
    """Slider controlling recipe tier unlocking bias:
    0 = completely uniform random across all remaining recipe tiers.
    100 = strictly unlock earlier tiers first.
    1-99 = weighted bias favoring earlier recipe tiers (70% = strongly favors earlier tiers)."""
    display_name = "Craft Recipe Tier Bias"
    range_start = 0
    range_end = 100
    default = 70


class RecipeUnlockMode(Choice):
    """Method for unlocking recipes when Recipe Unlock items are obtained:
    random_by_bias: Recipes are randomly unlocked based on the tier bias.
    ticket_mode_skilltree: Recipe Unlocks grant tickets to manually unlock recipes in the Skill Tree GUI."""
    display_name = "Recipe Unlock Mode"
    option_random_by_bias = 0
    option_ticket_mode_skilltree = 1
    default = 0


class RecipeTierThreshold(Range):
    """Percentage of recipes in the current tier that must be unlocked before recipes in the next tier can be unlocked (Ticket Mode).
    Default is 50%."""
    display_name = "Recipe Tier Unlock Percentage"
    range_start = 0
    range_end = 100
    default = 50

# Backward compatibility alias
RecipeTierUnlockPercentage = RecipeTierThreshold


class WanderingTraderTrades(Range):
    """Number of Archipelago item trades available from the Wandering Trader.
    The Wandering Trader sells items from across all games in the multiworld.
    Setting this to 0 disables Wandering Trader Archipelago trades."""
    display_name = "Wandering Trader Trades"
    range_start = 0
    range_end = 20
    default = 10


class StartingSharedChest(DefaultOnToggle):
    """Start the game with an Archipelago Shared Chest in your inventory.
    The shared chest synchronizes items with all other players in the room in real time."""
    display_name = "Starting Shared Chest"


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


class EggShardsRequired(Range):
    """Number of dragon egg shards to collect to spawn bosses."""
    display_name = "Egg Shards Required"
    range_start = 0
    range_end = 50
    default = 0


class EggShardsAvailable(Range):
    """Number of dragon egg shards available to collect."""
    display_name = "Egg Shards Available"
    range_start = 0
    range_end = 50
    default = 0


# === 3. UNLOCKABLES OPTIONS ===

class StructureCompasses(DefaultOnToggle):
    """Adds structure compasses to the item pool, which point to the nearest indicated structure."""
    display_name = "Structure Compasses"


class ReachRestriction(DefaultOnToggle):
    """Restricts player block mining and entity interaction distance until Reach Upgrades are obtained."""
    display_name = "Reach Distance Restriction"


class HPRestriction(DefaultOnToggle):
    """Restricts maximum health to 3 hearts (6 HP) initially. Each HP Upgrade adds +1 heart (+2 HP) up to 10 hearts."""
    display_name = "HP Restriction"


class HungerRestriction(DefaultOnToggle):
    """Restricts maximum food capacity to 4 drumsticks (8 hunger) initially. Each Hunger Upgrade adds +1 drumstick (+2 hunger)."""
    display_name = "Hunger Restriction"


class WorldBorderRestriction(DefaultOnToggle):
    """Restricts world size with an expanding world border (1000, 1800, 3000, 6000, and max), requiring World Border Upgrades."""
    display_name = "World Border Restriction"


class StructureRestriction(DefaultOnToggle):
    """Blocks entry to major structures (Villages, Fortresses, Bastions, Ancient Cities, Trial Chambers, etc.) until unlocked."""
    display_name = "Structure Restrictions"


class StructureUnlockShuffle(Toggle):
    """If enabled, structure unlocks occur in a randomized order instead of standard sequential order."""
    display_name = "Shuffle Structure Unlock Order"


class InventorySlotRestriction(DefaultOnToggle):
    """Restricts player inventory to only 9 hotbar slots initially. Each Inventory Slot Upgrade unlocks one extra slot."""
    display_name = "Inventory Slot Restriction"


class OffhandRestriction(DefaultOnToggle):
    """Locks the offhand slot (preventing shield and offhand item usage) until the Offhand Unlock upgrade is obtained."""
    display_name = "Offhand Restriction"


# === 4. TRAPS & CURSES ===

class BeeTraps(Range):
    """Replaces a percentage of filler items with random traps."""
    display_name = "Trap Percentage"
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
    """Enables permanent curses/punishments: monsters do not burn under daylight and can spawn at any light level."""
    display_name = "Permanent Curse"


# === OTHER / ADVANCED OPTIONS ===

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
    OptionGroup("Main", [
        ProgressionBalancing,
        Accessibility,
        RecipeTierBias,
        RecipeUnlockMode,
        RecipeTierThreshold,
        WanderingTraderTrades,
        StartingSharedChest,
        CombatDifficulty,
        ImmediateRespawn,
        DeathLink,
        EggShardsRequired,
        EggShardsAvailable,
    ]),
    OptionGroup("Checks", [
        CheckGoal,
        BossGoal,
        HintCost,
        AdvancementType,
        BACAPAdvancementStep,
        BiomeChecks,
        StructureChecks,
    ]),
    OptionGroup("Unlockables", [
        StructureCompasses,
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
    # Main
    recipe_tier_bias: RecipeTierBias
    recipe_unlock_mode: RecipeUnlockMode
    recipe_tier_threshold: RecipeTierThreshold
    wandering_trader_trades: WanderingTraderTrades
    starting_shared_chest: StartingSharedChest
    combat_difficulty: CombatDifficulty
    immediate_respawn: ImmediateRespawn
    death_link: DeathLink
    egg_shards_required: EggShardsRequired
    egg_shards_available: EggShardsAvailable

    # Checks
    check_goal: CheckGoal
    required_bosses: BossGoal
    hint_cost: HintCost
    advancement_type: AdvancementType
    bacap_advancement_step: BACAPAdvancementStep
    biome_checks: BiomeChecks
    structure_checks: StructureChecks

    # Unlockables
    structure_compasses: StructureCompasses
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
    starting_items: StartingItems

    @property
    def advancement_goal(self) -> CheckGoal:
        return self.check_goal

    @property
    def location_check_points(self) -> HintCost:
        return self.hint_cost

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
