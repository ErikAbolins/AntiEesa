package com.roach.antieesa;

import com.roach.antieesa.checks.*;
import com.roach.antieesa.checks.combat.*;
import com.roach.antieesa.checks.movement.*;
import com.roach.antieesa.commands.ACCommand;
import com.roach.antieesa.logging.ViolationLogger;
import com.roach.antieesa.violations.ViolationManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;


/**
 *AntiEesa - Silent log anticheat
 */
public class AntiEesa extends JavaPlugin implements Listener {
    private static AntiEesa instance;
    private CheckManager checkManager;
    private ViolationManager violationManager;
    private ViolationLogger logger;



    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        instance = this;

        this.violationManager = new ViolationManager();
        this.logger = new ViolationLogger(this);
        this.checkManager = new CheckManager(this, violationManager, logger);


        getServer().getPluginManager().registerEvents(checkManager, this);

        registerChecks();
        getCommand("ac").setExecutor(new ACCommand(this));
        getCommand("ac").setTabCompleter(new ACCommand(this));

        getLogger().info("AntiEesa is enabled");
    }

    @Override
    public void onDisable() {
        getLogger().info("AntiEesa is disabled");
    }

    private void registerChecks() {
        // Movement checks
        checkManager.registerCheck(new SpeedCheck());
        
        // Combat checks
        checkManager.registerCheck(new KillAuraCheck());
        checkManager.registerCheck(new RotationAuraCheck());
        checkManager.registerCheck(new MultiTargetCheck());
        checkManager.registerCheck(new ReachCheck());

        getLogger().info("Registered " + checkManager.getChecks().size() + " checks.");
    }

    public static AntiEesa getInstance() {
        return instance;
    }
    public CheckManager getCheckManager() {
        return checkManager;
    }
    public ViolationManager getViolationManager() {
        return violationManager;
    }
    public ViolationLogger getViolationLogger() {
        return logger;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();

        // Clean up check data
        for (Check check : checkManager.getChecks()) {
            if (check instanceof SpeedCheck) {
                ((SpeedCheck) check).cleanup(uuid);
            }
            if (check instanceof KillAuraCheck) {
                ((KillAuraCheck) check).cleanup(uuid);
            }
            if (check instanceof RotationAuraCheck) {
                ((RotationAuraCheck) check).cleanup(uuid);
            }
            if (check instanceof MultiTargetCheck) {
                ((MultiTargetCheck) check).cleanup(uuid);
            }
            if (check instanceof ReachCheck) {
                ((ReachCheck) check).cleanup(uuid);
            }
        }

        // Clean up violation data
        violationManager.clearViolations(uuid);
    }

}
