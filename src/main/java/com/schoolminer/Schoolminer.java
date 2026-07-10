package com.schoolminer;

import org.bukkit.plugin.java.JavaPlugin;
import java.util.logging.Level;

public class Schoolminer extends JavaPlugin {
    private static Schoolminer instance;
    private AutoMineManager autoMineManager;
    private AutoKillManager autoKillManager;
    private ConfigManager configManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        
        configManager = new ConfigManager(this);
        autoMineManager = new AutoMineManager(this);
        autoKillManager = new AutoKillManager(this);
        
        getCommand("automine").setExecutor(new AutoMineCommand(this));
        getCommand("autokill").setExecutor(new AutoKillCommand(this));
        getCommand("schoolminer").setExecutor(new SchoolminerCommand(this));
        
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        
        getLogger().log(Level.INFO, "§a✅ Schoolminer đã được khởi tạo thành công!");
    }

    @Override
    public void onDisable() {
        autoMineManager.stopAll();
        autoKillManager.stopAll();
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

    public ConfigManager getConfigManager() {
        return configManager;
    }
}
