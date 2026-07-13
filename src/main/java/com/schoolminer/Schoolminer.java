package com.schoolminer;

import org.bukkit.plugin.java.JavaPlugin;
import java.util.logging.Level;

public class Schoolminer extends JavaPlugin {
    private static Schoolminer instance;
    private AutoMineManager autoMineManager;
    private AutoKillManager autoKillManager;
    private AutoCraftManager autoCraftManager;
    private ConfigManager configManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        
        configManager = new ConfigManager(this);
        autoMineManager = new AutoMineManager(this);
        autoKillManager = new AutoKillManager(this);
        autoCraftManager = new AutoCraftManager(this);
        
        // Đăng ký listener - AutoKillManager đã tự đăng ký trong constructor
        getServer().getPluginManager().registerEvents(autoMineManager, this);
        // Không cần đăng ký autoKillManager ở đây vì nó đã tự đăng ký
        
        getCommand("automine").setExecutor(new AutoMineCommand(this));
        getCommand("autokill").setExecutor(new AutoKillCommand(this));
        getCommand("autocraft").setExecutor(new AutoCraftCommand(this));
        getCommand("schoolminer").setExecutor(new SchoolminerCommand(this));
        
        SchoolminerTabCompleter tabCompleter = new SchoolminerTabCompleter(this);
        getCommand("automine").setTabCompleter(tabCompleter);
        getCommand("autokill").setTabCompleter(tabCompleter);
        getCommand("autocraft").setTabCompleter(tabCompleter);
        getCommand("schoolminer").setTabCompleter(tabCompleter);
        
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        
        getLogger().log(Level.INFO, "§a✅ Schoolminer đã được khởi tạo thành công!");
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
}