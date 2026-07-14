package com.schoolminer;

import org.bukkit.plugin.java.JavaPlugin;
import java.util.logging.Level;

public class Schoolminer extends JavaPlugin {
    private static Schoolminer instance;
    private AutoMineManager autoMineManager;
    private AutoKillManager autoKillManager;
    private AutoCraftManager autoCraftManager;
    private ConfigManager configManager;
    private PlayerDataManager playerDataManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        
        // Khởi tạo ConfigManager trước
        configManager = new ConfigManager(this);
        
        // Khởi tạo PlayerDataManager để quản lý dữ liệu người chơi
        playerDataManager = new PlayerDataManager(this);
        
        // Khởi tạo các Manager
        autoMineManager = new AutoMineManager(this);
        autoKillManager = new AutoKillManager(this, playerDataManager);
        autoCraftManager = new AutoCraftManager(this);
        
        // Đăng ký sự kiện
        getServer().getPluginManager().registerEvents(autoMineManager, this);
        getServer().getPluginManager().registerEvents(autoKillManager, this);
        
        // Đăng ký Command Executors
        getCommand("automine").setExecutor(new AutoMineCommand(this));
        getCommand("autokill").setExecutor(new AutoKillCommand(this));
        getCommand("autocraft").setExecutor(new AutoCraftCommand(this));
        getCommand("schoolminer").setExecutor(new SchoolminerCommand(this));
        
        // Đăng ký Tab Completer
        SchoolminerTabCompleter tabCompleter = new SchoolminerTabCompleter(this);
        getCommand("automine").setTabCompleter(tabCompleter);
        getCommand("autokill").setTabCompleter(tabCompleter);
        getCommand("autocraft").setTabCompleter(tabCompleter);
        getCommand("schoolminer").setTabCompleter(tabCompleter);
        
        // Đăng ký PlayerListener
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        
        getLogger().log(Level.INFO, "§a✅ Schoolminer đã được khởi tạo thành công!");
        getLogger().log(Level.INFO, "§a✅ Đã load dữ liệu người chơi!");
    }

    @Override
    public void onDisable() {
        // Dừng tất cả các task
        if (autoMineManager != null) autoMineManager.stopAll();
        if (autoKillManager != null) autoKillManager.stopAll();
        if (autoCraftManager != null) autoCraftManager.stopAll();
        
        // Lưu dữ liệu người chơi
        if (playerDataManager != null) playerDataManager.saveData();
        
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

    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }
}