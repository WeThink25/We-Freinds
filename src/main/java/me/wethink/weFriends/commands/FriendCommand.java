package me.wethink.weFriends.commands;

import me.wethink.weFriends.WeFriends;
import me.wethink.weFriends.managers.FriendManager;
import me.wethink.weFriends.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class FriendCommand implements CommandExecutor {

    private final WeFriends plugin;
    private final FriendManager friendManager;

    public FriendCommand(WeFriends plugin) {
        this.plugin = plugin;
        this.friendManager = plugin.getFriendManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used in-game.");
            return true;
        }

        if (args.length == 0) {
            MessageUtil.send(player, "usage.friend");
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "add":
                if (args.length < 2) {
                    MessageUtil.send(player, "usage.friend_add");
                    return true;
                }
                handleAdd(player, args[1]);
                return true;
            case "accept":
                if (args.length < 2) {
                    MessageUtil.send(player, "usage.friend_accept");
                    return true;
                }
                friendManager.acceptRequest(player.getUniqueId(), args[1]);
                return true;
            case "deny":
                if (args.length < 2) {
                    MessageUtil.send(player, "usage.friend_deny");
                    return true;
                }
                friendManager.denyRequest(player.getUniqueId(), args[1]);
                return true;
            case "remove":
                if (args.length < 2) {
                    MessageUtil.send(player, "usage.friend_remove");
                    return true;
                }
                friendManager.removeFriend(player.getUniqueId(), args[1]);
                return true;
            case "list":
                friendManager.sendFriendList(player);
                return true;
            case "toggle":
                friendManager.toggleRequests(player.getUniqueId(), player);
                return true;
            case "requests":
                friendManager.sendPendingRequests(player);
                return true;
            case "chat":
            case "fchat":
                if (args.length < 2) {
                    MessageUtil.send(player, "usage.fchat");
                    return true;
                }
                String message = String.join(" ", args).substring(sub.length()).trim();
                friendManager.sendFriendChat(player, message);
                return true;
            default:
                MessageUtil.send(player, "usage.friend");
                return true;
        }
    }

    private void handleAdd(Player player, String targetName) {
        if (player.getName().equalsIgnoreCase(targetName)) {
            MessageUtil.send(player, "error.self_friend");
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayerIfCached(targetName);
        if (target == null) target = Bukkit.getOfflinePlayer(targetName);
        friendManager.sendRequest(player.getUniqueId(), player.getName(), target);
    }

    public static class FriendChatExecutor implements CommandExecutor {
        private final WeFriends plugin;

        public FriendChatExecutor(WeFriends plugin) {
            this.plugin = plugin;
        }

        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (!(sender instanceof Player player)) return true;
            if (args.length == 0) {
                MessageUtil.send(player, "usage.fchat");
                return true;
            }
            plugin.getFriendManager().sendFriendChat(player, String.join(" ", args));
            return true;
        }
    }

    public static class FriendMessageExecutor implements CommandExecutor {
        private final WeFriends plugin;

        public FriendMessageExecutor(WeFriends plugin) {
            this.plugin = plugin;
        }

        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (!(sender instanceof Player player)) return true;
            if (args.length < 2) {
                MessageUtil.send(player, "usage.fmsg");
                return true;
            }
            String target = args[0];
            String message = String.join(" ", args).substring(target.length()).trim();
            plugin.getFriendManager().sendPrivateMessage(player, target, message);
            return true;
        }
    }
}


