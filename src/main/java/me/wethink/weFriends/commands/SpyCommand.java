package me.wethink.weFriends.commands;

import me.wethink.weFriends.WeFriends;
import me.wethink.weFriends.managers.SpyManager;
import me.wethink.weFriends.utils.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SpyCommand implements CommandExecutor {

    private final WeFriends plugin;

    public SpyCommand(WeFriends plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used in-game.");
            return true;
        }
        SpyManager spy = plugin.getSpyManager();
        if (label.equalsIgnoreCase("fchatspy")) {
            boolean enabled = spy.toggleFriendSpy(player.getUniqueId());
            MessageUtil.send(player, enabled ? "spy.friend_on" : "spy.friend_off");
            return true;
        }
        if (label.equalsIgnoreCase("partyspy")) {
            boolean enabled = spy.togglePartySpy(player.getUniqueId());
            MessageUtil.send(player, enabled ? "spy.party_on" : "spy.party_off");
            return true;
        }
        return true;
    }
}


