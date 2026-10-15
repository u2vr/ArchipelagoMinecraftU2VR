package gg.archipelago.aprandomizer.ap.storage;

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.Nullable;

public record APMCMetaData(
        @Nullable String server,
        @SerializedName(value = "password", alternate = {"server_password", "room_password"})
        @Nullable String password
        // unused stuff left uncommented to avoid potential json deserialization errors on, well, unused stuff
//        int compatible_version,
//        int version,
//        int player,
//        String player_name,
//        String game,
//        String patch_file_ending
) {
}

