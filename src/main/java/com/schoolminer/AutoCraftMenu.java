package com.schoolminer;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
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

        // Lấy danh sách craft có permission
        List<String> availableCrafts = craftManager.getAvailableCrafts(player);
        
        // Thêm các craft vào menu
        int slot = 0;
        for (String craftType : availableCrafts) {
            AutoCraftConfig craftConfig = configManager.getCraftConfig(craftType);
            if (craftConfig == null) continue;
            
            ItemStack item = createCraftItem(craftType, craftConfig, player);
            menu.setItem(slot, item);
            slot++;
            
            // Mỗi hàng 9 slot, xuống hàng khi đủ
            if (slot % 9 == 0) slot++;
        }

        // Nút tắt tất cả
        ItemStack stopAll = new ItemStack(Material.BARRIER);
        ItemMeta stopMeta = stopAll.getItemMeta();
        stopMeta.setDisplayName("§c§l⛔ TẮT TẤT CẢ CRAFT");
        stopMeta.setLore(Arrays.asList(
            "§7Tắt tất cả các craft đang chạy",
            "§7Click để tắt tất cả"
        ));
        stopAll.setItemMeta(stopMeta);
        menu.setItem(49, stopAll);

        // Nút Reload Menu
        ItemStack reload = new ItemStack(Material.LIME_DYE);
        ItemMeta reloadMeta = reload.getItemMeta();
        reloadMeta.setDisplayName("§a§l🔄 LÀM MỚI");
        reloadMeta.setLore(Arrays.asList(
            "§7Làm mới menu craft"
        ));
        reload.setItemMeta(reloadMeta);
        menu.setItem(50, reload);

        // Thông tin
        ItemStack info = new ItemStack(Material.BOOK);
        ItemMeta infoMeta = info.getItemMeta();
        infoMeta.setDisplayName("§e§l📖 HƯỚNG DẪN");
        infoMeta.setLore(Arrays.asList(
            "§7Click vào craft để bật/tắt",
            "§a✅ Đang bật",
            "§c❌ Đang tắt",
            "",
            "§7Craft sẽ tự động chạy cho đến khi",
            "§7hết nguyên liệu hoặc túi đầy"
        ));
        info.setItemMeta(infoMeta);
        menu.setItem(51, info);

        player.openInventory(menu);
    }

    private ItemStack createCraftItem(String craftType, AutoCraftConfig craftConfig, Player player) {
        boolean isActive = craftManager.isCrafting(player);
        boolean isThisActive = false;
        
        // Kiểm tra xem craft này có đang chạy không
        if (isActive) {
            // Kiểm tra craft hiện tại của player
            // (Chỉ có thể 1 craft chạy 1 lúc)
            isThisActive = true;
        }

        // Xác định material dựa trên craft type
        Material material = getMaterialForCraft(craftType);
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        // Tên hiển thị
        String displayName = isThisActive ? 
            "§a✅ " + craftConfig.getDisplayName() : 
            "§c❌ " + craftConfig.getDisplayName();
        meta.setDisplayName(displayName);

        // Lore
        List<String> lore = new ArrayList<>();
        lore.add("§7" + craftType);
        lore.add("");
        
        // Hiển thị nguyên liệu
        lore.add("§6§lNguyên liệu:");
        for (ItemStack materialItem : craftConfig.getMaterials()) {
            if (materialItem != null && !materialItem.getType().isAir()) {
                String matName = materialItem.getType().name().replace("_", " ").toLowerCase();
                lore.add("§7- §f" + matName + " §7x§f" + materialItem.getAmount());
            }
        }
        
        lore.add("");
        lore.add("§6§lSản phẩm:");
        ItemStack result = craftConfig.getResult();
        if (result != null) {
            String resultName = result.getType().name().replace("_", " ").toLowerCase();
            lore.add("§7- §f" + resultName + " §7x§f" + result.getAmount());
        }
        
        lore.add("");
        lore.add("§7Trạng thái: " + (isThisActive ? "§aĐang chạy" : "§cĐã tắt"));
        lore.add("");
        lore.add("§eClick để " + (isThisActive ? "tắt" : "bật") + " craft này!");
        
        meta.setLore(lore);
        
        // Thêm glow nếu craft đang chạy
        if (isThisActive) {
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
            case "kimcuong": return Material.DIAMOND;
            case "sat": return Material.IRON_INGOT;
            case "vang": return Material.GOLD_INGOT;
            case "da_cuoi": return Material.COBBLESTONE;
            case "than_cui": return Material.CHARCOAL;
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
            craftManager.stopCraft(player);
            player.sendMessage("§c⛔ Đã tắt tất cả AutoCraft!");
            openMenu(player);
            return;
        }

        // Nút làm mới
        if (slot == 50) {
            openMenu(player);
            return;
        }

        // Xử lý click vào craft
        ItemStack clicked = inventory.getItem(slot);
        if (clicked == null || !clicked.hasItemMeta()) return;

        String displayName = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());
        
        // Tìm craft type từ display name
        for (String craftType : configManager.getCraftTypes()) {
            AutoCraftConfig craftConfig = configManager.getCraftConfig(craftType);
            if (craftConfig == null) continue;
            
            String cleanDisplay = ChatColor.stripColor(craftConfig.getDisplayName());
            if (displayName.contains(cleanDisplay) || displayName.contains(craftType)) {
                // Bật/tắt craft
                if (craftManager.isCrafting(player)) {
                    // Nếu đang craft, tắt craft hiện tại và bật craft mới
                    craftManager.stopCraft(player);
                    craftManager.startCraft(player, craftType);
                } else {
                    craftManager.startCraft(player, craftType);
                }
                openMenu(player);
                return;
            }
        }
    }
}