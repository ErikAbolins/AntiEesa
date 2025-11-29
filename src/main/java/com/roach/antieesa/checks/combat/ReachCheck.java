package com.roach.antieesa.checks.combat;

import com.roach.antieesa.checks.Check;
import com.roach.antieesa.checks.CheckResult;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Detects extended reach by measuring actual hit distance.
 * Vanilla max reach is 3.0 blocks, we add small buffer for lag.
 */
public class ReachCheck extends Check {
    // Track violations per player to avoid false positives from lag spikes
    private final Map<UUID, Integer> reachViolations = new HashMap<>();
    
    // Maximum legitimate reach distance (blocks)
    private static final double MAX_REACH = 3.0;
    
    // Buffer for latency/positioning quirks
    private static final double REACH_BUFFER = 0.15; // Reduced from 0.3
    
    // Effective max with buffer
    private static final double MAX_REACH_WITH_BUFFER = MAX_REACH + REACH_BUFFER;
    
    public ReachCheck() {
        super("Reach", "Detects extended reach in combat", 4);
    }

    @Override
    public CheckResult check(Player player, Event event) {
        if (!(event instanceof EntityDamageByEntityEvent)) {
            return CheckResult.pass();
        }
        
        EntityDamageByEntityEvent damageEvent = (EntityDamageByEntityEvent) event;
        
        if (!damageEvent.getDamager().equals(player)) {
            return CheckResult.pass();
        }
        
        Entity target = damageEvent.getEntity();
        UUID uuid = player.getUniqueId();
        
        // Calculate distance from player's eyes to target's center
        Location playerEye = player.getEyeLocation();
        Location targetCenter = target.getLocation().add(0, target.getHeight() / 2, 0);
        
        // Get the actual reach distance
        double distance = calculateReach(playerEye, targetCenter, target);
        
        // Check if exceeds max reach
        if (distance > MAX_REACH_WITH_BUFFER) {
            int violations = reachViolations.getOrDefault(uuid, 0) + 1;
            reachViolations.put(uuid, violations);
            
            // Need multiple violations to flag (avoid false positives from lag)
            if (violations >= 2) { // Reduced from 3
                reachViolations.put(uuid, 0); // Reset
                
                int violationLevel = calculateViolationLevel(distance);
                
                return CheckResult.fail(
                    String.format("Hit from %.2f blocks away (max: %.2f)", distance, MAX_REACH_WITH_BUFFER),
                    violationLevel
                );
            }
        } else {
            // Legitimate hit, reduce violation count
            reachViolations.put(uuid, Math.max(0, reachViolations.getOrDefault(uuid, 0) - 1));
        }
        
        return CheckResult.pass();
    }
    
    /**
     * Calculate actual reach distance accounting for entity hitbox
     */
    private double calculateReach(Location playerEye, Location targetCenter, Entity target) {
        // Get vector from player to target
        Vector toTarget = targetCenter.toVector().subtract(playerEye.toVector());
        double distance = toTarget.length();
        
        // Subtract target's hitbox radius (approximate)
        // Most mobs have ~0.3-0.6 block radius
        double hitboxRadius = target.getWidth() / 2.0;
        
        // Actual reach is distance minus hitbox
        return Math.max(0, distance - hitboxRadius);
    }
    
    /**
     * Calculate violation severity based on how far over the limit
     */
    private int calculateViolationLevel(double distance) {
        double excess = distance - MAX_REACH_WITH_BUFFER;
        
        if (excess > 3.0) return 5; // 6+ block reach = blatant
        if (excess > 2.0) return 4; // 5+ block reach = very suspicious  
        if (excess > 1.0) return 3; // 4+ block reach = suspicious
        if (excess > 0.5) return 2; // 3.8+ block reach = slightly sus
        return 1;                    // Just barely over
    }
    
    /**
     * Clean up player data
     */
    public void cleanup(UUID uuid) {
        reachViolations.remove(uuid);
    }
}
