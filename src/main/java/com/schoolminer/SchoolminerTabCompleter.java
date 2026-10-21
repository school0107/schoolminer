package com.schoolminer;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import java.util.*;

public class SchoolminerTabCompleter implements TabCompleter {
    private final Schoolminer plugin;

    public SchoolminerTabCompleter(Schoolminer plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.add("check");
            completions.add("add");
            completions.add("remove");
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