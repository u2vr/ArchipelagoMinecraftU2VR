package gg.archipelago.aprandomizer.ap.events;

import gg.archipelago.aprandomizer.managers.chest.SharedChestManager;
import io.github.archipelagomw.events.ArchipelagoEventListener;
import io.github.archipelagomw.events.RetrievedEvent;
import io.github.archipelagomw.events.SetReplyEvent;
import net.minecraft.server.MinecraftServer;

public class SharedChestEventListener {
    private final MinecraftServer server;

    public SharedChestEventListener(MinecraftServer server) {
        this.server = server;
    }

    @ArchipelagoEventListener
    public void onRetrieved(RetrievedEvent event) {
        if (event.containsKey(SharedChestManager.STORAGE_KEY)) {
            Object value = event.getObject(SharedChestManager.STORAGE_KEY);
            server.execute(() -> SharedChestManager.handleArchipelagoUpdate(value));
        }
        if (event.containsKey(SharedChestManager.LOCK_KEY)) {
            Object value = event.getObject(SharedChestManager.LOCK_KEY);
            server.execute(() -> SharedChestManager.handleLockRetrieved(value));
        }
    }

    @ArchipelagoEventListener
    public void onSetReply(SetReplyEvent event) {
        if (SharedChestManager.STORAGE_KEY.equals(event.key)) {
            Object value = event.value;
            server.execute(() -> SharedChestManager.handleArchipelagoUpdate(value));
        } else if (SharedChestManager.LOCK_KEY.equals(event.key)) {
            server.execute(() -> SharedChestManager.handleLockSetReply(event));
        }
    }
}
