package com.schoolminer;

import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

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

        ItemMeta meta = tool.getItemMeta();
        if (meta == null) return true;

        NamespacedKey key = plugin.getMultiBlockManager().multiBlockKey;

        // Xem MultiBlock
        if (args[0].equalsIgnoreCase("check") || args[0].equalsIgnoreCase("mutiblock")) {
            Integer level = meta.getPersistentDataContainer().get(key, PersistentDataType.INTEGER);
            if (level == null) level = 1;
            player.sendMessage(colorize(plugin.getConfigManager().getMessage("check-success").replace("%level%", String.valueOf(level))));
            return true;
        }

        // Cần quyền admin
        if (!player.hasPermission("schoolminer.admin") && !player.isOp()) {
            player.sendMessage(colorize(plugin.getConfigManager().getMessage("no-permission")));
            return true;
        }

        if (args[0].equalsIgnoreCase("add") && args.length >= 2) {
            try {
                int level = Integer.parseInt(args[1]);
                
                // Ghi NBT trực tiếp vào cúp
                meta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, level);
                
                // Thêm Lore RGB
                List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
                lore.removeIf(line -> ChatColor.stripColor(line).contains("MultiBlock") || ChatColor.stripColor(line).contains("Đào block"));
                while (!lore.isEmpty() && lore.get(lore.size() - 1).trim().isEmpty()) lore.remove(lore.size() - 1);
                
                if (level > 1) {
                    if (!lore.isEmpty()) lore.add("");
                    String c1 = net.md_5.bungee.api.ChatColor.of("#FF5555").toString();
                    String c2 = net.md_5.bungee.api.ChatColor.of("#FFAA00").toString();
                    String c3 = net.md_5.bungee.api.ChatColor.of("#FFFF55").toString();
                    lore.add(c1 + "✦ " + c2 + "MultiBlock: " + c3 + level + "x");
                    lore.add(net.md_5.bungee.api.ChatColor.of("#AAAAAA") + "Đào block nhận x" + level + " vật phẩm");
                }
                meta.setLore(lore);
                
                tool.setItemMeta(meta);
                player.updateInventory(); // Cập nhật ngay lập tức

                player.sendMessage(colorize(plugin.getConfigManager().getMessage("set-success").replace("%level%", String.valueOf(level))));
            } catch (NumberFormatException e) {
                player.sendMessage(colorize(plugin.getConfigManager().getMessage("invalid-number")));
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("remove")) {
            meta.getPersistentDataContainer().remove(key);
            
            List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
            lore.removeIf(line -> ChatColor.stripColor(line).contains("MultiBlock") || ChatColor.stripColor(line).contains("Đào block"));
            meta.setLore(lore);
            
            tool.setItemMeta(meta);
            player.updateInventory();
            
            player.sendMessage(colorize(plugin.getConfigManager().getMessage("remove-success")));
            return true;
        }

        sendHelp(sender);
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6§l===== §e§lSchoolminer MultiBlock §6§l=====");
        sender.sendMessage("§e/smn check §f- Xem MultiBlock của cúp");
        sender.sendMessage("§e/smn add <số> §f- Ép MultiBlock vào cúp (Admin)");
        sender.sendMessage("§e/smn remove §f- Xóa MultiBlock (Admin)");
        sender.sendMessage("§e/schoolminer reload §f- Reload config (Admin)");
    }

    private String colorize(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}