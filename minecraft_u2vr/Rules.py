from BaseClasses import CollectionState
try:
    from worlds.generic.Rules import exclusion_rules, add_item_rule
except ImportError:
    from worlds.generic.Rules import exclusion_rules
    def add_item_rule(spot, rule, combine="and"):
        if combine == "and":
            old_rule = spot.item_rule
            spot.item_rule = lambda item: rule(item) and old_rule(item)
        elif combine == "or":
            old_rule = spot.item_rule
            spot.item_rule = lambda item: rule(item) or old_rule(item)

from . import Constants
from typing import TYPE_CHECKING

if TYPE_CHECKING:
    from . import MinecraftWorld


# Helper functions for Archipelago U2VR logic

def recipe_count(state: CollectionState, player: int) -> int:
    return state.count("Recipe Unlock", player)


def has_iron_ingots(world: "MinecraftWorld", state: CollectionState, player: int) -> bool:
    return recipe_count(state, player) >= 4


def has_copper_ingots(world: "MinecraftWorld", state: CollectionState, player: int) -> bool:
    return recipe_count(state, player) >= 3


def has_gold_ingots(world: "MinecraftWorld", state: CollectionState, player: int) -> bool:
    return recipe_count(state, player) >= 8 or state.can_reach_region('The Nether', player)


def has_diamond_pickaxe(world: "MinecraftWorld", state: CollectionState, player: int) -> bool:
    return recipe_count(state, player) >= 15


def craft_crossbow(world: "MinecraftWorld", state: CollectionState, player: int) -> bool:
    return recipe_count(state, player) >= 8


def has_bottle(world: "MinecraftWorld", state: CollectionState, player: int) -> bool:
    return recipe_count(state, player) >= 5


def has_spyglass(world: "MinecraftWorld", state: CollectionState, player: int) -> bool:
    return recipe_count(state, player) >= 5 and can_adventure(world, state, player)


def can_enchant(world: "MinecraftWorld", state: CollectionState, player: int) -> bool:
    return recipe_count(state, player) >= 18


def can_use_anvil(world: "MinecraftWorld", state: CollectionState, player: int) -> bool:
    return recipe_count(state, player) >= 12


def fortress_loot(world: "MinecraftWorld", state: CollectionState, player: int) -> bool:
    return state.can_reach_region('Nether Fortress', player) and basic_combat(world, state, player)


def can_excavate(world: "MinecraftWorld", state: CollectionState, player: int) -> bool:
    return recipe_count(state, player) >= 5 and can_adventure(world, state, player)


def can_brew_potions(world: "MinecraftWorld", state: CollectionState, player: int) -> bool:
    return state.has('Blaze Rods', player) and recipe_count(state, player) >= 10


def can_piglin_trade(world: "MinecraftWorld", state: CollectionState, player: int) -> bool:
    return (has_gold_ingots(world, state, player)
            and (
                    state.can_reach_region('The Nether', player)
                    or state.can_reach_region('Bastion Remnant', player)
            ))


def overworld_villager(world: "MinecraftWorld", state: CollectionState, player: int) -> bool:
    village_region = state.multiworld.get_region('Village', player).entrances[0].parent_region.name
    if village_region == 'The Nether':
        return (state.can_reach_location('Zombie Doctor', player)
                or (
                        has_diamond_pickaxe(world, state, player)
                        and state.can_reach_region('Village', player)
                ))
    elif village_region == 'The End':
        return state.can_reach_location('Zombie Doctor', player)
    return state.can_reach_region('Village', player) or state.can_reach_location('Zombie Doctor', player)


def enter_stronghold(world: "MinecraftWorld", state: CollectionState, player: int) -> bool:
    return state.has('Blaze Rods', player) and recipe_count(state, player) >= 15


STRUCTURE_ORDER = {
    "Village": 1,
    "Pillager Outpost": 2,
    "Nether Fortress": 3,
    "Bastion Remnant": 4,
    "Trial Chambers": 5,
    "Ancient City": 6,
    "Ocean Monument": 7,
    "End City": 8
}


def can_access_connected_structure(world: "MinecraftWorld", state: CollectionState, entrance_name: str, player: int) -> bool:
    if not world.options.structure_restriction:
        return True
    try:
        entrance = state.multiworld.get_entrance(entrance_name, player)
        target = entrance.connected_region.name if entrance.connected_region else None
        if target and target in STRUCTURE_ORDER:
            return state.has("Structure Unlock", player, STRUCTURE_ORDER[target])
    except Exception:
        pass
    return True


# Difficulty-dependent functions
def combat_difficulty(world: "MinecraftWorld", state: CollectionState, player: int) -> str:
    return world.options.combat_difficulty.current_key


def can_adventure(world: "MinecraftWorld", state: CollectionState, player: int) -> bool:
    death_link_check = not world.options.death_link or recipe_count(state, player) >= 2
    if combat_difficulty(world, state, player) == 'easy':
        return recipe_count(state, player) >= 8 and death_link_check
    elif combat_difficulty(world, state, player) == 'hard':
        return True
    return recipe_count(state, player) >= 4 and death_link_check


def basic_combat(world: "MinecraftWorld", state: CollectionState, player: int) -> bool:
    if combat_difficulty(world, state, player) == 'easy':
        res = recipe_count(state, player) >= 10
        if world.options.offhand_restriction:
            res = res and state.has("Offhand Unlock", player)
        if world.options.hp_restriction:
            res = res and state.has("HP Upgrade", player, 2)
        return res
    elif combat_difficulty(world, state, player) == 'hard':
        return True
    res = recipe_count(state, player) >= 6
    if world.options.hp_restriction:
        res = res and state.has("HP Upgrade", player, 1)
    return res


def ominous_vaults(world: "MinecraftWorld", state: CollectionState, player: int) -> bool:
    return (state.can_reach_region("Pillager Outpost", player)
            and recipe_count(state, player) >= 10
            and basic_combat(world, state, player))


def complete_raid(world: "MinecraftWorld", state: CollectionState, player: int) -> bool:
    reach_regions = (state.can_reach_region('Village', player)
                     and state.can_reach_region('Pillager Outpost', player))
    return reach_regions and basic_combat(world, state, player) and recipe_count(state, player) >= 8


def can_kill_wither(world: "MinecraftWorld", state: CollectionState, player: int) -> bool:
    normal_kill = recipe_count(state, player) >= 20 and can_brew_potions(world, state, player)
    if world.options.hp_restriction:
        normal_kill = normal_kill and state.has("HP Upgrade", player, 2)
    if combat_difficulty(world, state, player) == 'easy':
        return fortress_loot(world, state, player) and normal_kill
    elif combat_difficulty(world, state, player) == 'hard':
        return fortress_loot(world, state, player) and (
                normal_kill
                or state.can_reach_region('The Nether', player)
                or state.can_reach_region('The End', player)
        )
    return fortress_loot(world, state, player) and normal_kill


def can_respawn_ender_dragon(world: "MinecraftWorld", state: CollectionState, player: int) -> bool:
    return (state.can_reach_region('The Nether', player)
            and state.can_reach_region('The End', player)
            and recipe_count(state, player) >= 6)


def can_kill_ender_dragon(world: "MinecraftWorld", state: CollectionState, player: int) -> bool:
    if combat_difficulty(world, state, player) == 'easy':
        return recipe_count(state, player) >= 20 and can_brew_potions(world, state, player)
    if combat_difficulty(world, state, player) == 'hard':
        return recipe_count(state, player) >= 5
    return recipe_count(state, player) >= 12


def has_structure_compass(world: "MinecraftWorld", state: CollectionState, entrance_name: str, player: int) -> bool:
    if not world.options.structure_compasses:
        return True
    return state.has(f"Structure Compass ({state.multiworld.get_entrance(entrance_name, player).connected_region.name})", player)


def get_rules_lookup(world, player: int):
    rules_lookup = {
        "entrances": {
            "Nether Portal": lambda state: (recipe_count(state, player) >= 4 or state.can_reach_region('Nether Fortress', player))
                                           and (not world.options.world_border_restriction or state.has("World Border Upgrade", player, 1)),
            "End Portal": lambda state: enter_stronghold(world, state, player)
                                        and can_adventure(world, state, player)
                                        and (not world.options.world_border_restriction or state.has("World Border Upgrade", player, 2)),
            "Overworld Structure 1": lambda state: can_adventure(world, state, player)
                                                   and has_structure_compass(world, state, "Overworld Structure 1", player)
                                                   and can_access_connected_structure(world, state, "Overworld Structure 1", player),
            "Overworld Structure 2": lambda state: can_adventure(world, state, player)
                                                   and has_structure_compass(world, state, "Overworld Structure 2", player)
                                                   and can_access_connected_structure(world, state, "Overworld Structure 2", player),
            "Nether Structure 1": lambda state: can_adventure(world, state, player)
                                                 and has_structure_compass(world, state, "Nether Structure 1", player)
                                                 and can_access_connected_structure(world, state, "Nether Structure 1", player),
            "Nether Structure 2": lambda state: can_adventure(world, state, player)
                                                 and has_structure_compass(world, state, "Nether Structure 2", player)
                                                 and can_access_connected_structure(world, state, "Nether Structure 2", player),
            "The End Structure": lambda state: can_adventure(world, state, player)
                                                and has_structure_compass(world, state, "The End Structure", player)
                                                and can_access_connected_structure(world, state, "The End Structure", player),
            "Ocean": lambda state: can_adventure(world, state, player)
                                    and has_structure_compass(world, state, "Ocean", player)
                                    and can_access_connected_structure(world, state, "Ocean", player),
            "Dark Forest": lambda state: can_adventure(world, state, player)
                                          and has_structure_compass(world, state, "Dark Forest", player),
            "Deep Dark": lambda state: can_adventure(world, state, player)
                                        and has_iron_ingots(world, state, player)
                                        and has_structure_compass(world, state, "Deep Dark", player)
                                        and can_access_connected_structure(world, state, "Deep Dark", player),
            "Ruins": lambda state: can_adventure(world, state, player)
                                    and has_structure_compass(world, state, "Ruins", player),
            "Underground": lambda state: can_adventure(world, state, player)
                                          and has_structure_compass(world, state, "Underground", player)
                                          and can_access_connected_structure(world, state, "Underground", player),
            "Sulfur Spring": lambda state: can_adventure(world, state, player)
                                            and has_structure_compass(world, state, "Sulfur Spring", player),
            "Biome Discovery": lambda state: can_adventure(world, state, player)
                                              and has_structure_compass(world, state, "Biome Discovery", player),
        },
        "locations": {
            "Ender Dragon": lambda state: can_respawn_ender_dragon(world, state, player)
                                           and can_kill_ender_dragon(world, state, player),
            "Wither": lambda state: can_kill_wither(world, state, player),
            "Blaze Rods": lambda state: fortress_loot(world, state, player),
            "Who is Cutting Onions?": lambda state: can_piglin_trade(world, state, player),
            "Oh Shiny": lambda state: can_piglin_trade(world, state, player),
            "Suit Up": lambda state: has_iron_ingots(world, state, player),
            "Very Very Frightening": lambda state: can_use_anvil(world, state, player)
                                                    and can_enchant(world, state, player)
                                                    and overworld_villager(world, state, player),
            "Hot Stuff": lambda state: recipe_count(state, player) >= 4
                                        and has_iron_ingots(world, state, player),
            "Free the End": lambda state: can_respawn_ender_dragon(world, state, player)
                                           and can_kill_ender_dragon(world, state, player),
            "A Furious Cocktail": lambda state: (can_brew_potions(world, state, player)
                                                 and recipe_count(state, player) >= 6
                                                 and state.can_reach_region("The Nether", player)
                                                 and state.can_reach_region("Village", player)
                                                 and state.can_reach_location("Bring Home the Beacon", player)
                                                 and can_adventure(world, state, player)
                                                 and state.can_reach_region("Trial Chambers", player)),
            "Bring Home the Beacon": lambda state: can_kill_wither(world, state, player)
                                                    and has_diamond_pickaxe(world, state, player)
                                                    and recipe_count(state, player) >= 10,
            "Not Today, Thank You": lambda state: recipe_count(state, player) >= 4
                                                  and has_iron_ingots(world, state, player)
                                                  and (not world.options.offhand_restriction or state.has("Offhand Unlock", player)),
            "Isn't It Iron Pick": lambda state: recipe_count(state, player) >= 4
                                                and has_iron_ingots(world, state, player),
            "Local Brewery": lambda state: can_brew_potions(world, state, player),
            "The Next Generation": lambda state: can_respawn_ender_dragon(world, state, player)
                                                 and can_kill_ender_dragon(world, state, player),
            "Fishy Business": lambda state: recipe_count(state, player) >= 2,
            "Hot Tourist Destinations": lambda state: has_structure_compass(world, state, "Biome Discovery", player),
            "This Boat Has Legs": lambda state: has_iron_ingots(world, state, player)
                                                and recipe_count(state, player) >= 6,
            "Sniper Duel": lambda state: recipe_count(state, player) >= 4,
            "Great View From Up Here": lambda state: basic_combat(world, state, player),
            "How Did We Get Here?": lambda state: (can_brew_potions(world, state, player)
                                                   and has_gold_ingots(world, state, player)
                                                   and state.can_reach_region('End City', player)
                                                   and state.can_reach_region('The Nether', player)
                                                   and state.can_reach_region('Ocean Monument', player)
                                                   and state.can_reach_region('Ancient City', player)
                                                   and state.can_reach_region('Trial Chambers', player)
                                                   and recipe_count(state, player) >= 12
                                                   and state.can_reach_location("Bring Home the Beacon", player)
                                                   and state.can_reach_location("Hero of the Village", player)),
            "Bullseye": lambda state: recipe_count(state, player) >= 6
                                       and has_iron_ingots(world, state, player),
            "Spooky Scary Skeleton": lambda state: basic_combat(world, state, player),
            "Two by Two": lambda state: can_excavate(world, state, player)
                                        and state.can_reach_region("The Nether", player)
                                        and state.can_reach_region("Ocean Monument", player)
                                        and state.can_reach_region("Village", player)
                                        and recipe_count(state, player) >= 6,
            "Two Birds, One Arrow": lambda state: craft_crossbow(world, state, player)
                                                  and can_enchant(world, state, player),
            "Who's the Pillager Now?": lambda state: craft_crossbow(world, state, player),
            "Getting an Upgrade": lambda state: recipe_count(state, player) >= 1,
            "Tactical Fishing": lambda state: recipe_count(state, player) >= 4
                                              and has_iron_ingots(world, state, player),
            "Zombie Doctor": lambda state: can_brew_potions(world, state, player)
                                           and has_gold_ingots(world, state, player),
            "Ice Bucket Challenge": lambda state: has_diamond_pickaxe(world, state, player),
            "Into Fire": lambda state: basic_combat(world, state, player),
            "War Pigs": lambda state: basic_combat(world, state, player),
            "Take Aim": lambda state: recipe_count(state, player) >= 4,
            "Total Beelocation": lambda state: can_use_anvil(world, state, player)
                                               and can_enchant(world, state, player),
            "Arbalistic": lambda state: (craft_crossbow(world, state, player)
                                         and can_use_anvil(world, state, player)
                                         and can_enchant(world, state, player)),
            "The End... Again...": lambda state: can_respawn_ender_dragon(world, state, player)
                                                 and can_kill_ender_dragon(world, state, player),
            "Acquire Hardware": lambda state: has_iron_ingots(world, state, player),
            "Not Quite \"Nine\" Lives": lambda state: can_piglin_trade(world, state, player)
                                                      and recipe_count(state, player) >= 8,
            "Cover Me with Diamonds": lambda state: recipe_count(state, player) >= 15
                                                    and has_iron_ingots(world, state, player),
            "Sky's the Limit": lambda state: basic_combat(world, state, player),
            "Hired Help": lambda state: recipe_count(state, player) >= 6
                                        and has_iron_ingots(world, state, player),
            "Sweet Dreams": lambda state: recipe_count(state, player) >= 2
                                          or state.can_reach_region('Village', player),
            "You Need a Mint": lambda state: can_respawn_ender_dragon(world, state, player)
                                             and has_bottle(world, state, player),
            "Monsters Hunted": lambda state: can_respawn_ender_dragon(world, state, player)
                                             and can_kill_ender_dragon(world, state, player)
                                             and can_kill_wither(world, state, player)
                                             and complete_raid(world, state, player)
                                             and state.can_reach_region('Bastion Remnant', player)
                                             and state.can_reach_region('End City', player)
                                             and state.can_reach_region('Trial Chambers', player)
                                             and state.can_reach_region('Ocean Monument', player)
                                             and recipe_count(state, player) >= 8,
            "Enchanter": lambda state: can_enchant(world, state, player),
            "Voluntary Exile": lambda state: basic_combat(world, state, player),
            "Eye Spy": lambda state: enter_stronghold(world, state, player),
            "Serious Dedication": lambda state: (state.can_reach_location("Hidden in the Depths", player)
                                                 and has_gold_ingots(world, state, player)
                                                 and recipe_count(state, player) >= 20),
            "Postmortal": lambda state: complete_raid(world, state, player),
            "Adventuring Time": lambda state: can_adventure(world, state, player)
                                              and has_iron_ingots(world, state, player)
                                              and recipe_count(state, player) >= 6,
            "Hero of the Village": lambda state: complete_raid(world, state, player),
            "Hidden in the Depths": lambda state: can_brew_potions(world, state, player)
                                                  and has_diamond_pickaxe(world, state, player),
            "Beaconator": lambda state: (can_kill_wither(world, state, player)
                                         and has_diamond_pickaxe(world, state, player)
                                         and recipe_count(state, player) >= 10),
            "Withering Heights": lambda state: can_kill_wither(world, state, player),
            "A Balanced Diet": lambda state: (has_bottle(world, state, player)
                                              and recipe_count(state, player) >= 6
                                              and state.can_reach_location("Overpowered", player)
                                              and state.can_reach_region('The End', player)),
            "Subspace Bubble": lambda state: has_diamond_pickaxe(world, state, player),
            "Country Lode, Take Me Home": lambda state: recipe_count(state, player) >= 6
                                                        and has_iron_ingots(world, state, player),
            "Bee Our Guest": lambda state: recipe_count(state, player) >= 4
                                           and has_bottle(world, state, player),
            "Uneasy Alliance": lambda state: has_diamond_pickaxe(world, state, player)
                                             and recipe_count(state, player) >= 4,
            "Diamonds!": lambda state: recipe_count(state, player) >= 6
                                       and has_iron_ingots(world, state, player),
            "A Throwaway Joke": lambda state: basic_combat(world, state, player),
            "Sticky Situation": lambda state: recipe_count(state, player) >= 4
                                              and has_bottle(world, state, player),
            "Ol' Betsy": lambda state: craft_crossbow(world, state, player),
            "Cover Me in Debris": lambda state: recipe_count(state, player) >= 25
                                                and state.can_reach_location("Hidden in the Depths", player),
            "Hot Topic": lambda state: recipe_count(state, player) >= 2,
            "The Lie": lambda state: has_iron_ingots(world, state, player)
                                     and recipe_count(state, player) >= 6,
            "On a Rail": lambda state: has_iron_ingots(world, state, player)
                                       and recipe_count(state, player) >= 6,
            "When Pigs Fly": lambda state: has_iron_ingots(world, state, player)
                                            and recipe_count(state, player) >= 6
                                            and can_adventure(world, state, player),
            "Overkill": lambda state: (
                                       can_brew_potions(world, state, player)
                                       and (
                                               recipe_count(state, player) >= 6
                                               or state.can_reach_region('The Nether', player)
                                       )
                                      )
                                      or (
                                           state.can_reach_location("Over-Overkill", player)
                                           and world.options.include_hard_advancements
                                           and "Over-Overkill" not in world.options.exclude_locations.value
                                      ),
            "Librarian": lambda state: can_enchant(world, state, player),
            "Overpowered": lambda state: has_iron_ingots(world, state, player)
                                         and recipe_count(state, player) >= 6
                                         and basic_combat(world, state, player),
            "Wax On": lambda state: recipe_count(state, player) >= 4
                                    and has_copper_ingots(world, state, player),
            "Wax Off": lambda state: (
                                      has_copper_ingots(world, state, player)
                                      and recipe_count(state, player) >= 4
                                     )
                                     or state.can_reach_region("Trial Chambers", player),
            "The Cutest Predator": lambda state: can_adventure(world, state, player)
                                                 and has_iron_ingots(world, state, player)
                                                 and recipe_count(state, player) >= 4,
            "The Healing Power of Friendship": lambda state: can_adventure(world, state, player)
                                                             and has_iron_ingots(world, state, player)
                                                             and recipe_count(state, player) >= 4,
            "Is It a Bird?": lambda state: has_spyglass(world, state, player),
            "Is It a Balloon?": lambda state: has_spyglass(world, state, player),
            "Is It a Plane?": lambda state: has_spyglass(world, state, player)
                                            and can_respawn_ender_dragon(world, state, player),
            "Surge Protector": lambda state: can_use_anvil(world, state, player)
                                             and can_enchant(world, state, player)
                                             and overworld_villager(world, state, player),
            "Light as a Rabbit": lambda state: can_adventure(world, state, player)
                                               and has_iron_ingots(world, state, player)
                                               and recipe_count(state, player) >= 4,
            "Glow and Behold!": lambda state: can_adventure(world, state, player),
            "Whatever Floats Your Goat!": lambda state: can_adventure(world, state, player),
            "Caves & Cliffs": lambda state: has_iron_ingots(world, state, player)
                                            and recipe_count(state, player) >= 6,
            "Feels Like Home": lambda state: has_iron_ingots(world, state, player)
                                             and recipe_count(state, player) >= 6,
            "Sound of Music": lambda state: recipe_count(state, player) >= 6
                                            and has_iron_ingots(world, state, player)
                                            and can_adventure(world, state, player)
                                            and (
                                              basic_combat(world, state, player)
                                              or state.can_reach_region("The Nether", player)
                                              or state.can_reach_region("Ancient City", player)
                                            ),
            "Star Trader": lambda state: has_iron_ingots(world, state, player)
                                         and recipe_count(state, player) >= 6
                                         and (
                                           state.can_reach_region("The Nether", player)
                                           or state.can_reach_region("Nether Fortress", player)
                                           or can_piglin_trade(world, state, player)
                                         )
                                         and overworld_villager(world, state, player),
            "Birthday Song": lambda state: state.can_reach_location("The Lie", player)
                                           and recipe_count(state, player) >= 6
                                           and has_iron_ingots(world, state, player)
                                           and (
                                               state.can_reach_region('Pillager Outpost', player)
                                               or (
                                                   basic_combat(world, state, player)
                                                   and state.can_reach_region('Woodland Mansion', player)
                                               )
                                            ),
            "Bukkit Bukkit": lambda state: recipe_count(state, player) >= 4
                                           and has_iron_ingots(world, state, player)
                                           and can_adventure(world, state, player),
            "It Spreads": lambda state: can_adventure(world, state, player)
                                        and has_iron_ingots(world, state, player)
                                        and recipe_count(state, player) >= 6,
            "Sneak 100": lambda state: can_adventure(world, state, player)
                                       and has_iron_ingots(world, state, player)
                                       and recipe_count(state, player) >= 6,
            "When the Squad Hops into Town": lambda state: can_adventure(world, state, player)
                                                           and recipe_count(state, player) >= 6
                                                           and has_iron_ingots(world, state, player),
            "With Our Powers Combined!": lambda state: can_adventure(world, state, player)
                                                       and state.can_reach_region("The Nether", player)
                                                       and recipe_count(state, player) >= 6
                                                       and has_iron_ingots(world, state, player),
            "You've Got a Friend in Me": lambda state: state.can_reach_region('Pillager Outpost', player)
                                                       or (
                                                           basic_combat(world, state, player)
                                                           and state.can_reach_region('Woodland Mansion', player)
                                                       ),
            "Smells Interesting": lambda state: can_excavate(world, state, player),
            "Little Sniffs": lambda state: can_excavate(world, state, player),
            "Planting the Past": lambda state: can_excavate(world, state, player),
            "Crafting a New Look": lambda state: has_iron_ingots(world, state, player)
                                                 and (
                                                     fortress_loot(world, state, player)
                                                     or (
                                                         state.can_reach_region("Pillager Outpost", player)
                                                         and basic_combat(world, state, player)
                                                     )
                                                     or (
                                                         state.can_reach_region("Bastion Remnant", player)
                                                         and basic_combat(world, state, player)
                                                     )
                                                     or (
                                                         state.can_reach_region("End City", player)
                                                         and basic_combat(world, state, player)
                                                     )
                                                     or (
                                                         state.can_reach_region("Ocean Monument", player)
                                                         and basic_combat(world, state, player)
                                                         and can_enchant(world, state, player)
                                                     )
                                                     or (
                                                         state.can_reach_region("Woodland Mansion", player)
                                                         and basic_combat(world, state, player)
                                                     )
                                                     or state.can_reach_region("Ancient City", player)
                                                     or (
                                                         state.can_reach_region("Trail Ruins", player)
                                                         and can_excavate(world, state, player)
                                                     )
                                                 ),
            "Smithing with Style": lambda state: can_excavate(world, state, player)
                                                 and fortress_loot(world, state, player)
                                                 and state.can_reach_region("Bastion Remnant", player)
                                                 and state.can_reach_region("End City", player)
                                                 and can_brew_potions(world, state, player)
                                                 and state.can_reach_region("Woodland Mansion", player)
                                                 and state.can_reach_region("Ancient City", player)
                                                 and state.can_reach_region("Trail Ruins", player)
                                                 and state.can_reach_region("Ocean Monument", player),
            "Respecting the Remnants": lambda state: can_excavate(world, state, player)
                                                     and (
                                                         state.can_reach_region("Ocean Monument", player)
                                                         or state.can_reach_region("Trail Ruins", player)
                                                     ),
            "Careful Restoration": lambda state: state.can_reach_region("Trial Chambers", player)
                                                 and basic_combat(world, state, player),
            "The Power of Books": lambda state: recipe_count(state, player) >= 4,
            "Isn't It Scute?": lambda state: can_adventure(world, state, player)
                                             and has_copper_ingots(world, state, player)
                                             and recipe_count(state, player) >= 4,
            "Shear Brilliance": lambda state: can_adventure(world, state, player)
                                              and has_copper_ingots(world, state, player)
                                              and recipe_count(state, player) >= 4,
            "Good as New": lambda state: can_adventure(world, state, player)
                                         and has_copper_ingots(world, state, player)
                                         and recipe_count(state, player) >= 4,
            "The Whole Pack": lambda state: can_adventure(world, state, player),
            "Under Lock and Key": lambda state: basic_combat(world, state, player),
            "Blowback": lambda state: basic_combat(world, state, player),
            "Who Needs Rockets?": lambda state: basic_combat(world, state, player),
            "Crafters Crafting Crafters": lambda state: has_iron_ingots(world, state, player)
                                                        and recipe_count(state, player) >= 6,
            "Lighten Up": lambda state: (
                                         fortress_loot(world, state, player)
                                         and recipe_count(state, player) >= 8
                                        )
                                        or state.can_reach_region("Trial Chambers", player),
            "Over-Overkill": lambda state: ominous_vaults(world, state, player),
            "Revaulting": lambda state: ominous_vaults(world, state, player),
            "Stay Hydrated!": lambda state: state.can_reach_region("The Nether", player)
                                            and can_piglin_trade(world, state, player),
            "Heart Transplanter": lambda state: can_adventure(world, state, player)
                                                and basic_combat(world, state, player),
            "Mob Kabob": lambda state: recipe_count(state, player) >= 4,
            "Uh Oh": lambda state: can_adventure(world, state, player)
                                   and basic_combat(world, state, player)
                                   and recipe_count(state, player) >= 4
        }
    }
    for i in range(1, 21):
        rules_lookup["locations"][f"Wandering Trader Trade {i}"] = lambda state: True

    # Exploration Biomes
    for name in Constants.OVERWORLD_BIOME_NAMES:
        if name in ("Explore Ocean", "Explore Deep Ocean", "Explore Cold Ocean", "Explore Deep Cold Ocean",
                    "Explore Frozen Ocean", "Explore Deep Frozen Ocean", "Explore Warm Ocean",
                    "Explore Lukewarm Ocean", "Explore Deep Lukewarm Ocean"):
            rules_lookup["locations"][name] = lambda state: recipe_count(state, player) >= 2
        elif name == "Explore Deep Dark":
            rules_lookup["locations"][name] = lambda state: basic_combat(world, state, player)
        else:
            rules_lookup["locations"][name] = lambda state: True

    # Exploration Structures
    rules_lookup["locations"]["Explore Stronghold"] = lambda state: enter_stronghold(world, state, player)
    rules_lookup["locations"]["Explore Mineshaft"] = lambda state: basic_combat(world, state, player)
    for struct_name in ("Explore Desert Pyramid", "Explore Jungle Temple", "Explore Swamp Hut",
                        "Explore Igloo", "Explore Shipwreck", "Explore Ocean Ruin",
                        "Explore Buried Treasure", "Explore Ruined Portal"):
        rules_lookup["locations"][struct_name] = lambda state: True

    for combat_struct in ("Explore Ocean Monument", "Explore Woodland Mansion", "Explore Ancient City",
                          "Explore Trial Chambers", "Explore Pillager Outpost", "Explore Nether Fortress",
                          "Explore Bastion Remnant"):
        rules_lookup["locations"][combat_struct] = lambda state: basic_combat(world, state, player)

    # Random BACAP Advancements
    if world.options.advancement_type.value == 2:
        for adv_id in getattr(world, "selected_bacap_map", {}).keys():
            adv_info = Constants.BACAP_ADVANCEMENT_BY_ID.get(adv_id)
            if adv_info:
                loc_name = adv_info["loc_name"]
                tags = adv_info.get("tags", [])
                if "combat" in tags:
                    rules_lookup["locations"][loc_name] = lambda state: basic_combat(world, state, player)
                else:
                    rules_lookup["locations"][loc_name] = lambda state: True

    return rules_lookup



def set_rules(self: "MinecraftWorld") -> None:
    multiworld = self.multiworld
    player = self.player

    rules_lookup = get_rules_lookup(self, player)

    # Set entrance rules
    for entrance_name, rule in rules_lookup["entrances"].items():
        try:
            multiworld.get_entrance(entrance_name, player).access_rule = rule
        except KeyError:
            pass

    # Set location rules
    for location_name, rule in rules_lookup["locations"].items():
        try:
            multiworld.get_location(location_name, player).access_rule = rule
        except KeyError:
            pass

    # Set rules surrounding completion
    bosses = self.options.required_bosses
    postgame_advancements = set()
    if bosses.dragon:
        postgame_advancements.update(Constants.exclusion_info["ender_dragon"])
    if bosses.wither:
        postgame_advancements.update(Constants.exclusion_info["wither"])

    def location_count(state: CollectionState) -> int:
        return len([location for location in multiworld.get_locations(player) if
                    location.address is not None and
                    location.can_reach(state)])

    def defeated_bosses(state: CollectionState) -> bool:
        return ((not bosses.dragon or state.has("Ender Dragon", player))
                and (not bosses.wither or state.has("Wither", player)))

    egg_shards = min(self.options.egg_shards_required.value, self.options.egg_shards_available.value)
    goal_locations = self.options.check_goal.value

    completion_requirements = lambda state: (location_count(state) >= goal_locations
                                             and (egg_shards == 0 or state.has("Dragon Egg Shard", player, egg_shards)))
    multiworld.completion_condition[player] = lambda state: completion_requirements(state) and defeated_bosses(state)

    # Set exclusions on hard/unreasonable/postgame (only in vanilla advancements mode)
    if self.options.advancement_type.value == 0:
        excluded_advancements = set()
        if not self.options.include_hard_advancements:
            excluded_advancements.update(Constants.exclusion_info["hard"])
        if not self.options.include_unreasonable_advancements:
            excluded_advancements.update(Constants.exclusion_info["unreasonable"])
        if not self.options.include_postgame_advancements:
            excluded_advancements.update(postgame_advancements)
        exclusion_rules(multiworld, player, excluded_advancements)

    # Forbid Structure Unlocks on difficult/challenge/end-game achievements
    forbid_structure_unlocks_on_hard_locations(self)


def forbid_structure_unlocks_on_hard_locations(world: "MinecraftWorld") -> None:
    multiworld = world.multiworld
    player = world.player

    hard_locations = set()

    # 1. Vanilla hard/challenge/postgame advancements
    hard_locations.update(Constants.exclusion_info.get("hard", []))
    hard_locations.update(Constants.exclusion_info.get("unreasonable", []))
    hard_locations.update(Constants.exclusion_info.get("ender_dragon", []))
    hard_locations.update(Constants.exclusion_info.get("wither", []))
    hard_locations.update({
        "Bullseye",
        "Sniper Duel",
        "Subspace Bubble",
        "Serious Dedication",
        "Great View From Up Here",
        "Overkill",
        "Overpowered",
        "Hot Tourist Destinations",
        "Caves & Cliffs",
        "Postmortal",
        "The City at the End of the Game",
        "Sky's the Limit",
    })

    # 2. BACAP challenge / The End advancements
    for entry in getattr(Constants, "bacap_advancements_info", []):
        if entry.get("frame") == "challenge" or entry.get("region") == "The End" or "end" in entry.get("tags", []):
            hard_locations.add(entry["loc_name"])

    # 3. BACAP late milestones (> 25)
    for i in range(26, Constants.MAX_BACAP_MILESTONES + 1):
        hard_locations.add(f"BACAP Milestone {i}")

    # 4. End biomes and End City exploration checks
    hard_locations.update(Constants.END_BIOME_NAMES)
    hard_locations.add("Explore End City")

    # Apply item rule to prevent Structure Unlock from being placed at difficult locations
    for loc_name in hard_locations:
        try:
            loc = multiworld.get_location(loc_name, player)
            add_item_rule(loc, lambda item: item.player != player or item.name != "Structure Unlock")
        except KeyError:
            pass
