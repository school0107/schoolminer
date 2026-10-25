package com.schoolminer;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MultiBlockManager implements Listener {
    private final Schoolminer plugin;
    public final NamespacedKey multiBlockKey;
    
    // Lưu vị trí các block do người chơi đặt (Chống Dupe)
    private final Set<Location> playerPlacedBlocks = new HashSet<>();

    public MultiBlockManager(Schoolminer plugin) {
        this.plugin = plugin;
        this.multiBlockKey = new NamespacedKey(plugin, "multiblock_level");
    }

    // 1. Ghi nhớ block do người chơi đặt
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        playerPlacedBlocks.add(event.getBlock().getLocation());
    }

    // 2. Xóa khỏi bộ nhớ nếu block bị phá
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreakCleanup(BlockBreakEvent event) {
        playerPlacedBlocks.remove(event.getBlock().getLocation());
    }

    // 3. Xử lý MultiBlock khi đào
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        ItemStack tool = player.getInventory().getItemInMainHand();

        if (tool.getType().isAir() || !tool.hasItemMeta()) return;

        // Chống Dupe: Bỏ qua nếu block do người chơi đặt
        if (playerPlacedBlocks.contains(block.getLocation())) return;

        // Đọc level từ cúp
        Integer level = tool.getItemMeta().getPersistentDataContainer().get(multiBlockKey, PersistentDataType.INTEGER);
        if (level == null || level <= 1) return;

        // Chỉ cho phép nhân bản nếu block nằm trong danh sách allowed-ores
        List<String> allowedOres = plugin.getConfig().getStringList("allowed-ores");
        if (!allowedOres.contains(block.getType().name())) return;

        // Xử lý drop
        event.setDropItems(false);
        PlayerInventory inventory = player.getInventory();
        Location dropLoc = block.getLocation().add(0.5, 0.5, 0.5);

        for (ItemStack drop : block.getDrops(tool)) {
            if (drop != null && !drop.getType().isAir()) {
                int finalAmount = drop.getAmount() * level;
                ItemStack finalDrop = drop.clone();
                finalDrop.setAmount(finalAmount);

                // Hút thẳng vào túi
                if (inventory.firstEmpty() != -1) {
                    inventory.addItem(finalDrop);
                } else {
                    block.getWorld().dropItemNaturally(dropLoc, finalDrop);
                }
            }
        }
    }
}