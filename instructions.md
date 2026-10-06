### Краткая сводка по ключевым аспектам:

#### 1. Обработка входящих от Archipelago предметов

• Точка входа пакетов: ReceiveItem.onReceiveItem слушает событие ReceiveItemEvent из библиотеки io.github.archipelagomw и передает его в поток сервера.

• Диспетчеризация наград: ItemManager.giveItemToAll сверяет ID предмета с таблицей DEFAULT_ITEMS, определяет текущий тир APTier и активирует награды APReward:

• RecipeReward — открывает рецепты игрокам и добавляет их в WorldData.

• ItemReward — выдает физические предметы (руды, снаряжение, шалкеры).

• CompassReward — настраивает компасы поиска структур через Lodestone-трекер.

• DragonEggShardReward — увеличивает счетчик осколков в GoalManager.

• MobTrap — активирует ловушки мобов.

• Ограничение крафта: принудительно включен геймрул limitedCrafting = true. Миксины MixinServerRecipeBook и MixinCrafterBlock блокируют добавление и крафт рецептов из пула рандомайзера до момента их разблокировки.


#### 2. Шафл структур (Structure Shuffle)

• Конфигурация: считывается из .apmc файла в поле APMCData.structures (маппинг слотов вроде "Overworld Structure 1" на имена структур).

• Подмена генерации: класс APStructureModifier (механизм NeoForge StructureModifier) перехватывает генерацию структур в мире и динамически переопределяет список разрешенных биомов (setBiomes) в зависимости от того, в каком измерении структура должна находиться в данном сиде.

• Кастомные структуры под Незер:

• NetherVillageStructure (aprandomizer:village_nether) — деревня для Незера.

• NetherPillagerOutpostStructure (aprandomizer:pillager_outpost_nether) — аванпост с фиксом высоты спавна, чтобы не появляться на бедроковом потолке.

• NetherEndCityStructure (aprandomizer:end_city_nether) — город Края в Незере.

• Компасы: через RandomizedStructureLevel и Utils.getStructureWorld компас знает, в каком именно мире находится искомая структура, а ПКМ переключает активную структуру.


#### 3. Другие важные аспекты мода

• Чеки и ачивки: AdvancementManager отслеживает 137 ванильных и кастомных достижений. OnAdvancement синхронизирует прогресс между игроками и отправляет checkLocation(id).

• Вкладка "Received Items" в GUI достижений: ReceivedAdvancementProvider строит наглядное дерево разблокированных предметов и уровней крафта прямо в интерфейсе ванильных ачивок.

• Условия победы и боссы: GoalManager контролирует цели (ачивки, осколки яйца, дракон/визер), выводит боссбары с прогрессом и статусом подключения, а при выполнении переводит статус игры в CLIENT_GOAL.

• Spawn Jail: при старте мира игроки заперты в коробке на Y=300 (spawnjail), игровые правила заморожены до ввода команды /start (StartCommand).

• DeathLink & Minecraft 35 (MC35): поддержка взаимных смертей (OnDeath, onDeathLink) и переброски убитых мобов другим игрокам через BouncePacket (onMC35, OnLivingHurt).

• Автонастройка сервера: MixinPropertyManager автоматически применяет сид и параметры мира из .apmc в server.properties.








# Архитектура и руководство по коду мода Archipelago (NeoForge 1.21.11)

Данный мод представляет собой клиент-рандомайзер для интеграции Minecraft с мультимирным рандомайзером **[Archipelago](https://archipelago.gg)** на платформе **NeoForge** (Minecraft 1.21.11, Java 21).

В основе взаимодействия лежит библиотека `io.github.archipelagomw:Java-Client`.

---

## 1. Точка входа и инициализация

- **Главный класс мода:** [`APRandomizer`](file:///D:/SYNC/Work/Minecraft/Mods/ArchipelagoU2VR/src/main/java/gg/archipelago/aprandomizer/APRandomizer.java)
- **Конфигурационный файл игры (`.apmc`):**
  - При запуске в статическом блоке `createApmcData()` сканируется папка `./APData/` на наличие файлов с расширением `.apmc` (поддерживаются как открытый JSON/Base64, так и zip-архивы с метаданными `archipelago.json`).
  - Данные маппятся в класс [`APMCData`](file:///D:/SYNC/Work/Minecraft/Mods/ArchipelagoU2VR/src/main/java/gg/archipelago/aprandomizer/ap/storage/APMCData.java), откуда считываются: сид мира (`world_seed`), имя игрока, адрес сервера Archipelago, распределение структур (`structures`), требуемые боссы (`required_bosses`), количество осколков яйца дракона (`egg_shards_required`) и количество требуемых ачивок (`advancements_required`).
- **События сервера:**
  - `ServerStartingEvent`: Настраивает правила игры (`LIMITED_CRAFTING = true`, `KEEP_INVENTORY = true`), загружает сохраненные данные мира [`WorldData`](file:///D:/SYNC/Work/Minecraft/Mods/ArchipelagoU2VR/src/main/java/gg/archipelago/aprandomizer/data/WorldData.java), создает менеджеры (`AdvancementManager`, `GoalManager`, `ItemManager`), подключает клиент [`APClient`](file:///D:/SYNC/Work/Minecraft/Mods/ArchipelagoU2VR/src/main/java/gg/archipelago/aprandomizer/ap/APClient.java) и генерирует предстартовую тюрьму (Spawn Jail).

---

## 2. Обработка входящих от Archipelago предметов

### 2.1. Получение сетевого события
1. Класс [`ReceiveItem`](file:///D:/SYNC/Work/Minecraft/Mods/ArchipelagoU2VR/src/main/java/gg/archipelago/aprandomizer/ap/events/ReceiveItem.java) слушает событие `ReceiveItemEvent` от Archipelago Java-Client.
2. В основном потоке сервера вызывается [`ItemManager.giveItemToAll(itemID, index)`](file:///D:/SYNC/Work/Minecraft/Mods/ArchipelagoU2VR/src/main/java/gg/archipelago/aprandomizer/managers/itemmanager/ItemManager.java#L192-L220).
3. Проверяется индекс предмета (чтобы избежать дублирования предметов при реконнектах или ресинхронизации).

### 2.2. Сопоставление ID предметов и система тиров
В [`ItemManager`](file:///D:/SYNC/Work/Minecraft/Mods/ArchipelagoU2VR/src/main/java/gg/archipelago/aprandomizer/managers/itemmanager/ItemManager.java) статическая мапа `DEFAULT_ITEMS` сопоставляет числовые Archipelago Item ID (1–52) с ключами реестра `ResourceKey<APItem>`:
- **Групповые рецепты (`group_recipes/*`):** стрельба из лука, зельеварение, зачарование, ведро, огниво, кровати, бутылочки, щит, удочки, костры, седло, подзорная труба, поводок, кисть.
- **Прогрессивные рецепты (`progressive_recipes/*`):** оружие (камень/медь -> железо -> алмаз), инструменты (камень/медь -> железо -> алмаз/незерит), броня (железо -> алмаз), переработка ресурсов (печи/самородки -> редстоун/блоки/наковальня/изумруды).
- **Стеки предметов (`itemstack/*`):** незеритовый лом, изумруды, зачарованные книги, руды, жемчуг Края, стрелы, шалкербокс и т.д.
- **Опыт (`experience/*`):** 50, 100, 500 опыта.
- **Компасы структур (`compass/*`):** деревня, аванпост, крепость Незера, бастион, город Энда, особняк, монумент, древний город, руины тропы, испытательные палаты.
- **Осколки яйца дракона (`dragon_egg_shard`, ID 43):** учитываются в [`GoalManager`](file:///D:/SYNC/Work/Minecraft/Mods/ArchipelagoU2VR/src/main/java/gg/archipelago/aprandomizer/managers/GoalManager.java).
- **Ловушки (`trap/*`):** например, спавн агрессивных пчел.

### 2.3. Механизм наград ([`APReward`](file:///D:/SYNC/Work/Minecraft/Mods/ArchipelagoU2VR/src/main/java/gg/archipelago/aprandomizer/items/APReward.java))
Каждый [`APItem`](file:///D:/SYNC/Work/Minecraft/Mods/ArchipelagoU2VR/src/main/java/gg/archipelago/aprandomizer/items/APItem.java) состоит из списка тиров [`APTier`](file:///D:/SYNC/Work/Minecraft/Mods/ArchipelagoU2VR/src/main/java/gg/archipelago/aprandomizer/items/APTier.java). При получении следующей копии предмета повышается тир:
- [`RecipeReward`](file:///D:/SYNC/Work/Minecraft/Mods/ArchipelagoU2VR/src/main/java/gg/archipelago/aprandomizer/items/RecipeReward.java):
  - Вызывает `player.awardRecipesByKey(...)` для выдачи рецепта игроку.
  - Добавляет рецепт в список разблокированных в [`WorldData.addUnlockedRecipe(...)`](file:///D:/SYNC/Work/Minecraft/Mods/ArchipelagoU2VR/src/main/java/gg/archipelago/aprandomizer/data/WorldData.java#L152).
- [`ItemReward`](file:///D:/SYNC/Work/Minecraft/Mods/ArchipelagoU2VR/src/main/java/gg/archipelago/aprandomizer/items/ItemReward.java): выдает стак предмета в инвентарь или дропает перед игроком.
- [`CompassReward`](file:///D:/SYNC/Work/Minecraft/Mods/ArchipelagoU2VR/src/main/java/gg/archipelago/aprandomizer/items/CompassReward.java): выдает или обновляет компас поиска структур с Lodestone-трекером.
- [`DragonEggShardReward`](file:///D:/SYNC/Work/Minecraft/Mods/ArchipelagoU2VR/src/main/java/gg/archipelago/aprandomizer/items/DragonEggShardReward.java): инкрементирует счетчик осколков в [`GoalManager`](file:///D:/SYNC/Work/Minecraft/Mods/ArchipelagoU2VR/src/main/java/gg/archipelago/aprandomizer/managers/GoalManager.java).
- [`MobTrap`](file:///D:/SYNC/Work/Minecraft/Mods/ArchipelagoU2VR/src/main/java/gg/archipelago/aprandomizer/items/traps/MobTrap.java): призывает указанных мобов вокруг игрока с настраиваемой агрессией.

### 2.4. Блокировка и разблокировка рецептов (Ограниченный крафт)
- В мире включен геймрул `limitedCrafting = true`. Это означает, что игрок не может скрафтить предмет, если рецепт не открыт в его книге рецептов.
- Модификатор [`MixinServerRecipeBook`](file:///D:/SYNC/Work/Minecraft/Mods/ArchipelagoU2VR/src/main/java/gg/archipelago/aprandomizer/mixin/MixinServerRecipeBook.java): перехватывает добавление рецептов в книгу и блокирует любые рецепты, находящиеся в пуле рандомайзера, пока они не получены от Archipelago.
- Модификатор [`MixinCrafterBlock`](file:///D:/SYNC/Work/Minecraft/Mods/ArchipelagoU2VR/src/main/java/gg/archipelago/aprandomizer/mixin/MixinCrafterBlock.java): запрещает авто-крафтеру (Crafter из 1.21) крафтить заблокированные рецепты.
- При входе нового игрока метод [`ItemManager.catchUpPlayer`](file:///D:/SYNC/Work/Minecraft/Mods/ArchipelagoU2VR/src/main/java/gg/archipelago/aprandomizer/managers/itemmanager/ItemManager.java#L226) нагоняет все полученные миром рецепты и предметы по сохраненному индексу игрока.

---

## 3. Шафл структур (Structure Shuffle) и генерация мира

Шафл структур позволяет поменять измерение появления ключевых структур (например, деревня или аванпост генерируются в Незере, или крепость в обычном мире).

### 3.1. Откуда берутся данные о шафле?
Из файла `.apmc` в [`APMCData.structures`](file:///D:/SYNC/Work/Minecraft/Mods/ArchipelagoU2VR/src/main/java/gg/archipelago/aprandomizer/ap/storage/APMCData.java#L12-L13):
```json
{
  "structures": {
    "Overworld Structure 1": "Village",
    "Overworld Structure 2": "Pillager Outpost",
    "Nether Structure 1": "Nether Fortress",
    "Nether Structure 2": "Bastion Remnant",
    "The End Structure": "End City"
  }
}
```

### 3.2. Как мод перенаправляет генерацию структур?
1. **NeoForge Structure Modifiers:**
   Класс [`APStructureModifier`](file:///D:/SYNC/Work/Minecraft/Mods/ArchipelagoU2VR/src/main/java/gg/archipelago/aprandomizer/modifiers/APStructureModifier.java) реализует интерфейс `StructureModifier`.
2. **Метод `loadTags()`:**
   Парсит мапу `APMCData.structures` и сопоставляет идентификаторы структур с целевыми измерениями (`Level.OVERWORLD`, `Level.NETHER`, `Level.END`).
3. **Метод `modify(...)`:**
   Во время генерации чанков NeoForge опрашивает зарегистрированные модификаторы:
   - Модификатор проверяет, в каком измерении должна находиться структура по настройкам Archipelago.
   - С помощью `builder.getStructureSettings().setBiomes(biomes)` **динамически подменяет список биомов**, в которых структуре разрешено появляться. Если структура должна быть в Незере, ее биомы для Обычного мира очищаются, и наоборот.
   - Базовые наборы биомов подтягиваются через дата-карты [`APDataMaps.DEFAULT_STRUCTURE_BIOMES`](file:///D:/SYNC/Work/Minecraft/Mods/ArchipelagoU2VR/src/main/java/gg/archipelago/aprandomizer/datamaps/APDataMaps.java) и провайдер [`APDataMapProvider`](file:///D:/SYNC/Work/Minecraft/Mods/ArchipelagoU2VR/src/main/java/gg/archipelago/aprandomizer/data/datamaps/APDataMapProvider.java).

### 3.3. Специальные структуры для Незера
Для корректной генерации ванильных структур обычного мира и Энда в Незере созданы кастомные структуры:
- [`NetherVillageStructure`](file:///D:/SYNC/Work/Minecraft/Mods/ArchipelagoU2VR/src/main/java/gg/archipelago/aprandomizer/structures/NetherVillageStructure.java) (`aprandomizer:village_nether`) — деревня, адаптированная под ландшафт Незера, с кастомными частями на NBT.
- [`NetherPillagerOutpostStructure`](file:///D:/SYNC/Work/Minecraft/Mods/ArchipelagoU2VR/src/main/java/gg/archipelago/aprandomizer/structures/NetherPillagerOutpostStructure.java) (`aprandomizer:pillager_outpost_nether`) — аванпост разбойников для Незера с защитой от спавна на бедрок-потолке.
- [`NetherEndCityStructure`](file:///D:/SYNC/Work/Minecraft/Mods/ArchipelagoU2VR/src/main/java/gg/archipelago/aprandomizer/structures/NetherEndCityStructure.java) (`aprandomizer:end_city_nether`) — город Края в Незере.
- Плотность и частота спавна настраиваются через [`APStructureSets`](file:///D:/SYNC/Work/Minecraft/Mods/ArchipelagoU2VR/src/main/java/gg/archipelago/aprandomizer/structures/APStructureSets.java) и JSON-файлы в `data/minecraft/worldgen/structure_set/`.

### 3.4. Компасы для поиска структур ([`CompassReward`](file:///D:/SYNC/Work/Minecraft/Mods/ArchipelagoU2VR/src/main/java/gg/archipelago/aprandomizer/items/CompassReward.java))
- Компасы не привязаны жестко к ванильным мирам.
- Через [`RandomizedStructureLevel`](file:///D:/SYNC/Work/Minecraft/Mods/ArchipelagoU2VR/src/main/java/gg/archipelago/aprandomizer/structures/level/RandomizedStructureLevel.java) и [`Utils.getStructureWorld(...)`](file:///D:/SYNC/Work/Minecraft/Mods/ArchipelagoU2VR/src/main/java/gg/archipelago/aprandomizer/common/Utils/Utils.java#L162) компас динамически определяет, в каком измерении сгенерировалась нужная структура для данного сида.
- В [`OnPlayerInteract`](file:///D:/SYNC/Work/Minecraft/Mods/ArchipelagoU2VR/src/main/java/gg/archipelago/aprandomizer/common/events/OnPlayerInteract.java#L60-L96) ПКМ с компасом в руке переключает его между всеми открытыми структурами игрока.
- В [`OnDimensionChange`](file:///D:/SYNC/Work/Minecraft/Mods/ArchipelagoU2VR/src/main/java/gg/archipelago/aprandomizer/common/events/OnDimensionChange.java) при смене измерения или респавне компасы автоматически обновляют координаты цели под текущее измерение.

---

## 4. Другие важные аспекты мода

### 4.1. Чеки Archipelago (Advancements / Локации)
- Класс [`AdvancementManager`](file:///D:/SYNC/Work/Minecraft/Mods/ArchipelagoU2VR/src/main/java/gg/archipelago/aprandomizer/managers/advancementmanager/AdvancementManager.java) содержит мапу `DEFAULT_LOCATIONS` (137 достижений ваниллы от 1.12 до 1.21.11, плюс кастомные достижения Archipelago).
- [`OnAdvancement`](file:///D:/SYNC/Work/Minecraft/Mods/ArchipelagoU2VR/src/main/java/gg/archipelago/aprandomizer/common/events/OnAdvancement.java):
  - При выполнении игроком критерия достижения прогресс синхронизируется на всех игроков сервера.
  - При получении достижения проверяется, зарегистрировано ли оно как чек Archipelago: отправляется `apClient.checkLocation(id)`, прогресс сохраняется в [`WorldData`](file:///D:/SYNC/Work/Minecraft/Mods/ArchipelagoU2VR/src/main/java/gg/archipelago/aprandomizer/data/WorldData.java) и обновляется цель.
- **Вкладка полученных предметов в GUI достижений:**
  [`ReceivedAdvancementProvider`](file:///D:/SYNC/Work/Minecraft/Mods/ArchipelagoU2VR/src/main/java/gg/archipelago/aprandomizer/data/advancements/ReceivedAdvancementProvider.java) создает специальное дерево достижений "Received Items", позволяя игроку прямо в меню достижений видеть, какие рецепты и предметы уже получены из Archipelago.
- **Кастомные достижения Archipelago:**
  [`APAdvancementProvider`](file:///D:/SYNC/Work/Minecraft/Mods/ArchipelagoU2VR/src/main/java/gg/archipelago/aprandomizer/data/advancements/APAdvancementProvider.java) добавляет достижения: "Getting Wood", "Time to Strike", "Time to Mine", "Hot Topic", "Bake Bread", "Cow Tipper", "When Pigs Fly", "Overkill", "On A Rail", "The Lie" и др.

### 4.2. Цели игры и боссы ([`GoalManager`](file:///D:/SYNC/Work/Minecraft/Mods/ArchipelagoU2VR/src/main/java/gg/archipelago/aprandomizer/managers/GoalManager.java))
- Поддерживаются условия победы:
  - Количество достижений (`advancements_required`).
  - Осколки яйца дракона (`egg_shards_required`).
  - Убийство требуемых боссов (`required_bosses`: `NONE`, `ENDER_DRAGON`, `WITHER`, `BOTH`).
- Статус отображается на экране через боссбары:
  - Синий: прогресс ачивок.
  - Белый: прогресс осколков яйца.
  - Красный: статус подключения к серверу Archipelago.
- При смерти Дракона или Визера ([`GoalManager.onBossDeath`](file:///D:/SYNC/Work/Minecraft/Mods/ArchipelagoU2VR/src/main/java/gg/archipelago/aprandomizer/managers/GoalManager.java#L178)): проверяется выполнение остальных условий, и клиенту Archipelago отсылается статус завершения игры `ClientStatus.CLIENT_GOAL`.
- [`MixinItemStack`](file:///D:/SYNC/Work/Minecraft/Mods/ArchipelagoU2VR/src/main/java/gg/archipelago/aprandomizer/mixin/MixinItemStack.java): добавляет подсказку (Lore) в череп визер-скелета и жемчуг Края, информируя, обязателен ли данный босс для победы в текущем сиде.
- [`MixinSpawnGateways`](file:///D:/SYNC/Work/Minecraft/Mods/ArchipelagoU2VR/src/main/java/gg/archipelago/aprandomizer/mixin/MixinSpawnGateways.java): сразу открывает все 20 врат Энда при первом спавне Дракона, облегчая доступ к островам Энда.

### 4.3. Предстартовая тюрьма (Spawn Jail)
- До старта игры флаг `jailPlayers = true` изолирует игроков в летающей коробке `spawnjail` на высоте Y=300 над спавном.
- Время, погода, спавн мобов, разрушение блоков и дроп предметов заморожены.
- Команда `/start` (или `/forcestart`):
  - Реализована в [`StartCommand`](file:///D:/SYNC/Work/Minecraft/Mods/ArchipelagoU2VR/src/main/java/gg/archipelago/aprandomizer/common/commands/StartCommand.java).
  - Стирает структуру тюрьмы, включает все игровые правила, лечит игроков, выдает стартовые предметы из Archipelago (`starting_items`), телепортирует игроков на точку спавна и переводит клиента в статус `CLIENT_PLAYING`.

### 4.4. DeathLink и Minecraft 35 (MC35)
- **DeathLink:**
  - Реализован в [`OnDeath`](file:///D:/SYNC/Work/Minecraft/Mods/ArchipelagoU2VR/src/main/java/gg/archipelago/aprandomizer/common/events/OnDeath.java) и [`onDeathLink`](file:///D:/SYNC/Work/Minecraft/Mods/ArchipelagoU2VR/src/main/java/gg/archipelago/aprandomizer/ap/events/onDeathLink.java).
  - При смерти игрока посылается пакет DeathLink. При получении внешнего пакета DeathLink все игроки на сервере мгновенно погибают с сохранением причины смерти в чате.
  - Управляется командой `/ap deathlink <true|false>`.
- **Minecraft 35 (MC35 — режим Battle Royale мобов):**
  - Реализован в [`OnLivingHurt`](file:///D:/SYNC/Work/Minecraft/Mods/ArchipelagoU2VR/src/main/java/gg/archipelago/aprandomizer/common/events/OnLivingHurt.java) и [`onMC35`](file:///D:/SYNC/Work/Minecraft/Mods/ArchipelagoU2VR/src/main/java/gg/archipelago/aprandomizer/ap/events/onMC35.java).
  - При убийстве моба игроком, данные моба упаковываются в `BouncePacket` с тегом `"MC35"` и отправляются на сервер Archipelago.
  - Другие подключенные игроки с включенным MC35 получают этого моба (с полным здоровьем), и он спавнится рядом с ними.
  - Управляется командой `/ap mc35 <true|false>` и отдельной командой `/bounce`.

### 4.5. Адаптация механик и измерений
- [`APDimensionTypes`](file:///D:/SYNC/Work/Minecraft/Mods/ArchipelagoU2VR/src/main/java/gg/archipelago/aprandomizer/dimensions/APDimensionTypes.java): в Незере включено свойство `CAN_START_RAID = true`, а пиглины больше не превращаются в зомби.
- [`MixinRaid`](file:///D:/SYNC/Work/Minecraft/Mods/ArchipelagoU2VR/src/main/java/gg/archipelago/aprandomizer/mixin/MixinRaid.java): корректирует спавн рейдов при нахождении деревни в Незере.
- [`OnModifyCustomSpawners`](file:///D:/SYNC/Work/Minecraft/Mods/ArchipelagoU2VR/src/main/java/gg/archipelago/aprandomizer/common/events/OnModifyCustomSpawners.java): добавляет `CatSpawner` в Незер и Край, чтобы кошки могли спавниться возле перемещенных деревень.
- [`APGlobalLootModifierProvider`](file:///D:/SYNC/Work/Minecraft/Mods/ArchipelagoU2VR/src/main/java/gg/archipelago/aprandomizer/data/loot/APGlobalLootModifierProvider.java): повышает шансы выпадения трезубца из утопленников (до 25%) и черепов визер-скелетов (до 33% базово + бонус добычи), чтобы ачивки были сбалансированы для прохождения рандомайзера.

### 4.6. Автоматическая настройка сервера ([`MixinPropertyManager`](file:///D:/SYNC/Work/Minecraft/Mods/ArchipelagoU2VR/src/main/java/gg/archipelago/aprandomizer/mixin/MixinPropertyManager.java))
При запуске сервера из `.apmc` автоматически подставляются значения в `server.properties`:
- `level-seed` = `world_seed` из рандомайзера.
- `level-name` = `Archipelago-<seed_name>-P<player_id>`.
- `spawn-protection` = 0.
- В режиме гонки (`race = true`) принудительно выставляется `gamemode=survival` и `view-distance=10`.

---

## 5. Переработанный пул предметов Архипелага (11 активных предметов)

В соответствии с конфигурацией пула в [`ItemManager`](file:///D:/SYNC/Work/Minecraft/Mods/ArchipelagoU2VR/src/main/java/gg/archipelago/aprandomizer/managers/itemmanager/ItemManager.java), в таблице `DEFAULT_ITEMS` активны только 11 предметов, а вся старая таблица сохранена без удаления кода.

### Таблица констант и наград:
| ID | Константа | Ресурсный ключ (`APItems`) | Награда (`APReward`) | Описание механики и ограничения |
| **1L** | `RECIPE_UNLOCK` | `aprandomizer:upgrade/recipe_unlock` | [`RecipeUnlockReward`](file:///D:/SYNC/Work/Minecraft/Mods/ArchipelagoU2VR/src/main/java/gg/archipelago/aprandomizer/items/RecipeUnlockReward.java) | Разблокирует 1 узел рецептов из дерева `recipe_tree.json` через [`RecipeTreeManager`](file:///D:/SYNC/Work/Minecraft/Mods/ArchipelagoU2VR/src/main/java/gg/archipelago/aprandomizer/managers/recipemanager/RecipeTreeManager.java). Распределение регулируется процентной настройкой `recipe_tier_bias` (0..100%): при 100% строго минимальный оставшийся тир, при 0% полный рандом, при 1..99% экспоненциальный градиент $(1 - P/100)^{T - T_{\min}}$. |
| **2L** | `HP_UPGRADE` | `aprandomizer:upgrade/hp` | [`HpReward`](file:///D:/SYNC/Work/Minecraft/Mods/ArchipelagoU2VR/src/main/java/gg/archipelago/aprandomizer/items/HpReward.java) | Базовое HP игрока: 6 (3 сердца). Каждый итем добавляет +2 HP (+1 сердце) вплоть до 40 HP. |
| **3L** | `HUNGER_UPGRADE` | `aprandomizer:upgrade/hunger` | [`HungerReward`](file:///D:/SYNC/Work/Minecraft/Mods/ArchipelagoU2VR/src/main/java/gg/archipelago/aprandomizer/items/HungerReward.java) | Базовый лимит сытости: 8 ед. (4 окорочка). Каждый итем добавляет +2 ед. сытости (+1 окорочек) до 20 ед. (10 окорочков). |
| **4L** | `REACH_UPGRADE` | `aprandomizer:upgrade/reach` | [`ReachReward`](file:///D:/SYNC/Work/Minecraft/Mods/ArchipelagoU2VR/src/main/java/gg/archipelago/aprandomizer/items/ReachReward.java) | Дальность копания блоков: базовая 2.5 блока. С каждым улучшением: 3.0 -> 3.5 -> 4.5 блока (ванильное значение). |
| **5L** | `WORLD_BORDER_UPGRADE` | `aprandomizer:upgrade/world_border` | [`WorldBorderReward`](file:///D:/SYNC/Work/Minecraft/Mods/ArchipelagoU2VR/src/main/java/gg/archipelago/aprandomizer/items/WorldBorderReward.java) | Диаметр границы мира: базовый 1000 блоков. С каждым улучшением: 1500, 2500, 4000, 6000, максимальный размер (в Незере пропорционально 1:8). |
| **6L** | `STRUCTURE_UNLOCK` | `aprandomizer:upgrade/structure_unlock` | [`StructureUnlockReward`](file:///D:/SYNC/Work/Minecraft/Mods/ArchipelagoU2VR/src/main/java/gg/archipelago/aprandomizer/items/StructureUnlockReward.java) | Открывает доступ к структурам. Порядок определяется настройкой `structure_unlock_shuffle`: при `false` — последовательно по дереву `recipe_tree.json` (Деревня -> Аванпост -> Палаты -> Древний город -> Монумент -> Крепость -> Бастион -> Город Энда); при `true` — в случайном порядке из оставшихся заблокированных. В закрытых структурах игрока отбрасывает назад. |
| **7L** | `OFFHAND_UNLOCK` | `aprandomizer:upgrade/offhand` | [`OffhandReward`](file:///D:/SYNC/Work/Minecraft/Mods/ArchipelagoU2VR/src/main/java/gg/archipelago/aprandomizer/items/OffhandReward.java) | Слот второй руки заблокирован до получения этого предмета. Предметы, положенные во вторую руку, возвращаются в инвентарь. |
| **8L** | `INVENTORY_SLOT_UPGRADE` | `aprandomizer:upgrade/inventory_slot` | [`InventorySlotReward`](file:///D:/SYNC/Work/Minecraft/Mods/ArchipelagoU2VR/src/main/java/gg/archipelago/aprandomizer/items/InventorySlotReward.java) | Доступен только хотбар (9 слотов). Каждый полученный итем открывает ровно по 1 слоту основного инвентаря (9..35) вплоть до 36 слотов. В заблокированные слоты нельзя подбирать предметы с земли (`ItemEntityPickupEvent.Pre`), нельзя помещать предметы вручную ([`MixinSlot`](file:///D:/SYNC/Work/Minecraft/Mods/ArchipelagoU2VR/src/main/java/gg/archipelago/aprandomizer/mixin/MixinSlot.java)), а в GUI заблокированные слоты отображаются со специальной иконкой блокировки ([`MixinAbstractContainerScreen`](file:///D:/SYNC/Work/Minecraft/Mods/ArchipelagoU2VR/src/main/java/gg/archipelago/aprandomizer/mixin/MixinAbstractContainerScreen.java)). |
| **9L** | `TRAP_ITEM` | `aprandomizer:trap/random` | [`RandomTrapReward`](file:///D:/SYNC/Work/Minecraft/Mods/ArchipelagoU2VR/src/main/java/gg/archipelago/aprandomizer/items/RandomTrapReward.java) | Активирует случайную ловушку из 6 видов: Спавн злых пчёл, Спавн скелетов, Спавн зомби, Мусор в инвентарь (хлам в свободные слоты игрока), Слепота, Падающий песок. |
| **10L** | `MONSTER_SUN_BURNING_DISABLED` | `aprandomizer:modifier/monster_sun_burning` | [`MonsterSunBurningReward`](file:///D:/SYNC/Work/Minecraft/Mods/ArchipelagoU2VR/src/main/java/gg/archipelago/aprandomizer/items/MonsterSunBurningReward.java) | Перманентное усложнение: зомби, скелеты и другие монстры больше не сгорают под лучами солнца. |
| **11L** | `MONSTER_SPAWN_LIGHT_DISABLED` | `aprandomizer:modifier/monster_spawn_light` | [`MonsterSpawnLightReward`](file:///D:/SYNC/Work/Minecraft/Mods/ArchipelagoU2VR/src/main/java/gg/archipelago/aprandomizer/items/MonsterSpawnLightReward.java) | Перманентное усложнение: монстры начинают спавниться при любом уровне освещения (отключение проверки света). |

### Менеджер ограничений ([`RestrictionManager`](file:///D:/SYNC/Work/Minecraft/Mods/ArchipelagoU2VR/src/main/java/gg/archipelago/aprandomizer/managers/RestrictionManager.java))
Все проверки и ограничения реализованы через события NeoForge без изменения исходных методов мода:
- `PlayerTickEvent.Post`: принудительное удержание лимита сытости (`getMaxFoodLevel()`), блокировка второй руки и инвентаря.
- `PlayerInteractEvent.RightClickItem`: запрет использования второй руки при ее блокировке.
- `LivingIncomingDamageEvent`: блокировка урона от огня для монстров на открытом воздухе днем.
- `MobSpawnEvent.PositionCheck` и `MobSpawnEvent.SpawnPlacementCheck`: подтверждение спавна монстров независимо от света.
- `WorldData` и `RestrictionsData`: сохранение прогресса всех 11 ограничений в NBT мира через DFU кодеки (`SavedData`).

---

## 6. Дополнительные механики

### 6.1. Общий сундук между слотами Архипелага ([`SharedChestManager`](file:///D:/SYNC/Work/Minecraft/Mods/ArchipelagoU2VR/src/main/java/gg/archipelago/aprandomizer/managers/chest/SharedChestManager.java))
- **Кастомный блок и предмет:** `aprandomizer:shared_chest` ([`SharedChestBlock`](file:///D:/SYNC/Work/Minecraft/Mods/ArchipelagoU2VR/src/main/java/gg/archipelago/aprandomizer/managers/chest/SharedChestBlock.java), [`APBlocks`](file:///D:/SYNC/Work/Minecraft/Mods/ArchipelagoU2VR/src/main/java/gg/archipelago/aprandomizer/APBlocks.java)).
  - Имеет уникальную текстуру и модель таинственного хранилища на базе блоков испытаний (Trial Vault) с испусканием частиц портала и атмосферными звуками сундука Края.
  - При добыче дропает сам блок сундука (не ломается на составные части, не требует Шелкового касания).
  - Предметы внутри хранятся глобально, при разрушении блока не выпадают на землю.
- **Общий инвентарь:** стандартные 27 слотов (3 ряда по 9), доступные одновременно всем слотам в комнате Архипелага.
- **Взаимодействие через Архипелаг:**
  - Ключ в Data Storage Архипелага: `mc_shared_chest` (комнатный ключ, общий для всех слотов).
  - При подключении сервер оформляет подписку на обновления (`dataStorageSetNotify`) и запрашивает текущее состояние (`dataStorageGet`).
  - Ответы и сетевые уведомления обрабатываются в [`SharedChestEventListener`](file:///D:/SYNC/Work/Minecraft/Mods/ArchipelagoU2VR/src/main/java/gg/archipelago/aprandomizer/ap/events/SharedChestEventListener.java) через события `RetrievedEvent` и `SetReplyEvent`.
  - При изменении инвентаря игроком срабатывает интеллектуальный дебаунс (2 тика) для батчинга быстрых кликов, после чего отправляется `SetPacket` с операцией `REPLACE` и полным списком слотов.
  - Данные предметов кодируются через нативный ванильный `ItemStackWithSlot.CODEC` в формате JSON, полностью сохраняя все компоненты, чары, кастомные имена, зелья и вложенные шалкеры.
  - При получении обновления от другого слота все открытые у игроков интерфейсы сундука мгновенно обновляются в реальном времени (`menu.sendAllDataToRemote()`).
  - Локальная копия сохраняется в [`WorldData`](file:///D:/SYNC/Work/Minecraft/Mods/ArchipelagoU2VR/src/main/java/gg/archipelago/aprandomizer/data/WorldData.java) мира, поэтому сундук работает даже при временном разрыве соединения с Архипелагом.
- **Крафт и команды:**
  - Рецепт 1: Обычный сундук, окруженный 4 обсидианами и 4 жемчужинами Края (`recipe/shared_chest.json`).
  - Рецепт 2: Сундук Края + Жемчуг Края (`recipe/shared_chest_from_ender_chest.json`).
  - Оба рецепта автоматически открываются игрокам при входе в мир.
  - Команда `/ap chest`: выдает 1 сундук Архипелага игроку.
  - Команда `/ap open_chest`: открывает общий сундук напрямую.

### 6.2. Товары Архипелага у Бродячего торговца ([`WanderingTraderManager`](file:///D:/SYNC/Work/Minecraft/Mods/ArchipelagoU2VR/src/main/java/gg/archipelago/aprandomizer/managers/trademanager/WanderingTraderManager.java))
- Торговец продает чеки Архипелага из всех игр мультимира за изумруды.
- Настройка `wandering_trader_trades` (0..20, по умолчанию 5) задает количество чеков (ID 139..158).
- Скаут данных запрашивается при подключении (`scoutLocations`), в подсказке предмета отображается имя и игра получателя.
- При покупке товара отправляется чек в Архипелаг (`checkLocation`).

### 6.3. Запрет установки кроватей в Энде ([`OnPlayerInteract`](file:///D:/SYNC/Work/Minecraft/Mods/ArchipelagoU2VR/src/main/java/gg/archipelago/aprandomizer/common/events/OnPlayerInteract.java))
- В измерении Края (`Level.END`) полностью заблокированы как клик по блоку кровати, так и попытка разместить блок кровати из руки, предотвращая взрывы кроватей.


