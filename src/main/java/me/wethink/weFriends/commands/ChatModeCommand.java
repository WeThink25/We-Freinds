package me.wethink.weFriends.commands;

import me.wethink.weFriends.WeFriends;
import me.wethink.weFriends.managers.ChatModeManager;
import me.wethink.weFriends.utils.MessageUtil;
import me.wethink.weFriends.utils.PermissionUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ChatModeCommand implements CommandExecutor {

    private final WeFriends plugin;

    public ChatModeCommand(WeFriends plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used in-game.");
            return true;
        }

        ChatModeManager chatModeManager = plugin.getChatModeManager();
        ChatModeManager.ChatMode currentMode = chatModeManager.getMode(player);

        switch (label.toLowerCase()) {
            case "fchatmode":
                if (!PermissionUtil.canUseFriendChat(player)) {
                    MessageUtil.send(player, "error.no_permission");
                    return true;
                }
                if (currentMode == ChatModeManager.ChatMode.FRIEND) {
                    chatModeManager.resetMode(player);
                } else {
                    chatModeManager.setMode(player, ChatModeManager.ChatMode.FRIEND);
                }
                return true;

            case "pchatmode":
                if (!PermissionUtil.canUsePartyChat(player)) {
                    MessageUtil.send(player, "error.no_permission");
                    return true;
                }
                if (currentMode == ChatModeManager.ChatMode.PARTY) {
                    chatModeManager.resetMode(player);
                } else {
                    chatModeManager.setMode(player, ChatModeManager.ChatMode.PARTY);
                }
                return true;

            case "chatmode":
                chatModeManager.resetMode(player);
                return true;

            default:
                return false;
        }
    }
}
