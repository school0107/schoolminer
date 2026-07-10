package com.schoolminer;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.util.List;

public class AutoCraftCommand implements CommandExecutor {
    private final Schoolminer plugin;
    private final AutoCraftManager craftManager;

    public AutoCraftCommand(Schoolminer plugin) {
        this.plugin = plugin;
        this.craftManager = plugin.getAutoCraftManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c❌ Chỉ player mới dùng được!");
            return true;
        }

        if (!player.hasPermission("schoolminer.autocraft")) {
            player.sendMessage("§c❌ Bạn không có quyền sử dụng lệnh này!");
            return true;
        }

        if (args.length == 0) {
            // Hiển thị danh sách craft có sẵn
            List<String> available = craftManager.getAvailableCrafts(player);
            if (available.isEmpty()) {
                player.sendMessage("§c❌ Bạn không có quyền sử dụng autocraft nào!");
                return true;
            }
            
            player.sendMessage("§6===== §eAutoCraft §6=====");
            for (String craft : available) {
                AutoCraftConfig config = plugin.getConfigManager().getCraftConfig(craft);
                if (config != null) {
                    player.sendMessage("§e/autocraft " + craft + " §7- §f" + config.getDisplayName());
                }
            }
            return true;
        }

        String craftType = args[0].toLowerCase();
        
        if (args.length >= 2 && args[1].equalsIgnoreCase("off")) {
            craftManager.stopCraft(player);
            return true;
        }

        // Kiểm tra xem craft có tồn tại không
        if (plugin.getConfigManager().getCraftConfig(craftType) == null) {
            player.sendMessage("§c❌ Không tìm thấy autocraft: " + craftType);
            return true;
        }

        craftManager.startCraft(player, craftType);
        return true;
    }
}
