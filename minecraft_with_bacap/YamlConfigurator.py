"""
Minecraft with BACAP - YAML Configurator
Interactive GUI tool for configuring Archipelago YAML settings for Minecraft with BACAP.
Features dynamic option visibility, live statistics of checks and items,
bilingual support (English & Russian, default English), interactive hover tooltips,
and persistent folder memory for saving/loading.
"""

import os
import sys
import json
import math
import tkinter as tk
from tkinter import ttk, messagebox, filedialog

# Constants for statistics calculation matching Minecraft with BACAP APWorld
TOTAL_RECIPES = 78
TOTAL_BACAP_ADVANCEMENTS = 1112
MAX_BACAP_MILESTONES = 300
VANILLA_ADVANCEMENT_COUNT = 138

MAJOR_BIOMES_COUNT = 36
SMALL_BIOMES_COUNT = 25
ENDGAME_BIOMES_COUNT = 5
STRUCTURES_COUNT = 22

ALL_STRUCTURE_COMPASS_COUNT = 22
MAJOR_BIOME_COMPASS_COUNT = 36
MINOR_BIOME_COMPASS_COUNT = 25
EXTRA_ABILITIES_COUNT = 12

RESTRICTION_ITEMS = {
    "hp_restriction": 7,
    "hunger_restriction": 6,
    "reach_restriction": 3,
    "world_border_restriction": 5,
    "structure_restriction": 9,
    "inventory_slot_restriction": 27,
    "offhand_restriction": 1,
    "curses_enabled": 2,
}

CONFIG_FILE_PATH = os.path.join(os.path.expanduser("~"), ".minecraft_bacap_configurator.json")


class ToolTip:
    """Creates a responsive hover tooltip for Tkinter widgets."""
    def __init__(self, widget, text_func, delay=350):
        self.widget = widget
        self.text_func = text_func
        self.delay = delay
        self.tip_window = None
        self.after_id = None
        self.widget.bind("<Enter>", self.schedule_tip)
        self.widget.bind("<Leave>", self.hide_tip)
        self.widget.bind("<ButtonPress>", self.hide_tip)

    def schedule_tip(self, event=None):
        self.unschedule()
        self.after_id = self.widget.after(self.delay, self.show_tip)

    def unschedule(self):
        after_id = self.after_id
        self.after_id = None
        if after_id:
            try:
                self.widget.after_cancel(after_id)
            except Exception:
                pass

    def show_tip(self, event=None):
        self.hide_tip()
        text = self.text_func() if callable(self.text_func) else self.text_func
        if not text:
            return

        try:
            x = self.widget.winfo_rootx() + 20
            y = self.widget.winfo_rooty() + self.widget.winfo_height() + 6
        except Exception:
            return

        self.tip_window = tw = tk.Toplevel(self.widget)
        tw.wm_overrideredirect(True)
        try:
            tw.wm_attributes("-topmost", True)
        except Exception:
            pass

        label = tk.Label(
            tw,
            text=text,
            justify=tk.LEFT,
            background="#252526",
            foreground="#f1f1f1",
            relief=tk.SOLID,
            borderwidth=1,
            font=("Segoe UI", 8),
            padx=9,
            pady=6,
            wraplength=380
        )
        label.pack()

        tw.update_idletasks()
        tip_w = tw.winfo_width()
        tip_h = tw.winfo_height()
        screen_w = tw.winfo_screenwidth()
        screen_h = tw.winfo_screenheight()

        if x + tip_w > screen_w - 12:
            x = screen_w - tip_w - 12
        if y + tip_h > screen_h - 12:
            y = self.widget.winfo_rooty() - tip_h - 6

        tw.wm_geometry(f"+{max(6, x)}+{max(6, y)}")

    def hide_tip(self, event=None):
        self.unschedule()
        tw = self.tip_window
        self.tip_window = None
        if tw:
            try:
                tw.destroy()
            except Exception:
                pass


TRANSLATIONS = {
    "en": {
        "title": "Minecraft with BACAP - YAML Configurator",
        "header_title": "Minecraft with BACAP — YAML Configurator",
        "lang_label": "Language:",
        "preset_label": "Preset:",
        "presets": [
            "BACAP Standard (Recommended)",
            "Quick Vanilla Game",
            "Full Hardcore"
        ],
        "player_info_grp": " 👤 Player & Base Information ",
        "player_name": "Player name (name):",
        "goal_grp": " 🎯 Goals & Bosses ",
        "check_goal": "Check goal (check_goal):",
        "required_bosses": "Required bosses (required_bosses):",
        "egg_shards_req": "Egg shards (required):",
        "egg_shards_avail": "Egg shards in pool (available):",
        "adv_grp": " 🏆 Advancements System ",
        "adv_vanilla": "Vanilla (138 vanilla advancements)",
        "adv_milestones": "BACAP Milestones (Checks every N pack advancements)",
        "adv_random": "Random BACAP (Random balanced advancement pool)",
        "milestone_step": "Milestone step (1-20):",
        "checks_grp": " 🗺️ World Checks (Checks) ",
        "biome_checks": "Enable biome exploration checks (biome_checks)",
        "major_biomes": "Major Overworld & Nether biomes (36 biomes)",
        "small_biomes": "Small biomes, rivers, caves, mountains (25 biomes)",
        "endgame_biomes": "The End biomes (5 biomes)",
        "structure_checks": "World structure checks (22 structures: Villages, Chambers, Fortresses...)",
        "trader_trades": "Wandering Trader trades (0-20):",
        "restr_grp": " 🔒 Restrictions & Difficulty Modifiers ",
        "hp_restr": "HP restriction (start with 3 hearts, 7 upgrades)",
        "hunger_restr": "Hunger restriction (start with 4 drumsticks, 6 upgrades)",
        "reach_restr": "Reach distance restriction (start 2.5 blocks, 3 upgrades)",
        "world_border_restr": "World border (start 1000 blocks, 5 expansions)",
        "inventory_restr": "Inventory slot locks (start 9 hotbar slots, 27 upgrades)",
        "offhand_restr": "Offhand slot lock (1 unlock)",
        "structure_restr": "Structure access locks (9 unlocks)",
        "structure_shuffle": "Randomized structure unlock order (structure_unlock_shuffle)",
        "curses": "Permanent curses (monsters don't burn, daylight spawning)",
        "tier_bias": "Craft tier bias (0=random, 100=strictly by tiers):",
        "comp_grp": " 🧭 Compasses & Abilities (QoL) ",
        "unvisited_struct": "Unvisited structures compass (dynamically points to nearest new)",
        "all_struct": "All individual structure compasses (22 compasses in pool)",
        "unvisited_biomes": "Unvisited biomes compass (dynamically points to nearest new)",
        "major_biomes_comp": "Individual major biome compasses (36 compasses)",
        "minor_biomes_comp": "Individual minor biome compasses (25 compasses)",
        "extra_abilities": "Extra QoL abilities (12 items: Mining, Tree Feller, Jump, Magnet, Torch...)",
        "trap_grp": " 🐝 Traps (Traps) ",
        "traps_pct": "Percentage of traps from remaining slots:",
        "dash_title": " 📊 Generation Summary (Live Stats) ",
        "chk_box": " Location Checks (In-Game) ",
        "chk_adv_vanilla": "Advancements (Vanilla): {count}",
        "chk_adv_milestones": "BACAP Milestones (step {step}): {count}",
        "chk_adv_random": "Random BACAP: {count}",
        "chk_biomes": "Biomes: {count}",
        "chk_struct": "Structures: {count}",
        "chk_trader": "Trader trades: {count}",
        "total_checks": "TOTAL CHECKS: {count}",
        "itm_box": " Items in Pool (Item Pool) ",
        "itm_recipes": "Recipes: {count}",
        "itm_restr": "Restrictions / Upgrades: {count}",
        "itm_compasses": "Compasses: {count}",
        "itm_abilities": "Abilities: {count}",
        "itm_shards": "Egg shards: {count}",
        "total_prog": "TOTAL PROGRESSION: {count}",
        "fill_box": " Fillers & Traps ",
        "traps_count": "Traps: {count}",
        "junk_count": "Junk / Resources: {count}",
        "balance_checking": "Checking balance...",
        "balance_ok": "✅ Balance OK!\nTotal checks: {total_checks} (free: {remaining}).\nProgression items: {total_prog}.",
        "balance_deficit": "⚠️ Warning: Check deficit!\nNeeded: {total_prog}, available: {total_checks}.\nMissing {deficit} checks! Enable biomes/structures or reduce milestone step.",
        "btn_save": "💾 Save YAML",
        "btn_save_as": "📂 Save As...",
        "btn_load": "📥 Load YAML",
        "btn_reset": "🔄 Reset to Default",
        "dialog_save_title": "Save YAML file",
        "dialog_load_title": "Open YAML file",
        "msg_save_success_title": "Success",
        "msg_save_success": "YAML file successfully saved:\n{path}",
        "msg_save_err_title": "Save Error",
        "msg_save_err": "Failed to write file:\n{error}",
        "msg_load_success": "File {filename} loaded successfully!",
        "msg_load_err_title": "Load Error",
        "msg_load_err": "Failed to read YAML file:\n{error}",

        # Tooltips
        "tip_player_name": "Your slot name in the Archipelago multiworld room. Must match the player name defined in the room.",
        "tip_preset": "Quickly load preconfigured settings: BACAP Standard, Quick Vanilla, or Full Hardcore.",
        "tip_language": "Switch interface language between English and Russian.",
        "tip_check_goal": "Percentage or count of checks required to complete the game victory condition.",
        "tip_required_bosses": "Which boss(es) must be defeated to win: None, Ender Dragon, Wither, or Both.",
        "tip_egg_shards_req": "Dragon Egg Shards required to summon or defeat the Ender Dragon (0 = disabled, standard fight).",
        "tip_egg_shards_avail": "Total number of Dragon Egg Shards placed into the item pool across the multiworld.",
        "tip_adv_vanilla": "Only the 138 vanilla Minecraft advancements are included as location checks.",
        "tip_adv_milestones": "Location checks unlock for every N advancements completed from the BlazeandCave's Advancements Pack (BACAP).",
        "tip_adv_random": "A balanced, randomized subset of individual BACAP advancements is chosen as location checks.",
        "tip_milestone_step": "Step size: how many BACAP advancements you must achieve for each milestone check (1 to 20, default 5).",
        "tip_biome_checks": "Grants Archipelago checks upon entering and exploring unique biomes.",
        "tip_major_biomes": "Checks for discovering 36 major Overworld and Nether biomes (Plains, Deserts, Nether Wastes...).",
        "tip_small_biomes": "Checks for discovering 25 rarer, cave, river, and mountain biomes (Dripstone, Badlands, Swamps...).",
        "tip_endgame_biomes": "Checks for discovering the 5 outer End biomes (End Highlands, End Midlands, Small Islands...).",
        "tip_structure_checks": "Checks for discovering 22 major world structures (Villages, Trial Chambers, Fortresses, Ancient Cities...).",
        "tip_trader_trades": "Wandering Trader sells Archipelago items for emeralds (0 to 20 checks).",
        "tip_hp_restr": "Start with 3 hearts (6 HP). Unlock 7 progressive HP Upgrades to reach full 10 hearts.",
        "tip_hunger_restr": "Start with 4 drumsticks (8 hunger). Unlock 6 progressive Hunger Upgrades to reach full 10 drumsticks.",
        "tip_reach_restr": "Start with 2.5 blocks interaction reach. Unlock 3 progressive Reach Upgrades to reach 4.5 blocks.",
        "tip_world_border_restr": "Limits Overworld border initially to 1000 blocks. 5 Border Upgrades gradually expand the world.",
        "tip_inventory_restr": "Locks the 27 main inventory slots (start with only 9 hotbar slots). Unlock slots one-by-one with 27 item upgrades.",
        "tip_offhand_restr": "Prevents holding items in the offhand (shield, torches, food) until obtaining the Offhand Unlock item.",
        "tip_structure_restr": "A barrier seals access to 9 major world structure types until unlocked with Structure Unlock items.",
        "tip_structure_shuffle": "Randomizes the order in which structure unlock items appear, instead of following the recipe tree order.",
        "tip_curses": "Adds permanent curses: monsters do not burn in sunlight and can spawn regardless of light levels.",
        "tip_tier_bias": "0 = Fully random recipe unlocks; 1-99 = Weighted bias toward basic recipes first; 100 = Strict tier-by-tier order.",
        "tip_unvisited_struct": "Adds a smart compass that dynamically points to the nearest unexplored world structure.",
        "tip_all_struct": "Adds 22 individual compasses targeting specific structures (Village Compass, Fortress Compass...).",
        "tip_unvisited_biomes": "Adds a smart compass that dynamically points to the nearest unexplored biome.",
        "tip_major_biomes_comp": "Adds 36 individual compasses directly pointing to each major Overworld and Nether biome.",
        "tip_minor_biomes_comp": "Adds 25 individual compasses pointing to rivers, caves, peaks, and minor biomes.",
        "tip_extra_abilities": "Adds 12 QoL abilities: Progressive Mining (3x3), Tree Feller, Double/Triple Jump, Magnet, Step Assist, Trash Slot, Torches...",
        "tip_bee_traps": "Percentage of remaining non-progression filler slots turned into traps (Bees, Skeletons, Zombies, Junk, Sand, Blindness).",
        "tip_balance_status": "Shows real-time check/item counts. Green means enough checks exist to place all progression items.",
        "tip_btn_save": "Save YAML configuration to a file (prompts for location and remembers your folder).",
        "tip_btn_save_as": "Save YAML configuration under a new name or location.",
        "tip_btn_load": "Open and load an existing Archipelago YAML file to edit its options.",
        "tip_btn_reset": "Reset all options back to the recommended BACAP standard settings.",
    },
    "ru": {
        "title": "Minecraft with BACAP — Настройщик YAML",
        "header_title": "Minecraft with BACAP — Настройщик YAML",
        "lang_label": "Язык:",
        "preset_label": "Пресет:",
        "presets": [
            "Стандарт BACAP (Рекомендуемый)",
            "Быстрая игра (Vanilla)",
            "Максимальный хардкор"
        ],
        "player_info_grp": " 👤 Имя игрока и базовая информация ",
        "player_name": "Имя игрока (name):",
        "goal_grp": " 🎯 Цели и боссы ",
        "check_goal": "Цель по чекам (check_goal):",
        "required_bosses": "Требуемые боссы (required_bosses):",
        "egg_shards_req": "Осколки яйца (требуется):",
        "egg_shards_avail": "Осколков в пуле (доступно):",
        "adv_grp": " 🏆 Система достижений (Advancements) ",
        "adv_vanilla": "Vanilla (138 ванильных достижений)",
        "adv_milestones": "BACAP Milestones (Майлстоуны за каждые N достижений пака)",
        "adv_random": "Random BACAP (Случайный сбалансированный пул достижений)",
        "milestone_step": "Шаг майлстоунов (1-20):",
        "checks_grp": " 🗺️ Проверки мира (Чеки) ",
        "biome_checks": "Включить чеки исследования биомов (biome_checks)",
        "major_biomes": "Основные биомы суши и Незера (36 биомов)",
        "small_biomes": "Малые биомы, реки, пещеры, горы (25 биомов)",
        "endgame_biomes": "Биомы Энда (5 биомов)",
        "structure_checks": "Чеки структур мира (22 структуры: Деревни, Палаты, Крепости...)",
        "trader_trades": "Сделки у Бродячего торговца (0-20):",
        "restr_grp": " 🔒 Ограничения и модификаторы сложности ",
        "hp_restr": "Ограничение HP (старт с 3 сердец, 7 апгрейдов)",
        "hunger_restr": "Ограничение сытости (старт с 4 окорочков, 6 апгрейдов)",
        "reach_restr": "Ограничение дистанции досягаемости (старт 2.5 блока, 3 апгрейда)",
        "world_border_restr": "Граница мира (старт 1000 блоков, 5 расширений границы)",
        "inventory_restr": "Блокировка слотов инвентаря (старт 9 слотов хотбара, 27 апгрейдов)",
        "offhand_restr": "Блокировка слота второй руки (1 разблокировка)",
        "structure_restr": "Блокировка доступа к структурам (9 разблокировок)",
        "structure_shuffle": "Случайный порядок открытия структур (structure_unlock_shuffle)",
        "curses": "Постоянные проклятия (монстры не горят на солнце, спавн на свету)",
        "tier_bias": "Приоритет тиров рецептов (0=рандом, 100=по тирам):",
        "comp_grp": " 🧭 Компасы и Способности (QoL) ",
        "unvisited_struct": "Компас непосещённых структур (динамически указывает на ближайшую новую)",
        "all_struct": "Все индивидуальные компасы структур (22 компаса в пуле)",
        "unvisited_biomes": "Компас непосещённых биомов (динамически указывает на ближайший новый)",
        "major_biomes_comp": "Индивидуальные компасы основных биомов (36 компасов)",
        "minor_biomes_comp": "Индивидуальные компасы малых биомов (25 компасов)",
        "extra_abilities": "Экстра-способности QoL (12 предметов: Буры, Дровосек, Прыжки, Магнит, Факел...)",
        "trap_grp": " 🐝 Ловушки (Traps) ",
        "traps_pct": "Процент ловушек от оставшихся слотов:",
        "dash_title": " 📊 Сводка генерации (Live Stats) ",
        "chk_box": " Чеки (Локации в мире) ",
        "chk_adv_vanilla": "Достижения (Vanilla): {count}",
        "chk_adv_milestones": "Майлстоуны BACAP (шаг {step}): {count}",
        "chk_adv_random": "Случайные BACAP: {count}",
        "chk_biomes": "Биомы: {count}",
        "chk_struct": "Структуры: {count}",
        "chk_trader": "Сделки торговца: {count}",
        "total_checks": "ВСЕГО ЧЕКОВ: {count}",
        "itm_box": " Предметы в пуле (Item Pool) ",
        "itm_recipes": "Рецепты: {count}",
        "itm_restr": "Ограничения / Апгрейды: {count}",
        "itm_compasses": "Компасы: {count}",
        "itm_abilities": "Способности: {count}",
        "itm_shards": "Осколки яйца: {count}",
        "total_prog": "ВСЕГО ПРОГРЕССИИ: {count}",
        "fill_box": " Заполнители и ловушки ",
        "traps_count": "Ловушки: {count}",
        "junk_count": "Мусор / Ресурсы: {count}",
        "balance_checking": "Проверка баланса...",
        "balance_ok": "✅ Баланс в норме!\nЧеков: {total_checks} (свободно: {remaining}).\nПрогрессионных предметов: {total_prog}.",
        "balance_deficit": "⚠️ Внимание: Недостаток чеков!\nНужно: {total_prog}, доступно: {total_checks}.\nНе хватает {deficit} чеков! Включите биомы/структуры или уменьшите шаг майлстоунов.",
        "btn_save": "💾 Сохранить YAML",
        "btn_save_as": "📂 Сохранить как...",
        "btn_load": "📥 Загрузить YAML",
        "btn_reset": "🔄 Сброс к стандарту",
        "dialog_save_title": "Сохранить YAML файл",
        "dialog_load_title": "Открыть YAML файл",
        "msg_save_success_title": "Успех",
        "msg_save_success": "YAML файл успешно сохранён:\n{path}",
        "msg_save_err_title": "Ошибка сохранения",
        "msg_save_err": "Не удалось записать файл:\n{error}",
        "msg_load_success": "Файл {filename} успешно загружен!",
        "msg_load_err_title": "Ошибка загрузки",
        "msg_load_err": "Не удалось прочитать YAML файл:\n{error}",

        # Tooltips
        "tip_player_name": "Имя вашего слота в комнате Архипелага. Должно точно совпадать с именем при генерации.",
        "tip_preset": "Быстрая загрузка готовой конфигурации: Стандарт BACAP, Ванилла или Хардкор.",
        "tip_language": "Переключение языка интерфейса между английским и русским.",
        "tip_check_goal": "Количество или процент выполненных чеков, необходимых для победы.",
        "tip_required_bosses": "Каких боссов нужно победить: Никого, Дракона Края, Иссушителя или Обоих.",
        "tip_egg_shards_req": "Сколько осколков яйца дракона требуется для призыва Дракона Края (0 = выключено, обычный спавн).",
        "tip_egg_shards_avail": "Общее количество осколков яйца дракона в пуле предметов мультиворлда.",
        "tip_adv_vanilla": "В качестве чеков используются только 138 стандартных ванильных достижений Minecraft.",
        "tip_adv_milestones": "Чеки открываются за каждые N любых выполненных достижений из пака BlazeandCave (BACAP).",
        "tip_adv_random": "Случайный сбалансированный набор конкретных достижений BACAP становится отдельными чеками.",
        "tip_milestone_step": "Шаг майлстоунов: сколько достижений нужно выполнить для одного чека (от 1 до 20, по умолчанию 5).",
        "tip_biome_checks": "Выдаёт чеки Архипелага при посещении и открытии новых биомов в мире.",
        "tip_major_biomes": "Чеки за открытие 36 основных биомов суши и Незера (Равнины, Пустыни, Пустоши Незера...).",
        "tip_small_biomes": "Чеки за открытие 25 малых, речных, пещерных и горных биомов (Реки, Карстовые пещеры, Болота...).",
        "tip_endgame_biomes": "Чеки за исследование 5 биомов дальних островов Энда (Высокогорья, Малые острова...).",
        "tip_structure_checks": "Чеки за обнаружение 22 ключевых структур мира (Деревни, Палаты испытаний, Крепости, Древние города...).",
        "tip_trader_trades": "Бродячий торговец продаёт предметы Архипелага за изумруды (от 0 до 20 чеков).",
        "tip_hp_restr": "Старт всего с 3 сердцами (6 HP). 7 апгрейдов здоровья постепенно восстанавливают 10 сердец.",
        "tip_hunger_restr": "Старт с 4 окорочками сытости. 6 апгрейдов сытости расширяют шкалу голода до 10 окорочков.",
        "tip_reach_restr": "Старт с укороченной дистанцией копания и ударов (2.5 блока). 3 апгрейда расширяют её до 4.5 блоков.",
        "tip_world_border_restr": "Ограничивает начальную границу Обычного мира 1000 блоков. 5 расширений открывают весь мир.",
        "tip_inventory_restr": "Блокирует 27 слотов инвентаря (доступен только хотбар). 27 апгрейдов открывают слоты по одному.",
        "tip_offhand_restr": "Запрещает использовать вторую руку (щиты, факелы, еду) до нахождения разблокировки второй руки.",
        "tip_structure_restr": "Барьер блокирует вход в 9 типов ключевых структур мира, пока не будет получен предмет разблокировки.",
        "tip_structure_shuffle": "Случайный порядок открытия структур вместо фиксированного следования по дереву рецептов.",
        "tip_curses": "Постоянные проклятия: монстры не горят на солнце и могут спавниться при любом уровне света.",
        "tip_tier_bias": "0 = Случайное открытие рецептов; 1-99 = Приоритет простых рецептов в начале; 100 = Строго по тирам.",
        "tip_unvisited_struct": "Умный компас, который автоматически указывает на ближайшую ещё не открытую структуру.",
        "tip_all_struct": "Добавляет 22 отдельных компаса для каждого типа структур (Компас деревни, Компас крепости...).",
        "tip_unvisited_biomes": "Умный компас, который автоматически указывает направление к ближайшему новому биому.",
        "tip_major_biomes_comp": "Добавляет 36 индивидуальных компасов к каждому из основных биомов суши и Незера.",
        "tip_minor_biomes_comp": "Добавляет 25 индивидуальных компасов к рекам, пещерам, пикам и малым биомам.",
        "tip_extra_abilities": "12 способностей: Буры (до 3х3х3), Дровосек (рубка дерева целиком), Двойной/Тройной прыжок, Магнит, Автоподъём, Мусорка...",
        "tip_bee_traps": "Процент свободных слотов, превращаемых в ловушки (Пчёлы, Скелеты, Зомби, Мусор, Песок, Слепота).",
        "tip_balance_status": "Сводка баланса генерации. Зелёный цвет означает, что чеков достаточно для всех предметов.",
        "tip_btn_save": "Сохранить YAML файл с выбором места и запоминанием выбранной папки.",
        "tip_btn_save_as": "Сохранить YAML файл под новым именем или в другую папку.",
        "tip_btn_load": "Открыть и загрузить существующий YAML файл для редактирования настроек.",
        "tip_btn_reset": "Сбросить все параметры к рекомендуемым стандартным настройкам BACAP.",
    }
}


class YamlConfigurator(tk.Tk):
    def __init__(self):
        super().__init__()
        self.geometry("1040x780")
        self.minsize(900, 660)

        # Apply ttk styling
        self.style = ttk.Style(self)
        try:
            self.style.theme_use("clam")
        except Exception:
            pass

        self._load_app_settings()
        self._init_variables()
        self._build_ui()
        self._bind_tooltips()
        self._apply_language()
        self._update_visibility()
        self._recalculate_stats()

    def _load_app_settings(self):
        """Loads persistent user settings (language and last saved folder)."""
        default_dir = r"C:\ProgramData\Archipelago\Players"
        if not os.path.isdir(default_dir):
            default_dir = os.path.expanduser("~")
        self.last_directory = default_dir
        self.current_lang = "en"  # Default is English

        try:
            if os.path.isfile(CONFIG_FILE_PATH):
                with open(CONFIG_FILE_PATH, "r", encoding="utf-8") as f:
                    data = json.load(f)
                    saved_lang = data.get("lang")
                    if saved_lang in ("en", "ru"):
                        self.current_lang = saved_lang
                    saved_dir = data.get("last_directory")
                    if saved_dir and os.path.isdir(saved_dir):
                        self.last_directory = saved_dir
        except Exception:
            pass

    def _save_app_settings(self):
        """Saves persistent settings to file."""
        try:
            data = {
                "lang": self.current_lang,
                "last_directory": self.last_directory
            }
            with open(CONFIG_FILE_PATH, "w", encoding="utf-8") as f:
                json.dump(data, f, indent=2, ensure_ascii=False)
        except Exception:
            pass

    def t(self, key: str, **kwargs) -> str:
        """Retrieves a translated string for the current language."""
        lang_dict = TRANSLATIONS.get(self.current_lang, TRANSLATIONS["en"])
        template = lang_dict.get(key, TRANSLATIONS["en"].get(key, key))
        if kwargs and isinstance(template, str):
            return template.format(**kwargs)
        return template

    def _add_tooltip(self, widget, key: str):
        """Attaches a dynamic localized tooltip to a widget."""
        ToolTip(widget, lambda: self.t(key))

    def _init_variables(self):
        # General
        self.player_name = tk.StringVar(value="Player")
        self.game_name = "Minecraft with BACAP"
        self.description = tk.StringVar(value="Generated by Minecraft with BACAP YAML Configurator")

        # Goals
        self.check_goal = tk.IntVar(value=50)
        self.required_bosses = tk.StringVar(value="ender_dragon")
        self.egg_shards_required = tk.IntVar(value=0)
        self.egg_shards_available = tk.IntVar(value=0)

        # Advancements: 0 = Vanilla, 1 = BACAP Milestones, 2 = Random BACAP
        self.advancement_type = tk.IntVar(value=1)
        self.bacap_advancement_step = tk.IntVar(value=5)

        # Biomes
        self.biome_checks = tk.BooleanVar(value=True)
        self.major_biome_checks = tk.BooleanVar(value=True)
        self.small_biome_checks = tk.BooleanVar(value=True)
        self.endgame_biome_checks = tk.BooleanVar(value=False)

        # Structures
        self.structure_checks = tk.BooleanVar(value=True)

        # Trader
        self.wandering_trader_trades = tk.IntVar(value=10)

        # Restrictions
        self.reach_restriction = tk.BooleanVar(value=True)
        self.hp_restriction = tk.BooleanVar(value=True)
        self.hunger_restriction = tk.BooleanVar(value=True)
        self.world_border_restriction = tk.BooleanVar(value=True)
        self.structure_restriction = tk.BooleanVar(value=True)
        self.structure_unlock_shuffle = tk.BooleanVar(value=False)
        self.inventory_slot_restriction = tk.BooleanVar(value=True)
        self.offhand_restriction = tk.BooleanVar(value=True)
        self.curses_enabled = tk.BooleanVar(value=False)
        self.craft_tier_bias = tk.IntVar(value=50)

        # Compasses & Abilities
        self.unvisited_structures_compass = tk.BooleanVar(value=True)
        self.structure_compasses = tk.BooleanVar(value=False)
        self.unvisited_biomes_compass = tk.BooleanVar(value=True)
        self.major_biomes_compasses = tk.BooleanVar(value=False)
        self.minor_biomes_compasses = tk.BooleanVar(value=False)
        self.extra_abilities = tk.BooleanVar(value=False)

        # Traps
        self.bee_traps = tk.IntVar(value=0)

        # Attach listeners for recalculation & visibility
        for var in [
            self.check_goal, self.required_bosses, self.egg_shards_required, self.egg_shards_available,
            self.advancement_type, self.bacap_advancement_step, self.biome_checks,
            self.major_biome_checks, self.small_biome_checks, self.endgame_biome_checks,
            self.structure_checks, self.wandering_trader_trades, self.reach_restriction,
            self.hp_restriction, self.hunger_restriction, self.world_border_restriction,
            self.structure_restriction, self.structure_unlock_shuffle,
            self.inventory_slot_restriction, self.offhand_restriction, self.curses_enabled,
            self.craft_tier_bias, self.unvisited_structures_compass, self.structure_compasses,
            self.unvisited_biomes_compass, self.major_biomes_compasses, self.minor_biomes_compasses,
            self.extra_abilities, self.bee_traps
        ]:
            var.trace_add("write", lambda *_: self._on_value_change())

    def _on_value_change(self):
        self._update_visibility()
        self._recalculate_stats()

    def _build_ui(self):
        # Top banner
        header = ttk.Frame(self, padding=(12, 8))
        header.pack(fill=tk.X)

        self.header_title_lbl = ttk.Label(
            header,
            text="",
            font=("Segoe UI", 13, "bold")
        )
        self.header_title_lbl.pack(side=tk.LEFT)

        header_right = ttk.Frame(header)
        header_right.pack(side=tk.RIGHT)

        # Language selector
        self.lang_lbl = ttk.Label(header_right, text="Language:")
        self.lang_lbl.pack(side=tk.LEFT, padx=(6, 2))

        self.lang_combo = ttk.Combobox(
            header_right,
            values=["English", "Русский"],
            state="readonly",
            width=10
        )
        self.lang_combo.set("English" if self.current_lang == "en" else "Русский")
        self.lang_combo.bind("<<ComboboxSelected>>", self._on_language_selected)
        self.lang_combo.pack(side=tk.LEFT, padx=(0, 10))

        # Preset selector
        self.preset_lbl = ttk.Label(header_right, text="Preset:")
        self.preset_lbl.pack(side=tk.LEFT, padx=(6, 2))

        self.preset_combo = ttk.Combobox(
            header_right,
            values=[],
            state="readonly",
            width=28
        )
        self.preset_combo.pack(side=tk.LEFT, padx=2)
        self.preset_combo.bind("<<ComboboxSelected>>", self._on_preset_selected)

        # Main splitter (Options on Left, Live Stats Dashboard on Right)
        main_pane = ttk.PanedWindow(self, orient=tk.HORIZONTAL)
        main_pane.pack(fill=tk.BOTH, expand=True, padx=8, pady=(0, 8))

        # Left: Scrollable options frame
        left_container = ttk.Frame(main_pane)
        main_pane.add(left_container, weight=3)

        canvas = tk.Canvas(left_container, highlightthickness=0)
        scrollbar = ttk.Scrollbar(left_container, orient="vertical", command=canvas.yview)
        self.scrollable_frame = ttk.Frame(canvas, padding=(4, 4))

        self.scrollable_frame.bind(
            "<Configure>",
            lambda e: canvas.configure(scrollregion=canvas.bbox("all"))
        )
        canvas_window = canvas.create_window((0, 0), window=self.scrollable_frame, anchor="nw")

        def _on_canvas_configure(event):
            canvas.itemconfig(canvas_window, width=event.width)
        canvas.bind("<Configure>", _on_canvas_configure)

        def _on_mousewheel(event):
            canvas.yview_scroll(int(-1 * (event.delta / 120)), "units")
        canvas.bind_all("<MouseWheel>", _on_mousewheel)

        canvas.configure(yscrollcommand=scrollbar.set)
        canvas.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)
        scrollbar.pack(side=tk.RIGHT, fill=tk.Y)

        # Right: Stats Dashboard & Actions
        right_container = ttk.Frame(main_pane, padding=(8, 4))
        main_pane.add(right_container, weight=2)

        self._build_options_form(self.scrollable_frame)
        self._build_dashboard(right_container)

    def _build_options_form(self, parent):
        # 0. Player Info
        self.info_grp = ttk.LabelFrame(parent, text="", padding=10)
        self.info_grp.pack(fill=tk.X, pady=4)

        row = ttk.Frame(self.info_grp)
        row.pack(fill=tk.X, pady=2)
        self.lbl_player_name = ttk.Label(row, text="", width=24)
        self.lbl_player_name.pack(side=tk.LEFT)
        self.entry_player_name = ttk.Entry(row, textvariable=self.player_name, width=28)
        self.entry_player_name.pack(side=tk.LEFT, padx=5)

        # 1. Goals & Bosses
        self.goal_grp = ttk.LabelFrame(parent, text="", padding=10)
        self.goal_grp.pack(fill=tk.X, pady=4)

        row = ttk.Frame(self.goal_grp)
        row.pack(fill=tk.X, pady=2)
        self.lbl_check_goal = ttk.Label(row, text="", width=28)
        self.lbl_check_goal.pack(side=tk.LEFT)
        self.check_goal_scale = ttk.Scale(row, from_=1, to=100, variable=self.check_goal, orient=tk.HORIZONTAL)
        self.check_goal_scale.pack(side=tk.LEFT, fill=tk.X, expand=True, padx=6)
        self.check_goal_lbl = ttk.Label(row, text=str(self.check_goal.get()), width=4)
        self.check_goal_lbl.pack(side=tk.LEFT)
        self.check_goal.trace_add("write", lambda *_: self.check_goal_lbl.config(text=str(self.check_goal.get())))

        row = ttk.Frame(self.goal_grp)
        row.pack(fill=tk.X, pady=4)
        self.lbl_required_bosses = ttk.Label(row, text="", width=28)
        self.lbl_required_bosses.pack(side=tk.LEFT)
        self.combo_bosses = ttk.Combobox(
            row,
            textvariable=self.required_bosses,
            values=["none", "ender_dragon", "wither", "both"],
            state="readonly",
            width=16
        )
        self.combo_bosses.pack(side=tk.LEFT, padx=6)

        row = ttk.Frame(self.goal_grp)
        row.pack(fill=tk.X, pady=2)
        self.lbl_egg_shards_req = ttk.Label(row, text="", width=28)
        self.lbl_egg_shards_req.pack(side=tk.LEFT)
        self.spin_shards_req = ttk.Spinbox(row, from_=0, to=50, textvariable=self.egg_shards_required, width=8)
        self.spin_shards_req.pack(side=tk.LEFT, padx=6)

        self.shards_avail_row = ttk.Frame(self.goal_grp)
        self.shards_avail_row.pack(fill=tk.X, pady=2)
        self.lbl_egg_shards_avail = ttk.Label(self.shards_avail_row, text="", width=28)
        self.lbl_egg_shards_avail.pack(side=tk.LEFT)
        self.spin_shards_avail = ttk.Spinbox(self.shards_avail_row, from_=0, to=50, textvariable=self.egg_shards_available, width=8)
        self.spin_shards_avail.pack(side=tk.LEFT, padx=6)

        # 2. Advancements Mode
        self.adv_grp = ttk.LabelFrame(parent, text="", padding=10)
        self.adv_grp.pack(fill=tk.X, pady=4)

        row = ttk.Frame(self.adv_grp)
        row.pack(fill=tk.X, pady=2)
        self.radio_adv_vanilla = ttk.Radiobutton(row, text="", value=0, variable=self.advancement_type)
        self.radio_adv_vanilla.pack(anchor="w")
        self.radio_adv_milestones = ttk.Radiobutton(row, text="", value=1, variable=self.advancement_type)
        self.radio_adv_milestones.pack(anchor="w")
        self.radio_adv_random = ttk.Radiobutton(row, text="", value=2, variable=self.advancement_type)
        self.radio_adv_random.pack(anchor="w")

        self.milestone_step_row = ttk.Frame(self.adv_grp)
        self.milestone_step_row.pack(fill=tk.X, pady=4)
        self.lbl_milestone_step = ttk.Label(self.milestone_step_row, text="", width=24)
        self.lbl_milestone_step.pack(side=tk.LEFT)
        self.step_scale = ttk.Scale(self.milestone_step_row, from_=1, to=20, variable=self.bacap_advancement_step, orient=tk.HORIZONTAL)
        self.step_scale.pack(side=tk.LEFT, fill=tk.X, expand=True, padx=6)
        self.step_lbl = ttk.Label(self.milestone_step_row, text=str(self.bacap_advancement_step.get()), width=4)
        self.step_lbl.pack(side=tk.LEFT)
        self.bacap_advancement_step.trace_add("write", lambda *_: self.step_lbl.config(text=str(self.bacap_advancement_step.get())))

        # 3. World Checks (Biomes, Structures, Trades)
        self.check_grp = ttk.LabelFrame(parent, text="", padding=10)
        self.check_grp.pack(fill=tk.X, pady=4)

        self.chk_biome_checks = ttk.Checkbutton(self.check_grp, text="", variable=self.biome_checks)
        self.chk_biome_checks.pack(anchor="w")

        self.biomes_subframe = ttk.Frame(self.check_grp, padding=(18, 2))
        self.biomes_subframe.pack(fill=tk.X)
        self.chk_major_biomes = ttk.Checkbutton(self.biomes_subframe, text="", variable=self.major_biome_checks)
        self.chk_major_biomes.pack(anchor="w")
        self.chk_small_biomes = ttk.Checkbutton(self.biomes_subframe, text="", variable=self.small_biome_checks)
        self.chk_small_biomes.pack(anchor="w")
        self.chk_endgame_biomes = ttk.Checkbutton(self.biomes_subframe, text="", variable=self.endgame_biome_checks)
        self.chk_endgame_biomes.pack(anchor="w")

        self.chk_structure_checks = ttk.Checkbutton(self.check_grp, text="", variable=self.structure_checks)
        self.chk_structure_checks.pack(anchor="w", pady=(4, 0))

        row = ttk.Frame(self.check_grp)
        row.pack(fill=tk.X, pady=6)
        self.lbl_trader_trades = ttk.Label(row, text="", width=32)
        self.lbl_trader_trades.pack(side=tk.LEFT)
        self.trader_scale = ttk.Scale(row, from_=0, to=20, variable=self.wandering_trader_trades, orient=tk.HORIZONTAL)
        self.trader_scale.pack(side=tk.LEFT, fill=tk.X, expand=True, padx=6)
        self.trader_lbl = ttk.Label(row, text=str(self.wandering_trader_trades.get()), width=4)
        self.trader_lbl.pack(side=tk.LEFT)
        self.wandering_trader_trades.trace_add("write", lambda *_: self.trader_lbl.config(text=str(self.wandering_trader_trades.get())))

        # 4. Restrictions & Difficulty
        self.restr_grp = ttk.LabelFrame(parent, text="", padding=10)
        self.restr_grp.pack(fill=tk.X, pady=4)

        self.chk_hp_restr = ttk.Checkbutton(self.restr_grp, text="", variable=self.hp_restriction)
        self.chk_hp_restr.pack(anchor="w")
        self.chk_hunger_restr = ttk.Checkbutton(self.restr_grp, text="", variable=self.hunger_restriction)
        self.chk_hunger_restr.pack(anchor="w")
        self.chk_reach_restr = ttk.Checkbutton(self.restr_grp, text="", variable=self.reach_restriction)
        self.chk_reach_restr.pack(anchor="w")
        self.chk_world_border_restr = ttk.Checkbutton(self.restr_grp, text="", variable=self.world_border_restriction)
        self.chk_world_border_restr.pack(anchor="w")
        self.chk_inventory_restr = ttk.Checkbutton(self.restr_grp, text="", variable=self.inventory_slot_restriction)
        self.chk_inventory_restr.pack(anchor="w")
        self.chk_offhand_restr = ttk.Checkbutton(self.restr_grp, text="", variable=self.offhand_restriction)
        self.chk_offhand_restr.pack(anchor="w")

        self.chk_structure_restr = ttk.Checkbutton(self.restr_grp, text="", variable=self.structure_restriction)
        self.chk_structure_restr.pack(anchor="w", pady=(2, 0))

        self.struct_shuffle_frame = ttk.Frame(self.restr_grp, padding=(18, 2))
        self.struct_shuffle_frame.pack(fill=tk.X)
        self.chk_structure_shuffle = ttk.Checkbutton(self.struct_shuffle_frame, text="", variable=self.structure_unlock_shuffle)
        self.chk_structure_shuffle.pack(anchor="w")

        self.chk_curses = ttk.Checkbutton(self.restr_grp, text="", variable=self.curses_enabled)
        self.chk_curses.pack(anchor="w", pady=(4, 0))

        row = ttk.Frame(self.restr_grp)
        row.pack(fill=tk.X, pady=6)
        self.lbl_tier_bias = ttk.Label(row, text="", width=42)
        self.lbl_tier_bias.pack(side=tk.LEFT)
        self.tier_scale = ttk.Scale(row, from_=0, to=100, variable=self.craft_tier_bias, orient=tk.HORIZONTAL)
        self.tier_scale.pack(side=tk.LEFT, fill=tk.X, expand=True, padx=6)
        self.tier_lbl = ttk.Label(row, text=str(self.craft_tier_bias.get()), width=4)
        self.tier_lbl.pack(side=tk.LEFT)
        self.craft_tier_bias.trace_add("write", lambda *_: self.tier_lbl.config(text=str(self.craft_tier_bias.get())))

        # 5. Compasses & Abilities
        self.comp_grp = ttk.LabelFrame(parent, text="", padding=10)
        self.comp_grp.pack(fill=tk.X, pady=4)

        self.chk_unvisited_struct = ttk.Checkbutton(self.comp_grp, text="", variable=self.unvisited_structures_compass)
        self.chk_unvisited_struct.pack(anchor="w")
        self.chk_all_struct = ttk.Checkbutton(self.comp_grp, text="", variable=self.structure_compasses)
        self.chk_all_struct.pack(anchor="w")
        self.chk_unvisited_biomes = ttk.Checkbutton(self.comp_grp, text="", variable=self.unvisited_biomes_compass)
        self.chk_unvisited_biomes.pack(anchor="w")
        self.chk_major_biomes_comp = ttk.Checkbutton(self.comp_grp, text="", variable=self.major_biomes_compasses)
        self.chk_major_biomes_comp.pack(anchor="w")
        self.chk_minor_biomes_comp = ttk.Checkbutton(self.comp_grp, text="", variable=self.minor_biomes_compasses)
        self.chk_minor_biomes_comp.pack(anchor="w")
        self.chk_extra_abilities = ttk.Checkbutton(self.comp_grp, text="", variable=self.extra_abilities)
        self.chk_extra_abilities.pack(anchor="w", pady=(2, 0))

        # 6. Traps
        self.trap_grp = ttk.LabelFrame(parent, text="", padding=10)
        self.trap_grp.pack(fill=tk.X, pady=4)

        row = ttk.Frame(self.trap_grp)
        row.pack(fill=tk.X, pady=2)
        self.lbl_traps_pct = ttk.Label(row, text="", width=36)
        self.lbl_traps_pct.pack(side=tk.LEFT)
        self.trap_scale = ttk.Scale(row, from_=0, to=100, variable=self.bee_traps, orient=tk.HORIZONTAL)
        self.trap_scale.pack(side=tk.LEFT, fill=tk.X, expand=True, padx=6)
        self.trap_lbl = ttk.Label(row, text=f"{self.bee_traps.get()}%", width=5)
        self.trap_lbl.pack(side=tk.LEFT)
        self.bee_traps.trace_add("write", lambda *_: self.trap_lbl.config(text=f"{self.bee_traps.get()}%"))

    def _build_dashboard(self, parent):
        self.dash_card = ttk.LabelFrame(parent, text="", padding=10)
        self.dash_card.pack(fill=tk.BOTH, expand=True, pady=4)

        # 1. Checks Box
        self.chk_box = ttk.LabelFrame(self.dash_card, text="", padding=8)
        self.chk_box.pack(fill=tk.X, pady=4)

        self.lbl_chk_adv = ttk.Label(self.chk_box, text="", font=("Segoe UI", 9))
        self.lbl_chk_adv.pack(anchor="w")
        self.lbl_chk_biomes = ttk.Label(self.chk_box, text="", font=("Segoe UI", 9))
        self.lbl_chk_biomes.pack(anchor="w")
        self.lbl_chk_struct = ttk.Label(self.chk_box, text="", font=("Segoe UI", 9))
        self.lbl_chk_struct.pack(anchor="w")
        self.lbl_chk_trader = ttk.Label(self.chk_box, text="", font=("Segoe UI", 9))
        self.lbl_chk_trader.pack(anchor="w")

        ttk.Separator(self.chk_box, orient=tk.HORIZONTAL).pack(fill=tk.X, pady=4)
        self.lbl_total_checks = ttk.Label(
            self.chk_box, text="", font=("Segoe UI", 10, "bold"), foreground="#0055bb"
        )
        self.lbl_total_checks.pack(anchor="w")

        # 2. Items Box
        self.itm_box = ttk.LabelFrame(self.dash_card, text="", padding=8)
        self.itm_box.pack(fill=tk.X, pady=4)

        self.lbl_itm_recipes = ttk.Label(self.itm_box, text="", font=("Segoe UI", 9))
        self.lbl_itm_recipes.pack(anchor="w")
        self.lbl_itm_restr = ttk.Label(self.itm_box, text="", font=("Segoe UI", 9))
        self.lbl_itm_restr.pack(anchor="w")
        self.lbl_itm_compasses = ttk.Label(self.itm_box, text="", font=("Segoe UI", 9))
        self.lbl_itm_compasses.pack(anchor="w")
        self.lbl_itm_abilities = ttk.Label(self.itm_box, text="", font=("Segoe UI", 9))
        self.lbl_itm_abilities.pack(anchor="w")
        self.lbl_itm_shards = ttk.Label(self.itm_box, text="", font=("Segoe UI", 9))
        self.lbl_itm_shards.pack(anchor="w")

        ttk.Separator(self.itm_box, orient=tk.HORIZONTAL).pack(fill=tk.X, pady=4)
        self.lbl_total_prog = ttk.Label(
            self.itm_box, text="", font=("Segoe UI", 10, "bold"), foreground="#773300"
        )
        self.lbl_total_prog.pack(anchor="w")

        # 3. Fillers & Traps Box
        self.fill_box = ttk.LabelFrame(self.dash_card, text="", padding=8)
        self.fill_box.pack(fill=tk.X, pady=4)

        self.lbl_traps = ttk.Label(self.fill_box, text="", font=("Segoe UI", 9))
        self.lbl_traps.pack(anchor="w")
        self.lbl_junk = ttk.Label(self.fill_box, text="", font=("Segoe UI", 9))
        self.lbl_junk.pack(anchor="w")

        # 4. Balance Indicator
        self.balance_frame = tk.Frame(self.dash_card, padx=8, pady=8, relief=tk.GROOVE, borderwidth=1)
        self.balance_frame.pack(fill=tk.X, pady=6)

        self.lbl_balance_status = tk.Label(
            self.balance_frame,
            text="",
            font=("Segoe UI", 9, "bold"),
            wraplength=260,
            justify=tk.LEFT
        )
        self.lbl_balance_status.pack(fill=tk.X)

        # 5. Buttons Panel
        btn_frame = ttk.Frame(self.dash_card)
        btn_frame.pack(fill=tk.X, side=tk.BOTTOM, pady=4)

        self.btn_save = ttk.Button(btn_frame, text="", command=self.save_yaml)
        self.btn_save.pack(fill=tk.X, pady=2)

        self.btn_save_as = ttk.Button(btn_frame, text="", command=self.save_yaml_as)
        self.btn_save_as.pack(fill=tk.X, pady=2)

        self.btn_load = ttk.Button(btn_frame, text="", command=self.load_yaml)
        self.btn_load.pack(fill=tk.X, pady=2)

        self.btn_reset = ttk.Button(btn_frame, text="", command=self.reset_to_default)
        self.btn_reset.pack(fill=tk.X, pady=2)

    def _bind_tooltips(self):
        """Attaches localized tooltips to interactive elements."""
        # Header
        self._add_tooltip(self.lang_combo, "tip_language")
        self._add_tooltip(self.preset_combo, "tip_preset")

        # Player Info
        self._add_tooltip(self.lbl_player_name, "tip_player_name")
        self._add_tooltip(self.entry_player_name, "tip_player_name")

        # Goals & Bosses
        self._add_tooltip(self.lbl_check_goal, "tip_check_goal")
        self._add_tooltip(self.check_goal_scale, "tip_check_goal")
        self._add_tooltip(self.lbl_required_bosses, "tip_required_bosses")
        self._add_tooltip(self.combo_bosses, "tip_required_bosses")
        self._add_tooltip(self.lbl_egg_shards_req, "tip_egg_shards_req")
        self._add_tooltip(self.spin_shards_req, "tip_egg_shards_req")
        self._add_tooltip(self.lbl_egg_shards_avail, "tip_egg_shards_avail")
        self._add_tooltip(self.spin_shards_avail, "tip_egg_shards_avail")

        # Advancements
        self._add_tooltip(self.radio_adv_vanilla, "tip_adv_vanilla")
        self._add_tooltip(self.radio_adv_milestones, "tip_adv_milestones")
        self._add_tooltip(self.radio_adv_random, "tip_adv_random")
        self._add_tooltip(self.lbl_milestone_step, "tip_milestone_step")
        self._add_tooltip(self.step_scale, "tip_milestone_step")

        # World Checks
        self._add_tooltip(self.chk_biome_checks, "tip_biome_checks")
        self._add_tooltip(self.chk_major_biomes, "tip_major_biomes")
        self._add_tooltip(self.chk_small_biomes, "tip_small_biomes")
        self._add_tooltip(self.chk_endgame_biomes, "tip_endgame_biomes")
        self._add_tooltip(self.chk_structure_checks, "tip_structure_checks")
        self._add_tooltip(self.lbl_trader_trades, "tip_trader_trades")
        self._add_tooltip(self.trader_scale, "tip_trader_trades")

        # Restrictions
        self._add_tooltip(self.chk_hp_restr, "tip_hp_restr")
        self._add_tooltip(self.chk_hunger_restr, "tip_hunger_restr")
        self._add_tooltip(self.chk_reach_restr, "tip_reach_restr")
        self._add_tooltip(self.chk_world_border_restr, "tip_world_border_restr")
        self._add_tooltip(self.chk_inventory_restr, "tip_inventory_restr")
        self._add_tooltip(self.chk_offhand_restr, "tip_offhand_restr")
        self._add_tooltip(self.chk_structure_restr, "tip_structure_restr")
        self._add_tooltip(self.chk_structure_shuffle, "tip_structure_shuffle")
        self._add_tooltip(self.chk_curses, "tip_curses")
        self._add_tooltip(self.lbl_tier_bias, "tip_tier_bias")
        self._add_tooltip(self.tier_scale, "tip_tier_bias")

        # Compasses & Abilities
        self._add_tooltip(self.chk_unvisited_struct, "tip_unvisited_struct")
        self._add_tooltip(self.chk_all_struct, "tip_all_struct")
        self._add_tooltip(self.chk_unvisited_biomes, "tip_unvisited_biomes")
        self._add_tooltip(self.chk_major_biomes_comp, "tip_major_biomes_comp")
        self._add_tooltip(self.chk_minor_biomes_comp, "tip_minor_biomes_comp")
        self._add_tooltip(self.chk_extra_abilities, "tip_extra_abilities")

        # Traps
        self._add_tooltip(self.lbl_traps_pct, "tip_bee_traps")
        self._add_tooltip(self.trap_scale, "tip_bee_traps")

        # Dashboard & Balance
        self._add_tooltip(self.balance_frame, "tip_balance_status")
        self._add_tooltip(self.lbl_balance_status, "tip_balance_status")

        # Buttons
        self._add_tooltip(self.btn_save, "tip_btn_save")
        self._add_tooltip(self.btn_save_as, "tip_btn_save_as")
        self._add_tooltip(self.btn_load, "tip_btn_load")
        self._add_tooltip(self.btn_reset, "tip_btn_reset")

    def _on_language_selected(self, _):
        chosen = self.lang_combo.get().strip()
        new_lang = "ru" if chosen == "Русский" else "en"
        if new_lang != self.current_lang:
            self.current_lang = new_lang
            self._save_app_settings()
            self._apply_language()
            self._recalculate_stats()

    def _apply_language(self):
        """Updates all text in the UI based on the active language."""
        self.title(self.t("title"))
        self.header_title_lbl.config(text=self.t("header_title"))
        self.lang_lbl.config(text=self.t("lang_label"))
        self.preset_lbl.config(text=self.t("preset_label"))

        # Presets combobox
        prev_idx = self.preset_combo.current()
        presets_list = self.t("presets")
        self.preset_combo.config(values=presets_list)
        if 0 <= prev_idx < len(presets_list):
            self.preset_combo.current(prev_idx)
        else:
            self.preset_combo.current(0)

        # Groups & Option Labels
        self.info_grp.config(text=self.t("player_info_grp"))
        self.lbl_player_name.config(text=self.t("player_name"))

        self.goal_grp.config(text=self.t("goal_grp"))
        self.lbl_check_goal.config(text=self.t("check_goal"))
        self.lbl_required_bosses.config(text=self.t("required_bosses"))
        self.lbl_egg_shards_req.config(text=self.t("egg_shards_req"))
        self.lbl_egg_shards_avail.config(text=self.t("egg_shards_avail"))

        self.adv_grp.config(text=self.t("adv_grp"))
        self.radio_adv_vanilla.config(text=self.t("adv_vanilla"))
        self.radio_adv_milestones.config(text=self.t("adv_milestones"))
        self.radio_adv_random.config(text=self.t("adv_random"))
        self.lbl_milestone_step.config(text=self.t("milestone_step"))

        self.check_grp.config(text=self.t("checks_grp"))
        self.chk_biome_checks.config(text=self.t("biome_checks"))
        self.chk_major_biomes.config(text=self.t("major_biomes"))
        self.chk_small_biomes.config(text=self.t("small_biomes"))
        self.chk_endgame_biomes.config(text=self.t("endgame_biomes"))
        self.chk_structure_checks.config(text=self.t("structure_checks"))
        self.lbl_trader_trades.config(text=self.t("trader_trades"))

        self.restr_grp.config(text=self.t("restr_grp"))
        self.chk_hp_restr.config(text=self.t("hp_restr"))
        self.chk_hunger_restr.config(text=self.t("hunger_restr"))
        self.chk_reach_restr.config(text=self.t("reach_restr"))
        self.chk_world_border_restr.config(text=self.t("world_border_restr"))
        self.chk_inventory_restr.config(text=self.t("inventory_restr"))
        self.chk_offhand_restr.config(text=self.t("offhand_restr"))
        self.chk_structure_restr.config(text=self.t("structure_restr"))
        self.chk_structure_shuffle.config(text=self.t("structure_shuffle"))
        self.chk_curses.config(text=self.t("curses"))
        self.lbl_tier_bias.config(text=self.t("tier_bias"))

        self.comp_grp.config(text=self.t("comp_grp"))
        self.chk_unvisited_struct.config(text=self.t("unvisited_struct"))
        self.chk_all_struct.config(text=self.t("all_struct"))
        self.chk_unvisited_biomes.config(text=self.t("unvisited_biomes"))
        self.chk_major_biomes_comp.config(text=self.t("major_biomes_comp"))
        self.chk_minor_biomes_comp.config(text=self.t("minor_biomes_comp"))
        self.chk_extra_abilities.config(text=self.t("extra_abilities"))

        self.trap_grp.config(text=self.t("trap_grp"))
        self.lbl_traps_pct.config(text=self.t("traps_pct"))

        # Dashboard & Buttons
        self.dash_card.config(text=self.t("dash_title"))
        self.chk_box.config(text=self.t("chk_box"))
        self.itm_box.config(text=self.t("itm_box"))
        self.fill_box.config(text=self.t("fill_box"))

        self.btn_save.config(text=self.t("btn_save"))
        self.btn_save_as.config(text=self.t("btn_save_as"))
        self.btn_load.config(text=self.t("btn_load"))
        self.btn_reset.config(text=self.t("btn_reset"))

    def _update_visibility(self):
        """Hides or reveals options depending on choices made in other options."""
        # 1. Shards available visibility (only if required > 0)
        if self.egg_shards_required.get() > 0:
            if not self.shards_avail_row.winfo_ismapped():
                self.shards_avail_row.pack(fill=tk.X, pady=2)
        else:
            if self.shards_avail_row.winfo_ismapped():
                self.shards_avail_row.pack_forget()

        # 2. BACAP Milestone Step (only if advancement_type == 1)
        if self.advancement_type.get() == 1:
            if not self.milestone_step_row.winfo_ismapped():
                self.milestone_step_row.pack(fill=tk.X, pady=4)
        else:
            if self.milestone_step_row.winfo_ismapped():
                self.milestone_step_row.pack_forget()

        # 3. Biome sub-checks (only if biome_checks == True)
        if self.biome_checks.get():
            if not self.biomes_subframe.winfo_ismapped():
                self.biomes_subframe.pack(fill=tk.X)
        else:
            if self.biomes_subframe.winfo_ismapped():
                self.biomes_subframe.pack_forget()

        # 4. Structure shuffle order (only if structure_restriction == True)
        if self.structure_restriction.get():
            if not self.struct_shuffle_frame.winfo_ismapped():
                self.struct_shuffle_frame.pack(fill=tk.X)
        else:
            if self.struct_shuffle_frame.winfo_ismapped():
                self.struct_shuffle_frame.pack_forget()

    def _recalculate_stats(self):
        """Calculates total checks and total items dynamically."""
        # 1. Progression items
        total_prog = TOTAL_RECIPES

        restr_count = 0
        if self.hp_restriction.get():
            restr_count += RESTRICTION_ITEMS["hp_restriction"]
        if self.hunger_restriction.get():
            restr_count += RESTRICTION_ITEMS["hunger_restriction"]
        if self.reach_restriction.get():
            restr_count += RESTRICTION_ITEMS["reach_restriction"]
        if self.world_border_restriction.get():
            restr_count += RESTRICTION_ITEMS["world_border_restriction"]
        if self.structure_restriction.get():
            restr_count += RESTRICTION_ITEMS["structure_restriction"]
        if self.inventory_slot_restriction.get():
            restr_count += RESTRICTION_ITEMS["inventory_slot_restriction"]
        if self.offhand_restriction.get():
            restr_count += RESTRICTION_ITEMS["offhand_restriction"]
        if self.curses_enabled.get():
            restr_count += RESTRICTION_ITEMS["curses_enabled"]
        total_prog += restr_count

        compass_count = 0
        if self.unvisited_structures_compass.get():
            compass_count += 1
        if self.structure_compasses.get():
            compass_count += ALL_STRUCTURE_COMPASS_COUNT
        if self.unvisited_biomes_compass.get():
            compass_count += 1
        if self.major_biomes_compasses.get():
            compass_count += MAJOR_BIOME_COMPASS_COUNT
        if self.minor_biomes_compasses.get():
            compass_count += MINOR_BIOME_COMPASS_COUNT
        total_prog += compass_count

        abilities_count = EXTRA_ABILITIES_COUNT if self.extra_abilities.get() else 0
        total_prog += abilities_count

        shards_count = self.egg_shards_available.get() if self.egg_shards_required.get() > 0 else 0
        total_prog += shards_count

        # 2. Checks calculation
        biome_checks_count = 0
        if self.biome_checks.get():
            if self.major_biome_checks.get():
                biome_checks_count += MAJOR_BIOMES_COUNT
            if self.small_biome_checks.get():
                biome_checks_count += SMALL_BIOMES_COUNT
            if self.endgame_biome_checks.get():
                biome_checks_count += ENDGAME_BIOMES_COUNT

        structure_checks_count = STRUCTURES_COUNT if self.structure_checks.get() else 0
        trader_checks_count = max(0, min(20, self.wandering_trader_trades.get()))
        other_checks = biome_checks_count + structure_checks_count + trader_checks_count

        # Traps & Slots
        trap_pct = max(0, min(100, self.bee_traps.get()))
        trap_slots = math.ceil(total_prog * (trap_pct / 100.0)) if trap_pct > 0 else 0
        total_needed_slots = total_prog + trap_slots

        adv_mode = self.advancement_type.get()
        if adv_mode == 0:  # Vanilla
            adv_checks_count = VANILLA_ADVANCEMENT_COUNT
        elif adv_mode == 1:  # BACAP Milestones
            step = max(1, min(20, self.bacap_advancement_step.get()))
            max_possible = min(TOTAL_BACAP_ADVANCEMENTS // step, MAX_BACAP_MILESTONES)
            needed_milestones = total_needed_slots - other_checks
            adv_checks_count = max(0, min(needed_milestones, max_possible))
        else:  # Random BACAP
            needed = total_needed_slots - other_checks
            adv_checks_count = max(10, self.check_goal.get()) if needed <= 0 else min(needed, 524)

        total_checks = adv_checks_count + other_checks

        # Fillers & Traps
        remaining_slots = total_checks - total_prog
        if remaining_slots > 0:
            traps_count = math.ceil(remaining_slots * (trap_pct / 100.0)) if trap_pct > 0 else 0
            traps_count = min(traps_count, remaining_slots)
            junk_count = remaining_slots - traps_count
        else:
            traps_count = 0
            junk_count = 0

        # Update UI labels with translations
        if adv_mode == 0:
            adv_text = self.t("chk_adv_vanilla", count=adv_checks_count)
        elif adv_mode == 1:
            step = self.bacap_advancement_step.get()
            adv_text = self.t("chk_adv_milestones", step=step, count=adv_checks_count)
        else:
            adv_text = self.t("chk_adv_random", count=adv_checks_count)

        self.lbl_chk_adv.config(text=adv_text)
        self.lbl_chk_biomes.config(text=self.t("chk_biomes", count=biome_checks_count))
        self.lbl_chk_struct.config(text=self.t("chk_struct", count=structure_checks_count))
        self.lbl_chk_trader.config(text=self.t("chk_trader", count=trader_checks_count))
        self.lbl_total_checks.config(text=self.t("total_checks", count=total_checks))

        self.lbl_itm_recipes.config(text=self.t("itm_recipes", count=TOTAL_RECIPES))
        self.lbl_itm_restr.config(text=self.t("itm_restr", count=restr_count))
        self.lbl_itm_compasses.config(text=self.t("itm_compasses", count=compass_count))
        self.lbl_itm_abilities.config(text=self.t("itm_abilities", count=abilities_count))
        self.lbl_itm_shards.config(text=self.t("itm_shards", count=shards_count))
        self.lbl_total_prog.config(text=self.t("total_prog", count=total_prog))

        self.lbl_traps.config(text=self.t("traps_count", count=traps_count))
        self.lbl_junk.config(text=self.t("junk_count", count=junk_count))

        # Balance indicator
        if total_checks >= total_prog:
            self.balance_frame.config(bg="#e8f8e8")
            self.lbl_balance_status.config(
                bg="#e8f8e8",
                fg="#155724",
                text=self.t("balance_ok", total_checks=total_checks, remaining=remaining_slots, total_prog=total_prog)
            )
        else:
            deficit = total_prog - total_checks
            self.balance_frame.config(bg="#f8e8e8")
            self.lbl_balance_status.config(
                bg="#f8e8e8",
                fg="#721c24",
                text=self.t("balance_deficit", total_prog=total_prog, total_checks=total_checks, deficit=deficit)
            )

    def _on_preset_selected(self, _):
        idx = self.preset_combo.current()
        if idx == 0:  # Recommended Standard BACAP
            self.advancement_type.set(1)
            self.bacap_advancement_step.set(5)
            self.biome_checks.set(True)
            self.major_biome_checks.set(True)
            self.small_biome_checks.set(True)
            self.endgame_biome_checks.set(False)
            self.structure_checks.set(True)
            self.wandering_trader_trades.set(10)
            self.hp_restriction.set(True)
            self.hunger_restriction.set(True)
            self.reach_restriction.set(True)
            self.world_border_restriction.set(True)
            self.structure_restriction.set(True)
            self.structure_unlock_shuffle.set(False)
            self.inventory_slot_restriction.set(True)
            self.offhand_restriction.set(True)
            self.curses_enabled.set(False)
            self.craft_tier_bias.set(50)
            self.unvisited_structures_compass.set(True)
            self.structure_compasses.set(False)
            self.unvisited_biomes_compass.set(True)
            self.major_biomes_compasses.set(False)
            self.minor_biomes_compasses.set(False)
            self.extra_abilities.set(False)
            self.bee_traps.set(0)
            self.egg_shards_required.set(0)
            self.egg_shards_available.set(0)
        elif idx == 1:  # Quick Vanilla Game
            self.advancement_type.set(0)
            self.biome_checks.set(False)
            self.major_biome_checks.set(False)
            self.small_biome_checks.set(False)
            self.endgame_biome_checks.set(False)
            self.structure_checks.set(False)
            self.wandering_trader_trades.set(0)
            self.hp_restriction.set(False)
            self.hunger_restriction.set(False)
            self.reach_restriction.set(False)
            self.world_border_restriction.set(False)
            self.structure_restriction.set(False)
            self.structure_unlock_shuffle.set(False)
            self.inventory_slot_restriction.set(False)
            self.offhand_restriction.set(False)
            self.curses_enabled.set(False)
            self.craft_tier_bias.set(0)
            self.unvisited_structures_compass.set(False)
            self.structure_compasses.set(False)
            self.unvisited_biomes_compass.set(False)
            self.major_biomes_compasses.set(False)
            self.minor_biomes_compasses.set(False)
            self.extra_abilities.set(False)
            self.bee_traps.set(0)
            self.egg_shards_required.set(0)
            self.egg_shards_available.set(0)
        elif idx == 2:  # Full Hardcore
            self.advancement_type.set(1)
            self.bacap_advancement_step.set(2)
            self.biome_checks.set(True)
            self.major_biome_checks.set(True)
            self.small_biome_checks.set(True)
            self.endgame_biome_checks.set(True)
            self.structure_checks.set(True)
            self.wandering_trader_trades.set(20)
            self.hp_restriction.set(True)
            self.hunger_restriction.set(True)
            self.reach_restriction.set(True)
            self.world_border_restriction.set(True)
            self.structure_restriction.set(True)
            self.structure_unlock_shuffle.set(True)
            self.inventory_slot_restriction.set(True)
            self.offhand_restriction.set(True)
            self.curses_enabled.set(True)
            self.craft_tier_bias.set(100)
            self.unvisited_structures_compass.set(True)
            self.structure_compasses.set(True)
            self.unvisited_biomes_compass.set(True)
            self.major_biomes_compasses.set(True)
            self.minor_biomes_compasses.set(True)
            self.extra_abilities.set(True)
            self.bee_traps.set(50)
            self.egg_shards_required.set(15)
            self.egg_shards_available.set(20)

    def reset_to_default(self):
        self.preset_combo.current(0)
        self._on_preset_selected(None)

    def save_yaml(self):
        """Saves the YAML file, always prompting with file dialog and remembering directory."""
        self.save_yaml_as()

    def save_yaml_as(self):
        """Prompts the user to choose save location, remembering the chosen folder."""
        p_name = self.player_name.get().strip() or "Player"
        initial_file = f"{p_name}.yaml"

        target_dir = self.last_directory
        if not os.path.isdir(target_dir):
            target_dir = os.path.expanduser("~")

        path = filedialog.asksaveasfilename(
            title=self.t("dialog_save_title"),
            initialdir=target_dir,
            initialfile=initial_file,
            defaultextension=".yaml",
            filetypes=[("YAML files", "*.yaml *.yml"), ("All files", "*.*")]
        )
        if path:
            self.last_directory = os.path.dirname(os.path.abspath(path))
            self._save_app_settings()
            self._write_file(path)

    def _write_file(self, target_path):
        data = self.generate_yaml_dict()
        text = self._render_yaml_text(data)
        try:
            with open(target_path, "w", encoding="utf-8") as f:
                f.write(text)
            messagebox.showinfo(
                self.t("msg_save_success_title"),
                self.t("msg_save_success", path=os.path.abspath(target_path))
            )
        except Exception as e:
            messagebox.showerror(
                self.t("msg_save_err_title"),
                self.t("msg_save_err", error=str(e))
            )

    def load_yaml(self):
        """Loads a YAML file from disk, remembering the chosen folder."""
        target_dir = self.last_directory
        if not os.path.isdir(target_dir):
            target_dir = os.path.expanduser("~")

        path = filedialog.askopenfilename(
            title=self.t("dialog_load_title"),
            initialdir=target_dir,
            filetypes=[("YAML files", "*.yaml *.yml"), ("All files", "*.*")]
        )
        if not path:
            return

        self.last_directory = os.path.dirname(os.path.abspath(path))
        self._save_app_settings()

        try:
            with open(path, "r", encoding="utf-8") as f:
                content = f.read()

            parsed = self._parse_yaml_simple(content)
            self._apply_parsed_yaml(parsed)
            messagebox.showinfo(
                self.t("msg_save_success_title"),
                self.t("msg_load_success", filename=os.path.basename(path))
            )
        except Exception as e:
            messagebox.showerror(
                self.t("msg_load_err_title"),
                self.t("msg_load_err", error=str(e))
            )

    def generate_yaml_dict(self) -> dict:
        """Constructs an Archipelago YAML options dictionary for Minecraft with BACAP."""
        opts = {
            "check_goal": self.check_goal.get(),
            "required_bosses": self.required_bosses.get(),
            "egg_shards_required": self.egg_shards_required.get(),
            "advancement_type": self.advancement_type.get(),
            "structure_checks": self.structure_checks.get(),
            "wandering_trader_trades": self.wandering_trader_trades.get(),
            "reach_restriction": self.reach_restriction.get(),
            "hp_restriction": self.hp_restriction.get(),
            "hunger_restriction": self.hunger_restriction.get(),
            "world_border_restriction": self.world_border_restriction.get(),
            "structure_restriction": self.structure_restriction.get(),
            "inventory_slot_restriction": self.inventory_slot_restriction.get(),
            "offhand_restriction": self.offhand_restriction.get(),
            "curses_enabled": self.curses_enabled.get(),
            "craft_tier_bias": self.craft_tier_bias.get(),
            "unvisited_structures_compass": self.unvisited_structures_compass.get(),
            "structure_compasses": self.structure_compasses.get(),
            "unvisited_biomes_compass": self.unvisited_biomes_compass.get(),
            "major_biomes_compasses": self.major_biomes_compasses.get(),
            "minor_biomes_compasses": self.minor_biomes_compasses.get(),
            "extra_abilities": self.extra_abilities.get(),
            "bee_traps": self.bee_traps.get(),
        }

        # Include only active conditional options
        if self.egg_shards_required.get() > 0:
            opts["egg_shards_available"] = self.egg_shards_available.get()

        if self.advancement_type.get() == 1:
            opts["bacap_advancement_step"] = self.bacap_advancement_step.get()

        if self.biome_checks.get():
            opts["major_biome_checks"] = self.major_biome_checks.get()
            opts["small_biome_checks"] = self.small_biome_checks.get()
            opts["endgame_biome_checks"] = self.endgame_biome_checks.get()
        else:
            opts["biome_checks"] = False
            opts["major_biome_checks"] = False
            opts["small_biome_checks"] = False
            opts["endgame_biome_checks"] = False

        if self.structure_restriction.get():
            opts["structure_unlock_shuffle"] = self.structure_unlock_shuffle.get()

        p_name = self.player_name.get().strip() or "Player"
        return {
            "name": p_name,
            "game": self.game_name,
            "description": self.description.get().strip(),
            self.game_name: opts,
        }

    def _render_yaml_text(self, data: dict) -> str:
        """Formats the yaml document cleanly with comments and proper structure."""
        p_name = data.get("name", "Player")
        game = data.get("game", self.game_name)
        desc = data.get("description", "Generated YAML")
        opts = data.get(game, {})

        lines = [
            f"# Archipelago Multiworld Configuration File",
            f"# Game: {game}",
            f"description: \"{desc}\"",
            f"name: \"{p_name}\"",
            f"game: \"{game}\"",
            f"",
            f"\"{game}\":",
        ]

        for k, v in opts.items():
            if isinstance(v, bool):
                val_str = "true" if v else "false"
            elif isinstance(v, str):
                val_str = f"\"{v}\""
            else:
                val_str = str(v)
            lines.append(f"  {k}: {val_str}")

        return "\n".join(lines) + "\n"

    def _parse_yaml_simple(self, text: str) -> dict:
        """Lightweight parser for Archipelago YAML without requiring PyYAML."""
        try:
            import yaml
            return yaml.safe_load(text) or {}
        except ImportError:
            pass

        # Fallback simple key: value parser
        result = {}
        sub_dict = {}
        in_game_block = False

        for line in text.splitlines():
            line = line.strip()
            if not line or line.startswith("#"):
                continue

            if ":" in line:
                k, v = line.split(":", 1)
                k = k.strip().strip("\"'")
                v = v.strip().strip("\"'")

                # Boolean parsing
                if v.lower() in ("true", "yes", "on"):
                    parsed_val = True
                elif v.lower() in ("false", "no", "off"):
                    parsed_val = False
                elif v.isdigit() or (v.startswith("-") and v[1:].isdigit()):
                    parsed_val = int(v)
                else:
                    parsed_val = v

                if k in ("Minecraft with BACAP", "minecraft_with_bacap", "minecraft_u2vr", "Minecraft"):
                    in_game_block = True
                    continue

                if in_game_block and line.startswith("  "):
                    sub_dict[k] = parsed_val
                else:
                    result[k] = parsed_val

        if sub_dict:
            result[self.game_name] = sub_dict
        return result

    def _apply_parsed_yaml(self, data: dict):
        if not isinstance(data, dict):
            return

        if "name" in data:
            self.player_name.set(str(data["name"]))
        if "description" in data:
            self.description.set(str(data["description"]))

        game_opts = data.get(self.game_name) or data.get("minecraft_with_bacap") or data.get("minecraft_u2vr") or data.get("Minecraft") or {}

        var_map = {
            "check_goal": self.check_goal,
            "required_bosses": self.required_bosses,
            "egg_shards_required": self.egg_shards_required,
            "egg_shards_available": self.egg_shards_available,
            "advancement_type": self.advancement_type,
            "bacap_advancement_step": self.bacap_advancement_step,
            "biome_checks": self.biome_checks,
            "major_biome_checks": self.major_biome_checks,
            "small_biome_checks": self.small_biome_checks,
            "endgame_biome_checks": self.endgame_biome_checks,
            "structure_checks": self.structure_checks,
            "wandering_trader_trades": self.wandering_trader_trades,
            "reach_restriction": self.reach_restriction,
            "hp_restriction": self.hp_restriction,
            "hunger_restriction": self.hunger_restriction,
            "world_border_restriction": self.world_border_restriction,
            "structure_restriction": self.structure_restriction,
            "structure_unlock_shuffle": self.structure_unlock_shuffle,
            "inventory_slot_restriction": self.inventory_slot_restriction,
            "offhand_restriction": self.offhand_restriction,
            "curses_enabled": self.curses_enabled,
            "craft_tier_bias": self.craft_tier_bias,
            "unvisited_structures_compass": self.unvisited_structures_compass,
            "structure_compasses": self.structure_compasses,
            "unvisited_biomes_compass": self.unvisited_biomes_compass,
            "major_biomes_compasses": self.major_biomes_compasses,
            "minor_biomes_compasses": self.minor_biomes_compasses,
            "extra_abilities": self.extra_abilities,
            "bee_traps": self.bee_traps,
        }

        for opt_key, var in var_map.items():
            if opt_key in game_opts:
                val = game_opts[opt_key]
                if isinstance(var, tk.BooleanVar):
                    var.set(bool(val))
                elif isinstance(var, tk.IntVar):
                    try:
                        var.set(int(val))
                    except Exception:
                        pass
                elif isinstance(var, tk.StringVar):
                    var.set(str(val))


def launch(*args):
    """Entry point for Archipelago Launcher."""
    app = YamlConfigurator()
    app.mainloop()


if __name__ == "__main__":
    launch()
