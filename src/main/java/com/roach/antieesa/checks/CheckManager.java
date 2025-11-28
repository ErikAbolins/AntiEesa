package com.roach.antieesa.checks;

import com.roach.antieesa.violations.ViolationManager;
import com.roach.antieesa.logging.ViolationLogger;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class CheckManager implements Listener {
    private final Plugin plugin;
    private final ViolationManager violationManager;
    private final ViolationLogger logger;
    private final List<Check> checks;
    private final Map<Class<? extends Event>, List<Check>> eventCheckMap;

    public CheckManager(Plugin plugin, ViolationManager violationManager, ViolationLogger logger) {
        this.plugin = plugin;
        this.violationManager = violationManager;
        this.logger = logger;
        this.checks = new ArrayList<>();
        this.eventCheckMap = new HashMap<>();
    }


    public void registerCheck(Check check) {
        checks.add(check);
    }


    public List<Check> getChecks() {
        return new ArrayList<>(checks);
    }


    public Check getCheck(String name) {
        return checks.stream()
                .filter(c -> c.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    //Movement Events
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        if (player.hasPermission("anticheat.bypass")) {
            return;
        }

        runChecks(player, event);
    }

    //Combat Events
    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getDamager();

        if (player.hasPermission("anticheat.bypass")) {
            return;
        }

        runChecks(player, event);
    }


    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (player.hasPermission("anticheat.bypass")) {
            return;
        }

        runChecks(player, event);
    }


    private void runChecks(Player player, Event event) {
        for (Check check : checks) {
            if (!check.isEnabled()) {
                continue;
            }

            try {
                CheckResult result = check.check(player, event);

                if (result.isViolated()) {
                    handleViolation(player, check, result);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    private void handleViolation(Player player, Check check, CheckResult result) {
        boolean shouldLog = violationManager.recordViolation(player, check, result);

        if (shouldLog) {
            int totalViolations = violationManager.getViolationCount(player.getUniqueId(), check.getName());
            logger.logViolation(player, check, result, totalViolations);
        }
    }
}