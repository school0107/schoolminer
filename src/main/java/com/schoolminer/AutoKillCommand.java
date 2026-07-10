package com.schoolminer;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
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

        // Kiểm tra permission
        if (!player.hasPermission("schoolminer.autokill")) {
            player.sendMessage("§c❌ Bạn không có quyền sử dụng lệnh này!");
            player.sendMessage("§7Yêu cầu permission: §eschoolminer.autokill");
            return true;
        }

        AutoKillManager manager = plugin.getAutoKillManager();
        
        // Kiểm tra nếu có argument "on" hoặc "off"
        if (args.length > 0) {
            if (args[0].equalsIgnoreCase("on")) {
                if (manager.isKilling(player)) {
                    player.sendMessage("§e⚠️ Auto Kill đã được bật rồi!");
                } else {
                    manager.startKilling(player);
                }
                return true;
            } else if (args[0].equalsIgnoreCase("off")) {
                if (!manager.isKilling(player)) {
                    player.sendMessage("§e⚠️ Auto Kill đã được tắt rồi!");
                } else {
                    manager.stopKilling(player);
                }
                return true;
            }
        }

        // Toggle
        if (manager.isKilling(player)) {
            manager.stopKilling(player);
        } else {
            manager.startKilling(player);
        }
        return true;
    }
}