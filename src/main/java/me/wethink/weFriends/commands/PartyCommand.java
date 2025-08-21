package me.wethink.weFriends.commands;

import me.wethink.weFriends.WeFriends;
import me.wethink.weFriends.managers.PartyManager;
import me.wethink.weFriends.utils.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PartyCommand implements CommandExecutor {

    private final WeFriends plugin;
    private final PartyManager partyManager;

    public PartyCommand(WeFriends plugin) {
        this.plugin = plugin;
        this.partyManager = plugin.getPartyManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used in-game.");
            return true;
        }

        if (args.length == 0) {
            MessageUtil.send(player, "usage.party");
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "create":
                partyManager.createParty(player);
                return true;
            case "invite":
                if (args.length < 2) {
                    MessageUtil.send(player, "usage.party_invite");
                    return true;
                }
                partyManager.invite(player, args[1]);
                return true;
            case "accept":
                if (args.length < 2) {
                    MessageUtil.send(player, "usage.party_accept");
                    return true;
                }
                partyManager.accept(player, args[1]);
                return true;
            case "deny":
                if (args.length < 2) {
                    MessageUtil.send(player, "usage.party_deny");
                    return true;
                }
                partyManager.deny(player, args[1]);
                return true;
            case "leave":
                partyManager.leave(player);
                return true;
            case "kick":
                if (args.length < 2) {
                    MessageUtil.send(player, "usage.party_kick");
                    return true;
                }
                partyManager.kick(player, args[1]);
                return true;
            case "promote":
                if (args.length < 2) {
                    MessageUtil.send(player, "usage.party_promote");
                    return true;
                }
                partyManager.promote(player, args[1]);
                return true;
            case "transfer":
                if (args.length < 2) {
                    MessageUtil.send(player, "usage.party_transfer");
                    return true;
                }
                partyManager.transfer(player, args[1]);
                return true;
            case "disband":
                partyManager.disband(player);
                return true;
            case "chat":
                if (args.length < 2) {
                    MessageUtil.send(player, "usage.pc");
                    return true;
                }
                partyManager.sendPartyChat(player, String.join(" ", args).substring(sub.length()).trim());
                return true;
            default:
                MessageUtil.send(player, "usage.party");
                return true;
        }
    }

    public static class PartyChatExecutor implements CommandExecutor {
        private final WeFriends plugin;

        public PartyChatExecutor(WeFriends plugin) {
            this.plugin = plugin;
        }

        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (!(sender instanceof Player player)) return true;
            if (args.length == 0) {
                MessageUtil.send(player, "usage.pc");
                return true;
            }
            plugin.getPartyManager().sendPartyChat(player, String.join(" ", args));
            return true;
        }
    }
}


