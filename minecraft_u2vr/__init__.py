import os
import json
import settings
import typing
from math import ceil
from base64 import b64encode, b64decode
from typing import Dict, Any

from BaseClasses import Region, Entrance, Item, Tutorial, ItemClassification, Location
from worlds.AutoWorld import World, WebWorld
from worlds.LauncherComponents import Component, components, Type, SuffixIdentifier

from . import Constants
from .Container import MinecraftContainer
from .Options import MinecraftOptions, option_groups
from .Structures import shuffle_structures
from .ItemPool import build_item_pool, get_junk_item_names
from .Rules import set_rules
from ..LauncherComponents import icon_paths

client_version = 11

icon_paths['mcicon'] = f"ap:{__name__}/assets/mcicon.png"

# register client
def launch_client(*args):
    from .MinecraftClient import launch_subprocess
    launch_subprocess(*args)

components.append(
    Component(
        "Minecraft U2VR Client",
        icon="mcicon",
        func=launch_client,
        component_type=Type.CLIENT,
        file_identifier=SuffixIdentifier('.apmc'),
    )
)


class MinecraftSettings(settings.Group):
    class ForgeDirectory(settings.OptionalUserFolderPath):
        pass

    class ReleaseChannel(str):
        """
        release channel, currently "release", or "beta"
        any games played on the "beta" channel have a high likelihood of no longer working on the "release" channel.
        """

    class JavaExecutable(settings.OptionalUserFilePath):
        """
        Path to Java executable. If not set, will attempt to fall back to Java system installation.
        """

    class ServerDirectory(settings.OptionalUserFolderPath):
        """
        Path to local directory to install Java, Neo Forge, etc.
        """
        @classmethod
        def validate(cls, path: str):
            if os.path.exists(path) and not os.path.isdir(path):
                raise ValueError(f"'{path}' must be a folder")

    server_directory: ServerDirectory = ServerDirectory("Minecraft U2VR AP Server Directory")
    max_heap_size: str = "2G"
    min_heap_size: str = "1G"
    release_channel: ReleaseChannel = ReleaseChannel("release")
    java: JavaExecutable | None = JavaExecutable(None)


class MinecraftWebWorld(WebWorld):
    theme = "jungle"
    bug_report_page = "https://github.com/qixils/NeoForgeAP/issues/new?assignees=&labels=bug&template=bug_report.yaml&title=%5BBug%5D%3A+Brief+Description+of+bug+here"

    setup = Tutorial(
        "Multiworld Setup Guide",
        "A guide to setting up the Archipelago Minecraft software on your computer. This guide covers"
        "single-player, multiworld, and related software.",
        "English",
        "minecraft_en.md",
        "minecraft/en",
        ["qixils"]
    )

    setup_es = Tutorial(
        setup.tutorial_name,
        setup.description,
        "Español",
        "minecraft_es.md",
        "minecraft/es",
        ["Edos"]
    )

    setup_sv = Tutorial(
        setup.tutorial_name,
        setup.description,
        "Swedish",
        "minecraft_sv.md",
        "minecraft/sv",
        ["Albinum"]
    )

    setup_fr = Tutorial(
        setup.tutorial_name,
        setup.description,
        "Français",
        "minecraft_fr.md",
        "minecraft/fr",
        ["TheLynk"]
    )

    tutorials = [setup, setup_es, setup_sv, setup_fr]
    option_groups = option_groups


class MinecraftWorld(World):
    """
    Minecraft is a game about creativity. In a world made entirely of cubes, you explore, discover, mine,
    craft, and try not to explode. Delve deep into the earth and discover abandoned mines, ancient
    structures, and materials to create a portal to another world. Defeat the Ender Dragon, and claim
    victory!
    """
    game = "minecraft_u2vr"
    options_dataclass = MinecraftOptions
    options: MinecraftOptions
    settings: typing.ClassVar[MinecraftSettings]
    topology_present = True
    web = MinecraftWebWorld()
    option_groups = option_groups

    item_name_to_id = Constants.item_name_to_id
    location_name_to_id = Constants.location_name_to_id

    hint_blacklist = {
        "Random Trap",
        "Disable Day Monster Ignition",
        "Disable Light Check for Monster Spawn",
    }

    item_name_groups = {
        "Compasses": {
            "Structure Compass (Village)",
            "Structure Compass (Pillager Outpost)",
            "Structure Compass (Nether Fortress)",
            "Structure Compass (Bastion Remnant)",
            "Structure Compass (End City)",
            "Structure Compass (Ocean Monument)",
            "Structure Compass (Woodland Mansion)",
            "Structure Compass (Ancient City)",
            "Structure Compass (Trail Ruins)",
            "Structure Compass (Trial Chambers)",
            "Structure Compass (Sulfur Caves)",
            "Structure Compass (Unvisited Biomes)",
        },
        "Upgrades": {
            "HP Upgrade",
            "Hunger Upgrade",
            "Reach Upgrade",
            "World Border Upgrade",
            "Inventory Slot Upgrade",
            "Offhand Unlock",
            "Structure Unlock",
            "Recipe Unlock",
        }
    }

    using_ut: bool
    passthrough: dict[str, Any]
    ut_can_gen_without_yaml = True

    def __init__(self, multiworld: Any, player: int):
        super().__init__(multiworld, player)
        self.selected_bacap_advancements = []
        self.selected_bacap_map = {}
        raw_options = getattr(multiworld, "player_options", {}).get(player, {})
        if "advancement_goal" in raw_options and "check_goal" not in raw_options:
            self.options.check_goal.value = int(raw_options["advancement_goal"])

    def _get_mc_data(self) -> Dict[str, Any]:
        exits = [connection[0] for connection in Constants.region_info["default_connections"]]
        return {
            # Mod data
            'game': self.game,
            'world_seed': self.random.getrandbits(32),
            'seed_name': self.multiworld.seed_name,
            'player_name': self.player_name,
            'player_id': self.player,
            'client_version': client_version,
            'structures': {exit: self.multiworld.get_entrance(exit, self.player).connected_region.name for exit in exits},
            'check_goal': self.options.check_goal.value,
            'advancement_goal': self.options.check_goal.value,
            'egg_shards_required': min(self.options.egg_shards_required.value,
                                       self.options.egg_shards_available.value),
            'egg_shards_available': self.options.egg_shards_available.value,
            'required_bosses': self.options.required_bosses.current_key,
            'MC35': False,
            'death_link': bool(self.options.death_link.value),
            'starting_items': json.dumps(self.options.starting_items.value),
            'race': self.multiworld.is_race,
            'immediate_respawn': bool(self.options.immediate_respawn.value),

            # U2VR Restrictions & Modifiers
            'reach_restriction': bool(self.options.reach_restriction.value),
            'hp_restriction': bool(self.options.hp_restriction.value),
            'hunger_restriction': bool(self.options.hunger_restriction.value),
            'world_border_restriction': bool(self.options.world_border_restriction.value),
            'structure_restriction': bool(self.options.structure_restriction.value),
            'structure_unlock_shuffle': bool(self.options.structure_unlock_shuffle.value),
            'inventory_slot_restriction': bool(self.options.inventory_slot_restriction.value),
            'offhand_restriction': bool(self.options.offhand_restriction.value),
            'traps_enabled': bool(self.options.bee_traps.value > 0),
            'trap_percentage': int(self.options.bee_traps.value),
            'monster_trap_percentage': int(self.options.monster_trap_percentage.value),
            'curses_enabled': bool(self.options.curses_enabled.value),
            'recipe_tier_bias': float(self.options.recipe_tier_bias.value),
            'recipe_unlock_mode': self.options.recipe_unlock_mode.current_key,
            'recipe_tier_unlock_percentage': int(self.options.recipe_tier_threshold.value),
            'wandering_trader_trades': int(self.options.wandering_trader_trades.value),
            'starting_shared_chest': bool(self.options.starting_shared_chest.value),
            'advancement_type': self.options.advancement_type.current_key,
            'bacap_advancement_step': int(self.options.bacap_advancement_step.value),
            'bacap_check_count': min(Constants.TOTAL_BACAP_ADVANCEMENTS // max(1, int(self.options.bacap_advancement_step.value)), Constants.MAX_BACAP_MILESTONES),
            'biome_checks': bool(self.options.biome_checks.value),
            'structure_checks': bool(self.options.structure_checks.value),
            'hint_cost': int(self.options.hint_cost.value),
            'location_check_points': int(self.options.hint_cost.value),
            'selected_bacap_advancements': getattr(self, "selected_bacap_advancements", []),
            'selected_bacap_map': getattr(self, "selected_bacap_map", {}),

            # Universal Tracker data
            'bosses_to_defeat': self.options.required_bosses.value,
            'shuffle_structures': 0,
            'structure_compasses': self.options.structure_compasses.value,
            'combat_difficulty': self.options.combat_difficulty.value,
            'include_hard_advancements': 0,
            'include_unreasonable_advancements': 0,
            'include_postgame_advancements': 0,
        }

    def generate_early(self: "MinecraftWorld") -> None:
        if hasattr(self.multiworld, "hint_cost"):
            self.multiworld.hint_cost[self.player] = int(self.options.hint_cost.value)

        re_gen_passthrough = getattr(self.multiworld, "re_gen_passthrough", {})
        if re_gen_passthrough and self.game in re_gen_passthrough:
            self.using_ut = True
            self.passthrough = re_gen_passthrough[self.game]
            goal_val = self.passthrough.get("check_goal", self.passthrough.get("advancement_goal", 40))
            self.options.check_goal.value = goal_val
            self.options.egg_shards_required.value = self.passthrough["egg_shards_required"]
            self.options.egg_shards_available.value = self.passthrough["egg_shards_available"]
            self.options.required_bosses.value = self.passthrough["bosses_to_defeat"]
            if "hint_cost" in self.passthrough:
                self.options.hint_cost.value = self.passthrough["hint_cost"]
            elif "location_check_points" in self.passthrough:
                self.options.hint_cost.value = self.passthrough["location_check_points"]
            self.options.structure_compasses.value = self.passthrough["structure_compasses"]
            self.options.combat_difficulty.value = self.passthrough["combat_difficulty"]
            self.options.death_link.value = self.passthrough["death_link"]
            self.options.immediate_respawn.value = self.passthrough["immediate_respawn"]
            if "reach_restriction" in self.passthrough:
                self.options.reach_restriction.value = self.passthrough["reach_restriction"]
            if "hp_restriction" in self.passthrough:
                self.options.hp_restriction.value = self.passthrough["hp_restriction"]
            if "hunger_restriction" in self.passthrough:
                self.options.hunger_restriction.value = self.passthrough["hunger_restriction"]
            if "world_border_restriction" in self.passthrough:
                self.options.world_border_restriction.value = self.passthrough["world_border_restriction"]
            if "structure_restriction" in self.passthrough:
                self.options.structure_restriction.value = self.passthrough["structure_restriction"]
            if "structure_unlock_shuffle" in self.passthrough:
                self.options.structure_unlock_shuffle.value = self.passthrough["structure_unlock_shuffle"]
            if "inventory_slot_restriction" in self.passthrough:
                self.options.inventory_slot_restriction.value = self.passthrough["inventory_slot_restriction"]
            if "offhand_restriction" in self.passthrough:
                self.options.offhand_restriction.value = self.passthrough["offhand_restriction"]
            if "curses_enabled" in self.passthrough:
                self.options.curses_enabled.value = self.passthrough["curses_enabled"]
            if "monster_trap_percentage" in self.passthrough:
                self.options.monster_trap_percentage.value = self.passthrough["monster_trap_percentage"]
            elif "mob_trap_percentage" in self.passthrough:
                self.options.monster_trap_percentage.value = self.passthrough["mob_trap_percentage"]
            if "recipe_tier_bias" in self.passthrough:
                self.options.recipe_tier_bias.value = self.passthrough["recipe_tier_bias"]
            if "recipe_unlock_mode" in self.passthrough:
                self.options.recipe_unlock_mode.value = self.passthrough["recipe_unlock_mode"]
            if "recipe_tier_threshold" in self.passthrough:
                self.options.recipe_tier_threshold.value = self.passthrough["recipe_tier_threshold"]
            elif "recipe_tier_unlock_percentage" in self.passthrough:
                self.options.recipe_tier_threshold.value = self.passthrough["recipe_tier_unlock_percentage"]
            if "wandering_trader_trades" in self.passthrough:
                self.options.wandering_trader_trades.value = self.passthrough["wandering_trader_trades"]
            if "starting_shared_chest" in self.passthrough:
                self.options.starting_shared_chest.value = self.passthrough["starting_shared_chest"]
            if "advancement_type" in self.passthrough:
                self.options.advancement_type.value = self.passthrough["advancement_type"]
            if "bacap_advancement_step" in self.passthrough:
                self.options.bacap_advancement_step.value = self.passthrough["bacap_advancement_step"]
            if "biome_checks" in self.passthrough:
                self.options.biome_checks.value = self.passthrough["biome_checks"]
            if "structure_checks" in self.passthrough:
                self.options.structure_checks.value = self.passthrough["structure_checks"]
            if "selected_bacap_advancements" in self.passthrough:
                self.selected_bacap_advancements = self.passthrough["selected_bacap_advancements"]
            if "selected_bacap_map" in self.passthrough:
                self.selected_bacap_map = self.passthrough["selected_bacap_map"]
        else:
            self.using_ut = False


    def create_item(self, name: str) -> Item:
        item_class = ItemClassification.filler
        if name in Constants.item_info["progression_items"]:
            item_class |= ItemClassification.progression
        if name in Constants.item_info["useful_items"]:
            item_class |= ItemClassification.useful
        if name in Constants.item_info["trap_items"]:
            item_class |= ItemClassification.trap

        return MinecraftItem(name, item_class, self.item_name_to_id.get(name, None), self.player)

    def create_event(self, region_name: str, event_name: str) -> None:
        region = self.multiworld.get_region(region_name, self.player)
        loc = MinecraftLocation(self.player, event_name, None, region)
        loc.place_locked_item(self.create_event_item(event_name))
        region.locations.append(loc)

    def create_event_item(self, name: str) -> Item:
        item = self.create_item(name)
        item.classification = ItemClassification.progression
        return item

    def _create_random_bacap_locations(self) -> None:
        # 1. Calculate total required locked items (slots that need unlocking)
        total_locked_slots = 77  # Recipe Unlocks
        if self.options.hp_restriction:
            total_locked_slots += 7
        if self.options.hunger_restriction:
            total_locked_slots += 6
        if self.options.reach_restriction:
            total_locked_slots += 3
        if self.options.world_border_restriction:
            total_locked_slots += 5
        if self.options.structure_restriction:
            total_locked_slots += 8
        if self.options.offhand_restriction:
            total_locked_slots += 1
        if self.options.inventory_slot_restriction:
            total_locked_slots += 27
        if self.options.curses_enabled:
            total_locked_slots += 2
        if self.options.structure_compasses:
            compasses = [name for name in self.item_name_to_id if "Structure Compass" in name]
            total_locked_slots += len(compasses)
        if self.options.egg_shards_required > 0 and "Dragon Egg Shard" in self.item_name_to_id:
            total_locked_slots += self.options.egg_shards_available.value

        # 2. Add extra slots for traps according to Trap Percentage (bee_traps)
        trap_pct = getattr(self.options, "bee_traps", None)
        trap_percent_val = trap_pct.value if trap_pct else 0
        trap_count = ceil(total_locked_slots * (trap_percent_val / 100.0)) if trap_percent_val > 0 else 0
        total_needed_slots = total_locked_slots + trap_count

        # 3. Other check tasks (traders, biomes, structures)
        other_check_tasks = self.options.wandering_trader_trades.value
        if self.options.biome_checks.value:
            other_check_tasks += len(Constants.all_biome_names)
        if self.options.structure_checks.value:
            other_check_tasks += len(Constants.STRUCTURE_REGION_MAPPING)

        # 4. Required random BACAP count
        needed_count = total_needed_slots - other_check_tasks
        if needed_count <= 0:
            needed_count = max(10, self.options.check_goal.value)

        # 5. Filter candidates (exclude all structures and biomes to prevent world expansion / structure softlocks)
        candidates = list(Constants.bacap_non_structure_biome_advancements)

        needed_count = min(needed_count, len(candidates))
        sampled_advancements = self.random.sample(candidates, needed_count)

        self.selected_bacap_advancements = [adv["id"] for adv in sampled_advancements]
        self.selected_bacap_map = {adv["id"]: adv["location_id"] for adv in sampled_advancements}

        for adv in sampled_advancements:
            region_name = adv.get("region", "Overworld")
            region = self.multiworld.get_region(region_name, self.player)
            loc = MinecraftLocation(self.player, adv["loc_name"], adv["location_id"], region)
            region.locations.append(loc)

    def create_regions(self) -> None:
        # Create regions
        for region_name, exits in Constants.region_info["regions"]:
            r = Region(region_name, self.player, self.multiworld)
            for exit_name in exits:
                r.exits.append(Entrance(self.player, exit_name, r))
            self.multiworld.regions.append(r)

        # Bind mandatory connections
        for entr_name, region_name in Constants.region_info["mandatory_connections"]:
            e = self.multiworld.get_entrance(entr_name, self.player)
            r = self.multiworld.get_region(region_name, self.player)
            e.connect(r)

        # Add locations
        if self.options.advancement_type.value == 0:  # Vanilla
            for region_name, locations in Constants.location_info["locations_by_region"].items():
                region = self.multiworld.get_region(region_name, self.player)
                for loc_name in locations:
                    loc = MinecraftLocation(self.player, loc_name,
                        self.location_name_to_id.get(loc_name, None), region)
                    region.locations.append(loc)
        elif self.options.advancement_type.value == 1:  # BACAP Milestone
            overworld_region = self.multiworld.get_region("Overworld", self.player)
            step = max(1, self.options.bacap_advancement_step.value)
            bacap_count = min(Constants.TOTAL_BACAP_ADVANCEMENTS // step, Constants.MAX_BACAP_MILESTONES)
            for i in range(1, bacap_count + 1):
                loc_name = f"BACAP Milestone {i}"
                loc = MinecraftLocation(self.player, loc_name,
                    self.location_name_to_id.get(loc_name, None), overworld_region)
                overworld_region.locations.append(loc)
        else:  # Random BACAP Achievements (option_random_bacap = 2)
            self._create_random_bacap_locations()

        # Add wandering trader trade locations
        overworld_region = self.multiworld.get_region("Overworld", self.player)
        for i in range(1, self.options.wandering_trader_trades.value + 1):
            loc_name = f"Wandering Trader Trade {i}"
            loc = MinecraftLocation(self.player, loc_name,
                self.location_name_to_id.get(loc_name, None), overworld_region)
            overworld_region.locations.append(loc)

        # Add exploration biome locations
        if self.options.biome_checks.value:
            for region_name, biome_locs in Constants.BIOMES_BY_REGION.items():
                region = self.multiworld.get_region(region_name, self.player)
                for loc_name in biome_locs:
                    loc = MinecraftLocation(self.player, loc_name,
                        self.location_name_to_id.get(loc_name, None), region)
                    region.locations.append(loc)

        # Add exploration structure locations
        if self.options.structure_checks.value:
            for loc_name, region_name in Constants.STRUCTURE_REGION_MAPPING.items():
                region = self.multiworld.get_region(region_name, self.player)
                loc = MinecraftLocation(self.player, loc_name,
                    self.location_name_to_id.get(loc_name, None), region)
                region.locations.append(loc)


        # Add events
        self.create_event("Nether Fortress", "Blaze Rods")
        self.create_event("The End", "Ender Dragon")
        self.create_event("Nether Fortress", "Wither")

        # Shuffle the connections
        shuffle_structures(self)

    def create_items(self) -> None:
        self.multiworld.itempool += build_item_pool(self)

    set_rules = set_rules

    def generate_output(self, output_directory: str) -> None:
        data = self._get_mc_data()
        filename = self.multiworld.get_out_file_name_base(self.player) + MinecraftContainer.patch_file_ending

        container = MinecraftContainer(data,
                                       filename,
                                       os.path.join(output_directory, filename),
                                       self.player,
                                       self.multiworld.get_file_safe_player_name(self.player),
                                       )
        container.write()

    def fill_slot_data(self) -> dict:
        return self._get_mc_data()

    def get_filler_item_name(self) -> str:
        return get_junk_item_names(self.random, 1)[0]

    # For UT
    @staticmethod
    def interpret_slot_data(slot_data: dict[str, Any]) -> dict[str, Any]:
        return slot_data


class MinecraftLocation(Location):
    game = "minecraft_u2vr"

class MinecraftItem(Item):
    game = "minecraft_u2vr"


def mc_update_output(raw_data, server, port):
    data = json.loads(b64decode(raw_data))
    data['server'] = server
    data['port'] = port
    return b64encode(bytes(json.dumps(data), 'utf-8'))
