from math import ceil
from typing import List

from BaseClasses import Item

from . import Constants
from typing import TYPE_CHECKING

if TYPE_CHECKING:
	from . import MinecraftWorld


def get_junk_item_names(rand, k: int) -> List[str]:
	junk_weights = Constants.item_info.get("junk_weights", {"Random Trap": 1})
	if not junk_weights or k <= 0:
		return ["Random Trap"] * k
	junk = rand.choices(
		list(junk_weights.keys()),
		weights=list(junk_weights.values()),
		k=k)
	return junk


def build_item_pool(world: "MinecraftWorld") -> List[Item]:
	multiworld = world.multiworld
	player = world.player

	itempool = []
	total_location_count = len(multiworld.get_unfilled_locations(player))

	# 1. Base recipe unlocks (77 recipe nodes in the U2VR recipe tree)
	recipe_unlock_count = 77
	itempool += [world.create_item("Recipe Unlock") for _ in range(recipe_unlock_count)]

	# 2. HP Restriction: 7 upgrades (+1 heart each, from 3 hearts to 10 hearts)
	if world.options.hp_restriction:
		itempool += [world.create_item("HP Upgrade") for _ in range(7)]

	# 3. Hunger Restriction: 6 upgrades (+1 drumstick each, from 4 to 10 drumsticks)
	if world.options.hunger_restriction:
		itempool += [world.create_item("Hunger Upgrade") for _ in range(6)]

	# 4. Reach Restriction: 3 upgrades (2.5 -> 3.0 -> 3.5 -> 4.5 blocks)
	if world.options.reach_restriction:
		itempool += [world.create_item("Reach Upgrade") for _ in range(3)]

	# 5. World Border Restriction: 5 upgrades (1500 -> 2000 -> 3000 -> 4500 -> 60000000)
	if world.options.world_border_restriction:
		itempool += [world.create_item("World Border Upgrade") for _ in range(5)]

	# 6. Structure Restriction: 8 structure unlocks
	if world.options.structure_restriction:
		itempool += [world.create_item("Structure Unlock") for _ in range(8)]

	# 7. Offhand Restriction: 1 offhand unlock
	if world.options.offhand_restriction:
		itempool += [world.create_item("Offhand Unlock") for _ in range(1)]

	# 8. Inventory Slot Restriction: 27 inventory slot upgrades (slots 9 to 35)
	if world.options.inventory_slot_restriction:
		itempool += [world.create_item("Inventory Slot Upgrade") for _ in range(27)]

	# 9. Permanent Curses: 2 curse items (daylight monster ignition & lightless mob spawning)
	if world.options.curses_enabled:
		itempool.append(world.create_item("Disable Day Monster Ignition"))
		itempool.append(world.create_item("Disable Light Check for Monster Spawn"))

	# 10. Structure Compasses
	if world.options.structure_compasses:
		compasses = [name for name in world.item_name_to_id if "Structure Compass" in name]
		for item_name in compasses:
			itempool.append(world.create_item(item_name))

	# 11. Dragon egg shards (if enabled and present in item pool)
	if world.options.egg_shards_required > 0 and "Dragon Egg Shard" in world.item_name_to_id:
		num = world.options.egg_shards_available
		itempool += [world.create_item("Dragon Egg Shard") for _ in range(num)]

	# 12. Traps & Junk filler for remaining unfilled locations
	remaining = total_location_count - len(itempool)
	if remaining > 0:
		junk = get_junk_item_names(world.random, remaining)
		itempool += [world.create_item(name) for name in junk]

	# If item pool exceeds total locations (e.g. custom reduced locations), truncate safely
	if len(itempool) > total_location_count:
		itempool = itempool[:total_location_count]

	return itempool
