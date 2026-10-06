package gg.archipelago.aprandomizer.common.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.context.CommandContext;
import gg.archipelago.aprandomizer.APRandomizer;
import gg.archipelago.aprandomizer.SlotData;
import gg.archipelago.aprandomizer.common.Utils.TitleQueue;
import gg.archipelago.aprandomizer.common.Utils.Utils;
import gg.archipelago.aprandomizer.managers.itemmanager.ItemManager;
import gg.archipelago.aprandomizer.managers.recipemanager.RecipeTreeManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber
public class APCommand {

    //build our command structure and submit it
    public static void Register(CommandDispatcher<CommandSourceStack> dispatcher) {

        dispatcher.register(
                Commands.literal("ap") //base slash command is "ap"
                        //First sub-command to set/retreive deathlink status
                        .then(Commands.literal("deathlink")
                                .executes(APCommand::queryDeathLink)
                                .then(Commands.argument("value", BoolArgumentType.bool())
                                        .executes(APCommand::setDeathLink)
                                )
                        )
                //second sub-command to stop titlequeue
                .then(Commands.literal("clearTitleQueue")
                        .executes(APCommand::clearTitleQueue)
                )
                //Recipe bias setting
                .then(Commands.literal("recipe_bias")
                        .executes(APCommand::queryRecipeBias)
                        .then(Commands.argument("value", DoubleArgumentType.doubleArg(0.0, 100.0))
                                .executes(APCommand::setRecipeBias)
                        )
                )
                .then(Commands.literal("recipe_tier_bias")
                        .executes(APCommand::queryRecipeBias)
                        .then(Commands.argument("value", DoubleArgumentType.doubleArg(0.0, 100.0))
                                .executes(APCommand::setRecipeBias)
                        )
                )
                .then(Commands.literal("recipe_mode")
                        .executes(ctx -> {
                            boolean ticket = RecipeTreeManager.isTicketMode();
                            ctx.getSource().sendSuccess(() -> Component.literal("recipe_unlock_mode is currently: " + (ticket ? "Ticket Mode (skilltree)" : "Random by Bias")), false);
                            return 1;
                        })
                        .then(Commands.literal("ticket")
                                .executes(ctx -> {
                                    RecipeTreeManager.setTicketMode(true);
                                    RecipeTreeManager.syncStateToAll(ctx.getSource().getServer());
                                    ctx.getSource().sendSuccess(() -> Component.literal("recipe_unlock_mode set to: Ticket Mode (skilltree)"), true);
                                    return 1;
                                })
                        )
                        .then(Commands.literal("random")
                                .executes(ctx -> {
                                    RecipeTreeManager.setTicketMode(false);
                                    RecipeTreeManager.syncStateToAll(ctx.getSource().getServer());
                                    ctx.getSource().sendSuccess(() -> Component.literal("recipe_unlock_mode set to: Random by Bias"), true);
                                    return 1;
                                })
                        )
                )
                .then(Commands.literal("recipe_tickets")
                        .executes(ctx -> {
                            var wd = APRandomizer.getWorldData();
                            int count = wd != null ? wd.getRecipeTickets() : 0;
                            ctx.getSource().sendSuccess(() -> Component.literal("Available recipe tickets: " + count), false);
                            return 1;
                        })
                        .then(Commands.literal("add")
                                .then(Commands.argument("amount", com.mojang.brigadier.arguments.IntegerArgumentType.integer())
                                        .executes(ctx -> {
                                            int amount = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "amount");
                                            var wd = APRandomizer.getWorldData();
                                            if (wd != null) {
                                                wd.addRecipeTickets(amount);
                                                RecipeTreeManager.syncStateToAll(ctx.getSource().getServer());
                                                ctx.getSource().sendSuccess(() -> Component.literal("Added " + amount + " tickets. Total: " + wd.getRecipeTickets()), true);
                                            }
                                            return 1;
                                        })
                                )
                        )
                        .then(Commands.literal("set")
                                .then(Commands.argument("amount", com.mojang.brigadier.arguments.IntegerArgumentType.integer(0))
                                        .executes(ctx -> {
                                            int amount = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "amount");
                                            var wd = APRandomizer.getWorldData();
                                            if (wd != null) {
                                                wd.setRecipeTickets(amount);
                                                RecipeTreeManager.syncStateToAll(ctx.getSource().getServer());
                                                ctx.getSource().sendSuccess(() -> Component.literal("Recipe tickets set to: " + amount), true);
                                            }
                                            return 1;
                                        })
                                )
                        )
                )
                .then(Commands.literal("recipe_tier_threshold")
                        .executes(ctx -> {
                            int pct = RecipeTreeManager.getRecipeTierUnlockPercentage();
                            ctx.getSource().sendSuccess(() -> Component.literal("recipe_tier_unlock_percentage is currently: " + pct + "%"), false);
                            return 1;
                        })
                        .then(Commands.argument("value", com.mojang.brigadier.arguments.IntegerArgumentType.integer(0, 100))
                                .executes(ctx -> {
                                    int val = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "value");
                                    RecipeTreeManager.setRecipeTierUnlockPercentage(val);
                                    RecipeTreeManager.syncStateToAll(ctx.getSource().getServer());
                                    ctx.getSource().sendSuccess(() -> Component.literal("recipe_tier_unlock_percentage is now set to: " + val + "%"), true);
                                    return 1;
                                })
                        )
                )
                .then(Commands.literal("unlock_recipe")
                        .executes(ctx -> {
                            RecipeTreeManager.unlockNextRecipeNode(ctx.getSource().getServer());
                            return 1;
                        })
                )
                .then(Commands.literal("chest")
                        .executes(ctx -> {
                            if (ctx.getSource().getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
                                Utils.giveItemToPlayer(player, new net.minecraft.world.item.ItemStack(gg.archipelago.aprandomizer.APBlocks.SHARED_CHEST_ITEM.get()));
                                ctx.getSource().sendSuccess(() -> Component.literal("Gave 1 Archipelago Chest"), false);
                            } else {
                                ctx.getSource().sendFailure(Component.literal("Only players can receive the chest"));
                            }
                            return 1;
                        })
                )
                .then(Commands.literal("open_chest")
                        .executes(ctx -> {
                            if (ctx.getSource().getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
                                gg.archipelago.aprandomizer.managers.chest.SharedChestManager.openChest(player);
                            }
                            return 1;
                        })
                )
                .then(Commands.literal("sync")
                        .executes(ctx -> {
                            var apClient = APRandomizer.getAP();
                            if (apClient != null && apClient.isConnected()) {
                                apClient.sync();
                            }
                            ItemManager itemManager = APRandomizer.getItemManager();
                            if (itemManager != null) {
                                itemManager.catchUp(ctx.getSource().getServer());
                                itemManager.syncAllPlayerAdvancements();
                            }
                            ctx.getSource().sendSuccess(() -> Component.literal("§a[Archipelago] Advancements and items synchronized!"), true);
                            return 1;
                        })
                )
                .then(Commands.literal("spawntrader")
                        .executes(ctx -> {
                            if (ctx.getSource().getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
                                boolean spawned = gg.archipelago.aprandomizer.managers.trademanager.ArchipelagoTraderSpawner.trySpawnTrader((net.minecraft.server.level.ServerLevel) player.level(), player);
                                if (spawned) {
                                    ctx.getSource().sendSuccess(() -> Component.literal("§a[Archipelago] Wandering Trader successfully spawned near you!"), false);
                                } else {
                                    ctx.getSource().sendFailure(Component.literal("§c[Archipelago] Could not find a suitable spawn position for Wandering Trader."));
                                }
                            } else {
                                ctx.getSource().sendFailure(Component.literal("Only players can execute this command."));
                            }
                            return 1;
                        })
                )

        );

    }

    private static int queryRecipeBias(CommandContext<CommandSourceStack> source) {
        double bias = RecipeTreeManager.getRecipeTierBias();
        source.getSource().sendSuccess(() -> Component.literal("recipe_tier_bias is currently " + bias + "%"), false);
        return 1;
    }

    private static int setRecipeBias(CommandContext<CommandSourceStack> source) {
        double value = DoubleArgumentType.getDouble(source, "value");
        RecipeTreeManager.setRecipeTierBias(value);
        source.getSource().sendSuccess(() -> Component.literal("recipe_tier_bias is now set to " + RecipeTreeManager.getRecipeTierBias() + "%"), false);
        return 1;
    }

    private static int clearTitleQueue(CommandContext<CommandSourceStack> commandSourceStackCommandContext) {
        Utils.sendMessageToAll("Title Queue Cleared");
        TitleQueue.clearTitleQueue();
        return 1;
    }

    private static int queryDeathLink(CommandContext<CommandSourceStack> source) {
        SlotData slotData;
        if (APRandomizer.getAP() == null || (slotData = APRandomizer.getAP().getSlotData()) == null) {
            source.getSource().sendFailure(Component.literal("Must be connected to an AP server to use this command"));
            return 0;
        }
        String enabled = slotData.deathlink ? "enabled" : "disabled";
        source.getSource().sendSuccess(() -> Component.literal("DeathLink is " + enabled), false);
        return 1;
    }

    private static int setDeathLink(CommandContext<CommandSourceStack> source) {
        SlotData slotData;
        if (APRandomizer.getAP() == null || (slotData = APRandomizer.getAP().getSlotData()) == null) {
            source.getSource().sendFailure(Component.literal("Must be connected to an AP server to use this command"));
            return 0;
        }

        slotData.deathlink = BoolArgumentType.getBool(source, "value");
        boolean deathlink = slotData.deathlink;
        if (deathlink) {
            APRandomizer.getAP().addTag("DeathLink");
        } else {
            APRandomizer.getAP().removeTag("DeathLink");
        }

        String enabled = (slotData.deathlink) ? "enabled" : "disabled";
        source.getSource().sendSuccess(() -> Component.literal("DeathLink is now " + enabled), false);
        return 1;
    }

    //wait for register commands event then register us as a command.
    @SubscribeEvent
    static void onRegisterCommandsEvent(RegisterCommandsEvent event) {
        APCommand.Register(event.getDispatcher());
    }
}
