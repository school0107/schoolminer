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
        
        configManager = new ConfigManager(this);
        playerDataManager = new PlayerDataManager(this);
        autoMineManager = new AutoMineManager(this);
        autoKillManager = new AutoKillManager(this, playerDataManager);
        autoCraftManager = new AutoCraftManager(this);
        
        getServer().getPluginManager().registerEvents(autoMineManager, this);
        getServer().getPluginManager().registerEvents(autoKillManager, this);
        
        // Đăng ký Commands an toàn
        if (getCommand("automine") != null) getCommand("automine").setExecutor(new AutoMineCommand(this));
        if (getCommand("autokill") != null) getCommand("autokill").setExecutor(new AutoKillCommand(this));
        if (getCommand("autocraft") != null) getCommand("autocraft").setExecutor(new AutoCraftCommand(this));
        if (getCommand("schoolminer") != null) getCommand("schoolminer").setExecutor(new SchoolminerCommand(this));
        
        // Đăng ký TabCompleter
        SchoolminerTabCompleter tabCompleter = new SchoolminerTabCompleter(this);
        if (getCommand("automine") != null) getCommand("automine").setTabCompleter(tabCompleter);
        if (getCommand("autokill") != null) getCommand("autokill").setTabCompleter(tabCompleter);
        if (getCommand("autocraft") != null) getCommand("autocraft").setTabCompleter(tabCompleter);
        if (getCommand("schoolminer") != null) getCommand("schoolminer").setTabCompleter(tabCompleter);
        
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        
        getLogger().log(Level.INFO, "§a✅ Schoolminer đã được khởi tạo thành công trên Purpur 1.21.11!");
    }

    @Override
    public void onDisable() {
        if (autoMineManager != null) autoMineManager.stopAll();
        if (autoKillManager != null) autoKillManager.stopAll();
        if (autoCraftManager != null) autoCraftManager.stopAll();
        if (playerDataManager != null) playerDataManager.saveData();
        getLogger().log(Level.INFO, "§c⛔ Schoolminer đã tắt!");
    }

    public static Schoolminer getInstance() { return instance; }
    public AutoMineManager getAutoMineManager() { return autoMineManager; }
    public AutoKillManager getAutoKillManager() { return autoKillManager; }
    public AutoCraftManager getAutoCraftManager() { return autoCraftManager; }
    public ConfigManager getConfigManager() { return configManager; }
    public PlayerDataManager getPlayerDataManager() { return playerDataManager; }
}