package com.roach.antieesa.violations;

import com.roach.antieesa.checks.Check;
import com.roach.antieesa.checks.CheckResult;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ViolationManager {
    // Player UUID -> Check Name -> Violation Count
    private final Map<UUID, Map<String, Integer>> violations;

    // Player UUID -> Check Name -> Last Violation Time
    private final Map<UUID, Map<String, Long>> lastViolationTime;

    private static final long DECAY_TIME = 30000; // 30 seconds

    public ViolationManager() {
        this.violations = new ConcurrentHashMap<>();
        this.lastViolationTime = new ConcurrentHashMap<>();
    }


    public boolean recordViolation(Player player, Check check, CheckResult result) {
        UUID uuid = player.getUniqueId();
        String checkName = check.getName();

        violations.putIfAbsent(uuid, new ConcurrentHashMap<>());
        lastViolationTime.putIfAbsent(uuid, new ConcurrentHashMap<>());

        Map<String, Integer> playerViolations = violations.get(uuid);
        Map<String, Long> playerTimes = lastViolationTime.get(uuid);

        // Check for decay
        long currentTime = System.currentTimeMillis();
        long lastTime = playerTimes.getOrDefault(checkName, 0L);

        if (currentTime - lastTime > DECAY_TIME) {
            // Decay violations over time
            int currentViolations = playerViolations.getOrDefault(checkName, 0);
            if (currentViolations > 0) {
                playerViolations.put(checkName, Math.max(0, currentViolations - 1));
            }
        }

        int newCount = playerViolations.getOrDefault(checkName, 0) + 1;
        playerViolations.put(checkName, newCount);
        playerTimes.put(checkName, currentTime);

        return newCount >= check.getViolationThreshold();
    }


    public int getViolationCount(UUID uuid, String checkName) {
        return violations.getOrDefault(uuid, new HashMap<>())
                .getOrDefault(checkName, 0);
    }


    public void clearViolations(UUID uuid) {
        violations.remove(uuid);
        lastViolationTime.remove(uuid);
    }

    public void clearViolations(UUID uuid, String checkName) {
        Map<String, Integer> playerViolations = violations.get(uuid);
        if (playerViolations != null) {
            playerViolations.remove(checkName);
        }

        Map<String, Long> playerTimes = lastViolationTime.get(uuid);
        if (playerTimes != null) {
            playerTimes.remove(checkName);
        }
    }


    public Map<String, Integer> getAllViolations(UUID uuid) {
        return new HashMap<>(violations.getOrDefault(uuid, new HashMap<>()));
    }
}