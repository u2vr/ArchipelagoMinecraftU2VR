package gg.archipelago.aprandomizer.exploration;

import gg.archipelago.aprandomizer.APRandomizer;
import gg.archipelago.aprandomizer.data.WorldData;
import gg.archipelago.aprandomizer.managers.advancementmanager.AdvancementManager;
import gg.archipelago.aprandomizer.managers.RestrictionManager;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@EventBusSubscriber(modid = APRandomizer.MODID)
public class ExplorationManager {
    private static final Logger LOGGER = LogManager.getLogger();

    public static boolean isBiomeChecksEnabled() {
        if (APRandomizer.isConnected() && APRandomizer.getAP() != null && APRandomizer.getAP().getSlotData() != null) {
            var sd = APRandomizer.getAP().getSlotData();
            if (sd.biomeChecks != null) return sd.isBiomeChecks();
        }
        if (APRandomizer.getApmcData() != null && APRandomizer.getApmcData().biome_checks != null) {
            return APRandomizer.getApmcData().isBiomeChecks();
        }
        return false;
    }

    public static boolean isStructureChecksEnabled() {
        if (APRandomizer.isConnected() && APRandomizer.getAP() != null && APRandomizer.getAP().getSlotData() != null) {
            var sd = APRandomizer.getAP().getSlotData();
            if (sd.structureChecks != null) return sd.isStructureChecks();
        }
        if (APRandomizer.getApmcData() != null && APRandomizer.getApmcData().structure_checks != null) {
            return APRandomizer.getApmcData().isStructureChecks();
        }
        return false;
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (APRandomizer.isJailPlayers()) return;
        if (player.tickCount % 20 != 0) return; // Check once per second

        checkExploration(player);
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        syncPlayerExploration(player);
    }

    public static void checkExploration(ServerPlayer player) {
        ServerLevel level = player.level();
        BlockPos pos = player.blockPosition();

        // 1. Check Biome
        Holder<Biome> biomeHolder = level.getBiome(pos);
        biomeHolder.unwrapKey().ifPresent(key -> {
            String biomeId = key.identifier().toString();
            ExplorationData.BiomeEntry biomeEntry = ExplorationData.getBiomeById(biomeId);
            if (biomeEntry != null) {
                onBiomeDiscovered(player, biomeEntry);
            }
        });

        // 2. Check Structures
        for (ExplorationData.StructureCheckEntry entry : ExplorationData.STRUCTURES) {
            if (isPlayerInStructure(level, pos, entry)) {
                if (RestrictionManager.isStructureLockedById(entry.id())) {
                    continue; // Structure is locked in Archipelago! Do not discover!
                }
                onStructureDiscovered(player, entry);
                break;
            }
        }
    }

    private static boolean isPlayerInStructure(ServerLevel level, BlockPos pos, ExplorationData.StructureCheckEntry entry) {
        if (entry.tag() != null) {
            StructureStart start = level.structureManager().getStructureWithPieceAt(pos, entry.tag());
            if (start != null && start.isValid()) return true;
        }
        if (entry.structureKeys() != null && !entry.structureKeys().isEmpty()) {
            StructureStart start = level.structureManager().getStructureWithPieceAt(pos, holder -> entry.structureKeys().stream().anyMatch(holder::is));
            if (start != null && start.isValid()) return true;
        }
        return false;
    }

    private static void onBiomeDiscovered(ServerPlayer player, ExplorationData.BiomeEntry biome) {
        WorldData worldData = APRandomizer.getWorldData();
        if (worldData == null) return;
        if (!worldData.addVisitedBiome(biome.id())) {
            return; // Already visited
        }

        LOGGER.info("{} discovered biome: {} ({})", player.getDisplayName().getString(), biome.displayName(), biome.id());
        MinecraftServer server = player.level().getServer();
        AdvancementManager am = APRandomizer.getAdvancementManager();

        // Award advancement in the dedicated tree to all online players
        awardExplorationAdvancement(server, biome.getAdvancementId(), biome.category().getAdvancementId());

        // Check Archipelago location if biome checks enabled
        boolean checksEnabled = isBiomeChecksEnabled();
        if (checksEnabled && am != null) {
            if (!am.hasAdvancement(biome.locationId())) {
                am.addAdvancement(biome.locationId());
            }
        }

        String msg = "§6[Archipelago] §aDiscovered new biome: §f" + biome.displayName() + "!" + (checksEnabled ? " §e[Location Checked]" : "");
        server.getPlayerList().broadcastSystemMessage(Component.literal(msg), true);
    }

    private static void onStructureDiscovered(ServerPlayer player, ExplorationData.StructureCheckEntry structure) {
        WorldData worldData = APRandomizer.getWorldData();
        if (worldData == null) return;
        if (!worldData.addVisitedStructure(structure.id())) {
            return; // Already visited
        }

        LOGGER.info("{} discovered structure: {} ({})", player.getDisplayName().getString(), structure.displayName(), structure.id());
        MinecraftServer server = player.level().getServer();
        AdvancementManager am = APRandomizer.getAdvancementManager();

        // Award advancement in the dedicated tree to all online players
        awardExplorationAdvancement(server, structure.getAdvancementId(), ExplorationData.STRUCTURES_CATEGORY_ID);

        // Check Archipelago location if structure checks enabled
        boolean checksEnabled = isStructureChecksEnabled();
        if (checksEnabled && am != null) {
            if (!am.hasAdvancement(structure.locationId())) {
                am.addAdvancement(structure.locationId());
            }
        }

        String msg = "§6[Archipelago] §aDiscovered structure: §f" + structure.displayName() + "!" + (checksEnabled ? " §e[Location Checked]" : "");
        server.getPlayerList().broadcastSystemMessage(Component.literal(msg), true);
    }

    private static void awardExplorationAdvancement(MinecraftServer server, Identifier childId, Identifier parentCategoryId) {
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            grantAdvancement(p, ExplorationData.ROOT_ADVANCEMENT_ID);
            grantAdvancement(p, parentCategoryId);
            grantAdvancement(p, childId);
        }
    }

    public static void grantAdvancement(ServerPlayer player, Identifier advId) {
        if (player.level().getServer() == null) return;
        AdvancementHolder holder = player.level().getServer().getAdvancements().get(advId);
        if (holder == null) return;
        AdvancementProgress progress = player.getAdvancements().getOrStartProgress(holder);
        if (progress.isDone()) return;
        for (String criterion : progress.getRemainingCriteria()) {
            player.getAdvancements().award(holder, criterion);
        }
    }


    public static void syncPlayerExploration(ServerPlayer player) {
        WorldData worldData = APRandomizer.getWorldData();
        if (worldData == null) return;

        if (!worldData.getVisitedBiomes().isEmpty() || !worldData.getVisitedStructures().isEmpty()) {
            grantAdvancement(player, ExplorationData.ROOT_ADVANCEMENT_ID);
        }

        for (String biomeId : worldData.getVisitedBiomes()) {
            ExplorationData.BiomeEntry b = ExplorationData.getBiomeById(biomeId);
            if (b != null) {
                grantAdvancement(player, b.category().getAdvancementId());
                grantAdvancement(player, b.getAdvancementId());
            }
        }

        for (String structId : worldData.getVisitedStructures()) {
            ExplorationData.StructureCheckEntry s = ExplorationData.getStructureById(structId);
            if (s != null) {
                grantAdvancement(player, ExplorationData.STRUCTURES_CATEGORY_ID);
                grantAdvancement(player, s.getAdvancementId());
            }
        }
    }
}
