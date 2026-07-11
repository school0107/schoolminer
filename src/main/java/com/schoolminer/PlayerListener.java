package com.schoolminer;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryView;

public class PlayerListener implements Listener {
    private final Schoolminer plugin;
    private AutoCraftMenu menu;

    public PlayerListener(Schoolminer plugin) {
        this.plugin = plugin;
        this.menu = new AutoCraftMenu(plugin);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        plugin.getAutoMineManager().stopMining(player);
        plugin.getAutoKillManager().stopKilling(player);
        plugin.getAutoCraftManager().stopCraft(player);
        menu.removeCrafting(player);
    }

    @EventHandler
    public void onPlayerKick(PlayerKickEvent event) {
        Player player = event.getPlayer();
        plugin.getAutoMineManager().stopMining(player);
        plugin.getAutoKillManager().stopKilling(player);
        plugin.getAutoCraftManager().stopCraft(player);
        menu.removeCrafting(player);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        InventoryView view = event.getView();
        if (view == null) return;
        
        String title = view.getTitle();
        if (title == null) return;
        
        if (title.equals("§6§l⚒️ AutoCraft Menu")) {
            event.setCancelled(true);
            
            int slot = event.getRawSlot();
            if (slot < 0 || slot >= event.getInventory().getSize()) return;
            
            menu.handleMenuClick(player, event.getInventory(), slot);
        }
    }
}