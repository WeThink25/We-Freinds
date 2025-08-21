package me.wethink.weFriends.models;

import java.util.UUID;

public class Friend {
    private final UUID userUuid;
    private final UUID friendUuid;
    private final long createdAt;

    public Friend(UUID userUuid, UUID friendUuid, long createdAt) {
        this.userUuid = userUuid;
        this.friendUuid = friendUuid;
        this.createdAt = createdAt;
    }

    public UUID getUserUuid() {
        return userUuid;
    }

    public UUID getFriendUuid() {
        return friendUuid;
    }

    public long getCreatedAt() {
        return createdAt;
    }
}


