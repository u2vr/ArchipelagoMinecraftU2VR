package gg.archipelago.aprandomizer.client;

import gg.archipelago.aprandomizer.APRandomizer;
import gg.archipelago.aprandomizer.data.WorldData;
import gg.archipelago.aprandomizer.managers.recipemanager.RecipeTreeManager;
import gg.archipelago.aprandomizer.managers.recipemanager.RecipeTreeNode;
import gg.archipelago.aprandomizer.network.SetTierThresholdPayload;
import gg.archipelago.aprandomizer.network.SyncRecipeStatePayload;
import gg.archipelago.aprandomizer.network.UnlockRecipeNodePayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.Recipe;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashSet;
import java.util.Set;

@OnlyIn(Dist.CLIENT)
public class ClientRecipeManager {
    private static int tickets = 0;
    private static int tierThreshold = 50;
    private static boolean ticketMode = false;
    private static final Set<String> UNLOCKED_NODE_IDS = new HashSet<>();

    public static synchronized void handleSync(SyncRecipeStatePayload payload) {
        tickets = payload.tickets();
        tierThreshold = payload.tierThreshold();
        ticketMode = payload.ticketMode();
        UNLOCKED_NODE_IDS.clear();
        UNLOCKED_NODE_IDS.addAll(payload.unlockedNodeIds());
    }

    public static synchronized int getTickets() {
        WorldData worldData = APRandomizer.getWorldData();
        if (worldData != null) {
            return worldData.getRecipeTickets();
        }
        return tickets;
    }

    public static synchronized int getTierThreshold() {
        WorldData worldData = APRandomizer.getWorldData();
        if (worldData != null) {
            return RecipeTreeManager.getRecipeTierUnlockPercentage();
        }
        return tierThreshold;
    }

    public static synchronized boolean isTicketMode() {
        WorldData worldData = APRandomizer.getWorldData();
        if (worldData != null) {
            return RecipeTreeManager.isTicketMode();
        }
        return ticketMode;
    }

    public static synchronized Set<ResourceKey<Recipe<?>>> getUnlockedRecipeKeys() {
        Set<ResourceKey<Recipe<?>>> set = new HashSet<>(RecipeTreeManager.getInitialUnlockedRecipes());
        WorldData worldData = APRandomizer.getWorldData();
        if (worldData != null) {
            set.addAll(worldData.getUnlockedRecipes());
        }
        for (String id : UNLOCKED_NODE_IDS) {
            RecipeTreeNode n = RecipeTreeManager.getNodeById(id);
            if (n != null) {
                set.addAll(n.recipes());
            }
        }
        return set;
    }

    public static synchronized boolean isNodeUnlocked(RecipeTreeNode node) {
        if (node == null) return false;
        WorldData worldData = APRandomizer.getWorldData();
        if (worldData != null) {
            Set<ResourceKey<Recipe<?>>> unlocked = new HashSet<>(worldData.getUnlockedRecipes());
            if (node.isUnlocked(unlocked)) return true;
        }
        return UNLOCKED_NODE_IDS.contains(node.id());
    }

    public static synchronized RecipeTreeManager.UnlockStatus getUnlockStatus(RecipeTreeNode node) {
        if (node == null) return RecipeTreeManager.UnlockStatus.LOCKED_TREE;
        Set<ResourceKey<Recipe<?>>> unlocked = getUnlockedRecipeKeys();
        int currentTickets = getTickets();
        int threshold = getTierThreshold();
        return RecipeTreeManager.getUnlockStatus(node, unlocked, currentTickets, threshold);
    }

    private static Set<ResourceKey<Recipe<?>>> getUnlockedRecipeKeysFromClient() {
        return getUnlockedRecipeKeys();
    }

    public static synchronized void requestUnlock(String nodeId) {
        RecipeTreeNode node = RecipeTreeManager.getNodeById(nodeId);
        if (node != null) {
            UNLOCKED_NODE_IDS.add(nodeId);
            if (tickets > 0) {
                tickets--;
            }
        }
        net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(new UnlockRecipeNodePayload(nodeId));
    }

    public static void requestSetThreshold(int threshold) {
        tierThreshold = threshold;
        net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(new SetTierThresholdPayload(threshold));
    }
}

