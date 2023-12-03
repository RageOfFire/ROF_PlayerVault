package me.rof_playervault.utils;

import me.rof_playervault.ROF_PlayerVault;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;

public class FuctionHandler {
    private final ROF_PlayerVault plugin;
    public FuctionHandler(ROF_PlayerVault plugin) {
        this.plugin = plugin;
    }
    public int getAmount(Player player) {
        String permissionPrefix = "rofvault.storage.";
        String permission;

        if(player.hasPermission(permissionPrefix + "*")) {
            return -1;
        }

        for (PermissionAttachmentInfo attachmentInfo : player.getEffectivePermissions()) {
            permission = attachmentInfo.getPermission();
            if (permission.startsWith(permissionPrefix)) {
                return Integer.parseInt(permission.substring(permission.lastIndexOf(".") + 1));
            }
        }

        return 0;
    }
    public Player checkPlayer(String player) {
        Player target = plugin.getServer().getPlayerExact(player);
        if (target.isOnline()) {
            return plugin.getServer().getPlayer(target.getUniqueId());
        } else {
            return plugin.getServer().getOfflinePlayer(target.getUniqueId()).getPlayer();
        }
    }

    public boolean isInteger(String str) {
        if (str == null) {
            return false;
        }
        int length = str.length();
        if (length == 0) {
            return false;
        }
        int i = 0;
        if (str.charAt(0) == '-') {
            if (length == 1) {
                return false;
            }
            i = 1;
        }
        for (; i < length; i++) {
            char c = str.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    }
}
