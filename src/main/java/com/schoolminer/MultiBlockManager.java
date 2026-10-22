package com.schoolminer;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.metadata.FixedMetadataValue;

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

        // Kiểm tra Blacklist
        List<String> blacklist = plugin.getConfig().getStringList("blacklist");
        if (blacklist.contains(block.getType().name())) return;

        // Lấy level MultiBlock
        int multiBlockLevel = plugin.getPlayerDataManager().getMultiBlockLevel(tool);
        if (multiBlockLevel <= 1) return;

        // Hủy drop mặc định để tự xử lý
        event.setDropItems(false);

        // Lấy drops chuẩn xác
        for (ItemStack drop : block.getDrops(tool)) {
            if (drop != null && !drop.getType().isAir()) {
                int amount = drop.getAmount() * multiBlockLevel;
                ItemStack finalDrop = drop.clone();
                finalDrop.setAmount(amount);

                // Đánh dấu item này để tự động nhặt
                Item item = block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 0.5, 0.5), finalDrop);
                item.setMetadata("MultiBlockDrop", new FixedMetadataValue(plugin, true));
                item.setPickupDelay(0); // Nhặt ngay lập tức
            }
        }
        
        if (Math.random() < 0.1) {
            player.sendMessage("§6✦ MultiBlock x" + multiBlockLevel + " §ađã kích hoạt!");
        }
    }

    // Tự động hút item vào túi (kể cả khi ở xa)
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        
        Item item = event.getItem();
        if (item.hasMetadata("MultiBlockDrop")) {
            PlayerInventory inventory = player.getInventory();
            ItemStack itemStack = item.getItemStack();
            
            // Nếu túi đầy thì không nhặt
            if (inventory.firstEmpty() == -1 && !inventory.containsAtLeast(itemStack, itemStack.getAmount())) {
                event.setCancelled(true);
                return;
            }
            
            // Thêm vào túi trực tiếp và xóa item dưới đất
            inventory.addItem(itemStack);
            item.remove();
            event.setCancelled(true);
        }
    }
}