🇷🇺 [Русская версия (ReadmeRus.md)](ReadmeRus.md)

# Minecraft Randomizer Setup Guide

Mod-Rework by U2VR (with AI), MjKey 

NeoForge Rewrite by qixils (Lexi), coehlrich, MrRedstone54 (Red)

Original (Forge) Implementation by KonoTyran.

## Rework Features & Changes

### Victory Conditions
- Required location checks goal (`check_goal`) combined with defeating bosses: **Ender Dragon**, **Wither**, or **Both** (or None).

### Location Checks
- **Advancements**:
  - **Vanilla Advancements**: Each vanilla advancement acts as an individual check (up to 138 checks).
  - **BACAP Milestones**: Checks unlocked every *N* completed BACAP advancements (configurable step from 1 to 20, default 5, up to 300 milestone checks).
  - **Random BACAP**: Checks tied to randomly selected, balanced advancements from the BlazeandCave's Advancements Pack (excluding softlock-prone locations).
- **Biome Exploration**: Checks awarded upon discovering unique biomes across the Overworld, Nether, and The End (major, small/micro, and endgame biomes).
- **Structure Discovery**: Checks for discovering 22 major unique world structures (Villages, Fortresses, Ancient Cities, Trial Chambers, etc.).
- **Wandering Trader Trades**: Configurable number of Archipelago item trade checks (0–20 checks) sold by the Wandering Trader for emeralds.

### Progressive Restrictions & Upgrades
- **Reach Distance**: Starts restricted at 2.5 blocks and expands via 3 **Reach Upgrades** up to 4.5 blocks.
- **Health (HP)**: Starts restricted at 3 hearts (6 HP) and expands via 7 **HP Upgrades** up to 10 hearts.
- **Hunger**: Starts restricted at 4 drumsticks (8 hunger points) and expands via 6 **Hunger Upgrades** up to 10 drumsticks.
- **World Border**: Expands the Overworld border in stages (diameters: 1000 -> 1800 -> 3000 -> 5000 -> 8000 -> max) using 5 **World Border Upgrades**.
- **Structure Locks**: Access to major world structures is sealed (repelling players) until unlocked via 9 **Structure Unlocks** (sequential or randomized order).
- **Inventory Slots**: Only the 9 hotbar slots are available initially. Remaining 27 main inventory slots (9–35) are locked and unlocked one by one via **Inventory Slot Upgrades**.
- **Offhand Slot**: The offhand slot is disabled until obtaining the **Offhand Unlock** item.
- **Locked Crafting Recipes**: Crafting recipes are locked behind the Recipe Tree.

### Recipe Progression Modes
1. **Tier Bias Mode**: Recipes unlock randomly with configurable tier bias (`craft_tier_bias`: 0 = pure random, 1–99 = weighted bias towards lower tiers first, 100 = strictly tier-by-tier).
2. **Skill Tree Interface**: Receiving a **Recipe Unlock** grants a point to choose and unlock desired recipes directly in an interactive Recipe Tree GUI (accessible via inventory button or hotkey).

### Traps & Permanent Curses
- **Situational Traps**:
  - Angry Bees Spawn
  - Skeleton Ambush
  - Zombie Horde
  - Inventory Junk (clutters free slots with debris)
  - Blindness
  - Falling Sand Trap
- **Permanent Curses (Modifiers)**:
  - *Daylight Monster Burning Disabled*: Zombies, skeletons, and phantoms no longer ignite under sunlight.
  - *Lightless Monster Spawning*: Monsters can spawn regardless of light levels.

### Additional Features & QoL
- **Shared Chest**: Special global chest (`aprandomizer:shared_chest`) allowing players across Archipelago slots/worlds to exchange items directly in-game.
- **Compasses**:
  - *Unvisited Structures Compass*: Dynamically tracks and points to the nearest unexplored structure.
  - *Unvisited Biomes Compass*: Dynamically points to the nearest unexplored biome.
  - *Individual Compasses*: Specific compasses targeting each structure and biome.
- **Extra Abilities (12 items)**:
  - *Progressive Mining*: Expands mining area (1x1, 1x2, 3x2, 3x3, 3x3x2, 3x3x3).
  - *Tree Feller*: Cuts down entire trees in one action.
  - *Progressive Jump*: Grants double and triple jumps.
  - *Item Magnet*: Attracts nearby dropped items.
  - *Step Assist*: Allows stepping up 1 full block smoothly without jumping.
  - *Trash Can Slot*: Convenient quick-trash slot in inventory.
  - *Light Holder*: Equippable chest light providing dynamic light.
- **Bed Placement Restriction in The End**: Prevents exploding beds in The End dimension to balance dragon fights.
- **In-Game Recipe & Upgrade Tracker**: Unlocked recipes, abilities, and upgrades are tracked directly in the advancements screen and dedicated HUD sidebar.
- **Interactive YAML Configurator**: Built-in GUI tool in Archipelago Launcher with dynamic option filtering and live checks/items balance statistics.

## Required Software

- [Minecraft Java Edition](https://www.minecraft.net/en-us/store/minecraft-java-edition)
- [Archipelago](https://github.com/ArchipelagoMW/Archipelago/releases)
- [Minecraft with BACAP APWorld](https://github.com/u2vr/ArchipelagoMinecraftU2VR/releases/)

## Installing the APWorld

1. Download `minecraft_with_bacap.apworld` from [Releases](https://github.com/u2vr/ArchipelagoMinecraftU2VR/releases/).
2. Open the **Archipelago Launcher** and click **"Install APWorld"** (or place the file into your Archipelago `custom_worlds` folder).
3. Select the downloaded `minecraft_with_bacap.apworld`.
4. Restart the Archipelago Launcher.
5. After restarting:
   - **Minecraft with BACAP yaml configurator** will appear in the Launcher tools list.
   - **Minecraft with BACAP Client** will appear under the Clients tab (with a custom Minecraft book icon).

### Updating the APWorld

Download the new `.apworld` file and reinstall it using the instructions above.

## Configuring your YAML file

### What is a YAML file and why do I need one?

See the guide on setting up a basic YAML at the Archipelago setup guide: [Basic Multiworld Setup Guide](https://archipelago.gg/tutorial/Archipelago/setup/en).

### Generating your YAML with the Built-In Configurator (Recommended)

This APWorld includes a dedicated graphical tool — **Minecraft with BACAP yaml configurator** — directly accessible from the Archipelago Launcher.

1. In the **Archipelago Launcher**, click on **"Minecraft with BACAP yaml configurator"**.
2. **Select Language**: The tool defaults to English, with full Russian localization available via the `Language` dropdown in the top header.
3. **Choose a Preset or Customize**:
   - Use the **Preset** dropdown to quickly load a ready-made profile:
     - **BACAP Standard (Recommended)**: Balanced experience with BACAP milestone checks (step 5), biome and structure exploration, trader checks, and core progressive restrictions.
     - **Quick Vanilla Game**: Vanilla 138 advancements with restrictions disabled for a fast, casual playthrough.
     - **Full Hardcore**: BACAP milestone step 2, End biomes, 20 trader trades, permanent curses, 50% traps, and dragon egg shard hunt.
   - Or customize every setting individually: Check goal, Required bosses, Dragon Egg shards, Advancement modes, Biomes, Structures, Progressive restrictions (HP, Hunger, Reach, World border, Inventory slots, Offhand, Structure locks), Craft tier bias, Dynamic compasses, 12 Extra abilities, and Traps percentage.
4. **Interactive Hover Tooltips**: Hover over any setting, slider, or button to see a detailed explanation of the option and its gameplay impact.
5. **Dynamic Visibility & Live Balance Dashboard**:
   - Dependent options appear or hide dynamically based on parent selections (e.g., milestone step only displays when BACAP Milestones are enabled; End biomes and structure shuffle only appear when relevant).
   - The **Live Stats Dashboard** on the right calculates in real time the total checks, progression items, compasses, abilities, traps, and fillers.
   - The **Balance Indicator** validates that your chosen check count is sufficient for all progression items, preventing generation failures before they happen.
6. **Save your YAML**:
   - Click **"💾 Save YAML"** (or **"📂 Save As..."**).
   - A file dialog will prompt for save location with filename `<PlayerName>.yaml`, defaulting to Archipelago's `Players` folder (`C:\ProgramData\Archipelago\Players`).
   - The configurator automatically remembers your last selected folder for future saves.
7. **Load & Edit Existing YAMLs**:
   - Click **"📥 Load YAML"** to import any previously created YAML file, inspect its live check/item balance, and adjust options.

## Joining a MultiWorld Game

### Obtain Your Minecraft Data File

When you join a multiworld game, you will be asked to provide your YAML file to whoever is hosting. The host also needs
to install the Minecraft APWorld into their Archipelago Instance, or else it will not generate.
**Only one YAML file needs to be submitted to the generator per Minecraft world regardless of how many players play on it.**
You can have 10 people working together on the same world or just 1, but regardless you just need to submit one YAML file (per group).

If you are the host, you can learn about _Generating a single player game_ or _Generating a multiplayer game_
[here](https://archipelago.gg/tutorial/Archipelago/setup_en#generating-a-single-player-game).
Note that since the game is not core-verified, you will need to follow the instructions for
_On your local installation_, **not** _On the website_.
Once the seed is generated, you can continue to
[_Hosting an archipelago server_](https://archipelago.gg/tutorial/Archipelago/setup_en#hosting-an-archipelago-server).
You can host from your PC or on the website; the latter is recommended for multiplayer games and generally easier setup.

Once that is done, the host will provide you with either a link to download your data file,
or with a zip file containing everyone's data files. Your data file should have a `.apmc` extension.

### Running the Minecraft Server

_If you're using a server hosting service, please see [**Manual Installation**](#manual-installation) instead._

Open the Minecraft Client from the Archipelago Launcher, and click on "Open APMC File".
This will allow you to select the APMC file you downloaded. Once opened, it will start the server.
If this is your first time running the server, it is required to ask if you accept Minecraft's EULA,
then it will automatically download the required Java and NeoForge Versions.
Once the server is running **you must keep the console open**, or else the server will shut down.

If you close the server, or the server does shut down unexpectedly, you can simply reopen the Minecraft Client
and select your Minecraft World from the list to restart the server. You can also rename the world to identify it from
other Minecraft Archipelago Games you may be running.

When you are finished with the Minecraft World, you may delete it from the list.

### Connect to the MultiServer

Using Minecraft 26.2, connect to the server. The IP address is:
- `localhost` if you're hosting
- provided to you by the server hosting service, if using one
- your friend's public IP address, if they're port forwarding

Once you are in game, the mod should auto connect to your Archipelago room. If it doesn't,
type `/connect <AP-Address> (Port) (Password)` where `<AP-Address>` is the address of the
Archipelago server, i.e. `archipelago.gg`.
`(Port)` is only required if the Archipelago server is not using the default port of 38281.
`(Password)` is only required if the Archipelago server you are using has a password set.

### Play the game

When the console tells you that you have joined the room, you're all set. Congratulations on successfully joining a
multiworld game! At this point any additional Minecraft players may connect to your NeoForge server.
To start the game once everyone is ready, use the command `/start`.

## Manual Installation

This is an alternative to the "Running the Minecraft Server" step; all other instructions must still be followed.
Please note that support for manual installations may be limited.
Additionally, these steps do not allow you to play a Singleplayer World; singleplayer is not supported at this time.

1. Download and install the latest version of [Java](https://adoptium.net/temurin/releases)
2. Download the NeoForgeAP mod from the [releases](https://github.com/u2vr/ArchipelagoMinecraftU2VR/releases/)
   and note the Minecraft version listed at the end of the filename.
3. Download the matching NeoForge [installer](https://neoforged.net/) and follow the prompts to Install a Server.
    - On a remote hosting service, you would instead use their configuration panel to do this automatically for you.
    - For further information, including how to install from a command line, see the [NeoForge guide](https://docs.neoforged.net/user/docs/server/)
4. In the root of the generated server, create a `mods` folder add the downloaded mod to it.
    - On a remote hosting service, look for a "Files" or "FTP" tab to do this.
5. In the root of the generated server, create an `APData` folder and add your .apmc to it.
6. Start the server.
   Note on the first launch it will generate a eula.txt file you have to edit to approve then relaunch the server.
