package gg.archipelago.aprandomizer.ap.events;

import gg.archipelago.aprandomizer.managers.advancementmanager.AdvancementManager;
import io.github.archipelagomw.events.ArchipelagoEventListener;
import io.github.archipelagomw.events.CheckedLocationsEvent;

public class LocationChecked {
    private final AdvancementManager advancementManager;

    public LocationChecked(AdvancementManager advancementManager) {
        this.advancementManager = advancementManager;
    }

    @ArchipelagoEventListener
    public void onLocationChecked(CheckedLocationsEvent event) {
        if (event.checkedLocations == null || event.checkedLocations.isEmpty()) return;
        boolean showToasts = event.checkedLocations.size() <= 2;
        advancementManager.addAdvancements(event.checkedLocations, showToasts);
    }
}
