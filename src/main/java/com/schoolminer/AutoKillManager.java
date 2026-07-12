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
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.damage.DamageSource;
import java.util.*;
import java.util.regex.Pattern;
import java.util.concurrent.ConcurrentHashMap;

public class AutoKillManager {
    private final Schoolminer plugin;
    private final Map<UUID, KillTask> tasks = new HashMap<>();
    private final ConfigManager config;
    private final Map<UUID, Location> lastLocations = new HashMap<>();
    private final Map<UUID, Integer> explosionLevels = new ConcurrentHashMap<>();
    private final Random random = new Random();

    public AutoKillManager(Schoolminer plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
    }

    public void startKilling(Player player) {
        UUID uuid = player.getUniqueId();
        if (tasks.containsKey(uuid)) return;
        
        lastLocations.put(uuid, player.getLocation().clone());
        KillTask task = new KillTask(player);
        int attackDelay = config.getAttackDelay();
        task.runTaskTimer(plugin, 0L, attackDelay);
        tasks.put(uuid, task);
        
        if (!explosionLevels.containsKey(uuid)) {
            explosionLevels.put(uuid, 0);
        }
        
        player.sendMessage(config.getMessage("kill-enabled"));
        int level = getExplosionLevel(player);
        if (level > 0) {
            player.sendMessage("§6✦ Sát thương nổ level §e" + level + " §6- Tỉ lệ: §e" + 
                String.format("%.1f", getExplosionChance(player) * 100) + "%");
        }
    }

    public void stopKilling(Player player) {
        UUID uuid = player.getUniqueId();
        KillTask task = tasks.remove(uuid);
        if (task != null) {
            task.cancel();
            player.sendMessage(config.getMessage("kill-disabled"));
        }
        lastLocations.remove(uuid);
    }

    public void stopAll() {
        for (KillTask task : tasks.values()) {
            task.cancel();
        }
        tasks.clear();
        lastLocations.clear();
    }

    public boolean isKilling(Player player) {
        return tasks.containsKey(player.getUniqueId());
    }

    public int getExplosionLevel(Player player) {
        return explosionLevels.getOrDefault(player.getUniqueId(), 0);
    }

    public void setExplosionLevel(Player player, int level) {
        explosionLevels.put(player.getUniqueId(), level);
    }

    public double getExplosionChance(Player player) {
        int level = getExplosionLevel(player);
        return config.getExplosionChanceAtLevel(level);
    }

    public double getExplosionRadius(Player player) {
        int level = getExplosionLevel(player);
        return config.getExplosionRadiusAtLevel(level);
    }

    private void triggerExplosion(Player player, Location location, double damage) {
        double radius = getExplosionRadius(player);
        double chance = getExplosionChance(player);
        
        if (random.nextDouble() > chance || radius <= 0) return;
        
        // Tạo vụ nổ (không gây sát thương từ explosion)
        player.getWorld().createExplosion(location, (float) radius, false, false);
        player.getWorld().playSound(location, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);
        
        player.sendMessage("§c§l💥 SÁT THƯƠNG NỔ! §7Bán kính: §e" + String.format("%.1f", radius) + " block");
        
        // Gây sát thương cho mob trong bán kính - KHÔNG ẢNH HƯỞNG NGƯỜI CHƠI
        for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (entity instanceof Player) continue;
            
            if (entity instanceof LivingEntity living) {
                if (entity.isValid() && !entity.isDead()) {
                    // Tạo sự kiện damage như player đánh
                    EntityDamageByEntityEvent damageEvent = new EntityDamageByEntityEvent(
                        player, 
                        living, 
                        EntityDamageEvent.DamageCause.ENTITY_ATTACK, 
                        damage
                    );
                    Bukkit.getPluginManager().callEvent(damageEvent);
                    
                    if (!damageEvent.isCancelled()) {
                        living.damage(damage, player);
                    }
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

            Location lastLoc = lastLocations.get(playerUUID);
            if (lastLoc != null) {
                Location currentLoc = player.getLocation();
                boolean isSitting = false;
                
                try {
                    if (player.hasMetadata("GSit")) isSitting = true;
                } catch (Exception ignored) {}
                
                try {
                    if (player.hasMetadata("CMISit")) isSitting = true;
                } catch (Exception ignored) {}
                
                try {
                    if (player.hasMetadata("Sit")) isSitting = true;
                } catch (Exception ignored) {}
                
                if (!isSitting && lastLoc.distance(currentLoc) > 0.1) {
                    player.sendMessage("§c⚠️ Bạn đã di chuyển, Auto Kill đã tắt!");
                    stopKilling(player);
                    return;
                }
            }

            Entity target = player.getNearbyEntities(range, range, range).stream()
                .filter(e -> {
                    if (e instanceof Monster) return config.isKillMonster();
                    if (e instanceof Animals) return config.isKillAnimal();
                    if (e instanceof Mob) return config.isKillMob();
                    return false;
                })
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
                // Tạo sự kiện damage như player đánh
                EntityDamageByEntityEvent damageEvent = new EntityDamageByEntityEvent(
                    player, 
                    living, 
                    EntityDamageEvent.DamageCause.ENTITY_ATTACK, 
                    damage
                );
                
                Bukkit.getPluginManager().callEvent(damageEvent);
                
                if (!damageEvent.isCancelled()) {
                    living.damage(damage, player);
                    
                    // Hiệu ứng
                    player.getWorld().playEffect(target.getLocation(), Effect.STEP_SOUND, 
                        Material.REDSTONE_BLOCK);
                    
                    // Kích hoạt sát thương nổ (KHÔNG ẢNH HƯỞNG NGƯỜI CHƠI)
                    triggerExplosion(player, target.getLocation(), damage);
                }
                
                // Nếu mob chết, plugin khác sẽ tự nhận diện qua EntityDamageByEntityEvent
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
                        if (living instanceof Monster) {
                            xp = config.getXpMonster() + new Random().nextInt(3);
                        } else if (living instanceof Animals) {
                            xp = config.getXpAnimal() + new Random().nextInt(3);
                        } else if (living instanceof Mob) {
                            xp = config.getXpMob() + new Random().nextInt(5);
                        }
                        
                        if (xp > 0) {
                            player.giveExp(xp);
                        }
                    }
                    
                    try {
                        if (living.hasMetadata("MythicMobs")) {
                            // MythicMobs sẽ tự xử lý
                        }
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
                    String displayName = armor.getItemMeta().getDisplayName();
                    String cleanName = ChatColor.stripColor(displayName);
                    
                    if (cleanName.toLowerCase().contains("lục bảo") ||
                        cleanName.toLowerCase().contains("bảo vệ") ||
                        cleanName.toLowerCase().contains("tấn công") ||
                        cleanName.toLowerCase().contains("sức mạnh") ||
                        cleanName.toLowerCase().contains("strength") ||
                        cleanName.toLowerCase().contains("attack") ||
                        cleanName.toLowerCase().contains("damage") ||
                        cleanName.toLowerCase().contains("sát thương")) {
                        armorAttack += 2.0;
                    }
                    
                    Pattern pattern = Pattern.compile("\\+([0-9.]+)");
                    java.util.regex.Matcher matcher = pattern.matcher(cleanName);
                    while (matcher.find()) {
                        try {
                            double value = Double.parseDouble(matcher.group(1));
                            if (cleanName.toLowerCase().contains("tấn công") ||
                                cleanName.toLowerCase().contains("sức mạnh") ||
                                cleanName.toLowerCase().contains("attack") ||
                                cleanName.toLowerCase().contains("damage")) {
                                armorAttack += value;
                            }
                        } catch (Exception ignored) {}
                    }
                }
                
                if (armor.hasItemMeta() && armor.getItemMeta().hasLore()) {
                    List<String> lore = armor.getItemMeta().getLore();
                    for (String line : lore) {
                        String cleanLine = ChatColor.stripColor(line);
                        
                        if (cleanLine.contains("Sức tấn công") || 
                            cleanLine.contains("Sức tận công") ||
                            cleanLine.contains("Attack") || 
                            cleanLine.contains("Damage") ||
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
            
            if (Math.random() < 0.2) {
                base *= 1.5;
            }
            
            return Math.max(base, 1.0);
        }
    }
}