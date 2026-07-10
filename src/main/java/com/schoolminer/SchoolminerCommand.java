package com.schoolminer;

import org.bukkit.command.*;

public class SchoolminerCommand implements CommandExecutor {
    private final Schoolminer plugin;

    public SchoolminerCommand(Schoolminer plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0 || !args[0].equalsIgnoreCase("reload")) {
            sender.sendMessage(plugin.getConfigManager().getMessage("wrong-usage"));
            return true;
        }

        if (!sender.hasPermission("schoolminer.admin")) {
            sender.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return true;
        }

        plugin.getConfigManager().reload();
        sender.sendMessage(plugin.getConfigManager().getMessage("reloaded"));
        return true;
    }
}
