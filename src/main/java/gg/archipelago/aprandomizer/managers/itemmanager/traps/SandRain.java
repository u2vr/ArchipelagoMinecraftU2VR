package gg.archipelago.aprandomizer.managers.itemmanager.traps;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public class SandRain implements Trap {
    @Override
    public void trigger(MinecraftServer server, ServerPlayer player) {
        server.execute(() -> {
            ServerLevel world = (ServerLevel) player.level();
            int startX = player.getBlockX() - 4;
            int startZ = player.getBlockZ() - 4;
            int baseY = player.getBlockY() + 6;

            for (int dy = 0; dy < 3; dy++) {
                for (int dx = 0; dx < 9; dx++) {
                    for (int dz = 0; dz < 9; dz++) {
                        BlockPos blockPos = new BlockPos(startX + dx, baseY + dy, startZ + dz);
                        if (world.isEmptyBlock(blockPos)) {
                            world.setBlock(blockPos, Blocks.SAND.defaultBlockState(), 3);
                        }
                    }
                }
            }
        });
    }
}