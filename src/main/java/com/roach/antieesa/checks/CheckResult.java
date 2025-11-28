package com.roach.antieesa.checks;

public class CheckResult {
    private final boolean violated;
    private final String reason;
    private final int violationLevel; // 1-5: 1 = sus, 5 = blatant

    public CheckResult(boolean violated, String reason, int violationLevel) {
        this.violated = violated;
        this.reason = reason;
        this.violationLevel = Math.min(5, Math.max(1, violationLevel));
    }


    public static CheckResult pass() {
        return new CheckResult(false, "", 0);
    }

    public static CheckResult fail(String reason, int violationLevel) {
        return new CheckResult(true, reason, violationLevel);
    }

    public boolean isViolated() {
        return violated;
    }
    public String getReason() {
        return reason;
    }
    public int getViolationLevel() {
        return violationLevel;
    }

    @Override
    public String toString() {
        return violated ? String.format("[Level %d] %s", violationLevel, reason) : "PASS";
    }
}
