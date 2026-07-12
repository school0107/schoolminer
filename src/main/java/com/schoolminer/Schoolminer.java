package com.schoolminer;

import org.bukkit.plugin.java.JavaPlugin;
import java.util.logging.Level;

public class Schoolminer extends JavaPlugin {
    private static Schoolminer instance;
    private AutoMineManager autoMineManager;
    private AutoKillManager autoKillManager;
    private AutoCraftManager autoCraftManager;
    private ConfigManager configManager;
    private VaultEconomy economy;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        
        configManager = new ConfigManager(this);
        economy = new VaultEconomy(this);
        autoMineManager = new AutoMineManager(this);
        autoKillManager = new AutoKillManager(this);
        autoCraftManager = new AutoCraftManager(this);
        
        // Đăng ký listener cho AutoMineManager
        getServer().getPluginManager().registerEvents(autoMineManager, this);
        
        // Command Executors
        getCommand("automine").setExecutor(new AutoMineCommand(this));
        getCommand("autokill").setExecutor(new AutoKillCommand(this, economy));
        getCommand("autocraft").setExecutor(new AutoCraftCommand(this));
        getCommand("schoolminer").setExecutor(new SchoolminerCommand(this));
        
        // Tab Completer
        SchoolminerTabCompleter tabCompleter = new SchoolminerTabCompleter(this);
        getCommand("automine").setTabCompleter(tabCompleter);
        getCommand("autokill").setTabCompleter(tabCompleter);
        getCommand("autocraft").setTabCompleter(tabCompleter);
        getCommand("schoolminer").setTabCompleter(tabCompleter);
        
        // Register events
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        
        getLogger().log(Level.INFO, "§a✅ Schoolminer đã được khởi tạo thành công!");
        if (economy.isEnabled()) {
            getLogger().log(Level.INFO, "§a✅ Đã kết nối Vault Economy!");
        } else {
            getLogger().log(Level.WARNING, "§c⚠️ Không tìm thấy Vault! Tính năng kinh tế sẽ không hoạt động!");
        }
    }

    @Override
    public void onDisable() {
        if (autoMineManager != null) autoMineManager.stopAll();
        if (autoKillManager != null) autoKillManager.stopAll();
        if (autoCraftManager != null) autoCraftManager.stopAll();
        getLogger().log(Level.INFO, "§c⛔ Schoolminer đã tắt!");
    }

    public static Schoolminer getInstance() {
        return instance;
    }

    public AutoMineManager getAutoMineManager() {
        return autoMineManager;
    }

    public AutoKillManager getAutoKillManager() {
        return autoKillManager;
    }

    public AutoCraftManager getAutoCraftManager() {
        return autoCraftManager;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public VaultEconomy getEconomy() {
        return economy;
    }
}