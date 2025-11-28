package com.roach.antieesa.checks;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;


public abstract class Check {
    private final String name;
    private final String description;
    private boolean enabled;
    private int violationThreshold;


    public Check(String name, String description, int violationThreshold) {
        this.name = name;
        this.description = description;
        this.violationThreshold = violationThreshold;
        this.enabled = true;
    }


    public abstract CheckResult check(Player player, Event event);

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getViolationThreshold() {
        return violationThreshold;
    }

    public void setViolationThreshold(int violationThreshold) {
        this.violationThreshold = violationThreshold;
    }
}
