package com.roach.antieesa.checks;

import com.roach.antieesa.checks.Check;
import com.roach.antieesa.checks.CheckResult;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


public class SpeedCheck extends Check {
    // Store last locations to calculate distance
    private final Map<UUID, Location> lastLocations = new HashMap<>();

    // Base speeds (blocks per tick)
    private static final double WALK_SPEED = 0.215;
    private static final double SPRINT_SPEED = 0.28;
    private static final double SPEED_BUFFER = 0.05; // 5% buffer for lag/legit movements

    public SpeedCheck() {
        super("Speed", "Detects players moving faster than possible", 3);
    }

    @Override
    public CheckResult check(Player player, Event event) {
        System.out.println("DEBUG: SpeedCheck.check() called for " + player.getName());
        // Only check PlayerMoveEvent
        if (!(event instanceof PlayerMoveEvent)) {
            return CheckResult.pass();
        }

        PlayerMoveEvent moveEvent = (PlayerMoveEvent) event;
        Location from = moveEvent.getFrom();
        Location to = moveEvent.getTo();

        // Ignore if not actually moving
        if (to == null || (from.getX() == to.getX() && from.getZ() == to.getZ())) {
            return CheckResult.pass();
        }

        double deltaX = to.getX() - from.getX();
        double deltaZ = to.getZ() - from.getZ();
        double horizontalDistance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

        double maxSpeed = player.isSprinting() ? SPRINT_SPEED : WALK_SPEED;

        if (player.hasPotionEffect(PotionEffectType.SPEED)) {
            int amplifier = player.getPotionEffect(PotionEffectType.SPEED).getAmplifier();
            maxSpeed += maxSpeed * 0.2 * (amplifier + 1); // 20% per level
        }

        maxSpeed += SPEED_BUFFER;

        if (horizontalDistance > maxSpeed) {
            double percentage = ((horizontalDistance / maxSpeed) - 1.0) * 100;
            int violationLevel = getViolationLevel(percentage);

            String reason = String.format("Moving %.1f%% faster than max speed (%.3f > %.3f)",
                    percentage, horizontalDistance, maxSpeed);

            return CheckResult.fail(reason, violationLevel);
        }

        // Store location for next check
        lastLocations.put(player.getUniqueId(), to);

        return CheckResult.pass();
    }


    private int getViolationLevel(double percentage) {
        if (percentage > 100) return 5; // Blatant (2x+ speed)
        if (percentage > 50) return 4;  // Very suspicious
        if (percentage > 30) return 3;  // Suspicious
        if (percentage > 15) return 2;  // Slightly sus
        return 1;                       // Barely over
    }


    public void cleanup(UUID uuid) {
        lastLocations.remove(uuid);
    }
}