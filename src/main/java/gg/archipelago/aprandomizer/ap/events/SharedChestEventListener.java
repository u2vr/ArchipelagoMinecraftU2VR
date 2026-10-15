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
        server.execute(() -> SharedChestManager.handleArchipelagoRetrieved(event));
    }

    @ArchipelagoEventListener
    public void onSetReply(SetReplyEvent event) {
        server.execute(() -> SharedChestManager.handleArchipelagoSetReply(event));
    }
}
