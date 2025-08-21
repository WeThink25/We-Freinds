package me.wethink.weFriends.commands;

import me.wethink.weFriends.WeFriends;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class WeFriendsTabCompleter implements TabCompleter {
    private final WeFriends plugin;

    public WeFriendsTabCompleter(WeFriends plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        switch (command.getName().toLowerCase()) {
            case "friend":
                completions = handleFriendTabComplete(sender, args);
                break;
            case "fchat":
                completions = handleFchatTabComplete(sender, args);
                break;
            case "fmsg":
                completions = handleFmsgTabComplete(sender, args);
                break;
            case "party":
                completions = handlePartyTabComplete(sender, args);
                break;
            case "pc":
                completions = handlePcTabComplete(sender, args);
                break;
            case "fchatmode":
            case "pchatmode":
            case "chatmode":
                completions = handleChatModeTabComplete(sender, args);
                break;
            case "fchatspy":
            case "partyspy":
                completions = handleSpyTabComplete(sender, args);
                break;
            case "wefriends":
                completions = handleAdminTabComplete(sender, args);
                break;
        }

        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
                .collect(Collectors.toList());
    }

    private List<String> handleFriendTabComplete(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("add");
            completions.add("accept");
            completions.add("deny");
            completions.add("remove");
            completions.add("list");
            completions.add("requests");
            completions.add("toggle");
            completions.add("chat");
        } else if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "add":
                    completions.addAll(getOnlinePlayerNames(sender));
                    break;
                case "accept":
                case "deny":
                    completions.addAll(getPendingRequests(sender));
                    break;
                case "remove":
                    completions.addAll(getFriendNames(sender));
                    break;
            }
        }

        return completions;
    }

    private List<String> handleFchatTabComplete(CommandSender sender, String[] args) {
        return new ArrayList<>();
    }

    private List<String> handleFmsgTabComplete(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(getFriendNames(sender));
        }

        return completions;
    }

    private List<String> handlePartyTabComplete(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("create");
            completions.add("invite");
            completions.add("accept");
            completions.add("deny");
            completions.add("leave");
            completions.add("kick");
            completions.add("promote");
            completions.add("transfer");
            completions.add("disband");
            completions.add("chat");
        } else if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "invite":
                    completions.addAll(getFriendNames(sender));
                    break;
                case "kick":
                case "promote":
                case "transfer":
                    completions.addAll(getPartyMemberNames(sender));
                    break;
            }
        }

        return completions;
    }

    private List<String> handlePcTabComplete(CommandSender sender, String[] args) {
        return new ArrayList<>();
    }

    private List<String> handleChatModeTabComplete(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("global");
            completions.add("friend");
            completions.add("party");
            completions.add("reset");
        }

        return completions;
    }

    private List<String> handleSpyTabComplete(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("on");
            completions.add("off");
            completions.add("toggle");
        }

        return completions;
    }

    private List<String> handleAdminTabComplete(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("reload");
            completions.add("debug");
        }

        return completions;
    }

    private List<String> getOnlinePlayerNames(CommandSender sender) {
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> !name.equals(sender.getName()))
                .collect(Collectors.toList());
    }

    private List<String> getFriendNames(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            return new ArrayList<>();
        }

        return plugin.getFriendManager().getFriendUuidsPublic(player.getUniqueId()).stream()
                .map(uuid -> Bukkit.getOfflinePlayer(uuid).getName())
                .filter(name -> name != null)
                .collect(Collectors.toList());
    }

    private List<String> getPendingRequests(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            return new ArrayList<>();
        }
        return plugin.getFriendManager().getPendingRequestNames(player.getUniqueId());
    }

    private List<String> getPartyMemberNames(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            return new ArrayList<>();
        }

        String partyId = plugin.getPartyManager().getPartyIdPublic(player.getUniqueId());
        if (partyId == null) {
            return new ArrayList<>();
        }

        return plugin.getPartyManager().getPartyMembers(partyId).stream()
                .map(uuid -> Bukkit.getOfflinePlayer(uuid).getName())
                .filter(name -> name != null)
                .filter(name -> !name.equals(sender.getName()))
                .collect(Collectors.toList());
    }
}
