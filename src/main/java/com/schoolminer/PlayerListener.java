package com.schoolminer;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerKickEvent;

public class PlayerListener implements Listener {
    private final Schoolminer plugin;

    public PlayerListener(Schoolminer plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        plugin.getAutoMineManager().stopMining(player);
        plugin.getAutoKillManager().stopKilling(player);
        plugin.getAutoCraftManager().stopCraft(player);
    }

    @EventHandler
    public void onPlayerKick(PlayerKickEvent event) {
        Player player = event.getPlayer();
        plugin.getAutoMineManager().stopMining(player);
        plugin.getAutoKillManager().stopKilling(player);
        plugin.getAutoCraftManager().stopCraft(player);
    }
}