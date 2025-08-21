package me.wethink.weFriends.models;

import java.util.UUID;

public class Party {
    private final String partyId;
    private UUID leaderUuid;

    public Party(String partyId, UUID leaderUuid) {
        this.partyId = partyId;
        this.leaderUuid = leaderUuid;
    }

    public String getPartyId() {
        return partyId;
    }

    public UUID getLeaderUuid() {
        return leaderUuid;
    }

    public void setLeaderUuid(UUID leaderUuid) {
        this.leaderUuid = leaderUuid;
    }
}


