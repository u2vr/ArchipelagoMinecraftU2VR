package gg.archipelago.aprandomizer.common.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import gg.archipelago.aprandomizer.APRandomizer;
import gg.archipelago.aprandomizer.ap.APClient;
import gg.archipelago.aprandomizer.common.Utils.Utils;
import gg.archipelago.aprandomizer.managers.advancementmanager.AdvancementManager;
import gg.archipelago.aprandomizer.managers.itemmanager.ItemManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber
public class SyncCommand {
    //build our command structure and submit it
    public static void Register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("sync") //base slash command is "sync"
                        .executes(SyncCommand::sync)
        );
    }

    private static int sync(CommandContext<CommandSourceStack> source) {
        var server = source.getSource().getServer();
        APClient apClient = APRandomizer.getAP();
        AdvancementManager am = APRandomizer.getAdvancementManager();
        if (apClient != null && apClient.isConnected()) {
            Utils.sendMessageToAll("§6[Archipelago] §fСинхронизация с сервером Archipelago...");
            apClient.sync();
            if (am != null) {
                am.resendAdvancements(apClient);
            }
        }

        ItemManager itemManager = APRandomizer.getItemManager();
        if (itemManager != null) {
            itemManager.catchUp(server);
            itemManager.syncAllPlayerAdvancements();
        }

        if (am != null) {
            am.syncAllAdvancements();
        }

        gg.archipelago.aprandomizer.managers.recipemanager.RecipeTreeManager.syncStateToAll(server);

        for (var player : server.getPlayerList().getPlayers()) {
            gg.archipelago.aprandomizer.exploration.ExplorationManager.syncPlayerExploration(player);
        }

        var goalManager = APRandomizer.getGoalManager();
        if (goalManager != null) {
            goalManager.updateGoal(true);
            goalManager.updateInfoBar();
        }

        source.getSource().sendSuccess(() -> Component.literal("§a[Archipelago] Все достижения, чеки, предметы и рецепты успешно синхронизированы!"), true);
        return 1;
    }

    //wait for register commands event then register us as a command.
    @SubscribeEvent
    static void onRegisterCommandsEvent(RegisterCommandsEvent event) {
        SyncCommand.Register(event.getDispatcher());
    }
}
