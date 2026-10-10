package gg.archipelago.aprandomizer.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.AdvancementToast;
import net.minecraft.client.gui.components.toasts.RecipeToast;
import net.minecraft.client.gui.components.toasts.Toast;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ToastAddEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@OnlyIn(Dist.CLIENT)
public class ClientToastHandler {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final int MAX_BURST_TOASTS = 2;
    private static final long BURST_WINDOW_MS = 2000L;
    private static final long SUPPRESS_COOLDOWN_MS = 4000L;

    private static long windowStartTime = 0L;
    private static int recentToastCount = 0;
    private static long suppressUntil = 0L;

    public static void init() {
        NeoForge.EVENT_BUS.register(ClientToastHandler.class);
    }

    public static void suppressToastsFor(long durationMs) {
        suppressUntil = Math.max(suppressUntil, System.currentTimeMillis() + durationMs);
        clearToastQueue();
    }

    public static void clearToastQueue() {
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.gui != null && mc.gui.toastManager() != null) {
            mc.gui.toastManager().clear();
        }
    }

    @SubscribeEvent
    public static void onToastAdd(ToastAddEvent event) {
        Toast toast = event.getToast();
        if (!(toast instanceof AdvancementToast || toast instanceof RecipeToast)) {
            return;
        }

        long now = System.currentTimeMillis();

        if (now < suppressUntil) {
            event.setCanceled(true);
            return;
        }

        if (now - windowStartTime > BURST_WINDOW_MS) {
            windowStartTime = now;
            recentToastCount = 1;
        } else {
            recentToastCount++;
            if (recentToastCount > MAX_BURST_TOASTS) {
                LOGGER.info("Toast flood detected ({} toasts in {}ms). Suppressing toast spam and clearing queue.",
                        recentToastCount, now - windowStartTime);
                event.setCanceled(true);
                suppressUntil = now + SUPPRESS_COOLDOWN_MS;
                clearToastQueue();
            }
        }
    }
}
