package gg.archipelago.aprandomizer.client;

import gg.archipelago.aprandomizer.APRandomizer;
import gg.archipelago.aprandomizer.managers.AbilityManager;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

public class DynamicLightIntegration {

    public static void init() {
        if (!tryRegisterLambCompatLayer()) {
            tryRegisterOldLambDynamicLights();
        }
    }

    @SuppressWarnings("unchecked")
    private static boolean tryRegisterLambCompatLayer() {
        try {
            Class<?> compatLayerClass = Class.forName("dev.lambdaurora.lambdynlights.compat.CompatLayer");
            Field layersField = compatLayerClass.getField("LAYERS");
            List<Object> layers = (List<Object>) layersField.get(null);

            Object compatProxy = Proxy.newProxyInstance(
                    compatLayerClass.getClassLoader(),
                    new Class<?>[]{ compatLayerClass },
                    (proxy, method, args) -> {
                        String name = method.getName();
                        if ("getLivingEntityLuminanceFromItems".equals(name) && args != null && args.length >= 3) {
                            if (args[1] instanceof Player player) {
                                ItemStack stack = ItemStack.EMPTY;
                                if (player == Minecraft.getInstance().player) {
                                    stack = ClientAbilityManager.getLightHolderItem();
                                }
                                if (stack.isEmpty()) {
                                    stack = AbilityManager.getLightHolderItem(player);
                                }
                                if (!stack.isEmpty()) {
                                    if (args[0] != null) {
                                        try {
                                            Method getLum = args[0].getClass().getMethod("getLuminance", ItemStack.class, boolean.class);
                                            Object res = getLum.invoke(args[0], stack, args[2]);
                                            if (res instanceof Integer lum && lum > 0) {
                                                return lum;
                                            }
                                        } catch (Throwable ignored) {}
                                    }
                                    if (AbilityManager.isLightEmittingBlock(stack)) {
                                        if (stack.is(Items.LAVA_BUCKET)) return 15;
                                        Block block = Block.byItem(stack.getItem());
                                        if (block != Blocks.AIR) {
                                            return block.defaultBlockState().getLightEmission();
                                        }
                                    }
                                }
                            }
                            return 0;
                        }
                        if ("hashCode".equals(name)) return System.identityHashCode(proxy);
                        if ("equals".equals(name) && args != null && args.length == 1) return proxy == args[0];
                        if ("toString".equals(name)) return "APRandomizer$LambDynLightsCompatLayer";
                        return null;
                    }
            );

            layers.add(compatProxy);
            APRandomizer.LOGGER.info("Successfully registered Light Holder dynamic light layer with LambDynamicLights (CompatLayer).");
            return true;
        } catch (ClassNotFoundException e) {
            APRandomizer.LOGGER.debug("LambDynamicLights CompatLayer not found, trying legacy hook.");
            return false;
        } catch (Throwable t) {
            APRandomizer.LOGGER.warn("Failed to register dynamic light layer with LambDynamicLights CompatLayer", t);
            return false;
        }
    }

    private static void tryRegisterOldLambDynamicLights() {
        try {
            Class<?> handlersClass = Class.forName("dev.lambdaurora.lambdynlights.api.DynamicLightHandlers");
            Class<?> handlerInterface = Class.forName("dev.lambdaurora.lambdynlights.api.DynamicLightHandler");

            Object handlerProxy = Proxy.newProxyInstance(
                    handlerInterface.getClassLoader(),
                    new Class<?>[]{ handlerInterface },
                    (proxy, method, args) -> {
                        String name = method.getName();
                        if ("getLuminance".equals(name) && args != null && args.length > 0) {
                            if (args[0] instanceof Player player) {
                                ItemStack stack = ItemStack.EMPTY;
                                if (player == Minecraft.getInstance().player) {
                                    stack = ClientAbilityManager.getLightHolderItem();
                                }
                                if (stack.isEmpty()) {
                                    stack = AbilityManager.getLightHolderItem(player);
                                }
                                if (!stack.isEmpty()) {
                                    Block block = Block.byItem(stack.getItem());
                                    if (block != Blocks.AIR) {
                                        return block.defaultBlockState().getLightEmission();
                                    }
                                    if (stack.is(Items.LAVA_BUCKET)) {
                                        return 15;
                                    }
                                }
                            }
                            return 0;
                        }
                        if ("isWaterSensitive".equals(name)) {
                            return false;
                        }
                        if ("hashCode".equals(name)) {
                            return System.identityHashCode(proxy);
                        }
                        if ("equals".equals(name) && args != null && args.length == 1) {
                            return proxy == args[0];
                        }
                        if ("toString".equals(name)) {
                            return "APRandomizer$DynamicLightHandler";
                        }
                        return null;
                    }
            );

            Method registerMethod = handlersClass.getMethod("registerDynamicLightHandler", EntityType.class, handlerInterface);
            registerMethod.invoke(null, EntityTypes.PLAYER, handlerProxy);
            APRandomizer.LOGGER.info("Successfully registered Light Holder dynamic light handler with LambDynamicLights (legacy API).");
        } catch (ClassNotFoundException e) {
            APRandomizer.LOGGER.debug("LambDynamicLights not detected, dynamic light hook skipped.");
        } catch (Exception e) {
            APRandomizer.LOGGER.warn("Failed to register dynamic light handler with LambDynamicLights", e);
        }
    }
}
