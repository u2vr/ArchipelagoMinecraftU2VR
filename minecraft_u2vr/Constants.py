import json
import pkgutil


def load_data_file(*args) -> dict:
    fname = "/".join(["data", *args])
    return json.loads(pkgutil.get_data(__name__, fname).decode())


item_info = load_data_file("items.json")
item_name_to_id = {name: index \
                   for index, name in enumerate(item_info["all_items"], start=1)}

location_info = load_data_file("locations.json")
location_name_to_id = {name: index \
                       for index, name in enumerate(location_info["all_locations"], start=1)}

TOTAL_BACAP_ADVANCEMENTS = 1200
BACAP_LOCATION_START_ID = 201
MAX_BACAP_MILESTONES = 300
for i in range(1, MAX_BACAP_MILESTONES + 1):
    location_name_to_id[f"BACAP Milestone {i}"] = BACAP_LOCATION_START_ID + i - 1

# Exploration Biomes (501..566)
OVERWORLD_BIOME_NAMES = [
    "Explore Plains", "Explore Sunflower Plains", "Explore Snowy Plains", "Explore Ice Spikes",
    "Explore Desert", "Explore Swamp", "Explore Mangrove Swamp", "Explore Forest",
    "Explore Flower Forest", "Explore Birch Forest", "Explore Dappled Forest", "Explore Dark Forest",
    "Explore Pale Garden", "Explore Old Growth Birch Forest", "Explore Old Growth Pine Taiga",
    "Explore Old Growth Spruce Taiga", "Explore Taiga", "Explore Snowy Taiga", "Explore Savanna",
    "Explore Savanna Plateau", "Explore Windswept Hills", "Explore Windswept Gravelly Hills",
    "Explore Windswept Forest", "Explore Windswept Savanna", "Explore Jungle", "Explore Sparse Jungle",
    "Explore Bamboo Jungle", "Explore Badlands", "Explore Eroded Badlands", "Explore Wooded Badlands",
    "Explore Meadow", "Explore Cherry Grove", "Explore Grove", "Explore River", "Explore Frozen River",
    "Explore Beach", "Explore Snowy Beach", "Explore Stony Shore", "Explore Warm Ocean",
    "Explore Lukewarm Ocean", "Explore Deep Lukewarm Ocean", "Explore Ocean", "Explore Deep Ocean",
    "Explore Cold Ocean", "Explore Deep Cold Ocean", "Explore Frozen Ocean", "Explore Deep Frozen Ocean",
    "Explore Mushroom Fields", "Explore Dripstone Caves", "Explore Lush Caves", "Explore Deep Dark",
    "Explore Sulfur Caves", "Explore Snowy Slopes", "Explore Frozen Peaks", "Explore Jagged Peaks",
    "Explore Stony Peaks"
]

NETHER_BIOME_NAMES = [
    "Explore Nether Wastes", "Explore Warped Forest", "Explore Crimson Forest",
    "Explore Soul Sand Valley", "Explore Basalt Deltas"
]

END_BIOME_NAMES = [
    "Explore The End", "Explore End Highlands", "Explore End Midlands",
    "Explore Small End Islands", "Explore End Barrens"
]

BIOME_LOCATION_START_ID = 501
all_biome_names = OVERWORLD_BIOME_NAMES + NETHER_BIOME_NAMES + END_BIOME_NAMES
for idx, name in enumerate(all_biome_names):
    location_name_to_id[name] = BIOME_LOCATION_START_ID + idx

BIOMES_BY_REGION = {
    "Overworld": OVERWORLD_BIOME_NAMES,
    "The Nether": NETHER_BIOME_NAMES,
    "The End": END_BIOME_NAMES
}

# Exploration Structures (601..621)
STRUCTURE_REGION_MAPPING = {
    "Explore Village": "Village",
    "Explore Pillager Outpost": "Pillager Outpost",
    "Explore Mineshaft": "Overworld",
    "Explore Woodland Mansion": "Woodland Mansion",
    "Explore Jungle Temple": "Overworld",
    "Explore Desert Pyramid": "Overworld",
    "Explore Igloo": "Overworld",
    "Explore Shipwreck": "Overworld",
    "Explore Swamp Hut": "Overworld",
    "Explore Stronghold": "Overworld",
    "Explore Ocean Monument": "Ocean Monument",
    "Explore Ocean Ruin": "Overworld",
    "Explore Nether Fortress": "Nether Fortress",
    "Explore Nether Fossil": "The Nether",
    "Explore End City": "End City",
    "Explore Buried Treasure": "Overworld",
    "Explore Bastion Remnant": "Bastion Remnant",
    "Explore Ruined Portal": "Overworld",
    "Explore Ancient City": "Ancient City",
    "Explore Trail Ruins": "Trail Ruins",
    "Explore Trial Chambers": "Trial Chambers"
}

STRUCTURE_LOCATION_START_ID = 601
for idx, name in enumerate(STRUCTURE_REGION_MAPPING.keys()):
    location_name_to_id[name] = STRUCTURE_LOCATION_START_ID + idx

# Random BACAP Advancements (1001..2200)
BACAP_RANDOM_LOCATION_START_ID = 1001
bacap_advancements_info = load_data_file("bacap_advancements.json")
BACAP_ADVANCEMENT_BY_ID = {}
BACAP_ADVANCEMENT_BY_NAME = {}
for idx, entry in enumerate(bacap_advancements_info):
    loc_id = BACAP_RANDOM_LOCATION_START_ID + idx
    loc_name = entry["loc_name"]
    location_name_to_id[loc_name] = loc_id
    entry["location_id"] = loc_id
    BACAP_ADVANCEMENT_BY_ID[entry["id"]] = entry
    BACAP_ADVANCEMENT_BY_NAME[loc_name] = entry

bacap_non_structure_biome_advancements = [
    entry for entry in bacap_advancements_info
    if not entry.get("is_structure_or_biome", False)
]

exclusion_info = load_data_file("excluded_locations.json")

region_info = load_data_file("regions.json")


