package com.schoolminer;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AutoKillCommand implements CommandExecutor {
    private final Schoolminer plugin;
    private final VaultEconomy economy;
    private AutoKillUpgradeMenu upgradeMenu;

    public AutoKillCommand(Schoolminer plugin, VaultEconomy economy) {
        this.plugin = plugin;
        this.economy = economy;
        this.upgradeMenu = new AutoKillUpgradeMenu(plugin, economy);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c❌ Chỉ player mới dùng được!");
            return true;
        }

        AutoKillManager manager = plugin.getAutoKillManager();

        // Mở menu nâng cấp
        if (args.length > 0 && args[0].equalsIgnoreCase("upgrade")) {
            if (!economy.isEnabled()) {
                player.sendMessage("§c❌ Hệ thống kinh tế chưa được kích hoạt! Cần Vault plugin.");
                return true;
            }
            upgradeMenu.openMenu(player);
            return true;
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