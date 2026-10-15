package gg.archipelago.aprandomizer.common.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.context.CommandContext;
import gg.archipelago.aprandomizer.APRandomizer;
import gg.archipelago.aprandomizer.SlotData;
import gg.archipelago.aprandomizer.common.Utils.TitleQueue;
import gg.archipelago.aprandomizer.common.Utils.Utils;
import gg.archipelago.aprandomizer.attachments.APAttachmentTypes;
import gg.archipelago.aprandomizer.data.WorldData;
import gg.archipelago.aprandomizer.items.CompassReward;
import gg.archipelago.aprandomizer.managers.itemmanager.ItemManager;
import gg.archipelago.aprandomizer.managers.recipemanager.RecipeTreeManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Map;

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
                .then(Commands.literal("sync")
                        .executes(ctx -> {
                            var server = ctx.getSource().getServer();
                            var apClient = APRandomizer.getAP();
                            var am = APRandomizer.getAdvancementManager();
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
                                ItemManager.ensurePlayerHasCompasses(player);
                                gg.archipelago.aprandomizer.exploration.ExplorationManager.syncPlayerExploration(player);
                            }
                            var goalManager = APRandomizer.getGoalManager();
                            if (goalManager != null) {
                                goalManager.updateGoal(true);
                                goalManager.updateInfoBar();
                            }
                            ctx.getSource().sendSuccess(() -> Component.literal("§a[Archipelago] Все достижения, чеки, предметы и рецепты успешно синхронизированы!"), true);
                            return 1;
                        })
                )
                .then(Commands.literal("give")
                        .then(Commands.argument("itemId", com.mojang.brigadier.arguments.LongArgumentType.longArg(1))
                                .executes(ctx -> {
                                    long id = com.mojang.brigadier.arguments.LongArgumentType.getLong(ctx, "itemId");
                                    var im = APRandomizer.getItemManager();
                                    var wd = APRandomizer.getWorldData();
                                    if (im != null && wd != null) {
                                        int nextIdx = Math.max(im.getIndex(), wd.getItemIndex()) + 1;
                                        boolean success = im.giveItemToAll(id, nextIdx);
                                        if (success) {
                                            ctx.getSource().sendSuccess(() -> Component.literal("§a[Archipelago] Successfully gave item ID " + id), true);
                                        } else {
                                            ctx.getSource().sendFailure(Component.literal("§c[Archipelago] Failed to give item ID " + id + " (not found in DEFAULT_ITEMS or already received)"));
                                        }
                                    }
                                    return 1;
                                })
                        )
                )
                .then(Commands.literal("ability")
                        .executes(ctx -> {
                            var wd = APRandomizer.getWorldData();
                            if (wd == null) {
                                ctx.getSource().sendFailure(Component.literal("World data not loaded."));
                                return 0;
                            }
                            ctx.getSource().sendSuccess(() -> Component.literal("§6=== Archipelago Abilities ==="), false);
                            ctx.getSource().sendSuccess(() -> Component.literal("§eProgressive Mining: §f" + wd.getMiningLevel() + "/5"), false);
                            ctx.getSource().sendSuccess(() -> Component.literal("§eTree Feller: §f" + (wd.isTreeFellerUnlocked() ? "§aUnlocked" : "§cLocked")), false);
                            ctx.getSource().sendSuccess(() -> Component.literal("§eProgressive Jump: §f" + wd.getJumpLevel() + "/2"), false);
                            ctx.getSource().sendSuccess(() -> Component.literal("§eItem Magnet: §f" + (wd.isItemMagnetUnlocked() ? "§aUnlocked" : "§cLocked")), false);
                            ctx.getSource().sendSuccess(() -> Component.literal("§eStep Assist: §f" + (wd.isStepAssistUnlocked() ? "§aUnlocked" : "§cLocked")), false);
                            ctx.getSource().sendSuccess(() -> Component.literal("§eTrash Can: §f" + (wd.isTrashCanUnlocked() ? "§aUnlocked" : "§cLocked")), false);
                            ctx.getSource().sendSuccess(() -> Component.literal("§eLight Holder: §f" + (wd.isLightHolderUnlocked() ? "§aUnlocked" : "§cLocked")), false);
                            return 1;
                        })
                        .then(Commands.literal("jump")
                                .executes(ctx -> setAbilityLevel(ctx.getSource(), "jump", 1))
                                .then(Commands.argument("level", com.mojang.brigadier.arguments.IntegerArgumentType.integer(0, 2))
                                        .executes(ctx -> setAbilityLevel(ctx.getSource(), "jump", com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "level")))
                                )
                        )
                        .then(Commands.literal("mining")
                                .executes(ctx -> setAbilityLevel(ctx.getSource(), "mining", 5))
                                .then(Commands.argument("level", com.mojang.brigadier.arguments.IntegerArgumentType.integer(0, 5))
                                        .executes(ctx -> setAbilityLevel(ctx.getSource(), "mining", com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "level")))
                                )
                        )
                        .then(Commands.literal("treefeller")
                                .executes(ctx -> toggleAbility(ctx.getSource(), "treefeller"))
                        )
                        .then(Commands.literal("magnet")
                                .executes(ctx -> toggleAbility(ctx.getSource(), "magnet"))
                        )
                        .then(Commands.literal("stepassist")
                                .executes(ctx -> toggleAbility(ctx.getSource(), "stepassist"))
                        )
                        .then(Commands.literal("trashcan")
                                .executes(ctx -> toggleAbility(ctx.getSource(), "trashcan"))
                        )
                        .then(Commands.literal("lightholder")
                                .executes(ctx -> toggleAbility(ctx.getSource(), "lightholder"))
                        )
                        .then(Commands.literal("sync")
                                .executes(ctx -> {
                                    var server = ctx.getSource().getServer();
                                    var im = APRandomizer.getItemManager();
                                    if (im != null) {
                                        im.reconcileReceivedItems(server);
                                    }
                                    ctx.getSource().sendSuccess(() -> Component.literal("§a[Archipelago] Abilities reconciled with received items!"), true);
                                    return 1;
                                })
                        )
                )
                .then(Commands.literal("compass")
                        .executes(ctx -> {
                            if (ctx.getSource().getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
                                giveCompassToPlayer(ctx.getSource(), player, null);
                            } else {
                                ctx.getSource().sendFailure(Component.literal("Only players can execute this command."));
                            }
                            return 1;
                        })
                        .then(Commands.literal("give")
                                .executes(ctx -> {
                                    if (ctx.getSource().getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
                                        giveCompassToPlayer(ctx.getSource(), player, null);
                                    } else {
                                        ctx.getSource().sendFailure(Component.literal("Only players can execute this command."));
                                    }
                                    return 1;
                                })
                        )
                        .then(Commands.argument("target", com.mojang.brigadier.arguments.StringArgumentType.greedyString())
                                .executes(ctx -> {
                                    if (ctx.getSource().getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
                                        String target = com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "target");
                                        giveCompassToPlayer(ctx.getSource(), player, target);
                                    } else {
                                        ctx.getSource().sendFailure(Component.literal("Only players can execute this command."));
                                    }
                                    return 1;
                                })
                        )
                )
                .then(Commands.literal("give_compass")
                        .executes(ctx -> {
                            if (ctx.getSource().getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
                                giveCompassToPlayer(ctx.getSource(), player, null);
                            } else {
                                ctx.getSource().sendFailure(Component.literal("Only players can execute this command."));
                            }
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

        dispatcher.register(
                Commands.literal("compass")
                        .executes(ctx -> {
                            if (ctx.getSource().getEntity() instanceof ServerPlayer player) {
                                giveCompassToPlayer(ctx.getSource(), player, null);
                            } else {
                                ctx.getSource().sendFailure(Component.literal("Only players can execute this command."));
                            }
                            return 1;
                        })
                        .then(Commands.literal("give")
                                .executes(ctx -> {
                                    if (ctx.getSource().getEntity() instanceof ServerPlayer player) {
                                        giveCompassToPlayer(ctx.getSource(), player, null);
                                    } else {
                                        ctx.getSource().sendFailure(Component.literal("Only players can execute this command."));
                                    }
                                    return 1;
                                })
                        )
                        .then(Commands.argument("target", com.mojang.brigadier.arguments.StringArgumentType.greedyString())
                                .executes(ctx -> {
                                    if (ctx.getSource().getEntity() instanceof ServerPlayer player) {
                                        String target = com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "target");
                                        giveCompassToPlayer(ctx.getSource(), player, target);
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

    private static void giveCompassToPlayer(CommandSourceStack source, ServerPlayer player, @org.jetbrains.annotations.Nullable String targetName) {
        Map<Identifier, CompassReward> unlocked = player.getData(APAttachmentTypes.AP_PLAYER).getUnlockedCompassRewards();

        CompassReward chosenReward = null;
        if (targetName != null && !targetName.isBlank()) {
            if (unlocked.isEmpty()) {
                source.sendFailure(Component.literal("§c[Archipelago] У вас пока нет открытых целей для компаса."));
                return;
            }
            String search = targetName.trim().toLowerCase();
            for (CompassReward reward : unlocked.values()) {
                String nameStr = reward.name().getString().toLowerCase();
                String idStr = reward.id().toString().toLowerCase();
                String pathStr = reward.id().getPath().toLowerCase();
                if (nameStr.contains(search) || idStr.contains(search) || pathStr.contains(search)) {
                    chosenReward = reward;
                    break;
                }
            }
            if (chosenReward == null) {
                source.sendFailure(Component.literal("§c[Archipelago] Не найдено открытой цели компаса по запросу: " + targetName));
                return;
            }
        } else if (!unlocked.isEmpty()) {
            for (CompassReward reward : unlocked.values()) {
                chosenReward = reward;
            }
        }

        ItemStack existingCompass = ItemStack.EMPTY;
        for (ItemStack item : player.getInventory()) {
            if (ItemManager.isArchipelagoCompass(item, player)) {
                existingCompass = item;
                break;
            }
        }

        if (existingCompass.isEmpty()) {
            ItemStack compass = CompassReward.DEFAULT_COMPASS.create().copy();
            if (chosenReward != null) {
                ItemManager.updateCompassLocation(chosenReward, player, compass);
                CompoundTag tag = compass.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
                tag.putInt("index", unlocked.size() - 1);
                compass.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                Utils.giveItemToPlayer(player, compass);
                final String targetTitle = chosenReward.name().getString();
                source.sendSuccess(() -> Component.literal("§a[Archipelago] Выдан компас Архипелага (" + targetTitle + ")"), false);
            } else {
                Utils.giveItemToPlayer(player, compass);
                source.sendSuccess(() -> Component.literal("§a[Archipelago] Выдан компас Архипелага (цели еще не открыты)"), false);
            }
        } else {
            if (chosenReward != null) {
                ItemManager.updateCompassLocation(chosenReward, player, existingCompass);
                final String targetTitle = chosenReward.name().getString();
                source.sendSuccess(() -> Component.literal("§a[Archipelago] Компас перенаправлен на " + targetTitle), false);
            } else {
                source.sendSuccess(() -> Component.literal("§e[Archipelago] Компас Архипелага уже находится в вашем инвентаре."), false);
            }
        }
        ItemManager.cleanCompasses(player);
    }

    private static int setAbilityLevel(CommandSourceStack source, String ability, int level) {
        WorldData wd = APRandomizer.getWorldData();
        if (wd == null) {
            source.sendFailure(Component.literal("World data not loaded."));
            return 0;
        }
        var server = source.getServer();
        var im = APRandomizer.getItemManager();

        if ("jump".equalsIgnoreCase(ability)) {
            wd.setJumpLevel(level);
            source.sendSuccess(() -> Component.literal("§a[Archipelago] Progressive Jump level set to " + level + "/2"), true);
        } else if ("mining".equalsIgnoreCase(ability)) {
            wd.setMiningLevel(level);
            source.sendSuccess(() -> Component.literal("§a[Archipelago] Progressive Mining level set to " + level + "/5"), true);
        }

        gg.archipelago.aprandomizer.managers.AbilityManager.syncToAll(server);
        if (im != null) {
            im.syncAllPlayerAdvancements();
        }
        return 1;
    }

    private static int toggleAbility(CommandSourceStack source, String ability) {
        WorldData wd = APRandomizer.getWorldData();
        if (wd == null) {
            source.sendFailure(Component.literal("World data not loaded."));
            return 0;
        }
        var server = source.getServer();
        var im = APRandomizer.getItemManager();

        switch (ability.toLowerCase()) {
            case "treefeller" -> {
                boolean state = !wd.isTreeFellerUnlocked();
                wd.setTreeFellerUnlocked(state);
                source.sendSuccess(() -> Component.literal("§a[Archipelago] Tree Feller " + (wd.isTreeFellerUnlocked() ? "§aunlocked" : "§clocked")), true);
            }
            case "magnet" -> {
                boolean state = !wd.isItemMagnetUnlocked();
                wd.setItemMagnetUnlocked(state);
                source.sendSuccess(() -> Component.literal("§a[Archipelago] Item Magnet " + (wd.isItemMagnetUnlocked() ? "§aunlocked" : "§clocked")), true);
            }
            case "stepassist" -> {
                boolean state = !wd.isStepAssistUnlocked();
                wd.setStepAssistUnlocked(state);
                source.sendSuccess(() -> Component.literal("§a[Archipelago] Step Assist " + (wd.isStepAssistUnlocked() ? "§aunlocked" : "§clocked")), true);
            }
            case "trashcan" -> {
                boolean state = !wd.isTrashCanUnlocked();
                wd.setTrashCanUnlocked(state);
                source.sendSuccess(() -> Component.literal("§a[Archipelago] Trash Can " + (wd.isTrashCanUnlocked() ? "§aunlocked" : "§clocked")), true);
            }
            case "lightholder" -> {
                boolean state = !wd.isLightHolderUnlocked();
                wd.setLightHolderUnlocked(state);
                source.sendSuccess(() -> Component.literal("§a[Archipelago] Light Holder " + (wd.isLightHolderUnlocked() ? "§aunlocked" : "§clocked")), true);
            }
        }

        gg.archipelago.aprandomizer.managers.AbilityManager.syncToAll(server);
        if (im != null) {
            im.syncAllPlayerAdvancements();
        }
        return 1;
    }

    //wait for register commands event then register us as a command.
    @SubscribeEvent
    static void onRegisterCommandsEvent(RegisterCommandsEvent event) {
        APCommand.Register(event.getDispatcher());
    }
}
