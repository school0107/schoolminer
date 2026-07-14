package com.schoolminer;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.Location;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import java.util.*;
import java.util.regex.Pattern;
import java.util.concurrent.ConcurrentHashMap;

public class AutoKillManager implements Listener {
    private final Schoolminer plugin;
    private final Map<UUID, KillTask> tasks = new HashMap<>();
    private final ConfigManager config;
    private final Map<UUID, Location> lastLocations = new HashMap<>();
    private final Map<UUID, Integer> explosionLevels = new ConcurrentHashMap<>();
    private final Random random = new Random();
    private final Map<UUID, Boolean> wasSneaking = new HashMap<>();

    public AutoKillManager(Schoolminer plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
        loadExplosionLevels();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    private void saveExplosionLevels() {
        FileConfiguration config = plugin.getConfig();
        config.set("autokill.levels", null);
        for (Map.Entry<UUID, Integer> entry : explosionLevels.entrySet()) {
            config.set("autokill.levels." + entry.getKey().toString(), entry.getValue());
        }
        plugin.saveConfig();
    }

    private void loadExplosionLevels() {
        explosionLevels.clear();
        FileConfiguration config = plugin.getConfig();
        if (config.contains("autokill.levels")) {
            for (String key : config.getConfigurationSection("autokill.levels").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    int level = config.getInt("autokill.levels." + key, 0);
                    explosionLevels.put(uuid, level);
                } catch (IllegalArgumentException ignored) {}
            }
        }
    }

    // CHỈ CHẶN KHI NGƯỜI CHƠI TỰ DI CHUYỂN (KHÔNG PHẢI BỊ ĐẨY)
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        
        if (!tasks.containsKey(uuid)) return;
        
        Location from = event.getFrom();
        Location to = event.getTo();
        
        // Kiểm tra xem có phải do bị đẩy không (knockback)
        // Nếu người chơi đang bị đẩy, velocity sẽ khác 0
        boolean isKnockedBack = player.getVelocity().length() > 0.1;
        
        // Kiểm tra nếu người chơi đang bị đẩy bởi quái
        if (isKnockedBack) {
            // Nếu bị đẩy, cho phép di chuyển nhưng không tắt AutoKill
            return;
        }
        
        // Kiểm tra nếu người chơi tự ngồi xuống (shift)
        if (player.isSneaking()) {
            wasSneaking.put(uuid, true);
            return;
        }
        
        // Kiểm tra nếu người chơi tự di chuyển (không phải bị đẩy)
        if (from.getBlockX() != to.getBlockX() || from.getBlockZ() != to.getBlockZ()) {
            // Cho phép di chuyển nhưng tắt AutoKill
            player.sendMessage("§c⚠️ Bạn đã di chuyển! Đã tắt Auto Kill!");
            stopKilling(player);
            return;
        }
        
        // Kiểm tra block dưới chân
        Location below = player.getLocation().subtract(0, 1, 0);
        if (below.getBlock().getType() == Material.AIR) {
            // Nếu rơi xuống (không có block dưới chân) nhưng không phải tự di chuyển
            // Ví dụ: bị đẩy xuống hố
            if (!isKnockedBack) {
                player.sendMessage("§c⚠️ Không có block dưới chân! Đã tắt Auto Kill!");
                stopKilling(player);
            }
        }
    }

    // KHÔNG TẮT KHI BỊ DỊCH CHUYỂN (TELEPORT) - CHỈ TẮT KHI TỰ DỊCH CHUYỂN
    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        if (tasks.containsKey(player.getUniqueId())) {
            // Kiểm tra nếu là teleport do plugin (không phải do người chơi tự di chuyển)
            if (event.getCause() == PlayerTeleportEvent.TeleportCause.PLUGIN ||
                event.getCause() == PlayerTeleportEvent.TeleportCause.COMMAND) {
                // Nếu do plugin hoặc lệnh, tắt AutoKill
                player.sendMessage("§c⚠️ Bạn đã bị dịch chuyển! Đã tắt Auto Kill!");
                stopKilling(player);
            }
            // Nếu là do ender pearl hoặc chorus fruit, không tắt
        }
    }

    // KHÔNG TẮT KHI BỊ TẤN CÔNG
    @EventHandler
    public void onPlayerDamage(EntityDamageEvent event) {
        // KHÔNG LÀM GÌ - KHÔNG TẮT AUTOKILL KHI BỊ TẤN CÔNG
        // Chỉ log nếu cần debug
        if (event.getEntity() instanceof Player player) {
            if (tasks.containsKey(player.getUniqueId())) {
                // Kiểm tra nếu bị tấn công bởi entity (quái hoặc người chơi)
                if (event instanceof EntityDamageByEntityEvent) {
                    EntityDamageByEntityEvent e = (EntityDamageByEntityEvent) event;
                    Entity damager = e.getDamager();
                    if (damager instanceof LivingEntity) {
                        // Không tắt, chỉ thông báo nhỏ
                        if (Math.random() < 0.05) { // 5% chance thông báo
                            player.sendMessage("§e⚔️ Bạn đang bị tấn công! Auto Kill vẫn hoạt động!");
                        }
                    }
                }
            }
        }
    }

    // TẮT KHI NGƯỜI CHƠI CHẾT
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (tasks.containsKey(player.getUniqueId())) {
            stopKilling(player);
        }
    }

    // TẮT KHI NGƯỜI CHƠI THOÁT GAME
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (tasks.containsKey(player.getUniqueId())) {
            stopKilling(player);
        }
    }

    // TẮT KHI NGƯỜI CHƠI BỊ KICK
    @EventHandler
    public void onPlayerKick(PlayerKickEvent event) {
        Player player = event.getPlayer();
        if (tasks.containsKey(player.getUniqueId())) {
            stopKilling(player);
        }
    }

    // TẮT KHI NGƯỜI CHƠI TỰ NGỒI XUỐNG (SHIFT) - CÓ THỂ TẮT HOẶC KHÔNG
    @EventHandler
    public void onPlayerSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        
        if (!tasks.containsKey(uuid)) return;
        
        if (event.isSneaking()) {
            // Người chơi đang ngồi xuống, không tắt
            wasSneaking.put(uuid, true);
        } else {
            // Người chơi đứng dậy
            wasSneaking.remove(uuid);
        }
    }

    public void startKilling(Player player) {
        UUID uuid = player.getUniqueId();
        if (tasks.containsKey(uuid)) {
            player.sendMessage("§e⚠️ Auto Kill đã được bật rồi!");
            return;
        }
        
        // Kiểm tra block dưới chân
        Location below = player.getLocation().subtract(0, 1, 0);
        if (below.getBlock().getType() == Material.AIR) {
            player.sendMessage("§c❌ Không có block dưới chân! Không thể bật Auto Kill!");
            player.sendMessage("§7Hãy đứng trên block rắn!");
            return;
        }
        
        lastLocations.put(uuid, player.getLocation().clone());
        KillTask task = new KillTask(player);
        int attackDelay = config.getAttackDelay();
        task.runTaskTimer(plugin, 0L, attackDelay);
        tasks.put(uuid, task);
        
        if (!explosionLevels.containsKey(uuid)) {
            explosionLevels.put(uuid, 0);
            saveExplosionLevels();
        }
        
        player.sendMessage(config.getMessage("kill-enabled"));
        int level = getExplosionLevel(player);
        if (level > 0) {
            player.sendMessage("§6✦ Sát thương nổ level §e" + level + " §6- Tỉ lệ: §e" + 
                String.format("%.1f", getExplosionChance(player) * 100) + "%");
        }
        player.sendMessage("§7⚠️ Đứng yên để duy trì Auto Kill!");
        player.sendMessage("§7⚠️ Bị quái tấn công sẽ không làm tắt Auto Kill!");
    }

    public void stopKilling(Player player) {
        UUID uuid = player.getUniqueId();
        KillTask task = tasks.remove(uuid);
        if (task != null) {
            task.cancel();
            player.sendMessage(config.getMessage("kill-disabled"));
        }
        lastLocations.remove(uuid);
        wasSneaking.remove(uuid);
    }

    public void stopAll() {
        for (KillTask task : tasks.values()) {
            if (task != null) task.cancel();
        }
        tasks.clear();
        lastLocations.clear();
        wasSneaking.clear();
    }

    public boolean isKilling(Player player) { 
        return tasks.containsKey(player.getUniqueId()); 
    }
    
    public int getExplosionLevel(Player player) { 
        return explosionLevels.getOrDefault(player.getUniqueId(), 0); 
    }
    
    public void setExplosionLevel(Player player, int level) { 
        explosionLevels.put(player.getUniqueId(), level); 
        saveExplosionLevels(); 
    }
    
    public double getExplosionChance(Player player) { 
        return config.getExplosionChanceAtLevel(getExplosionLevel(player)); 
    }
    
    public double getExplosionRadius(Player player) { 
        return config.getExplosionRadiusAtLevel(getExplosionLevel(player)); 
    }

    private void triggerExplosion(Player player, Location location, double damage) {
        double radius = getExplosionRadius(player);
        double chance = getExplosionChance(player);
        if (random.nextDouble() > chance || radius <= 0) return;
        
        player.getWorld().createExplosion(location, (float) radius, false, false);
        player.getWorld().playSound(location, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);
        player.sendMessage("§c§l💥 SÁT THƯƠNG NỔ! §7Bán kính: §e" + String.format("%.1f", radius) + " block");
        
        for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (entity instanceof Player) continue;
            if (entity instanceof LivingEntity living && entity.isValid() && !entity.isDead()) {
                EntityDamageByEntityEvent damageEvent = new EntityDamageByEntityEvent(
                    player, living, EntityDamageEvent.DamageCause.ENTITY_ATTACK, damage);
                Bukkit.getPluginManager().callEvent(damageEvent);
                if (!damageEvent.isCancelled()) {
                    living.damage(damage, player);
                }
            }
        }
    }

    private class KillTask extends BukkitRunnable {
        private final Player player;
        private final int range;
        private final UUID playerUUID;

        public KillTask(Player player) {
            this.player = player;
            this.playerUUID = player.getUniqueId();
            this.range = config.getKillRange();
        }

        @Override
        public void run() {
            if (!player.isOnline() || player.isDead()) { 
                stopKilling(player); 
                return; 
            }
            
            // Kiểm tra block dưới chân
            Location below = player.getLocation().subtract(0, 1, 0);
            if (below.getBlock().getType() == Material.AIR && tasks.containsKey(playerUUID)) {
                // Nếu không có block dưới chân, tắt AutoKill
                player.sendMessage("§c⚠️ Không có block dưới chân! Đã tắt Auto Kill!");
                stopKilling(player);
                return;
            }

            // Kiểm tra nếu người chơi tự di chuyển (không phải bị đẩy)
            Location lastLoc = lastLocations.get(playerUUID);
            if (lastLoc != null) {
                double distance = lastLoc.distance(player.getLocation());
                // Nếu di chuyển hơn 0.1 block và không bị đẩy
                if (distance > 0.1 && player.getVelocity().length() < 0.1) {
                    if (tasks.containsKey(playerUUID)) {
                        player.sendMessage("§c⚠️ Bạn đã di chuyển! Đã tắt Auto Kill!");
                        stopKilling(player);
                    }
                    return;
                }
                // Cập nhật vị trí nếu di chuyển nhỏ (do bị đẩy)
                if (distance > 0.01) {
                    lastLocations.put(playerUUID, player.getLocation().clone());
                }
            }

            Entity target = player.getNearbyEntities(range, range, range).stream()
                .filter(e -> (e instanceof Monster && config.isKillMonster()) ||
                             (e instanceof Animals && config.isKillAnimal()) ||
                             (e instanceof Mob && config.isKillMob()))
                .filter(e -> e.isValid() && !e.isDead())
                .min((e1, e2) -> Double.compare(
                    e1.getLocation().distanceSquared(player.getLocation()),
                    e2.getLocation().distanceSquared(player.getLocation())))
                .orElse(null);

            if (target == null) return;
            if (target.getLocation().distance(player.getLocation()) > range) return;

            ItemStack weapon = player.getInventory().getItemInMainHand();
            double damage = calculateDamage(player, weapon);
            
            if (target instanceof LivingEntity living) {
                EntityDamageByEntityEvent damageEvent = new EntityDamageByEntityEvent(
                    player, living, EntityDamageEvent.DamageCause.ENTITY_ATTACK, damage);
                Bukkit.getPluginManager().callEvent(damageEvent);
                
                if (!damageEvent.isCancelled()) {
                    living.damage(damage, player);
                    player.getWorld().playEffect(target.getLocation(), Effect.STEP_SOUND, Material.REDSTONE_BLOCK);
                    triggerExplosion(player, target.getLocation(), damage);
                }
                
                if (living.isDead() || living.getHealth() <= 0) {
                    if (config.isDropItems() && living instanceof Monster monster) {
                        ItemStack handItem = monster.getEquipment().getItemInMainHand();
                        if (handItem != null && !handItem.getType().isAir()) {
                            player.getInventory().addItem(handItem);
                        }
                        for (ItemStack armor : monster.getEquipment().getArmorContents()) {
                            if (armor != null && !armor.getType().isAir()) {
                                player.getInventory().addItem(armor);
                            }
                        }
                    }
                    
                    if (config.isDropXp()) {
                        int xp = 0;
                        if (living instanceof Monster) xp = config.getXpMonster() + new Random().nextInt(3);
                        else if (living instanceof Animals) xp = config.getXpAnimal() + new Random().nextInt(3);
                        else if (living instanceof Mob) xp = config.getXpMob() + new Random().nextInt(5);
                        if (xp > 0) player.giveExp(xp);
                    }
                    
                    try {
                        if (living.hasMetadata("MythicMobs")) {}
                    } catch (Exception ignored) {}
                }
            }
        }

        private double calculateDamage(Player player, ItemStack weapon) {
            double base = config.getBaseDamage();
            if (weapon != null && !weapon.getType().isAir()) {
                String name = weapon.getType().name();
                if (name.contains("NETHERITE_SWORD")) base = 8.0;
                else if (name.contains("DIAMOND_SWORD")) base = 7.0;
                else if (name.contains("IRON_SWORD")) base = 6.0;
                else if (name.contains("STONE_SWORD")) base = 5.0;
                else if (name.contains("WOODEN_SWORD") || name.contains("WOOD_SWORD")) base = 4.0;
                else if (name.contains("GOLDEN_SWORD") || name.contains("GOLD_SWORD")) base = 4.0;
                else if (name.contains("NETHERITE_AXE")) base = 10.0;
                else if (name.contains("DIAMOND_AXE")) base = 9.0;
                else if (name.contains("IRON_AXE")) base = 8.0;
                else if (name.contains("STONE_AXE")) base = 7.0;
                else if (name.contains("WOODEN_AXE") || name.contains("WOOD_AXE")) base = 5.0;
                else if (name.contains("GOLDEN_AXE") || name.contains("GOLD_AXE")) base = 5.0;
                else if (name.contains("TRIDENT")) base = 9.0;
                else if (name.contains("MACE")) base = 12.0;
                
                int sharpness = weapon.getEnchantmentLevel(Enchantment.SHARPNESS);
                if (sharpness > 0) base += (sharpness * 1.5);
                int smite = weapon.getEnchantmentLevel(Enchantment.SMITE);
                if (smite > 0) base += (smite * 2.5);
                int bane = weapon.getEnchantmentLevel(Enchantment.BANE_OF_ARTHROPODS);
                if (bane > 0) base += (bane * 2.5);
                int fireAspect = weapon.getEnchantmentLevel(Enchantment.FIRE_ASPECT);
                if (fireAspect > 0) base += 1.0;
            }
            
            double armorAttack = 0.0;
            for (ItemStack armor : player.getInventory().getArmorContents()) {
                if (armor == null || armor.getType().isAir()) continue;
                if (armor.hasItemMeta() && armor.getItemMeta().hasDisplayName()) {
                    String cleanName = ChatColor.stripColor(armor.getItemMeta().getDisplayName());
                    if (cleanName.toLowerCase().contains("lục bảo") || cleanName.toLowerCase().contains("bảo vệ") ||
                        cleanName.toLowerCase().contains("tấn công") || cleanName.toLowerCase().contains("sức mạnh") ||
                        cleanName.toLowerCase().contains("attack") || cleanName.toLowerCase().contains("damage") ||
                        cleanName.toLowerCase().contains("sát thương")) {
                        armorAttack += 2.0;
                    }
                    Pattern pattern = Pattern.compile("\\+([0-9.]+)");
                    java.util.regex.Matcher matcher = pattern.matcher(cleanName);
                    while (matcher.find()) {
                        try {
                            double value = Double.parseDouble(matcher.group(1));
                            if (cleanName.toLowerCase().contains("tấn công") || cleanName.toLowerCase().contains("sức mạnh") ||
                                cleanName.toLowerCase().contains("attack") || cleanName.toLowerCase().contains("damage")) {
                                armorAttack += value;
                            }
                        } catch (Exception ignored) {}
                    }
                }
                if (armor.hasItemMeta() && armor.getItemMeta().hasLore()) {
                    for (String line : armor.getItemMeta().getLore()) {
                        String cleanLine = ChatColor.stripColor(line);
                        if (cleanLine.contains("Sức tấn công") || cleanLine.contains("Sức tận công") ||
                            cleanLine.contains("Attack") || cleanLine.contains("Damage") ||
                            cleanLine.contains("sát thương")) {
                            Pattern pattern = Pattern.compile("\\+([0-9.]+)");
                            java.util.regex.Matcher matcher = pattern.matcher(cleanLine);
                            while (matcher.find()) {
                                try {
                                    double value = Double.parseDouble(matcher.group(1));
                                    if (cleanLine.contains("%")) {
                                        armorAttack += base * (value / 100.0);
                                    } else {
                                        armorAttack += value;
                                    }
                                } catch (Exception ignored) {}
                            }
                        }
                    }
                }
            }
            base += armorAttack;
            if (player.hasPotionEffect(PotionEffectType.STRENGTH)) {
                int level = player.getPotionEffect(PotionEffectType.STRENGTH).getAmplifier() + 1;
                base *= (1 + (0.3 * level));
            }
            if (Math.random() < 0.2) base *= 1.5;
            return Math.max(base, 1.0);
        }
    }
}