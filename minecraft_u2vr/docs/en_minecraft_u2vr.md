# Minecraft (Archipelago U2VR)

## Where is the options page?

The [player options page for this game](../player-options) contains all the options you need to configure and export a
config file.

## What does randomization do to this game?

Minecraft U2VR introduces a rich recipe tree and modular restriction systems:
- **Recipe Progression**: Recipes across 7 tiers are locked behind the Recipe Tree. Receiving **Recipe Unlock** items unlocks recipes (with configurable tier bias).
- **Reach Distance Restriction**: Player mining and entity interaction distance starts restricted and is restored via **Reach Upgrades**.
- **HP Restriction**: Player health starts at 3 hearts (6 HP) and expands up to 10 hearts via **HP Upgrades**.
- **Hunger Restriction**: Player hunger capacity starts at 4 drumsticks (8 hunger) and expands via **Hunger Upgrades**.
- **World Border Restriction**: The world border starts at 1500 blocks and expands via **World Border Upgrades**.
- **Structure Restriction**: Entry into major structures (Villages, Nether Fortresses, Bastions, Ancient Cities, Trial Chambers, etc.) is blocked until unlocked via **Structure Unlocks**.
- **Inventory Slot Restriction**: Player inventory is restricted to only 9 hotbar slots initially. Receiving **Inventory Slot Upgrades** unlocks remaining slots up to 36.
- **Offhand Restriction**: The offhand slot is locked until the **Offhand Unlock** item is obtained.
- **Permanent Curses**: Adds difficulty modifiers (monsters do not burn in daylight and can spawn at any light level).
- **Craft Recipe Tier Bias Slider**: Adjusts recipe unlocking bias from fully random (0) to strictly tier by tier (100).
- **Wandering Trader Archipelago Trades**: Configurable number of Archipelago item trade checks (`wandering_trader_trades`). The Wandering Trader sells items from across the multiworld (any game) for emeralds.
- **End Bed Placement Restriction**: Placing and using beds in The End dimension is prohibited to prevent bed-bombing.
- **Traps**: Unfilled item pool slots can include **Random Traps** (bees, monsters, inventory junk, blindness, etc.).

## What is considered a location check in Minecraft?

Location checks are completed when the player completes various Minecraft advancements, or by purchasing Archipelago trades from the Wandering Trader. Opening the advancements menu
in-game by pressing "L" will display outstanding advancements.

## When the player receives an item, what happens?

When the player receives an item in Minecraft, it either unlocks crafting recipes in the Recipe Tree, expands player attributes/capabilities (health, hunger, reach, inventory, offhand, world border), grants structure access, or triggers traps.

## What is the victory condition?

Victory is achieved when the player kills the Ender Dragon, enters the portal in The End, and completes the credits
sequence either by skipping it or watching it play out.

Depending on configuration, victory may instead be achieved after defeating the Wither,
or after completing both conditions.
