package gg.archipelago.aprandomizer.common.Utils;

import gg.archipelago.aprandomizer.APRandomizer;
import gg.archipelago.aprandomizer.data.WorldData;
import gg.archipelago.aprandomizer.managers.advancementmanager.AdvancementManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class QueuedTitle {
    @NotNull
    private final MinecraftServer server;
    private final int ticks;
    private final List<ServerPlayer> players;
    private final int fadeIn;
    private final int stay;
    private final int fadeOut;
    private final Component subTitle;
    private final Component title;
    private Component chatMessage = null;
    private final boolean screenTitle;

    public QueuedTitle(MinecraftServer server, List<ServerPlayer> players, int fadeIn, int stay, int fadeOut, Component subTitle, Component title) {
        this(server, players, fadeIn, stay, fadeOut, subTitle, title, false);
    }

    public QueuedTitle(MinecraftServer server, List<ServerPlayer> players, int fadeIn, int stay, int fadeOut, Component subTitle, Component title, boolean screenTitle) {
        this.server = server;
        this.players = players;
        this.fadeIn = fadeIn;
        this.stay = stay;
        this.fadeOut = fadeOut;
        this.subTitle = subTitle;
        this.title = title;
        this.screenTitle = screenTitle;
        this.ticks = screenTitle ? (fadeIn + stay + fadeOut + 20) : Math.clamp(fadeIn + stay + fadeOut, 25, 45);
    }

    public QueuedTitle(MinecraftServer server, List<ServerPlayer> players, int fadeIn, int stay, int fadeOut, Component subTitle, Component title, Component chatMessage) {
        this(server, players, fadeIn, stay, fadeOut, subTitle, title, false);
        this.chatMessage = chatMessage;
    }

    public static Component formatActionBarMessage(Component title, Component subTitle) {
        boolean hasTitle = title != null && !title.getString().isBlank();
        boolean hasSubTitle = subTitle != null && !subTitle.getString().isBlank();

        Component base;
        if (!hasTitle && !hasSubTitle) {
            base = Component.empty();
        } else if (!hasTitle) {
            base = subTitle;
        } else if (!hasSubTitle) {
            base = title;
        } else {
            String titleStr = title.getString().trim();
            if ("Received".equalsIgnoreCase(titleStr)) {
                base = Component.literal("§6[Archipelago] §fПолучено: ").append(subTitle);
            } else {
                base = Component.empty().append(title).append(Component.literal(" §7| ")).append(subTitle);
            }
        }

        // In BACAP Milestones mode, append remaining advancements to next milestone check if not already present
        if (APRandomizer.getApmcData() != null && APRandomizer.getApmcData().isBacapMilestones()) {
            String str = base.getString();
            if (!str.contains("майлстоун") && !str.contains("Майлстоун") && !str.contains("До чека")) {
                WorldData wd = APRandomizer.getWorldData();
                if (wd != null) {
                    int step = APRandomizer.getApmcData().getBacapStep();
                    int maxChecks = APRandomizer.getApmcData().getBacapCheckCount();
                    int total = wd.getEarnedBacapAdvancementsCount();
                    int milestones = total / step;
                    if (milestones < maxChecks && milestones < AdvancementManager.MAX_BACAP_MILESTONES) {
                        int rem = step - (total % step);
                        if (rem == 0) rem = step;
                        base = Component.empty().append(base).append(Component.literal(" §7[До чека: §e" + Utils.pluralizeAdvancements(rem) + "§7]"));
                    }
                }
            }
        }

        return base;
    }

    public void sendTitle() {
        server.execute(() -> {
            if (screenTitle) {
                TitleUtils.setTimes(players, fadeIn, stay, fadeOut);
                TitleUtils.showTitle(players, title, subTitle);
            } else {
                Component message = formatActionBarMessage(title, subTitle);
                TitleUtils.showActionBar(players, message);
            }
            if (chatMessage != null) {
                Utils.sendMessageToAll(chatMessage);
            }
        });
    }

    public int getTicks() {
        return ticks;
    }
}
