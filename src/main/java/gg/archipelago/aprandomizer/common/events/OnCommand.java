package gg.archipelago.aprandomizer.common.events;

import gg.archipelago.aprandomizer.APRandomizer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.CommandEvent;

import java.util.Locale;
import java.util.Set;

@EventBusSubscriber
public class OnCommand {

    private static final Set<String> ALLOWED_RACE_COMMANDS = Set.of(
            "connect",
            "disconnect",
            "sync",
            "start",
            "forcestart",
            "stop",
            "kick",
            "ban",
            "ban-ip",
            "pardon",
            "pardon-ip",
            "whitelist",
            "me",
            "say"
    );

    private static final Set<String> ALLOWED_BASE_COMMANDS = Set.of(
            "connect",
            "disconnect",
            "sync",
            "start",
            "forcestart",
            "compass",
            "bounce",
            "help",
            "list",
            "me",
            "say",
            "tell",
            "msg",
            "w",
            "teammsg",
            "trigger",
            "stop",
            "kick",
            "ban",
            "ban-ip",
            "pardon",
            "pardon-ip",
            "whitelist",
            "save-all",
            "save-on",
            "save-off"
    );

    private static final Set<String> ALLOWED_AP_SUBCOMMANDS = Set.of(
            "deathlink",
            "cleartitlequeue",
            "sync",
            "compass",
            "give_compass"
    );

    private static final Set<String> ALLOWED_ABILITY_SUBCOMMANDS = Set.of(
            "sync",
            "treefeller",
            "magnet",
            "stepassist",
            "trashcan",
            "lightholder"
    );

    public static boolean isAllowedInRace(String command) {
        if (command == null || command.isBlank()) return false;
        String raw = command.startsWith("/") ? command.substring(1).trim() : command.trim();
        String[] parts = raw.split("\\s+");
        String root = parts.length > 0 ? parts[0].toLowerCase(Locale.ROOT) : "";
        return ALLOWED_RACE_COMMANDS.contains(root);
    }

    public static boolean isAllowedWithoutCheats(String command) {
        if (command == null || command.isBlank()) return false;
        String raw = command.startsWith("/") ? command.substring(1).trim() : command.trim();
        String[] parts = raw.split("\\s+");
        String root = parts.length > 0 ? parts[0].toLowerCase(Locale.ROOT) : "";

        if (ALLOWED_BASE_COMMANDS.contains(root)) {
            return true;
        }

        if ("ap".equals(root)) {
            if (parts.length == 1) {
                return true;
            }
            String sub = parts[1].toLowerCase(Locale.ROOT);
            if (ALLOWED_AP_SUBCOMMANDS.contains(sub)) {
                return true;
            }
            if ("ability".equals(sub)) {
                if (parts.length == 2) {
                    return true;
                }
                String abilitySub = parts[2].toLowerCase(Locale.ROOT);
                return ALLOWED_ABILITY_SUBCOMMANDS.contains(abilitySub);
            }
            return false;
        }

        return false;
    }

    @SubscribeEvent
    static void onCommandEvent(CommandEvent event) {
        CommandSourceStack source = event.getParseResults().getContext().getSource();
        String command = event.getParseResults().getReader().getRead();

        if (APRandomizer.isRace()) {
            if (!isAllowedInRace(command)) {
                event.setCanceled(true);
                source.sendFailure(Component.literal("Non-essential commands are disabled in race mode."));
            }
            return;
        }

        if (!APRandomizer.isCheatsEnabled()) {
            if (source.getEntity() instanceof ServerPlayer) {
                if (!isAllowedWithoutCheats(command)) {
                    event.setCanceled(true);
                    source.sendFailure(Component.translatable("aprandomizer.command.cheats_disabled"));
                }
            }
        }
    }
}
