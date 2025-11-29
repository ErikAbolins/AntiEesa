package com.roach.antieesa.checks.combat;

import com.roach.antieesa.checks.Check;
import com.roach.antieesa.checks.CheckResult;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.*;

/**
 * Detects killaura by tracking multiple entities hit in rapid succession.
 * Legitimate players can only focus on one target at a time.
 * Killaura often switches between multiple targets rapidly.
 */
public class MultiTargetCheck extends Check {
    // Track recent hit targets per player
    private final Map<UUID, List<HitRecord>> recentHits = new HashMap<>();
    
    // Time window to check for multi-target (milliseconds)
    private static final long TIME_WINDOW = 500; // 0.5 seconds
    
    // Max unique targets in time window before flagging
    private static final int MAX_UNIQUE_TARGETS = 2;
    
    private static class HitRecord {
        final UUID targetId;
        final long timestamp;
        
        HitRecord(UUID targetId, long timestamp) {
            this.targetId = targetId;
            this.timestamp = timestamp;
        }
    }
    
    public MultiTargetCheck() {
        super("MultiTarget", "Detects hitting multiple entities too quickly", 3);
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
        if (!(target instanceof LivingEntity)) {
            return CheckResult.pass();
        }
        
        UUID playerUuid = player.getUniqueId();
        UUID targetUuid = target.getUniqueId();
        long currentTime = System.currentTimeMillis();
        
        // Get or create hit history for this player
        List<HitRecord> hits = recentHits.computeIfAbsent(playerUuid, k -> new ArrayList<>());
        
        // Add current hit
        hits.add(new HitRecord(targetUuid, currentTime));
        
        // Remove hits older than time window
        hits.removeIf(hit -> currentTime - hit.timestamp > TIME_WINDOW);
        
        // Count unique targets in time window
        Set<UUID> uniqueTargets = new HashSet<>();
        for (HitRecord hit : hits) {
            uniqueTargets.add(hit.targetId);
        }
        

        // Check if hitting too many different targets
        if (uniqueTargets.size() > MAX_UNIQUE_TARGETS) {
            int violationLevel = calculateViolationLevel(uniqueTargets.size(), hits.size());
            
            return CheckResult.fail(
                String.format("Hit %d different targets in %.1fs (%d total hits)",
                    uniqueTargets.size(),
                    TIME_WINDOW / 1000.0,
                    hits.size()),
                violationLevel
            );
        }
        
        return CheckResult.pass();
    }
    
    /**
     * Calculate violation severity based on targets and hit count
     */
    private int calculateViolationLevel(int uniqueTargets, int totalHits) {
        // More targets = more blatant
        if (uniqueTargets >= 5) return 5; // 5+ targets = blatant killaura
        if (uniqueTargets >= 4) return 4; // 4 targets = very suspicious
        if (uniqueTargets >= 3) return 3; // 3 targets = suspicious
        
        // Also factor in total hit count
        if (totalHits >= 10) return Math.min(5, calculateViolationLevel(uniqueTargets, 0) + 1);
        
        return 2;
    }
    
    /**
     * Clean up player data
     */
    public void cleanup(UUID uuid) {
        recentHits.remove(uuid);
    }
}
