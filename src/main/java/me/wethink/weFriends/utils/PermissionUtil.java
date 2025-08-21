package me.wethink.weFriends.utils;

import me.wethink.weFriends.WeFriends;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.UUID;

public final class PermissionUtil {
    private PermissionUtil() {
    }

    public static boolean canAddFriend(Player player) {
        return player.hasPermission("wefriends.friend.add");
    }

    public static boolean canAcceptFriend(Player player) {
        return player.hasPermission("wefriends.friend.accept");
    }

    public static boolean canDenyFriend(Player player) {
        return player.hasPermission("wefriends.friend.deny");
    }

    public static boolean canRemoveFriend(Player player) {
        return player.hasPermission("wefriends.friend.remove");
    }

    public static boolean canListFriends(Player player) {
        return player.hasPermission("wefriends.friend.list");
    }

    public static boolean canToggleRequests(Player player) {
        return player.hasPermission("wefriends.friend.toggle");
    }

    public static boolean canUseFriendChat(Player player) {
        return player.hasPermission("wefriends.friend.chat");
    }

    public static boolean canSendFriendMessage(Player player) {
        return player.hasPermission("wefriends.friend.msg");
    }

    public static boolean canCreateParty(Player player) {
        return player.hasPermission("wefriends.party.create");
    }

    public static boolean canInviteToParty(Player player) {
        return player.hasPermission("wefriends.party.invite");
    }

    public static boolean canAcceptPartyInvite(Player player) {
        return player.hasPermission("wefriends.party.accept");
    }

    public static boolean canDenyPartyInvite(Player player) {
        return player.hasPermission("wefriends.party.deny");
    }

    public static boolean canLeaveParty(Player player) {
        return player.hasPermission("wefriends.party.leave");
    }

    public static boolean canKickFromParty(Player player) {
        return player.hasPermission("wefriends.party.kick");
    }

    public static boolean canDisbandParty(Player player) {
        return player.hasPermission("wefriends.party.disband");
    }

    public static boolean canUsePartyChat(Player player) {
        return player.hasPermission("wefriends.party.chat");
    }

    public static boolean canSpyFriendChat(Player player) {
        return player.hasPermission("wefriends.admin.spy.fchat");
    }

    public static boolean canSpyPartyChat(Player player) {
        return player.hasPermission("wefriends.admin.spy.party");
    }

    public static boolean canSpyAll(Player player) {
        return player.hasPermission("wefriends.admin.spy.all");
    }

    public static boolean canReload(Player player) {
        return player.hasPermission("wefriends.admin.reload");
    }

    public static boolean canDebug(Player player) {
        return player.hasPermission("wefriends.admin.debug");
    }

    public static boolean canUseFriendSpy(Player player) {
        return canSpyFriendChat(player) || canSpyAll(player);
    }

    public static boolean canUsePartySpy(Player player) {
        return canSpyPartyChat(player) || canSpyAll(player);
    }

    public static boolean canAddMoreFriends(WeFriends plugin, Player player) {
        if (player.hasPermission("wefriends.limits.unlimited")) {
            return true;
        }

        Set<UUID> friends = plugin.getFriendManager().getFriendUuidsPublic(player.getUniqueId());
        int maxFriends = ConfigUtil.getMaxFriends();

        return friends.size() < maxFriends;
    }

    public static boolean canAddMorePartyMembers(WeFriends plugin, Player player) {
        if (player.hasPermission("wefriends.limits.unlimited")) {
            return true;
        }

        String partyId = plugin.getPartyManager().getPartyIdPublic(player.getUniqueId());
        if (partyId == null) {
            return true;
        }

        int memberCount = plugin.getPartyManager().getPartyMembers(partyId).size();
        int maxPartySize = ConfigUtil.getMaxPartySize();

        return memberCount < maxPartySize;
    }

    public static String getFriendLimitMessage(Player player) {
        if (player.hasPermission("wefriends.limits.unlimited")) {
            return "You have unlimited friends!";
        }

        int maxFriends = ConfigUtil.getMaxFriends();
        return "You can have up to " + maxFriends + " friends.";
    }

    public static String getPartyLimitMessage(Player player) {
        if (player.hasPermission("wefriends.limits.unlimited")) {
            return "You have unlimited party size!";
        }

        int maxPartySize = ConfigUtil.getMaxPartySize();
        return "Your party can have up to " + maxPartySize + " members.";
    }
}
