package me.rageoffire.rofplayervault.listeners;

import me.rageoffire.rofplayervault.ROFPlayerVault;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Handles auto-pickup on 1.12+ via EntityPickupItemEvent.
 *
 * This class is compiled but only REGISTERED at runtime when the server
 * is 1.12+. On 1.8–1.11 the class exists in the jar but is never loaded
 * by Bukkit, so the missing event class never causes a NoClassDefFoundError.
 *
 * Registration is done in ROFPlayerVault#startPickupTask() via:
 *   PickupListenerFactory.register(plugin)
 */
public class EntityPickupListener implements Listener {

    private final PickupListener delegate;

    public EntityPickupListener(ROFPlayerVault plugin) {
        this.delegate = new PickupListener(plugin);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();
        if (!delegate.shouldIntercept(player)) return;

        ItemStack item = event.getItem().getItemStack();
        if (!delegate.shouldPickup(item)) return;
        if (!delegate.redirectToVault(player, item)) return;

        event.getItem().remove();
        event.setCancelled(true);
    }
}
