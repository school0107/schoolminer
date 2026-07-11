package com.schoolminer;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.util.List;

public class AutoCraftCommand implements CommandExecutor {
    private final Schoolminer plugin;
    private AutoCraftMenu menu;

    public AutoCraftCommand(Schoolminer plugin) {
        this.plugin = plugin;
        this.menu = new AutoCraftMenu(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c❌ Chỉ player mới dùng được!");
            return true;
        }

        AutoCraftManager craftManager = plugin.getAutoCraftManager();

        if (args.length == 0) {
            List<String> available = craftManager.getAvailableCrafts(player);
            if (available.isEmpty()) {
                player.sendMessage("§c❌ Bạn không có quyền sử dụng autocraft nào!");
                player.sendMessage("§7Liên hệ admin để được cấp quyền.");
                return true;
            }
            
            menu.openMenu(player);
            return true;
        }

        String craftType = args[0].toLowerCase();
        
        // Tắt craft
        if (args.length >= 2 && args[1].equalsIgnoreCase("off")) {
            if (craftManager.isCrafting(player, craftType)) {
                craftManager.stopCraft(player, craftType);
            } else {
                player.sendMessage("§e⚠️ Bạn không đang craft " + craftType + "!");
            }
            return true;
        }

        // Bật craft
        craftManager.startCraft(player, craftType);
        return true;
    }
}