package com.schoolminer;

import org.bukkit.command.*;
import org.bukkit.entity.Player;

public class AutoKillCommand implements CommandExecutor {
    private final Schoolminer plugin;

    public AutoKillCommand(Schoolminer plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c❌ Chỉ player mới dùng được!");
            return true;
        }

        if (!player.hasPermission("schoolminer.autokill")) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return true;
        }

        AutoKillManager manager = plugin.getAutoKillManager();
        if (manager.isKilling(player)) {
            manager.stopKilling(player);
        } else {
            manager.startKilling(player);
        }
        return true;
    }
}
