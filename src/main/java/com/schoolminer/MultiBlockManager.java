package com.schoolminer;

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

public class MultiBlockManager implements Listener {
    private final Schoolminer plugin;

    public MultiBlockManager(Schoolminer plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        ItemStack tool = player.getInventory().getItemInMainHand();

        if (tool.getType().isAir()) return;

        // Lấy level MultiBlock từ PlayerDataManager
        int multiBlockLevel = plugin.getPlayerDataManager().getMultiBlockLevel(tool);
        if (multiBlockLevel <= 1) return;

        // Lấy drops mặc định của block
        Collection<ItemStack> drops = block.getDrops(tool);
        if (drops.isEmpty()) return;

        // Hủy drop mặc định để tự xử lý
        event.setDropItems(false);
        event.setExpToDrop(0);

        PlayerInventory inventory = player.getInventory();

        for (ItemStack drop : drops) {
            if (drop != null && !drop.getType().isAir()) {
                int amount = drop.getAmount() * multiBlockLevel;
                ItemStack finalDrop = drop.clone();
                finalDrop.setAmount(amount);

                // Thêm vào túi hoặc drop ra đất nếu túi đầy
                if (inventory.firstEmpty() != -1) {
                    inventory.addItem(finalDrop);
                } else {
                    block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 0.5, 0.5), finalDrop);
                }
            }
        }

        // Thông báo nhỏ (5% tỉ lệ để tránh spam)
        if (Math.random() < 0.05) {
            player.sendMessage("§6✦ MultiBlock x" + multiBlockLevel + " §ađã kích hoạt!");
        }
    }
}