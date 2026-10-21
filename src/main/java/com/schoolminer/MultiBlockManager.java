package com.schoolminer;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.Collection;
import java.util.List;

public class MultiBlockManager implements Listener {
    private final Schoolminer plugin;

    public MultiBlockManager(Schoolminer plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        ItemStack tool = player.getInventory().getItemInMainHand();

        if (tool.getType().isAir()) return;

        // Kiểm tra Blacklist
        List<String> blacklist = plugin.getConfig().getStringList("blacklist");
        if (blacklist.contains(block.getType().name())) return;

        // Lấy level
        int multiBlockLevel = plugin.getPlayerDataManager().getMultiBlockLevel(tool);
        if (multiBlockLevel <= 1) return;

        // Lấy drops
        Collection<ItemStack> drops = block.getDrops(tool);
        if (drops.isEmpty()) return;

        // Chặn drop mặc định
        event.setDropItems(false);
        
        // Dùng Scheduler để đợi block bị phá xong hoàn toàn (tránh lỗi rollback trên Purpur 1.21)
        Bukkit.getScheduler().runTask(plugin, () -> {
            PlayerInventory inventory = player.getInventory();
            for (ItemStack drop : drops) {
                if (drop != null && !drop.getType().isAir()) {
                    int amount = drop.getAmount() * multiBlockLevel;
                    ItemStack finalDrop = drop.clone();
                    finalDrop.setAmount(amount);

                    // Thêm vào túi, nếu đầy thì rơi ra đất
                    if (inventory.firstEmpty() != -1) {
                        inventory.addItem(finalDrop);
                    } else {
                        block.getWorld().dropItemNaturally(block.getLocation(), finalDrop);
                    }
                }
            }
            // Thông báo
            if (Math.random() < 0.1) {
                player.sendMessage("§6✦ MultiBlock x" + multiBlockLevel + " §ađã kích hoạt!");
            }
        });
    }
}