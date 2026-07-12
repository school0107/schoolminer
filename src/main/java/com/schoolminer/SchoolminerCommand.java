package com.schoolminer;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

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

        // Lệnh multi block: /smn add mutiblock <level>
        if (args.length >= 3 && args[0].equalsIgnoreCase("add") && args[1].equalsIgnoreCase("mutiblock")) {
            if (!sender.hasPermission("schoolminer.admin")) {
                sender.sendMessage("§c❌ Bạn không có quyền sử dụng lệnh này!");
                return true;
            }

            if (!(sender instanceof Player player)) {
                sender.sendMessage("§c❌ Chỉ player mới dùng được!");
                return true;
            }

            try {
                int level = Integer.parseInt(args[2]);
                if (level