package com.schoolminer;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import java.util.*;

public class SchoolminerTabCompleter implements TabCompleter {
    private final Schoolminer plugin;

    public SchoolminerTabCompleter(Schoolminer plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        String cmdName = command.getName().toLowerCase();

        switch (cmdName) {
            case "automine":
                completions = handleAutoMine(sender, args);
                break;
            case "autokill":
                completions = handleAutoKill(sender, args);
                break;
            case "autocraft":
                completions = handleAutoCraft(sender, args);
                break;
            case "schoolminer":
                completions = handleSchoolminer(sender, args);
                break;
        }

        return completions;
    }

    private List<String> handleAutoMine(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.add("on");
            completions.add("off");
            return filter(completions, args[0]);
        }
        return completions;
    }

    private List<String> handleAutoKill(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.add("on");
            completions.add("off");
            return filter(completions, args[0]);
        }
        return completions;
    }

    private List<String> handleAutoCraft(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (!(sender instanceof Player player)) {
            return completions;
        }

        if (args.length == 1) {
            AutoCraftManager craftManager = plugin.getAutoCraftManager();
            List<String> availableCrafts = craftManager.getAvailableCrafts(player);
            availableCrafts.add("off");
            return filter(availableCrafts, args[0]);
        }

        if (args.length == 2) {
            if (plugin.getConfigManager().getCraftConfig(args[0].toLowerCase()) != null) {
                completions.add("off");
                return filter(completions, args[1]);
            }
        }

        return completions;
    }

    private List<String> handleSchoolminer(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.add("reload");
            completions.add("help");
            return filter(completions, args[0]);
        }
        return completions;
    }

    private List<String> filter(List<String> list, String input) {
        List<String> result = new ArrayList<>();
        for (String s : list) {
            if (s.toLowerCase().startsWith(input.toLowerCase())) {
                result.add(s);
            }
        }
        return result;
    }
}