package com.schoolminer;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class SchoolminerCommand implements CommandExecutor {
    private final Schoolminer plugin;

    public SchoolminerCommand(Schoolminer plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(sender);
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("schoolminer.admin")) {
                sender.sendMessage(colorize(plugin.getConfigManager().getMessage("no-permission")));
                return true;
            }
            plugin.getConfigManager().reload();
            sender.sendMessage(colorize(plugin.getConfigManager().getMessage("reloaded")));
            return true;
        }

        // Lệnh multi block cho cúp: /smn add mutiblock <level>
        if (args.length >= 3 && args[0].equalsIgnoreCase("add") && args[1].equalsIgnoreCase("mutiblock")) {
            if (!sender.hasPermission("schoolminer.admin")) {
                sender.sendMessage("§c❌ Bạn không có quyền sử dụng lệnh này!");
                return true;
            }

            if (!(sender instanceof Player player)) {
                sender.sendMessage("§c❌ Chỉ player mới dùng được!");
                return true;
            }

            // Kiểm tra cầm cúp
            ItemStack tool = player.getInventory().getItemInMainHand();
            if (tool.getType().isAir()) {
                sender.sendMessage("§c❌ Hãy cầm cúp trên tay!");
                return true;
            }
            
            String toolName = tool.getType().name();
            if (!toolName.contains("PICKAXE") && !toolName.contains("AXE") && 
                !toolName.contains("SHOVEL") && !toolName.contains("HOE")) {
                sender.sendMessage("§c❌ Vui lòng cầm cúp, rìu, thuổng hoặc cuốc!");
                return true;
            }

            try {
                int level = Integer.parseInt(args[2]);
                if (level < 1) {
                    sender.sendMessage("§c⚠️ Cấp số nhân phải lớn hơn 0!");
                    return true;
                }
                if (level > 100) {
                    sender.sendMessage("§c⚠️ Cấp số nhân tối đa là 100!");
                    return true;
                }
                
                // Set MultiBlock cho cúp (truyền ItemStack)
                plugin.getConfigManager().setMultiBlockLevel(tool, level);
                
                String toolDisplay = tool.getType().name().replace("_", " ").toLowerCase();
                sender.sendMessage("§a✅ Đã set MultiBlock level §e" + level + " §acho §6" + toolDisplay);
                sender.sendMessage("§7Khi đào block sẽ nhân §ex" + level + " §7vật phẩm!");
                return true;
            } catch (NumberFormatException e) {
                sender.sendMessage("§c⚠️ Vui lòng nhập số hợp lệ!");
                return true;
            }
        }

        // Xem multi block của cúp hiện tại
        if (args.length >= 1 && args[0].equalsIgnoreCase("mutiblock")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("§c❌ Chỉ player mới dùng được!");
                return true;
            }
            
            ItemStack tool = player.getInventory().getItemInMainHand();
            if (tool.getType().isAir()) {
                sender.sendMessage("§c❌ Hãy cầm cúp trên tay!");
                return true;
            }
            
            int level = plugin.getConfigManager().getMultiBlockLevel(tool);
            String toolDisplay = tool.getType().name().replace("_", " ").toLowerCase();
            sender.sendMessage("§6✦ MultiBlock của §e" + toolDisplay + "§6: §e" + level + "x");
            sender.sendMessage("§7Sử dụng §e/smn add mutiblock <số> §7để thay đổi");
            return true;
        }

        sendHelp(sender);
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6===== §eSchoolminer Help §6=====");
        sender.sendMessage("§7/automine §f- Bật/tắt Auto Mine");
        sender.sendMessage("§7/autokill §f- Bật/tắt Auto Kill");
        sender.sendMessage("§7/autokill upgrade §f- Mở menu nâng cấp AutoKill");
        sender.sendMessage("§7/autocraft §f- Mở menu AutoCraft");
        sender.sendMessage("§7/smn mutiblock §f- Xem MultiBlock của cúp hiện tại");
        sender.sendMessage("§7/smn add mutiblock <số> §f- Set MultiBlock cho cúp (Admin)");
        sender.sendMessage("§7/schoolminer reload §f- Reload config");
        sender.sendMessage("§7/schoolminer help §f- Hiển thị trợ giúp");
    }

    private String colorize(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}