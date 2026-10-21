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
import java.util.List;

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

        // 1. Kiểm tra Blacklist
        List<String> blacklist = plugin.getConfig().getStringList("blacklist");
        if (blacklist.contains(block.getType().name())) return;

        // 2. Lấy level MultiBlock
        int multiBlockLevel = plugin.getPlayerDataManager().getMultiBlockLevel(tool);
        if (multiBlockLevel <= 1) return;

        // 3. Lấy drops
        Collection<ItemStack> drops = block.getDrops(tool);
        if (drops.isEmpty()) return;

        // 4. Hủy drop mặc định
        event.setDropItems(false);
        event.setExpToDrop(0);

        PlayerInventory inventory = player.getInventory();

        for (ItemStack drop : drops) {
            if (drop != null && !drop.getType().isAir()) {
                int amount = drop.getAmount() * multiBlockLevel;
                ItemStack finalDrop = drop.clone();
                finalDrop.setAmount(amount);

                // 5. Đưa thẳng vào túi đồ
                if (inventory.firstEmpty() != -1) {
                    inventory.addItem(finalDrop);
                } else {
                    // Nếu túi đầy thì mới rơi ra đất
                    block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 0.5, 0.5), finalDrop);
                }
            }
        }

        // Thông báo nhỏ (5% tỉ lệ)
        if (Math.random() < 0.05) {
            player.sendMessage("§6✦ MultiBlock x" + multiBlockLevel + " §ađã kích hoạt!");
        }
    }
}