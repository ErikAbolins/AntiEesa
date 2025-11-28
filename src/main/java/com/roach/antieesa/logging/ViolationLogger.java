package com.roach.antieesa.logging;

import com.roach.antieesa.checks.Check;
import com.roach.antieesa.checks.CheckResult;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ViolationLogger {
    private final Plugin plugin;
    private final File logsDirectory;
    private final SimpleDateFormat dateFormat;
    private final SimpleDateFormat timestampFormat;

    public ViolationLogger(Plugin plugin) {
        this.plugin = plugin;
        this.logsDirectory = new File(plugin.getDataFolder(), "logs");
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        this.timestampFormat = new SimpleDateFormat("HH:mm:ss");

        if (!logsDirectory.exists()) {
            logsDirectory.mkdirs();
        }
    }


    public void logViolation(Player player, Check check, CheckResult result, int totalViolations) {
        String date = dateFormat.format(new Date());
        String timestamp = timestampFormat.format(new Date());

        // Create log file per player per day
        File logFile = new File(logsDirectory, String.format("%s_%s.log",
                player.getName(), date));

        String logEntry = String.format("[%s] %s | Level %d | Count: %d | %s%n",
                timestamp,
                check.getName(),
                result.getViolationLevel(),
                totalViolations,
                result.getReason());

        try (FileWriter fw = new FileWriter(logFile, true);
             PrintWriter pw = new PrintWriter(fw)) {
            pw.print(logEntry);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to write violation log: " + e.getMessage());
        }

        // log to console
        plugin.getLogger().info(String.format("[VIOLATION] %s: %s",
                player.getName(), logEntry.trim()));
    }


    public File getLogsDirectory() {
        return logsDirectory;
    }


    public File getLogFile(String playerName, Date date) {
        String dateStr = dateFormat.format(date);
        return new File(logsDirectory, String.format("%s_%s.log", playerName, dateStr));
    }
}