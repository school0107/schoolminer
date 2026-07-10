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

        // Kiểm tra permission
        if (!player.hasPermission("schoolminer.automine")) {
            player.sendMessage("§c❌ Bạn không có quyền sử dụng lệnh này!");
            player.sendMessage("§7Yêu cầu permission: §eschoolminer.automine");
            return true;
        }

        AutoMineManager manager = plugin.getAutoMineManager();
        
        // Kiểm tra nếu có argument "on" hoặc "off"
        if (args.length > 0) {
            if (args[0].equalsIgnoreCase("on")) {
                if (manager.isMining(player)) {
                    player.sendMessage("§e⚠️ Auto Mine đã được bật rồi!");
                } else {
                    manager.startMining(player);
                }
                return true;
            } else if (args[0].equalsIgnoreCase("off")) {
                if (!manager.isMining(player)) {
                    player.sendMessage("§e⚠️ Auto Mine đã được tắt rồi!");
                } else {
                    manager.stopMining(player);
                }
                return true;
            }
        }

        // Toggle
        if (manager.isMining(player)) {
            manager.stopMining(player);
        } else {
            manager.startMining(player);
        }
        return true;
    }
}