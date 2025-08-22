package me.wethink.weFriends.utils;

import me.wethink.weFriends.WeFriends;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MessageUtil {
    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static WeFriends plugin;
    private static File messagesFile;
    private static FileConfiguration messages;

    private MessageUtil() {
    }

    public static void load(WeFriends pl) {
        plugin = pl;
        if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        messages = YamlConfiguration.loadConfiguration(messagesFile);
    }

    private static String getRaw(String key) {
        if (messages == null) return key;
        String val = messages.getString(key);
        return val != null ? val : key;
    }

    private static String translateColors(String message) {
        if (message == null) return "";

        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String hexColor = matcher.group(1);
            matcher.appendReplacement(buffer, "<#" + hexColor + ">");
        }
        matcher.appendTail(buffer);

        String result = buffer.toString();
        result = result.replace("&0", "<black>")
                .replace("&1", "<dark_blue>")
                .replace("&2", "<dark_green>")
                .replace("&3", "<dark_aqua>")
                .replace("&4", "<dark_red>")
                .replace("&5", "<dark_purple>")
                .replace("&6", "<gold>")
                .replace("&7", "<gray>")
                .replace("&8", "<dark_gray>")
                .replace("&9", "<blue>")
                .replace("&a", "<green>")
                .replace("&b", "<aqua>")
                .replace("&c", "<red>")
                .replace("&d", "<light_purple>")
                .replace("&e", "<yellow>")
                .replace("&f", "<white>")
                .replace("&l", "<bold>")
                .replace("&n", "<underlined>")
                .replace("&o", "<italic>")
                .replace("&m", "<strikethrough>")
                .replace("&k", "<obfuscated>")
                .replace("&r", "<reset>");

        return result;
    }

    public static void send(Player player, String key) {
        String message = translateColors(getRaw(key));
        player.sendMessage(MM.deserialize(message));
    }

    public static void send(Player player, String key, Map<String, String> placeholders) {
        String raw = getRaw(key);
        for (Map.Entry<String, String> e : placeholders.entrySet()) {
            raw = raw.replace('%' + e.getKey() + '%', e.getValue());
        }
        String message = translateColors(raw);
        player.sendMessage(MM.deserialize(message));
    }

    public static void actionbar(Player player, String key, Map<String, String> placeholders) {
        String raw = getRaw(key);
        for (Map.Entry<String, String> e : placeholders.entrySet()) {
            raw = raw.replace('%' + e.getKey() + '%', e.getValue());
        }
        String message = translateColors(raw);
        Component comp = MM.deserialize(message);
        player.sendActionBar(comp);
    }

    public static void actionbar(Player player, String key) {
        String message = translateColors(getRaw(key));
        Component comp = MM.deserialize(message);
        player.sendActionBar(comp);
    }
}


