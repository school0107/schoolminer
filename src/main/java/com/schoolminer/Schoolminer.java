package com.schoolminer;

import org.bukkit.plugin.java.JavaPlugin;
import java.util.logging.Level;

public class Schoolminer extends JavaPlugin {
    private static Schoolminer instance;
    private ConfigManager configManager;
    private MultiBlockManager multiBlockManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        
        configManager = new ConfigManager(this);
        multiBlockManager = new MultiBlockManager(this);
        
        if (getCommand("schoolminer") != null) {
            getCommand("schoolminer").setExecutor(new SchoolminerCommand(this));
            getCommand("schoolminer").setTabCompleter(new SchoolminerTabCompleter(this));
        }
        
        getServer().getPluginManager().registerEvents(multiBlockManager, this);
        
        getLogger().log(Level.INFO, "§a✅ Schoolminer (MultiBlock NBT) đã khởi tạo!");
    }

    @Override
    public void onDisable() {
        getLogger().log(Level.INFO, "§c⛔ Schoolminer đã tắt!");
    }

    public static Schoolminer getInstance() { return instance; }
    public ConfigManager getConfigManager() { return configManager; }
    public MultiBlockManager getMultiBlockManager() { return multiBlockManager; }
}