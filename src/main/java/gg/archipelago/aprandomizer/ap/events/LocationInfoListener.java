package gg.archipelago.aprandomizer.ap.events;

import gg.archipelago.aprandomizer.managers.trademanager.WanderingTraderManager;
import io.github.archipelagomw.events.ArchipelagoEventListener;
import io.github.archipelagomw.events.LocationInfoEvent;

public class LocationInfoListener {

    @ArchipelagoEventListener
    public void onLocationInfo(LocationInfoEvent event) {
        if (event != null && event.locations != null) {
            WanderingTraderManager.onScoutReceived(event.locations);
        }
    }
}
