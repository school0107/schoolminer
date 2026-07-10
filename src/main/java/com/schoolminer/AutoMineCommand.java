package com.schoolminer;

import org.bukkit.command.*;
import org.bukkit.entity.Player;

public class AutoMineCommand implements CommandExecutor {
    private final Schoolminer plugin;

    public AutoMineCommand(Schoolminer plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c❌ Chỉ player mới dùng được!");
            return true;
        }

        if (!player.hasPermission("schoolminer.automine")) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return true;
        }

        AutoMineManager manager = plugin.getAutoMineManager();
        if (manager.isMining(player)) {
            manager.stopMining(player);
        } else {
            manager.startMining(player);
        }
        return true;
    }
}
