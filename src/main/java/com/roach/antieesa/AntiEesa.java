package com.roach.antieesa;

import com.roach.antieesa.checks.CheckManager;
import com.roach.antieesa.logging.ViolationLogger;
import com.roach.antieesa.violations.ViolationManager;
import org.bukkit.plugin.java.JavaPlugin;


/**
 *AntiEesa - Silent log anticheat
 */
public class AntiEesa extends JavaPlugin {
    private static AntiEesa instance;
    private CheckManager checkManager;
    private ViolationManager violationManager;
    private ViolationLogger logger;



    @Override
    public void onEnable() {
        instance = this;

        this.violationManager = new ViolationManager();
        this.checkManager = new CheckManager(this, violationManager, logger);
        this.logger = new ViolationLogger(this);

        getServer().getPluginManager().registerEvents(checkManager, this);

        registerChecks();

        getLogger().info("AntiEesa is enabled");
    }

    @Override
    public void onDisable() {
        getLogger().info("AntiEesa is disabled");
    }

    private void registerChecks() {
        //Checks added like this:
        //checkManager.registerCheck(new CheckName());

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

}
























