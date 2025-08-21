package me.wethink.weFriends.placeholders;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.wethink.weFriends.WeFriends;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

public class WeFriendsExpansion extends PlaceholderExpansion {
    private final WeFriends plugin;

    public WeFriendsExpansion(WeFriends plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "wefriends";
    }

    @Override
    public String getAuthor() {
        return "WeThink";
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if (player == null) return "";

        String[] args = params.split("_");
        if (args.length < 1) return "";

        switch (args[0].toLowerCase()) {
            case "friends":
                return handleFriends(player, args);
            case "party":
                return handleParty(player, args);
            case "lastseen":
                return handleLastSeen(player, args);
            case "status":
                return handleStatus(player, args);
            default:
                return "";
        }
    }

    private String handleFriends(OfflinePlayer player, String[] args) {
        if (args.length < 2) return "0";

        switch (args[1].toLowerCase()) {
            case "count":
                return String.valueOf(getFriendCount(player.getUniqueId()));
            case "online":
                return String.valueOf(getOnlineFriendCount(player.getUniqueId()));
            case "list":
                return getFriendList(player.getUniqueId());
            default:
                return "0";
        }
    }

    private String handleParty(OfflinePlayer player, String[] args) {
        if (args.length < 2) return "";

        switch (args[1].toLowerCase()) {
            case "name":
                return getPartyName(player.getUniqueId());
            case "leader":
                return getPartyLeader(player.getUniqueId());
            case "members":
                return String.valueOf(getPartyMemberCount(player.getUniqueId()));
            case "role":
                return getPartyRole(player.getUniqueId());
            default:
                return "";
        }
    }

    private String handleLastSeen(OfflinePlayer player, String[] args) {
        if (args.length < 2) return "Never";

        String targetName = args[1];
        long lastSeen = getLastSeen(targetName);
        if (lastSeen == 0) return "Never";

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(new Date(lastSeen));
    }

    private String handleStatus(OfflinePlayer player, String[] args) {
        if (args.length < 2) return "offline";

        String targetName = args[1];
        return isOnline(targetName) ? "online" : "offline";
    }

    private int getFriendCount(UUID playerUuid) {
        try {
            Set<UUID> friends = plugin.getFriendManager().getFriendUuidsPublic(playerUuid);
            return friends.size();
        } catch (Exception e) {
            return 0;
        }
    }

    private int getOnlineFriendCount(UUID playerUuid) {
        try {
            Set<UUID> friends = plugin.getFriendManager().getFriendUuidsPublic(playerUuid);
            int online = 0;
            for (UUID friendUuid : friends) {
                if (plugin.getServer().getOfflinePlayer(friendUuid).isOnline()) {
                    online++;
                }
            }
            return online;
        } catch (Exception e) {
            return 0;
        }
    }

    private String getFriendList(UUID playerUuid) {
        try {
            Set<UUID> friends = plugin.getFriendManager().getFriendUuidsPublic(playerUuid);
            StringBuilder sb = new StringBuilder();
            for (UUID friendUuid : friends) {
                OfflinePlayer friend = plugin.getServer().getOfflinePlayer(friendUuid);
                if (sb.length() > 0) sb.append(", ");
                sb.append(friend.getName());
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private String getPartyName(UUID playerUuid) {
        try {
            String partyId = plugin.getPartyManager().getPartyIdPublic(playerUuid);
            return partyId != null ? "Party" : "";
        } catch (Exception e) {
            return "";
        }
    }

    private String getPartyLeader(UUID playerUuid) {
        try {
            String partyId = plugin.getPartyManager().getPartyIdPublic(playerUuid);
            if (partyId == null) return "";

            UUID leaderUuid = plugin.getPartyManager().getPartyLeader(partyId);
            if (leaderUuid == null) return "";

            OfflinePlayer leader = plugin.getServer().getOfflinePlayer(leaderUuid);
            return leader.getName();
        } catch (Exception e) {
            return "";
        }
    }

    private int getPartyMemberCount(UUID playerUuid) {
        try {
            String partyId = plugin.getPartyManager().getPartyIdPublic(playerUuid);
            if (partyId == null) return 0;

            return plugin.getPartyManager().getPartyMembers(partyId).size();
        } catch (Exception e) {
            return 0;
        }
    }

    private String getPartyRole(UUID playerUuid) {
        try {
            String partyId = plugin.getPartyManager().getPartyIdPublic(playerUuid);
            if (partyId == null) return "";

            return plugin.getPartyManager().getPartyRole(playerUuid, partyId);
        } catch (Exception e) {
            return "";
        }
    }

    private long getLastSeen(String playerName) {
        try {
            return plugin.getFriendManager().getLastSeen(playerName);
        } catch (Exception e) {
            return 0;
        }
    }

    private boolean isOnline(String playerName) {
        try {
            Player player = plugin.getServer().getPlayerExact(playerName);
            return player != null && player.isOnline();
        } catch (Exception e) {
            return false;
        }
    }
}
