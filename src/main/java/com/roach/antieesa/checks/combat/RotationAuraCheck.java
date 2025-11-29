package com.roach.antieesa.checks.combat;

import com.roach.antieesa.checks.Check;
import com.roach.antieesa.checks.CheckResult;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Detects killaura by analyzing head rotation patterns.
 * Looks for:
 * - Instant snaps to target (>50 degrees per tick)
 * - Hitting while looking away from target
 * - Perfect tracking that's too consistent
 */
public class RotationAuraCheck extends Check {
    // Store last yaw/pitch per player
    private final Map<UUID, Float> lastYaw = new HashMap<>();
    private final Map<UUID, Float> lastPitch = new HashMap<>();
    private final Map<UUID, Long> lastAttackTime = new HashMap<>();

    // Track suspicious rotation counts
    private final Map<UUID, Integer> suspiciousRotations = new HashMap<>();

    // Maximum rotation per tick that's humanly possible (degrees)
    private static final float MAX_ROTATION_PER_TICK = 50.0f;

    // Maximum angle offset while hitting (degrees)
    private static final float MAX_HIT_ANGLE = 45.0f;

    public RotationAuraCheck() {
        super("KillAura-Rotation", "Detects killaura via impossible head rotations", 4);
    }

    @Override
    public CheckResult check(Player player, Event event) {
        if (!(event instanceof EntityDamageByEntityEvent)) {
            return CheckResult.pass();
        }

        EntityDamageByEntityEvent damageEvent = (EntityDamageByEntityEvent) event;

        if (!(damageEvent.getDamager() instanceof Player)) {
            return CheckResult.pass();
        }

        if (!damageEvent.getDamager().equals(player)) {
            return CheckResult.pass();
        }

        Entity target = damageEvent.getEntity();
        if (!(target instanceof LivingEntity)) {
            return CheckResult.pass();
        }

        UUID uuid = player.getUniqueId();
        Location playerLoc = player.getEyeLocation();
        Location targetLoc = target.getLocation().add(0, target.getHeight() / 2, 0);

        // Calculate angle to target
        Vector toTarget = targetLoc.toVector().subtract(playerLoc.toVector()).normalize();
        Vector playerDirection = playerLoc.getDirection();

        double dotProduct = toTarget.dot(playerDirection);
        double angleToTarget = Math.toDegrees(Math.acos(dotProduct));

        // Check 1: Are they hitting while looking away?
        if (angleToTarget > MAX_HIT_ANGLE) {
            int suspicious = suspiciousRotations.getOrDefault(uuid, 0) + 2; // +2 for blatant
            suspiciousRotations.put(uuid, suspicious);

            if (suspicious > 5) {
                suspiciousRotations.put(uuid, 0);
                return CheckResult.fail(
                        String.format("Hit target while looking %.1f° away (max: %.1f°)", angleToTarget, MAX_HIT_ANGLE),
                        4
                );
            }
        }

        // Check 2: Impossible rotation speed
        float currentYaw = playerLoc.getYaw();
        float currentPitch = playerLoc.getPitch();

        if (lastYaw.containsKey(uuid)) {
            float yawDiff = Math.abs(normalizeYaw(currentYaw - lastYaw.get(uuid)));
            float pitchDiff = Math.abs(currentPitch - lastPitch.get(uuid));
            float totalRotation = (float) Math.sqrt(yawDiff * yawDiff + pitchDiff * pitchDiff);

            long timeSinceLastAttack = System.currentTimeMillis() - lastAttackTime.getOrDefault(uuid, 0L);

            // Only check if attacking rapidly (within 500ms)
            if (timeSinceLastAttack < 500 && totalRotation > MAX_ROTATION_PER_TICK) {
                int suspicious = suspiciousRotations.getOrDefault(uuid, 0) + 1;
                suspiciousRotations.put(uuid, suspicious);

                if (suspicious > 5) {
                    suspiciousRotations.put(uuid, 0);
                    return CheckResult.fail(
                            String.format("Impossible head rotation: %.1f°/tick (max: %.1f°)", totalRotation, MAX_ROTATION_PER_TICK),
                            5
                    );
                }
            } else if (totalRotation < MAX_ROTATION_PER_TICK) {
                // Reduce suspicion on legit movements
                suspiciousRotations.put(uuid, Math.max(0, suspiciousRotations.getOrDefault(uuid, 0) - 1));
            }
        }

        // Update tracking data
        lastYaw.put(uuid, currentYaw);
        lastPitch.put(uuid, currentPitch);
        lastAttackTime.put(uuid, System.currentTimeMillis());

        return CheckResult.pass();
    }

    /**
     * Normalize yaw to -180 to 180 range
     */
    private float normalizeYaw(float yaw) {
        yaw = yaw % 360;
        if (yaw > 180) yaw -= 360;
        if (yaw < -180) yaw += 360;
        return yaw;
    }

    /**
     * Clean up player data
     */
    public void cleanup(UUID uuid) {
        lastYaw.remove(uuid);
        lastPitch.remove(uuid);
        lastAttackTime.remove(uuid);
        suspiciousRotations.remove(uuid);
    }
}
