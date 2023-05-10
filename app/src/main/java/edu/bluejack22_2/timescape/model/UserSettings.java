package edu.bluejack22_2.timescape.model;

import java.util.List;

public class UserSettings {
    private List<String> mutedChats;

    public List<String> getMutedChats() {
        return mutedChats;
    }

    public void setMutedChats(List<String> mutedChats) {
        this.mutedChats = mutedChats;
    }
}
