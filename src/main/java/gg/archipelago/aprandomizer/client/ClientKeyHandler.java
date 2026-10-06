package gg.archipelago.aprandomizer.client;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;

import gg.archipelago.aprandomizer.APMenus;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@OnlyIn(Dist.CLIENT)
public class ClientKeyHandler {
    public static final KeyMapping OPEN_RECIPE_TREE = new KeyMapping(
            "key.aprandomizer.recipe_tree",
            73, // GLFW_KEY_I
            KeyMapping.Category.INVENTORY
    );

    public static void init(IEventBus modEventBus) {
        modEventBus.addListener(ClientKeyHandler::onRegisterKeyMappings);
        modEventBus.addListener(ClientKeyHandler::onRegisterMenuScreens);
        NeoForge.EVENT_BUS.register(ClientKeyHandler.class);
    }

    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_RECIPE_TREE);
    }

    public static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(APMenus.SHARED_CHEST.get(), SharedChestScreen::new);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        while (OPEN_RECIPE_TREE.consumeClick()) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.gui.screen() == null) {
                mc.gui.setScreen(new RecipeSkillTreeScreen(null));
            } else if (mc.gui.screen() instanceof RecipeSkillTreeScreen treeScreen) {
                treeScreen.onClose();
            } else if (mc.gui.screen() instanceof PauseScreen pauseScreen) {
                mc.gui.setScreen(new RecipeSkillTreeScreen(pauseScreen));
            }
        }
    }
}
