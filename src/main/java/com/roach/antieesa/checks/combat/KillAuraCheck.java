package com.roach.antieesa.checks.combat;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.roach.antieesa.AntiEesa;
import com.roach.antieesa.checks.Check;
import com.roach.antieesa.checks.CheckResult;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Detects Killaura... I'm watching you eesa
 */
public class KillAuraCheck extends Check {
    // Track last flying packet time per player
    private final Map<UUID, Long> lastFlyingPacket = new HashMap<>();

    // Track violations per player (for threshold before flagging)
    private final Map<UUID, Integer> violations = new HashMap<>();

    public KillAuraCheck() {
        super("KillAura", "Detects killaura by packet timing", 5);

        // Register packet listener
        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(
                AntiEesa.getInstance(),
                PacketType.Play.Client.POSITION,
                PacketType.Play.Client.LOOK,
                PacketType.Play.Client.POSITION_LOOK,
                PacketType.Play.Client.FLYING,
                PacketType.Play.Client.USE_ENTITY) {

            @Override
            public void onPacketReceiving(PacketEvent event) {
                Player player = event.getPlayer();
                UUID uuid = player.getUniqueId();

                // Skip if player has bypass
                if (player.hasPermission("anticheat.bypass")) {
                    return;
                }

                // Check if this is an attack packet
                if (event.getPacketType().equals(PacketType.Play.Client.USE_ENTITY)) {
                    long timeSinceFlying = System.currentTimeMillis() -
                            lastFlyingPacket.getOrDefault(uuid, 0L);


                    if (timeSinceFlying < 5) {
                        int currentViolations = violations.getOrDefault(uuid, 0) + 1;
                        violations.put(uuid, currentViolations);

                        // If violations exceed threshold, flag it
                        if (currentViolations > 10) {
                            handlePacketViolation(player, timeSinceFlying);
                            violations.put(uuid, 0); // Reset after flagging
                        }
                    } else {
                        violations.put(uuid, Math.max(0, violations.getOrDefault(uuid, 0) - 1));
                    }
                } else {
                    // Update last flying packet time for movement packets
                    lastFlyingPacket.put(uuid, System.currentTimeMillis());
                }
            }
        });
    }

    @Override
    public CheckResult check(Player player, Event event) {
        return CheckResult.pass();
    }


    private void handlePacketViolation(Player player, long timeDiff) {
        // Create a violation result
        CheckResult result = CheckResult.fail(
                String.format("Attack packet sent %dms after flying packet (expected >5ms)", timeDiff),
                4  // High severity - packet manipulation is pretty blatant
        );

        // Manually trigger violation handling through the managers
        boolean shouldLog = AntiEesa.getInstance().getViolationManager()
                .recordViolation(player, this, result);

        if (shouldLog) {
            int totalViolations = AntiEesa.getInstance().getViolationManager()
                    .getViolationCount(player.getUniqueId(), getName());

            AntiEesa.getInstance().getViolationLogger()
                    .logViolation(player, this, result, totalViolations);
        }
    }


    public void cleanup(UUID uuid) {
        lastFlyingPacket.remove(uuid);
        violations.remove(uuid);
    }
}