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
            if (!sender.hasPermission("schoolminer.admin") && !sender.isOp()) {
                sender.sendMessage(colorize(plugin.getConfigManager().getMessage("no-permission")));
                return true;
            }
            plugin.getConfigManager().reload();
            sender.sendMessage(colorize(plugin.getConfigManager().getMessage("reloaded")));
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c❌ Chỉ player mới dùng được!");
            return true;
        }

        ItemStack tool = player.getInventory().getItemInMainHand();
        if (tool.getType().isAir()) {
            player.sendMessage(colorize(plugin.getConfigManager().getMessage("no-tool")));
            return true;
        }

        // Xem MultiBlock
        if (args[0].equalsIgnoreCase("check") || args[0].equalsIgnoreCase("mutiblock")) {
            int level = plugin.getPlayerDataManager().getMultiBlockLevel(tool);
            player.sendMessage(colorize(plugin.getConfigManager().getMessage("check-success").replace("%level%", String.valueOf(level))));
            return true;
        }

        // Set / Xóa MultiBlock (Cần quyền admin)
        if (!player.hasPermission("schoolminer.admin") && !player.isOp()) {
            player.sendMessage(colorize(plugin.getConfigManager().getMessage("no-permission")));
            return true;
        }

        if (args[0].equalsIgnoreCase("add") && args.length >= 2) {
            try {
                int level = Integer.parseInt(args[1]);
                plugin.getPlayerDataManager().setMultiBlockLevel(tool, level);
                player.sendMessage(colorize(plugin.getConfigManager().getMessage("set-success").replace("%level%", String.valueOf(level))));
            } catch (NumberFormatException e) {
                player.sendMessage(colorize(plugin.getConfigManager().getMessage("invalid-number")));
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("remove")) {
            plugin.getPlayerDataManager().removeMultiBlockLevel(tool);
            player.sendMessage(colorize(plugin.getConfigManager().getMessage("remove-success")));
            return true;
        }

        sendHelp(sender);
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6§l===== §e§lSchoolminer MultiBlock §6§l=====");
        sender.sendMessage("§e/smn check §f- Xem MultiBlock của cúp");
        sender.sendMessage("§e/smn add <số> §f- Set MultiBlock (Admin)");
        sender.sendMessage("§e/smn remove §f- Xóa MultiBlock (Admin)");
        sender.sendMessage("§e/schoolminer reload §f- Reload config (Admin)");
    }

    private String colorize(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}