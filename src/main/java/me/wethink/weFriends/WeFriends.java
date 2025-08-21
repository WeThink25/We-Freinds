package me.wethink.weFriends;

import me.wethink.weFriends.commands.*;
import me.wethink.weFriends.listeners.ChatListener;
import me.wethink.weFriends.listeners.PlayerConnectionListener;
import me.wethink.weFriends.managers.*;
import me.wethink.weFriends.placeholders.WeFriendsExpansion;
import me.wethink.weFriends.utils.ConfigUtil;
import me.wethink.weFriends.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class WeFriends extends JavaPlugin {

    private static WeFriends instance;

    private DatabaseManager databaseManager;
    private FriendManager friendManager;
    private PartyManager partyManager;
    private SpyManager spyManager;
    private RedisManager redisManager;
    private ChatModeManager chatModeManager;
    private WeFriendsExpansion placeholderExpansion;

    public static WeFriends getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        ConfigUtil.load(this);
        MessageUtil.load(this);

        this.databaseManager = new DatabaseManager(this);
        this.friendManager = new FriendManager(this, databaseManager);
        this.partyManager = new PartyManager(this, databaseManager);
        this.spyManager = new SpyManager();
        this.redisManager = new RedisManager(this);
        this.chatModeManager = new ChatModeManager(this);

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            this.placeholderExpansion = new WeFriendsExpansion(this);
            this.placeholderExpansion.register();
            log("&aPlaceholderAPI expansion registered");
        }

        getServer().getPluginManager().registerEvents(new PlayerConnectionListener(this), this);
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);

        registerCommands();
        registerTabCompleters();

        Bukkit.getConsoleSender().sendMessage("§b==========================================");
        Bukkit.getConsoleSender().sendMessage("§a             §lWe-Friends");
        Bukkit.getConsoleSender().sendMessage("§b==========================================");
        Bukkit.getConsoleSender().sendMessage("§ePlugin Information:");
        Bukkit.getConsoleSender().sendMessage(" §a• §fVersion: §d" + getDescription().getVersion());
        Bukkit.getConsoleSender().sendMessage(" §a• §fAuthor(s): §dWeThink");
        Bukkit.getConsoleSender().sendMessage("");
        Bukkit.getConsoleSender().sendMessage("§eServer Information:");
        Bukkit.getConsoleSender().sendMessage(" §a• §fSoftware: §b" + Bukkit.getServer().getName());
        Bukkit.getConsoleSender().sendMessage(" §a• §fVersion: §b" + Bukkit.getServer().getVersion());
        Bukkit.getConsoleSender().sendMessage("§b==========================================");
        Bukkit.getConsoleSender().sendMessage("§a§lWe-Friends §fhas been §a§lENABLED§f!");
        Bukkit.getConsoleSender().sendMessage("§b==========================================");
    }

    private void registerCommands() {
        PluginCommand friend = getCommand("friend");
        if (friend != null) friend.setExecutor(new FriendCommand(this));

        PluginCommand fchat = getCommand("fchat");
        if (fchat != null) fchat.setExecutor(new FriendCommand.FriendChatExecutor(this));

        PluginCommand fmsg = getCommand("fmsg");
        if (fmsg != null) fmsg.setExecutor(new FriendCommand.FriendMessageExecutor(this));

        PluginCommand party = getCommand("party");
        if (party != null) party.setExecutor(new PartyCommand(this));

        PluginCommand pc = getCommand("pc");
        if (pc != null) pc.setExecutor(new PartyCommand.PartyChatExecutor(this));

        PluginCommand fchatmode = getCommand("fchatmode");
        if (fchatmode != null) fchatmode.setExecutor(new ChatModeCommand(this));

        PluginCommand pchatmode = getCommand("pchatmode");
        if (pchatmode != null) pchatmode.setExecutor(new ChatModeCommand(this));

        PluginCommand chatmode = getCommand("chatmode");
        if (chatmode != null) chatmode.setExecutor(new ChatModeCommand(this));

        PluginCommand fspy = getCommand("fchatspy");
        if (fspy != null) fspy.setExecutor(new SpyCommand(this));

        PluginCommand pspy = getCommand("partyspy");
        if (pspy != null) pspy.setExecutor(new SpyCommand(this));

        PluginCommand admin = getCommand("wefriends");
        if (admin != null) admin.setExecutor(new AdminCommand(this));
    }

    private void registerTabCompleters() {
        WeFriendsTabCompleter weFriendsTabCompleter = new WeFriendsTabCompleter(this);

        PluginCommand friend = getCommand("friend");
        if (friend != null) friend.setTabCompleter(weFriendsTabCompleter);

        PluginCommand fchat = getCommand("fchat");
        if (fchat != null) fchat.setTabCompleter(weFriendsTabCompleter);

        PluginCommand fmsg = getCommand("fmsg");
        if (fmsg != null) fmsg.setTabCompleter(weFriendsTabCompleter);

        PluginCommand party = getCommand("party");
        if (party != null) party.setTabCompleter(weFriendsTabCompleter);

        PluginCommand pc = getCommand("pc");
        if (pc != null) pc.setTabCompleter(weFriendsTabCompleter);

        PluginCommand fchatmode = getCommand("fchatmode");
        if (fchatmode != null) fchatmode.setTabCompleter(weFriendsTabCompleter);

        PluginCommand pchatmode = getCommand("pchatmode");
        if (pchatmode != null) pchatmode.setTabCompleter(weFriendsTabCompleter);

        PluginCommand chatmode = getCommand("chatmode");
        if (chatmode != null) chatmode.setTabCompleter(weFriendsTabCompleter);

        PluginCommand fspy = getCommand("fchatspy");
        if (fspy != null) fspy.setTabCompleter(weFriendsTabCompleter);

        PluginCommand pspy = getCommand("partyspy");
        if (pspy != null) pspy.setTabCompleter(weFriendsTabCompleter);

        PluginCommand admin = getCommand("wefriends");
        if (admin != null) admin.setTabCompleter(weFriendsTabCompleter);
    }

    @Override
    public void onDisable() {
        if (partyManager != null) partyManager.shutdown();
        if (friendManager != null) friendManager.shutdown();
        if (databaseManager != null) databaseManager.shutdown();
        if (redisManager != null) redisManager.shutdown();
        if (placeholderExpansion != null) placeholderExpansion.unregister();
        log("&cWeFriends disabled");
    }

    // ✅ Utility for colored console logs
    private void log(String msg) {
        getLogger().info(ChatColor.translateAlternateColorCodes('&', msg));
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public FriendManager getFriendManager() {
        return friendManager;
    }

    public PartyManager getPartyManager() {
        return partyManager;
    }

    public SpyManager getSpyManager() {
        return spyManager;
    }

    public RedisManager getRedisManager() {
        return redisManager;
    }

    public ChatModeManager getChatModeManager() {
        return chatModeManager;
    }
}
