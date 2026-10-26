import os
import io
import json
import zipfile
try:
    import settings
except ImportError:
    settings = None
import typing
from math import ceil
from base64 import b64encode, b64decode
from typing import Dict, Any, Tuple

from BaseClasses import Region, Entrance, Item, Tutorial, ItemClassification, Location
from worlds.AutoWorld import World, WebWorld
from worlds.LauncherComponents import Component, components, Type, SuffixIdentifier

from . import Constants
from .Container import MinecraftContainer
from .Options import MinecraftOptions, option_groups
from .Structures import shuffle_structures
from .ItemPool import build_item_pool, get_junk_item_names
from .Rules import set_rules
try:
    from ..LauncherComponents import icon_paths
except (ImportError, ValueError):
    try:
        from LauncherComponents import icon_paths
    except (ImportError, ValueError):
        icon_paths = {}

client_version = 11

icon_paths['mcicon'] = f"ap:{__name__}/assets/mcicon.png"

# register client
def launch_client(*args):
    from .MinecraftClient import launch_subprocess
    launch_subprocess(*args)

components.append(
    Component(
        "Minecraft with BACAP Client",
        icon="mcicon",
        func=launch_client,
        component_type=Type.CLIENT,
        file_identifier=SuffixIdentifier('.apmc'),
    )
)

# register yaml configurator
def launch_configurator(*args):
    from .YamlConfigurator import launch
    launch(*args)

components.append(
    Component(
        "Minecraft with BACAP yaml configurator",
        icon="mcicon",
        func=launch_configurator,
        component_type=getattr(Type, "TOOL", getattr(Type, "MISC", Type.CLIENT)),
        description="Interactive YAML configuration tool with live checks and items statistics",
    )
)


if settings is None:
    class _DummyGroup:
        pass
    class _DummyFolder(str):
        pass
    class _DummyFile(str):
        pass
    settings_Group = _DummyGroup
    settings_OptionalUserFolderPath = _DummyFolder
    settings_OptionalUserFilePath = _DummyFile
else:
    settings_Group = settings.Group
    settings_OptionalUserFolderPath = settings.OptionalUserFolderPath
    settings_OptionalUserFilePath = settings.OptionalUserFilePath


class MinecraftSettings(settings_Group):
    class ForgeDirectory(settings_OptionalUserFolderPath):
        pass

    class ReleaseChannel(str):
        """
        release channel, currently "release", or "beta"
        any games played on the "beta" channel have a high likelihood of no longer working on the "release" channel.
        """

    class JavaExecutable(settings_OptionalUserFilePath):
        """
        Path to Java executable. If not set, will attempt to fall back to Java system installation.
        """

    class ServerDirectory(settings_OptionalUserFolderPath):
        """
        Path to local directory to install Java, Neo Forge, etc.
        """
        @classmethod
        def validate(cls, path: str):
            if os.path.exists(path) and not os.path.isdir(path):
                raise ValueError(f"'{path}' must be a folder")

    server_directory: ServerDirectory = ServerDirectory("Minecraft with BACAP AP Server Directory")
    max_heap_size: str = "2G"
    min_heap_size: str = "1G"
    release_channel: ReleaseChannel = ReleaseChannel("release")
    java: JavaExecutable | None = JavaExecutable(None)


class MinecraftWebWorld(WebWorld):
    game = "Minecraft with BACAP"
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
    game = "Minecraft with BACAP"
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
        "Compasses": set(Constants.ALL_STRUCTURE_COMPASS_NAMES) | {
            Constants.UNVISITED_STRUCTURES_COMPASS_NAME,
            Constants.UNVISITED_BIOMES_COMPASS_NAME,
        } | set(Constants.ALL_BIOME_COMPASS_NAMES),
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

    def _get_server_info(self) -> Tuple[str, int, str]:
        server_host = ""
        server_port = 0
        server_password = ""

        # 1. Multiworld attributes
        for attr in ("server_address", "server", "host", "hostname", "address"):
            val = getattr(self.multiworld, attr, None)
            if val and isinstance(val, str) and val.strip():
                server_host = val.strip()
                break

        for attr in ("server_port", "port"):
            val = getattr(self.multiworld, attr, None)
            if val and isinstance(val, int) and val > 0:
                server_port = val
                break

        for attr in ("server_password", "password"):
            val = getattr(self.multiworld, attr, None)
            if val:
                if isinstance(val, dict) and self.player in val:
                    server_password = str(val[self.player]).strip()
                    break
                elif isinstance(val, str) and val.strip():
                    server_password = val.strip()
                    break

        # 2. Raw player options (from YAML)
        raw_options = getattr(self.multiworld, "player_options", {}).get(self.player, {})
        if not server_host:
            for opt in ("server_address", "server", "host", "hostname", "address"):
                val = raw_options.get(opt)
                if val and isinstance(val, str) and val.strip():
                    server_host = val.strip()
                    break

        if not server_port:
            for opt in ("server_port", "port"):
                val = raw_options.get(opt)
                if val:
                    try:
                        server_port = int(val)
                        break
                    except (ValueError, TypeError):
                        pass

        if not server_password:
            for opt in ("password", "server_password"):
                val = raw_options.get(opt)
                if val and str(val).strip():
                    server_password = str(val).strip()
                    break

        # 3. Running in WebHost
        if not server_host:
            try:
                from WebHostLib import app
                host_addr = app.config.get("HOST_ADDRESS")
                if host_addr and str(host_addr).strip():
                    server_host = str(host_addr).strip()
            except Exception:
                pass

        # 4. Archipelago settings / host.yaml
        try:
            import Utils
            settings_obj = Utils.get_settings()
            server_opts = getattr(settings_obj, "server_options", None)
            if server_opts:
                if not server_host:
                    h = getattr(server_opts, "host_address", None) or getattr(server_opts, "host", None)
                    if h and str(h).strip() and str(h).strip().lower() not in ("null", "none", "0.0.0.0"):
                        server_host = str(h).strip()
                if not server_port:
                    p = getattr(server_opts, "port", None)
                    if p:
                        try:
                            server_port = int(p)
                        except (ValueError, TypeError):
                            pass
                if not server_password:
                    pwd = getattr(server_opts, "password", None) or getattr(server_opts, "server_password", None)
                    if pwd and str(pwd).strip() and str(pwd).strip().lower() not in ("null", "none"):
                        server_password = str(pwd).strip()
        except Exception:
            pass

        # 5. Environment variables
        if not server_host:
            for env in ("ARCHIPELAGO_SERVER", "AP_SERVER", "SERVER_ADDRESS", "HOST_ADDRESS", "ARCHIPELAGO_HOST"):
                val = os.environ.get(env)
                if val and val.strip():
                    server_host = val.strip()
                    break

        if not server_port:
            for env in ("ARCHIPELAGO_PORT", "AP_PORT", "SERVER_PORT"):
                val = os.environ.get(env)
                if val:
                    try:
                        server_port = int(val.strip())
                        break
                    except (ValueError, TypeError):
                        pass

        if not server_password:
            for env in ("ARCHIPELAGO_PASSWORD", "AP_PASSWORD", "SERVER_PASSWORD"):
                val = os.environ.get(env)
                if val and val.strip():
                    server_password = val.strip()
                    break

        # If server_host contains port, split it
        if ":" in server_host:
            parts = server_host.rsplit(":", 1)
            try:
                server_host = parts[0]
                server_port = int(parts[1])
            except ValueError:
                pass

        return server_host, server_port, server_password

    def _get_mc_data(self) -> Dict[str, Any]:
        exits = [connection[0] for connection in Constants.region_info["default_connections"]]
        server_host, server_port, server_password = self._get_server_info()

        world_random = random.Random(f"{self.multiworld.seed}_mc_world_seed")
        world_seed = world_random.getrandbits(32)

        return {
            # Mod data
            'game': self.game,
            'server': server_host,
            'port': server_port,
            'password': server_password,
            'world_seed': world_seed,
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
            'cheats_enabled': bool(self.options.cheats_enabled.value),

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
            'bacap_check_count': self._get_bacap_milestone_count(),
            'major_biome_checks': bool(self.options.major_biome_checks.value),
            'small_biome_checks': bool(self.options.small_biome_checks.value),
            'endgame_biome_checks': bool(self.options.endgame_biome_checks.value),
            'biome_checks': bool(self.options.major_biome_checks.value or self.options.small_biome_checks.value or self.options.endgame_biome_checks.value),
            'structure_checks': bool(self.options.structure_checks.value),
            'unvisited_structures_compass': bool(self.options.unvisited_structures_compass.value),
            'structure_compasses': bool(self.options.structure_compasses.value),
            'unvisited_biomes_compass': bool(self.options.unvisited_biomes_compass.value),
            'major_biomes_compasses': bool(self.options.major_biomes_compasses.value),
            'minor_biomes_compasses': bool(self.options.minor_biomes_compasses.value),
            'extra_abilities': bool(self.options.extra_abilities.value),
            'hint_cost': 10,
            'location_check_points': 10,
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
        re_gen_passthrough = getattr(self.multiworld, "re_gen_passthrough", {})
        if re_gen_passthrough and self.game in re_gen_passthrough:
            self.using_ut = True
            self.passthrough = re_gen_passthrough[self.game]
            goal_val = self.passthrough.get("check_goal", self.passthrough.get("advancement_goal", 40))
            self.options.check_goal.value = goal_val
            self.options.egg_shards_required.value = self.passthrough["egg_shards_required"]
            self.options.egg_shards_available.value = self.passthrough["egg_shards_available"]
            self.options.required_bosses.value = self.passthrough["bosses_to_defeat"]
            if "structure_compasses" in self.passthrough:
                self.options.structure_compasses.value = self.passthrough["structure_compasses"]
            if "unvisited_structures_compass" in self.passthrough:
                self.options.unvisited_structures_compass.value = self.passthrough["unvisited_structures_compass"]
            if "unvisited_biomes_compass" in self.passthrough:
                self.options.unvisited_biomes_compass.value = self.passthrough["unvisited_biomes_compass"]
            if "major_biomes_compasses" in self.passthrough:
                self.options.major_biomes_compasses.value = self.passthrough["major_biomes_compasses"]
            if "minor_biomes_compasses" in self.passthrough:
                self.options.minor_biomes_compasses.value = self.passthrough["minor_biomes_compasses"]
            self.options.combat_difficulty.value = self.passthrough["combat_difficulty"]
            self.options.death_link.value = self.passthrough["death_link"]
            self.options.immediate_respawn.value = self.passthrough["immediate_respawn"]
            if "cheats_enabled" in self.passthrough:
                self.options.cheats_enabled.value = self.passthrough["cheats_enabled"]
            if "extra_abilities" in self.passthrough:
                self.options.extra_abilities.value = self.passthrough["extra_abilities"]
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
            if "major_biome_checks" in self.passthrough:
                self.options.major_biome_checks.value = self.passthrough["major_biome_checks"]
            if "small_biome_checks" in self.passthrough:
                self.options.small_biome_checks.value = self.passthrough["small_biome_checks"]
            if "endgame_biome_checks" in self.passthrough:
                self.options.endgame_biome_checks.value = self.passthrough["endgame_biome_checks"]
            elif "end_biome_checks" in self.passthrough:
                self.options.endgame_biome_checks.value = self.passthrough["end_biome_checks"]
            elif "biome_checks" in self.passthrough:
                val = bool(self.passthrough["biome_checks"])
                self.options.major_biome_checks.value = val
                self.options.small_biome_checks.value = val
                self.options.endgame_biome_checks.value = False
            if "structure_checks" in self.passthrough:
                self.options.structure_checks.value = self.passthrough["structure_checks"]
            if "selected_bacap_advancements" in self.passthrough:
                self.selected_bacap_advancements = self.passthrough["selected_bacap_advancements"]
            if "selected_bacap_map" in self.passthrough:
                self.selected_bacap_map = self.passthrough["selected_bacap_map"]
        else:
            self.using_ut = False


    def create_item(self, name: str) -> Item:
        if name in Constants.item_info["progression_items"]:
            item_class = ItemClassification.progression
        elif name in Constants.item_info["useful_items"]:
            item_class = ItemClassification.useful
        elif name in Constants.item_info["trap_items"]:
            item_class = ItemClassification.trap
        else:
            item_class = ItemClassification.filler

        if name == "Dragon Egg Shard" and self.options.egg_shards_required > 0:
            item_class = ItemClassification.progression

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

    def _calculate_total_needed_slots(self) -> int:
        total_locked_slots = 78  # Recipe Unlocks
        if self.options.hp_restriction:
            total_locked_slots += 7
        if self.options.hunger_restriction:
            total_locked_slots += 6
        if self.options.reach_restriction:
            total_locked_slots += 3
        if self.options.world_border_restriction:
            total_locked_slots += 5
        if self.options.structure_restriction:
            total_locked_slots += 9
        if self.options.offhand_restriction:
            total_locked_slots += 1
        if self.options.inventory_slot_restriction:
            total_locked_slots += 27
        if self.options.curses_enabled:
            total_locked_slots += 2
        if self.options.unvisited_structures_compass:
            total_locked_slots += 1
        if self.options.structure_compasses:
            total_locked_slots += len(Constants.ALL_STRUCTURE_COMPASS_NAMES)
        if self.options.unvisited_biomes_compass:
            total_locked_slots += 1
        if self.options.major_biomes_compasses:
            total_locked_slots += len(Constants.MAJOR_BIOME_COMPASS_NAMES)
        if self.options.minor_biomes_compasses:
            total_locked_slots += len(Constants.MINOR_BIOME_COMPASS_NAMES)
        if getattr(self.options, "extra_abilities", False):
            total_locked_slots += 12
        if self.options.egg_shards_required > 0 and "Dragon Egg Shard" in self.item_name_to_id:
            total_locked_slots += self.options.egg_shards_available.value

        trap_pct = getattr(self.options, "bee_traps", None)
        trap_percent_val = trap_pct.value if trap_pct else 0
        trap_count = ceil(total_locked_slots * (trap_percent_val / 100.0)) if trap_percent_val > 0 else 0
        return total_locked_slots + trap_count

    def _get_other_check_tasks(self) -> int:
        other_check_tasks = self.options.wandering_trader_trades.value
        if self.options.major_biome_checks.value:
            other_check_tasks += len(Constants.MAJOR_BIOME_NAMES)
        if self.options.small_biome_checks.value:
            other_check_tasks += len(Constants.SMALL_BIOME_NAMES)
        if self.options.endgame_biome_checks.value:
            other_check_tasks += len(Constants.ENDGAME_BIOME_NAMES)
        if self.options.structure_checks.value:
            other_check_tasks += len(Constants.STRUCTURE_REGION_MAPPING)
        return other_check_tasks

    def _get_bacap_milestone_count(self) -> int:
        step = max(1, self.options.bacap_advancement_step.value)
        max_possible = min(Constants.TOTAL_BACAP_ADVANCEMENTS // step, Constants.MAX_BACAP_MILESTONES)
        total_needed = self._calculate_total_needed_slots()
        other_checks = self._get_other_check_tasks()
        needed_milestones = total_needed - other_checks
        if needed_milestones <= 0:
            return 0
        return min(needed_milestones, max_possible)

    def _create_random_bacap_locations(self) -> None:
        total_needed_slots = self._calculate_total_needed_slots()
        other_check_tasks = self._get_other_check_tasks()

        needed_count = total_needed_slots - other_check_tasks
        if needed_count <= 0:
            needed_count = max(10, self.options.check_goal.value)

        # Filter candidates (exclude all structures and biomes to prevent world expansion / structure softlocks)
        candidates = list(Constants.bacap_non_structure_biome_advancements)
        candidates.sort(key=lambda adv: adv["id"])

        needed_count = min(needed_count, len(candidates))
        bacap_random = random.Random(f"{self.multiworld.seed}_random_bacap")
        sampled_advancements = sorted(bacap_random.sample(candidates, needed_count), key=lambda adv: adv["id"])

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
            bacap_count = self._get_bacap_milestone_count()
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
        active_biome_names = set()
        if self.options.major_biome_checks.value:
            active_biome_names.update(Constants.MAJOR_BIOME_NAMES)
        if self.options.small_biome_checks.value:
            active_biome_names.update(Constants.SMALL_BIOME_NAMES)
        if self.options.endgame_biome_checks.value:
            active_biome_names.update(Constants.ENDGAME_BIOME_NAMES)

        if active_biome_names:
            for region_name, biome_locs in Constants.BIOMES_BY_REGION.items():
                region = self.multiworld.get_region(region_name, self.player)
                for loc_name in biome_locs:
                    if loc_name in active_biome_names:
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
        server_val = data.get('server', '') or ''
        port_val = data.get('port', 0) or 0
        if port_val and ':' not in server_val:
            server_str = f"{server_val}:{port_val}" if server_val else ""
        else:
            server_str = server_val
        password_val = data.get('password', '') or ''

        container = MinecraftContainer(data,
                                       filename,
                                       os.path.join(output_directory, filename),
                                       self.player,
                                       self.multiworld.get_file_safe_player_name(self.player),
                                       server=server_str,
                                       password=password_val,
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
    game = "Minecraft with BACAP"

class MinecraftItem(Item):
    game = "Minecraft with BACAP"


def mc_update_output(raw_data, server, port, password=None, *args, **kwargs):
    if isinstance(raw_data, (bytes, bytearray)) and raw_data[:2] == b"PK":
        bio_in = io.BytesIO(raw_data)
        bio_out = io.BytesIO()
        server_str = f"{server}:{port}" if port and ":" not in str(server) else str(server)
        with zipfile.ZipFile(bio_in, "r") as z_in, zipfile.ZipFile(bio_out, "w") as z_out:
            for item in z_in.infolist():
                content = z_in.read(item)
                if item.filename == "archipelago.json":
                    try:
                        manifest = json.loads(content.decode("utf-8"))
                        manifest["server"] = server_str
                        if password is not None:
                            manifest["password"] = password
                        elif "password" in kwargs:
                            manifest["password"] = kwargs["password"]
                        content = json.dumps(manifest).encode("utf-8")
                    except Exception:
                        pass
                elif item.filename.endswith(".apmcmeta") or item.filename.endswith(".apmc"):
                    try:
                        meta = json.loads(content.decode("utf-8"))
                        meta["server"] = str(server)
                        if port:
                            meta["port"] = int(port)
                        if password is not None:
                            meta["password"] = password
                        elif "password" in kwargs:
                            meta["password"] = kwargs["password"]
                        content = json.dumps(meta).encode("utf-8")
                    except Exception:
                        pass
                z_out.writestr(item.filename, content, item.compress_type)
        return bio_out.getvalue()

    if isinstance(raw_data, bytes):
        try:
            data = json.loads(b64decode(raw_data).decode('utf-8'))
            is_b64 = True
        except Exception:
            data = json.loads(raw_data.decode('utf-8'))
            is_b64 = False
    elif isinstance(raw_data, str):
        try:
            data = json.loads(b64decode(raw_data.encode('utf-8')).decode('utf-8'))
            is_b64 = True
        except Exception:
            data = json.loads(raw_data)
            is_b64 = False
    else:
        data = raw_data
        is_b64 = False

    data['server'] = server
    data['port'] = port
    if password is not None:
        data['password'] = password
    elif 'password' in kwargs:
        data['password'] = kwargs['password']

    out_bytes = json.dumps(data).encode('utf-8')
    if is_b64:
        return b64encode(out_bytes)
    return out_bytes

minecraft_with_bacap_update_output = mc_update_output
minecraft_u2vr_update_output = mc_update_output
