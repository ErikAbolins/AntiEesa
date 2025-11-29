package com.roach.antieesa.commands;

import com.roach.antieesa.AntiEesa;
import com.roach.antieesa.checks.Check;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Command handler for /ac
 * Usage: /ac <checks|logs|clear|toggle> [args]
 */
public class ACCommand implements CommandExecutor, TabCompleter {
    private final AntiEesa plugin;

    public ACCommand(AntiEesa plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Permission check
        if (!sender.hasPermission("anticheat.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "checks":
            case "list":
                listChecks(sender);
                break;

            case "toggle":
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /ac toggle <check>");
                    return true;
                }
                toggleCheck(sender, args[1]);
                break;

            case "info":
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /ac info <check>");
                    return true;
                }
                checkInfo(sender, args[1]);
                break;

            case "logs":
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /ac logs <player>");
                    return true;
                }
                // TODO: Implement log viewing
                sender.sendMessage(ChatColor.YELLOW + "Log viewing coming soon!");
                break;

            case "clear":
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /ac clear <player>");
                    return true;
                }
                // TODO: Implement violation clearing
                sender.sendMessage(ChatColor.YELLOW + "Violation clearing coming soon!");
                break;

            default:
                sendHelp(sender);
                break;
        }

        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== AntiEesa Commands ===");
        sender.sendMessage(ChatColor.YELLOW + "/ac checks" + ChatColor.GRAY + " - List all checks");
        sender.sendMessage(ChatColor.YELLOW + "/ac info <check>" + ChatColor.GRAY + " - Get info about a check");
        sender.sendMessage(ChatColor.YELLOW + "/ac toggle <check>" + ChatColor.GRAY + " - Enable/disable a check");
        sender.sendMessage(ChatColor.YELLOW + "/ac logs <player>" + ChatColor.GRAY + " - View player logs");
        sender.sendMessage(ChatColor.YELLOW + "/ac clear <player>" + ChatColor.GRAY + " - Clear player violations");
    }

    private void listChecks(CommandSender sender) {
        List<Check> checks = plugin.getCheckManager().getChecks();

        if (checks.isEmpty()) {
            sender.sendMessage(ChatColor.RED + "No checks registered!");
            return;
        }

        sender.sendMessage(ChatColor.GOLD + "=== Registered Checks (" + checks.size() + ") ===");
        for (Check check : checks) {
            String status = check.isEnabled() ?
                    ChatColor.GREEN + "✓" :
                    ChatColor.RED + "✗";

            sender.sendMessage(String.format("%s %s%s %s- %s",
                    status,
                    ChatColor.YELLOW,
                    check.getName(),
                    ChatColor.GRAY,
                    check.getDescription()));
        }
    }

    private void toggleCheck(CommandSender sender, String checkName) {
        Check check = plugin.getCheckManager().getCheck(checkName);

        if (check == null) {
            sender.sendMessage(ChatColor.RED + "Check not found: " + checkName);
            return;
        }

        check.setEnabled(!check.isEnabled());

        String status = check.isEnabled() ?
                ChatColor.GREEN + "enabled" :
                ChatColor.RED + "disabled";

        sender.sendMessage(String.format("%s%s%s has been %s%s",
                ChatColor.YELLOW,
                check.getName(),
                ChatColor.GRAY,
                status,
                ChatColor.GRAY));
    }

    private void checkInfo(CommandSender sender, String checkName) {
        Check check = plugin.getCheckManager().getCheck(checkName);

        if (check == null) {
            sender.sendMessage(ChatColor.RED + "Check not found: " + checkName);
            return;
        }

        sender.sendMessage(ChatColor.GOLD + "=== " + check.getName() + " ===");
        sender.sendMessage(ChatColor.GRAY + "Description: " + ChatColor.WHITE + check.getDescription());
        sender.sendMessage(ChatColor.GRAY + "Status: " +
                (check.isEnabled() ? ChatColor.GREEN + "Enabled" : ChatColor.RED + "Disabled"));
        sender.sendMessage(ChatColor.GRAY + "Violation Threshold: " + ChatColor.WHITE + check.getViolationThreshold());
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("anticheat.admin")) {
            return new ArrayList<>();
        }

        if (args.length == 1) {
            // Subcommands
            return Arrays.asList("checks", "info", "toggle", "logs", "clear")
                    .stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            String subCommand = args[0].toLowerCase();

            if (subCommand.equals("toggle") || subCommand.equals("info")) {
                return plugin.getCheckManager().getChecks().stream()
                        .map(Check::getName)
                        .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }

            if (subCommand.equals("logs") || subCommand.equals("clear")) {
                return plugin.getServer().getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }

        return new ArrayList<>();
    }
}