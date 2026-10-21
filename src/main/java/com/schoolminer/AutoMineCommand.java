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
        // Mặc định hiển thị Help nếu không có args
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(sender);
            return true;
        }

        // Lệnh Reload
        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("schoolminer.admin") && !sender.isOp()) {
                sender.sendMessage("§c❌ Bạn không có quyền sử dụng lệnh này! (Cần schoolminer.admin)");
                return true;
            }
            plugin.getConfigManager().reload();
            sender.sendMessage("§a🔄 Đã reload config thành công!");
            return true;
        }

        // Các lệnh MultiBlock (chỉ dành cho Player)
        if (sender instanceof Player player) {
            if (args[0].equalsIgnoreCase("add") && args.length >= 3 && args[1].equalsIgnoreCase("mutiblock")) {
                if (!player.hasPermission("schoolminer.admin") && !player.isOp()) {
                    player.sendMessage("§c❌ Bạn không có quyền sử dụng lệnh này!");
                    return true;
                }

                ItemStack tool = player.getInventory().getItemInMainHand();
                if (tool.getType().isAir()) {
                    player.sendMessage("§c❌ Hãy cầm cúp trên tay!");
                    return true;
                }

                try {
                    int level = Integer.parseInt(args[2]);
                    plugin.getPlayerDataManager().setMultiBlockLevel(tool, level);
                    player.sendMessage("§a✅ Đã set MultiBlock level §e" + level + "x §acho cúp!");
                } catch (NumberFormatException e) {
                    player.sendMessage("§c⚠️ Vui lòng nhập số hợp lệ!");
                }
                return true;
            }

            if (args[0].equalsIgnoreCase("remove") && args.length >= 2 && args[1].equalsIgnoreCase("mutiblock")) {
                if (!player.hasPermission("schoolminer.admin") && !player.isOp()) {
                    player.sendMessage("§c❌ Bạn không có quyền sử dụng lệnh này!");
                    return true;
                }
                
                ItemStack tool = player.getInventory().getItemInMainHand();
                if (tool.getType().isAir()) {
                    player.sendMessage("§c❌ Hãy cầm cúp trên tay!");
                    return true;
                }
                
                plugin.getPlayerDataManager().removeMultiBlockLevel(tool);
                player.sendMessage("§a✅ Đã xóa MultiBlock khỏi cúp!");
                return true;
            }

            if (args[0].equalsIgnoreCase("mutiblock")) {
                ItemStack tool = player.getInventory().getItemInMainHand();
                if (tool.getType().isAir()) {
                    player.sendMessage("§c❌ Hãy cầm cúp trên tay!");
                    return true;
                }
                
                int level = plugin.getPlayerDataManager().getMultiBlockLevel(tool);
                player.sendMessage("§6✦ MultiBlock hiện tại: §e" + level + "x");
                return true;
            }
        } else {
            sender.sendMessage("§c❌ Lệnh này chỉ dành cho người chơi trong game!");
            return true;
        }

        sendHelp(sender);
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6§l===== §e§lSchoolminer Help §6§l=====");
        sender.sendMessage("§e/automine §f- Bật/tắt Auto Mine");
        sender.sendMessage("§e/autokill §f- Bật/tắt Auto Kill");
        sender.sendMessage("§e/autokill upgrade §f- Menu nâng cấp AutoKill");
        sender.sendMessage("§e/autocraft §f- Menu AutoCraft");
        sender.sendMessage("§e/smn mutiblock §f- Xem MultiBlock");
        sender.sendMessage("§e/smn add mutiblock <số> §f- Set MultiBlock (Admin)");
        sender.sendMessage("§e/smn remove mutiblock §f- Xóa MultiBlock (Admin)");
        sender.sendMessage("§e/schoolminer reload §f- Reload config (Admin)");
    }
}