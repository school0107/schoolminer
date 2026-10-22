package com.schoolminer;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public class MultiBlockManager implements Listener {
    private final Schoolminer plugin;
    public final NamespacedKey multiBlockKey;

    public MultiBlockManager(Schoolminer plugin) {
        this.plugin = plugin;
        this.multiBlockKey = new NamespacedKey(plugin, "multiblock_level");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        ItemStack tool = player.getInventory().getItemInMainHand();

        if (tool.getType().isAir() || !tool.hasItemMeta()) return;

        // Đọc level trực tiếp từ Cúp (NBT)
        Integer level = tool.getItemMeta().getPersistentDataContainer().get(multiBlockKey, PersistentDataType.INTEGER);
        if (level == null || level <= 1) return;

        // Check Blacklist
        List<String> blacklist = plugin.getConfig().getStringList("blacklist");
        if (blacklist.contains(block.getType().name())) return;

        // Hủy drop mặc định
        event.setDropItems(false);

        // Xử lý add thẳng vào túi
        PlayerInventory inventory = player.getInventory();
        Location dropLoc = block.getLocation().add(0.5, 0.5, 0.5);

        for (ItemStack drop : block.getDrops(tool)) {
            if (drop != null && !drop.getType().isAir()) {
                int finalAmount = drop.getAmount() * level;
                ItemStack finalDrop = drop.clone();
                finalDrop.setAmount(finalAmount);

                // Thêm vào túi, nếu đầy thì rơi ra đất
                if (inventory.firstEmpty() != -1) {
                    inventory.addItem(finalDrop);
                } else {
                    block.getWorld().dropItemNaturally(dropLoc, finalDrop);
                }
            }
        }
        
        if (Math.random() < 0.1) {
            player.sendMessage("§6✦ MultiBlock x" + level + " §ađã kích hoạt!");
        }
    }
}