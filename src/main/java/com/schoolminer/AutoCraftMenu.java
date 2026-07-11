package com.schoolminer;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.*;

public class AutoCraftMenu {
    private final Schoolminer plugin;
    private final AutoCraftManager craftManager;
    private final ConfigManager configManager;

    public AutoCraftMenu(Schoolminer plugin) {
        this.plugin = plugin;
        this.craftManager = plugin.getAutoCraftManager();
        this.configManager = plugin.getConfigManager();
    }

    public void openMenu(Player player) {
        Inventory menu = Bukkit.createInventory(null, 54, "§6§l⚒️ AutoCraft Menu");

        // Tất cả craft theo thứ tự
        List<String> craftOrder = Arrays.asList(
            "go", "da", "da_cuoi", "than_cui", "sat", "vang", 
            "kimcuong", "ngoc_luc_bao", "da_nether", "manh_vo_co_dai",
            "da_bazan", "da_blackstone", "quang_nether_gold", 
            "quang_nether_quartz", "soul_sand", "soul_soil",
            "gach_nether", "gach_nether_do"
        );

        Set<String> activeCrafts = craftManager.getActiveCrafts(player);

        int slot = 0;
        for (String craftType : craftOrder) {
            AutoCraftConfig craftConfig = configManager.getCraftConfig(craftType);
            if (craftConfig == null) continue;
            
            if (!player.hasPermission("schoolminer.autocraft." + craftType)) continue;
            
            boolean isActive = activeCrafts.contains(craftType);
            ItemStack item = createCraftItem(craftType, craftConfig, isActive);
            menu.setItem(slot, item);
            slot++;
            
            if (slot % 9 == 0) slot++;
        }

        // Nút tắt tất cả
        ItemStack stopAll = new ItemStack(Material.BARRIER);
        ItemMeta stopMeta = stopAll.getItemMeta();
        stopMeta.setDisplayName(ChatColor.RED + "⛔ TẮT TẤT CẢ CRAFT");
        stopMeta.setLore(Arrays.asList(
            ChatColor.GRAY + "Tắt tất cả craft đang chạy",
            ChatColor.GRAY + "Click để tắt tất cả"
        ));
        stopAll.setItemMeta(stopMeta);
        menu.setItem(49, stopAll);

        // Nút làm mới
        ItemStack reload = new ItemStack(Material.LIME_DYE);
        ItemMeta reloadMeta = reload.getItemMeta();
        reloadMeta.setDisplayName(ChatColor.GREEN + "🔄 LÀM MỚI");
        reloadMeta.setLore(Arrays.asList(ChatColor.GRAY + "Làm mới menu craft"));
        reload.setItemMeta(reloadMeta);
        menu.setItem(50, reload);

        // Thông tin
        ItemStack info = new ItemStack(Material.BOOK);
        ItemMeta infoMeta = info.getItemMeta();
        infoMeta.setDisplayName(ChatColor.YELLOW + "📖 HƯỚNG DẪN");
        infoMeta.setLore(Arrays.asList(
            ChatColor.GRAY + "Click vào craft để bật/tắt",
            ChatColor.GREEN + "✅ Đang bật",
            ChatColor.RED + "❌ Đang tắt",
            "",
            ChatColor.GRAY + "Có thể bật §eNHIỀU §7craft cùng lúc",
            ChatColor.GRAY + "Đang chạy: §e" + activeCrafts.size() + " §7/" + craftOrder.size() + " craft"
        ));
        info.setItemMeta(infoMeta);
        menu.setItem(51, info);

        player.openInventory(menu);
    }

    private ItemStack createCraftItem(String craftType, AutoCraftConfig craftConfig, boolean isActive) {
        Material material = getMaterialForCraft(craftType);
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        String displayName = isActive ? 
            ChatColor.GREEN + "✅ " + craftConfig.getDisplayName() : 
            ChatColor.RED + "❌ " + craftConfig.getDisplayName();
        meta.setDisplayName(displayName);

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + craftType);
        lore.add("");
        
        lore.add(ChatColor.GOLD + "Nguyên liệu:");
        for (ItemStack materialItem : craftConfig.getMaterials()) {
            if (materialItem != null && !materialItem.getType().isAir()) {
                String matName = materialItem.getType().name().replace("_", " ").toLowerCase();
                lore.add(ChatColor.GRAY + "- " + ChatColor.WHITE + matName + ChatColor.GRAY + " x" + ChatColor.WHITE + materialItem.getAmount());
            }
        }
        
        lore.add("");
        lore.add(ChatColor.GOLD + "Sản phẩm:");
        ItemStack result = craftConfig.getResult();
        if (result != null) {
            String resultName = result.getType().name().replace("_", " ").toLowerCase();
            lore.add(ChatColor.GRAY + "- " + ChatColor.WHITE + resultName + ChatColor.GRAY + " x" + ChatColor.WHITE + result.getAmount());
        }
        
        lore.add("");
        lore.add(ChatColor.GRAY + "Trạng thái: " + (isActive ? ChatColor.GREEN + "Đang chạy" : ChatColor.RED + "Đã tắt"));
        lore.add("");
        lore.add(ChatColor.YELLOW + "Click để " + (isActive ? "tắt" : "bật") + " craft này!");
        
        meta.setLore(lore);
        
        if (isActive) {
            meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
        }
        
        item.setItemMeta(meta);
        return item;
    }

    private Material getMaterialForCraft(String craftType) {
        switch (craftType.toLowerCase()) {
            case "go": return Material.OAK_LOG;
            case "da": return Material.STONE;
            case "da_cuoi": return Material.COBBLESTONE;
            case "than_cui": return Material.CHARCOAL;
            case "sat": return Material.IRON_INGOT;
            case "vang": return Material.GOLD_INGOT;
            case "kimcuong": return Material.DIAMOND;
            case "ngoc_luc_bao": return Material.EMERALD;
            case "da_nether": return Material.NETHERRACK;
            case "manh_vo_co_dai": return Material.ANCIENT_DEBRIS;
            case "da_bazan": return Material.BASALT;
            case "da_blackstone": return Material.BLACKSTONE;
            case "quang_nether_gold": return Material.NETHER_GOLD_ORE;
            case "quang_nether_quartz": return Material.NETHER_QUARTZ_ORE;
            case "soul_sand": return Material.SOUL_SAND;
            case "soul_soil": return Material.SOUL_SOIL;
            case "gach_nether": return Material.NETHER_BRICK;
            case "gach_nether_do": return Material.RED_NETHER_BRICKS;
            default: return Material.CRAFTING_TABLE;
        }
    }

    public void handleMenuClick(Player player, Inventory inventory, int slot) {
        // Nút tắt tất cả
        if (slot == 49) {
            craftManager.stopAllCraft(player);
            openMenu(player);
            return;
        }

        // Nút làm mới
        if (slot == 50) {
            openMenu(player);
            return;
        }

        ItemStack clicked = inventory.getItem(slot);
        if (clicked == null || !clicked.hasItemMeta()) return;

        String displayName = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());
        
        String targetCraft = null;
        for (String craftType : configManager.getCraftTypes()) {
            AutoCraftConfig craftConfig = configManager.getCraftConfig(craftType);
            if (craftConfig == null) continue;
            
            String rawDisplayName = ChatColor.stripColor(craftConfig.getDisplayName());
            if (displayName.contains(rawDisplayName) || displayName.contains(craftType)) {
                targetCraft = craftType;
                break;
            }
        }
        
        if (targetCraft == null) return;
        
        if (!player.hasPermission("schoolminer.autocraft." + targetCraft)) {
            player.sendMessage(ChatColor.RED + "❌ Bạn không có quyền sử dụng craft này!");
            return;
        }
        
        // Nếu craft đang chạy -> TẮT
        if (craftManager.isCrafting(player, targetCraft)) {
            craftManager.stopCraft(player, targetCraft);
            openMenu(player);
            return;
        }
        
        // Bật craft mới (KHÔNG GIỚI HẠN SỐ LƯỢNG)
        craftManager.startCraft(player, targetCraft);
        openMenu(player);
    }
}