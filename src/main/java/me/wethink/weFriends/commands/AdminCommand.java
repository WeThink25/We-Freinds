package me.wethink.weFriends.commands;

import me.wethink.weFriends.WeFriends;
import me.wethink.weFriends.utils.ConfigUtil;
import me.wethink.weFriends.utils.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AdminCommand implements CommandExecutor {

    private final WeFriends plugin;

    public AdminCommand(WeFriends plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("/wefriends <reload|version|debug|spy>");
            return true;
        }
        String sub = args[0].toLowerCase();
        switch (sub) {
            case "reload":
                plugin.reloadConfig();
                ConfigUtil.load(plugin);
                MessageUtil.load(plugin);
                sender.sendMessage("We-Friends reloaded.");
                return true;
            case "version":
                sender.sendMessage("We-Friends v" + plugin.getDescription().getVersion());
                return true;
            case "debug":
                sender.sendMessage(plugin.getDatabaseManager().debugString());
                return true;
            case "spy":
                if (!(sender instanceof Player player)) return true;
                boolean enabled = plugin.getSpyManager().toggleAll(player.getUniqueId());
                MessageUtil.send(player, enabled ? "spy.all_on" : "spy.all_off");
                return true;
            default:
                sender.sendMessage("/wefriends <reload|version|debug|spy>");
                return true;
        }
    }
}


