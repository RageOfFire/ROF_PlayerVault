package me.rageoffire.rofplayervault.utils;

import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Sends a title + subtitle to a player in a version-safe way.
 *
 * On 1.11+  → uses the official Player#sendTitle API.
 * On 1.8–1.10 → uses NMS packets via reflection.
 */
public final class TitleHelper {

    private TitleHelper() {}

    /** Whether Player#sendTitle(String,String,int,int,int) is available (1.11+). */
    private static final boolean USE_API;

    static {
        boolean api = false;
        try {
            Player.class.getMethod("sendTitle", String.class, String.class, int.class, int.class, int.class);
            api = true;
        } catch (NoSuchMethodException ignored) {}
        USE_API = api;
    }

    public static void sendTitle(Player player, String title, String subtitle,
                                 int fadeIn, int stay, int fadeOut) {
        if (USE_API) {
            player.sendTitle(title, subtitle, fadeIn, stay, fadeOut);
        } else {
            sendTitleReflection(player, title, subtitle, fadeIn, stay, fadeOut);
        }
    }

    // ── NMS reflection path (1.8–1.10) ───────────────────────────────────────

    private static void sendTitleReflection(Player player, String title, String subtitle,
                                            int fadeIn, int stay, int fadeOut) {
        try {
            String version  = player.getServer().getClass().getPackage().getName().split("\\.")[3];
            String nmsBase  = "net.minecraft.server." + version + ".";
            String cbBase   = "org.bukkit.craftbukkit." + version + ".";

            Class<?> craftPlayerClass    = Class.forName(cbBase + "entity.CraftPlayer");
            Object   craftPlayer         = craftPlayerClass.cast(player);
            Method   getHandle           = craftPlayerClass.getMethod("getHandle");
            Object   nmsPlayer           = getHandle.invoke(craftPlayer);
            Field    connectionField      = nmsPlayer.getClass().getField("playerConnection");
            Object   connection           = connectionField.get(nmsPlayer);
            Method   sendPacket           = connection.getClass().getMethod("sendPacket",
                    Class.forName(nmsBase + "Packet"));

            Class<?> chatSerializerClass  = Class.forName(nmsBase + "IChatBaseComponent$ChatSerializer");
            Class<?> iChatBaseComponent   = Class.forName(nmsBase + "IChatBaseComponent");
            Method   fromJson             = chatSerializerClass.getMethod("a", String.class);

            Class<?> titlePacketClass     = Class.forName(nmsBase + "PacketPlayOutTitle");
            Class<?> titleActionClass     = titlePacketClass.getDeclaredClasses()[0];

            // TIMES packet
            Object timesAction = Enum.valueOf((Class<Enum>) titleActionClass, "TIMES");
            Constructor<?> timesConstructor = titlePacketClass.getConstructor(
                    titleActionClass, iChatBaseComponent, int.class, int.class, int.class);
            Object timesPacket = timesConstructor.newInstance(timesAction, null, fadeIn, stay, fadeOut);
            sendPacket.invoke(connection, timesPacket);

            // TITLE packet
            Object titleAction  = Enum.valueOf((Class<Enum>) titleActionClass, "TITLE");
            Object titleComp    = fromJson.invoke(null, "{\"text\":\"" + escapeJson(title) + "\"}");
            Constructor<?> titleCtor = titlePacketClass.getConstructor(titleActionClass, iChatBaseComponent);
            Object titlePacket  = titleCtor.newInstance(titleAction, titleComp);
            sendPacket.invoke(connection, titlePacket);

            // SUBTITLE packet
            if (subtitle != null && !subtitle.isEmpty()) {
                Object subtitleAction = Enum.valueOf((Class<Enum>) titleActionClass, "SUBTITLE");
                Object subtitleComp   = fromJson.invoke(null, "{\"text\":\"" + escapeJson(subtitle) + "\"}");
                Object subtitlePacket = titleCtor.newInstance(subtitleAction, subtitleComp);
                sendPacket.invoke(connection, subtitlePacket);
            }

        } catch (Exception e) {
            // Title display is non-critical — swallow silently
        }
    }

    private static String escapeJson(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
