package com.schoolminer;

import org.bukkit.inventory.ItemStack;
import java.util.*;

public class AutoCraftConfig {
    private final String id;
    private final String displayName;
    private final List<ItemStack> materials;
    private final ItemStack result;
    private final boolean glow;

    public AutoCraftConfig(String id, String displayName, List<ItemStack> materials, ItemStack result, boolean glow) {
        this.id = id;
        this.displayName = displayName;
        this.materials = materials;
        this.result = result;
        this.glow = glow;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public List<ItemStack> getMaterials() {
        return materials;
    }

    public ItemStack getResult() {
        return result;
    }

    public boolean isGlow() {
        return glow;
    }
}
