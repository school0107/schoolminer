package com.schoolminer;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AutoMineCommand implements CommandExecutor {
    private final Schoolminer plugin;  // Lưu plugin để dùng

    public AutoMineCommand(Schoolminer plugin) {  // Constructor
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        // 1. Kiểm tra người gửi có phải player không
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c❌ Chỉ player mới dùng được!");
            return true;
        }

        // 2. Lấy AutoMineManager
        AutoMineManager manager = plugin.getAutoMineManager();
        
        // 3. Xử lý argument "on" hoặc "off"
        if (args.length > 0) {
            if (args[0].equalsIgnoreCase("on")) {
                if (manager.isMining(player)) {
                    player.sendMessage("§e⚠️ Auto Mine đã được bật rồi!");
                } else {
                    manager.startMining(player);  // Bật
                }
                return true;
            } else if (args[0].equalsIgnoreCase("off")) {
                if (!manager.isMining(player)) {
                    player.sendMessage("§e⚠️ Auto Mine đã được tắt rồi!");
                } else {
                    manager.stopMining(player);  // Tắt
                }
                return true;
            }
        }

        // 4. Toggle (bật/tắt)
        if (manager.isMining(player)) {
            manager.stopMining(player);  // Đang bật -> tắt
        } else {
            manager.startMining(player);  // Đang tắt -> bật
        }
        return true;
    }
}