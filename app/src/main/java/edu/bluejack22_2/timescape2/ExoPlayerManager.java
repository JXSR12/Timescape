package edu.bluejack22_2.timescape2;

import com.google.android.exoplayer2.ExoPlayer;

import java.util.ArrayList;
import java.util.List;

public class ExoPlayerManager {
    private static ExoPlayerManager instance;
    private List<ExoPlayer> activePlayers;

    private ExoPlayerManager() {
        activePlayers = new ArrayList<>();
    }

    public static ExoPlayerManager getInstance() {
        if (instance == null) {
            instance = new ExoPlayerManager();
        }
        return instance;
    }

    public void registerPlayer(ExoPlayer player) {
        if (!activePlayers.contains(player)) {
            activePlayers.add(player);
        }
    }

    public void unregisterPlayer(ExoPlayer player) {
        activePlayers.remove(player);
    }

    public void stopAllPlayers() {
        for (ExoPlayer player : activePlayers) {
            player.stop();
            player.release();
        }
        activePlayers.clear();
    }
}
