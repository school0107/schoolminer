package com.schoolminer;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AutoKillCommand implements CommandExecutor {
    private final Schoolminer plugin;
    private AutoKillUpgradeMenu upgradeMenu;

    public AutoKillCommand(Schoolminer plugin) {
        this.plugin = plugin;
        this.upgradeMenu = new AutoKillUpgradeMenu(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c❌ Chỉ player mới dùng được!");
            return true;
        }

        AutoKillManager manager = plugin.getAutoKillManager();

        if (args.length > 0 && args[0].equalsIgnoreCase("upgrade")) {
            upgradeMenu.openMenu(player);
            return true;
        }

        if (manager.isKilling(player)) {
            manager.stopKilling(player);
        } else {
            manager.startKilling(player);
        }
        return true;
    }
}