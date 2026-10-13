package me.rageoffire.rofplayervault.utils;

import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.*;
import java.util.Base64;

/**
 * Serialises and deserialises ItemStack objects to/from byte arrays and Base64 strings.
 * Uses Bukkit's own serialisation so NBT data (enchants, names, lore, etc.) is preserved.
 */
public final class ItemSerializer {

    private ItemSerializer() {}

    // ── byte[] ────────────────────────────────────────────────────────────────

    public static byte[] serialize(ItemStack item) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             BukkitObjectOutputStream oos = new BukkitObjectOutputStream(bos)) {
            oos.writeObject(item);
            return bos.toByteArray();
        }
    }

    public static ItemStack deserialize(byte[] data) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(data);
             BukkitObjectInputStream ois = new BukkitObjectInputStream(bis)) {
            return (ItemStack) ois.readObject();
        }
    }

    // ── Base64 (for MySQL TEXT column) ────────────────────────────────────────

    public static String serializeBase64(ItemStack item) throws IOException {
        return Base64.getEncoder().encodeToString(serialize(item));
    }

    public static ItemStack deserializeBase64(String base64) throws IOException, ClassNotFoundException {
        return deserialize(Base64.getDecoder().decode(base64));
    }
}
