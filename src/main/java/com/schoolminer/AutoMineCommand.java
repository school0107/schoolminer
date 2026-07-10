package com.schoolminer;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
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

        // KHÔNG KIỂM TRA PERMISSION - TẤT CẢ ĐỀU DÙNG ĐƯỢC
        AutoMineManager manager = plugin.getAutoMineManager();
        
        // Toggle
        if (manager.isMining(player)) {
            manager.stopMining(player);
        } else {
            manager.startMining(player);
        }
        return true;
    }
}