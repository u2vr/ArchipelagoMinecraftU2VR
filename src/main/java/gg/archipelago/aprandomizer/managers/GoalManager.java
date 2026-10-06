package gg.archipelago.aprandomizer.managers;

import gg.archipelago.aprandomizer.APRandomizer;
import gg.archipelago.aprandomizer.ap.APClient;
import gg.archipelago.aprandomizer.ap.storage.APMCData;
import gg.archipelago.aprandomizer.common.Utils.Utils;
import gg.archipelago.aprandomizer.data.WorldData;
import gg.archipelago.aprandomizer.managers.advancementmanager.AdvancementManager;
import gg.archipelago.aprandomizer.managers.itemmanager.ItemManager;
import io.github.archipelagomw.ClientStatus;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.bossevents.CustomBossEvent;
import net.minecraft.server.bossevents.CustomBossEvents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import org.jetbrains.annotations.Nullable;

import java.awt.Color;

@EventBusSubscriber
public class GoalManager {

    int advancementsRequired;
    int dragonEggShardsRequired;
    int totalDragonEggShards;
    private boolean gameCompletedAnnounced = false;

    @Nullable
    private CustomBossEvent advancementInfoBar;
    @Nullable
    private CustomBossEvent eggInfoBar;
    @Nullable
    private CustomBossEvent connectionInfoBar;

    private final MinecraftServer server;
    private final APMCData apmc;
    private final AdvancementManager advancementManager;
    private final WorldData worldData;

    public GoalManager(MinecraftServer server, APMCData apmc, AdvancementManager advancementManager, WorldData worldData) {
        this.server = server;
        this.apmc = apmc;
        this.advancementManager = advancementManager;
        this.worldData = worldData;
        advancementsRequired = apmc.advancements_required;
        dragonEggShardsRequired = apmc.egg_shards_required;
        totalDragonEggShards = apmc.egg_shards_available;
        initializeInfoBar();
    }

    public void initializeInfoBar() {
        CustomBossEvents bossInfoManager = server.getCustomBossEvents();
        advancementInfoBar = bossInfoManager.create(server.getLevel(Level.OVERWORLD).getRandom(), Identifier.fromNamespaceAndPath(APRandomizer.MODID, "advancementinfobar"), Component.literal(""));
        advancementInfoBar.setMax(Math.max(1, advancementsRequired));
        advancementInfoBar.setColor(BossEvent.BossBarColor.BLUE);
        advancementInfoBar.setOverlay(BossEvent.BossBarOverlay.NOTCHED_10);

        eggInfoBar = bossInfoManager.create(server.getLevel(Level.OVERWORLD).getRandom(), Identifier.fromNamespaceAndPath(APRandomizer.MODID, "egginfobar"), Component.literal(""));
        eggInfoBar.setMax(Math.max(1, dragonEggShardsRequired));
        eggInfoBar.setColor(BossEvent.BossBarColor.WHITE);
        eggInfoBar.setOverlay(BossEvent.BossBarOverlay.NOTCHED_6);

        connectionInfoBar = bossInfoManager.create(server.getLevel(Level.OVERWORLD).getRandom(), Identifier.fromNamespaceAndPath(APRandomizer.MODID, "connectioninfobar"), Component.literal("Нет подключения к Архипелагу").withStyle(Style.EMPTY.withColor(ChatFormatting.RED)));
        connectionInfoBar.setMax(1);
        connectionInfoBar.setValue(1);
        connectionInfoBar.setColor(BossEvent.BossBarColor.RED);
        connectionInfoBar.setOverlay(BossEvent.BossBarOverlay.PROGRESS);

        if (isGameCompleted()) {
            gameCompletedAnnounced = true;
        }

        updateInfoBar();
        advancementInfoBar.setVisible(true);
        eggInfoBar.setVisible(dragonEggShardsRequired > 0 && currentEggShards() < dragonEggShardsRequired);
        connectionInfoBar.setVisible(true);
    }

    public void updateGoal(boolean canFinish) {
        updateInfoBar();
        if (canFinish)
            checkGoalCompletion();
        checkBossMessages();
    }

    public boolean isChecksDone() {
        return advancementsRequired <= 0 || advancementManager.getFinishedAmount() >= advancementsRequired;
    }

    public boolean isGameCompleted() {
        if (!goalsDone()) return false;
        WorldData wd = APRandomizer.getWorldData();
        if (wd == null) return false;
        if (apmc.required_bosses.hasDragon() && !wd.isDragonKilled()) return false;
        if (apmc.required_bosses.hasWither() && !wd.isWitherKilled()) return false;
        return true;
    }

    public String getAdvancementRemainingString() {
        if (isGameCompleted()) {
            return "Игра пройдена";
        }
        if (!isChecksDone()) {
            return String.format("Чеки: %d / %d", advancementManager.getFinishedAmount(), advancementsRequired);
        }
        WorldData wd = APRandomizer.getWorldData();
        boolean dragonKilled = wd != null && wd.isDragonKilled();
        boolean witherKilled = wd != null && wd.isWitherKilled();

        boolean needDragon = apmc.required_bosses.hasDragon() && !dragonKilled;
        boolean needWither = apmc.required_bosses.hasWither() && !witherKilled;

        if (needDragon && needWither) {
            return "Убить дракона Края и Иссушителя";
        } else if (needDragon) {
            return "Убить дракона Края";
        } else if (needWither) {
            return "Убить Иссушителя";
        } else {
            return "Собрать осколки яйца дракона";
        }
    }

    public String getEggShardsRemainingString() {
        if (dragonEggShardsRequired > 0) {
            return String.format("Осколки яйца дракона: %d / %d", currentEggShards(), dragonEggShardsRequired);
        }
        return "";
    }

    private int currentEggShards() {
        return worldData.getDragonEggShards();
    }

    public void incrementDragonEggShards() {
        worldData.incrementDragonEggShards();
        updateGoal(true);
    }

    public void updateInfoBar() {
        MinecraftServer server = APRandomizer.getServer();
        if (server == null || advancementInfoBar == null || connectionInfoBar == null || eggInfoBar == null)
            return;
        server.execute(() -> {
            var players = server.getPlayerList().getPlayers();
            advancementInfoBar.setPlayers(players);
            eggInfoBar.setPlayers(players);
            connectionInfoBar.setPlayers(players);
        });

        connectionInfoBar.setVisible(!APRandomizer.isConnected());

        boolean showEggBar = dragonEggShardsRequired > 0 && currentEggShards() < dragonEggShardsRequired;
        eggInfoBar.setVisible(showEggBar);
        if (showEggBar) {
            eggInfoBar.setMax(Math.max(1, dragonEggShardsRequired));
            eggInfoBar.setValue(currentEggShards());
            eggInfoBar.setName(Component.literal(getEggShardsRemainingString()));
        }

        if (isGameCompleted()) {
            advancementInfoBar.setName(Component.literal("Игра пройдена").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
            advancementInfoBar.setColor(BossEvent.BossBarColor.GREEN);
            advancementInfoBar.setOverlay(BossEvent.BossBarOverlay.PROGRESS);
            advancementInfoBar.setMax(1);
            advancementInfoBar.setValue(1);
            advancementInfoBar.setVisible(true);
        } else if (!isChecksDone()) {
            advancementInfoBar.setName(Component.literal(String.format("Чеки: %d / %d", advancementManager.getFinishedAmount(), advancementsRequired)));
            advancementInfoBar.setColor(BossEvent.BossBarColor.BLUE);
            advancementInfoBar.setOverlay(BossEvent.BossBarOverlay.NOTCHED_10);
            advancementInfoBar.setMax(Math.max(1, advancementsRequired));
            advancementInfoBar.setValue(advancementManager.getFinishedAmount());
            advancementInfoBar.setVisible(true);
        } else {
            // Все необходимые чеки сделаны. Число чеков пропадает.
            // Вместо Check Goal пишется что нужно убить босса (дракона и/или визера).
            WorldData wd = APRandomizer.getWorldData();
            boolean dragonKilled = wd != null && wd.isDragonKilled();
            boolean witherKilled = wd != null && wd.isWitherKilled();

            boolean needDragon = apmc.required_bosses.hasDragon() && !dragonKilled;
            boolean needWither = apmc.required_bosses.hasWither() && !witherKilled;

            if (needDragon && needWither) {
                advancementInfoBar.setName(Component.literal("Убить дракона Края и Иссушителя").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
                advancementInfoBar.setColor(BossEvent.BossBarColor.PURPLE);
                advancementInfoBar.setOverlay(BossEvent.BossBarOverlay.NOTCHED_6);
                advancementInfoBar.setMax(2);
                advancementInfoBar.setValue((dragonKilled ? 1 : 0) + (witherKilled ? 1 : 0));
            } else if (needDragon) {
                advancementInfoBar.setName(Component.literal("Убить дракона Края").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
                advancementInfoBar.setColor(BossEvent.BossBarColor.PURPLE);
                advancementInfoBar.setOverlay(BossEvent.BossBarOverlay.PROGRESS);
                advancementInfoBar.setMax(1);
                advancementInfoBar.setValue(0);
            } else if (needWither) {
                advancementInfoBar.setName(Component.literal("Убить Иссушителя").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD));
                advancementInfoBar.setColor(BossEvent.BossBarColor.PURPLE);
                advancementInfoBar.setOverlay(BossEvent.BossBarOverlay.PROGRESS);
                advancementInfoBar.setMax(1);
                advancementInfoBar.setValue(0);
            } else {
                advancementInfoBar.setName(Component.literal("Собрать осколки яйца дракона").withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD));
                advancementInfoBar.setColor(BossEvent.BossBarColor.WHITE);
                advancementInfoBar.setOverlay(BossEvent.BossBarOverlay.PROGRESS);
                advancementInfoBar.setMax(Math.max(1, dragonEggShardsRequired));
                advancementInfoBar.setValue(currentEggShards());
            }
            advancementInfoBar.setVisible(true);
        }
    }

    public void checkGoalCompletion() {
        if (!isGameCompleted())
            return;

        if (!gameCompletedAnnounced) {
            gameCompletedAnnounced = true;
            Utils.sendTitleToAll(Component.literal("§aИгра пройдена!"), Component.literal("§eПоздравляем с победой!"), 20, 100, 20);
            Utils.sendScreenTitleToAll(Component.literal("§aИгра пройдена!"), Component.literal("§eПоздравляем с победой!"), 20, 100, 20);
            Utils.PlaySoundToAll(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE);
            Utils.sendMessageToAll("§a[Archipelago] Игра пройдена! Поздравляем!");
        }

        if (APRandomizer.isConnected()) {
            APClient apClient = APRandomizer.getAP();
            if (apClient != null) {
                apClient.setGameState(ClientStatus.CLIENT_GOAL);
            }
        }

        ItemManager itemManager = APRandomizer.getItemManager();
        if (itemManager != null) {
            itemManager.syncAllPlayerAdvancements();
        }
    }

    public void checkBossMessages() {
        WorldData wd = APRandomizer.getWorldData();
        if (wd == null) return;

        //check if the dragon message has been sent, and send it if needed.
        if (goalsDone() && wd.getDragonState() == WorldData.ASLEEP && isBossRequired(APMCData.Bosses.ENDER_DRAGON)) {
            wd.setDragonState(WorldData.WAITING);
            Utils.PlaySoundToAll(SoundEvents.ENDER_DRAGON_AMBIENT);
            Utils.sendMessageToAll("The Dragon is waiting...");
            Utils.sendTitleToAll(Component.literal("The Dragon").withStyle(Style.EMPTY.withColor(TextColor.fromRgb(Color.ORANGE.getRGB()))), Component.literal("is waiting..."), 40, 120, 40);
        }

        //check if the wither message has been sent, and send it if needed.
        if (goalsDone() && wd.getWitherState() == WorldData.ASLEEP && isBossRequired(APMCData.Bosses.WITHER)) {
            wd.setWitherState(WorldData.WAITING);
            Utils.PlaySoundToAll(SoundEvents.WITHER_AMBIENT);
            Utils.sendMessageToAll("The Darkness is calling...");
            Utils.sendTitleToAll(Component.literal("The Darkness").withStyle(Style.EMPTY.withColor(TextColor.fromRgb(Color.BLACK.getRGB()))), Component.literal("is calling..."), 40, 120, 40);
        }
    }

    public boolean goalsDone() {
        return isChecksDone() && (dragonEggShardsRequired <= 0 || this.currentEggShards() >= dragonEggShardsRequired);
    }

    //subscribe to living death event to check for wither/dragon kills;
    @SubscribeEvent
    public static void onBossDeath(LivingDeathEvent event) {
        LivingEntity mob = event.getEntity();
        GoalManager goalManager = APRandomizer.getGoalManager();
        if (goalManager == null) return;
        WorldData worldData = APRandomizer.getWorldData();
        if (worldData == null) return;
        if (mob instanceof EnderDragon && isBossRequired(APMCData.Bosses.ENDER_DRAGON)) {
            if (!worldData.isDragonKilled()) {
                worldData.setDragonKilled();
                Utils.sendMessageToAll("She is no more...");
            }
            goalManager.updateGoal(true);
            ItemManager itemManager = APRandomizer.getItemManager();
            if (itemManager != null) {
                itemManager.syncAllPlayerAdvancements();
            }
        }
        if (mob instanceof WitherBoss && isBossRequired(APMCData.Bosses.WITHER)) {
            if (!worldData.isWitherKilled()) {
                worldData.setWitherKilled();
                Utils.sendMessageToAll("The Darkness has lifted...");
            }
            goalManager.updateGoal(true);
            ItemManager itemManager = APRandomizer.getItemManager();
            if (itemManager != null) {
                itemManager.syncAllPlayerAdvancements();
            }
        }
    }

    // check APMC.required_bosses to see if the boss is required
    public static boolean isBossRequired(APMCData.Bosses boss) {
        var apmc = APRandomizer.getApmcData();
        if (apmc == null || apmc.required_bosses == null) return false;
        var required = apmc.required_bosses;

        // if it matches our goal its true
        if (required == boss) return true;
        // a boss is required and you asked about none.
        if (boss == APMCData.Bosses.NONE) return false;
        // if both bosses are required, and you didn't ask about none, return true;
        return required == APMCData.Bosses.BOTH;
    }
}
