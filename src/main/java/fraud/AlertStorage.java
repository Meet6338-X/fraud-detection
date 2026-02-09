package fraud;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory storage for fraud alerts with CRUD operations.
 */
public class AlertStorage {
    private final Map<String, FraudAlert> alerts = new ConcurrentHashMap<>();

    /**
     * Add a new alert.
     */
    public FraudAlert addAlert(FraudAlert alert) {
        if (alert.getAlertId() == null) {
            alert.setAlertId("ALT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        }
        if (alert.getTimestamp() == null) {
            alert.setTimestamp(Instant.now());
        }
        if (alert.getCreatedAt() == null) {
            alert.setCreatedAt(Instant.now());
        }
        if (alert.getStatus() == null) {
            alert.setStatus("NEW");
        }
        if (alert.getTriggeredRules() == null) {
            alert.setTriggeredRules(Collections.emptyList());
        }
        if (alert.getDescription() == null && alert.getReasons() != null) {
            alert.setDescription(String.join("; ", alert.getReasons()));
        }

        // Set alert type and severity based on triggered rules and risk score
        if (alert.getAlertType() == null && alert.getTriggeredRules() != null && !alert.getTriggeredRules().isEmpty()) {
            String firstRule = alert.getTriggeredRules().get(0).toUpperCase();
            if (firstRule.contains("AMOUNT")) {
                alert.setAlertType("AMOUNT");
            } else if (firstRule.contains("VELOCITY")) {
                alert.setAlertType("VELOCITY");
            } else if (firstRule.contains("LOCATION")) {
                alert.setAlertType("LOCATION");
            } else {
                alert.setAlertType("HIGH_RISK");
            }
        }

        // Set severity based on risk score
        if (alert.getSeverity() == null) {
            if (alert.getRiskScore() >= 80) {
                alert.setSeverity("CRITICAL");
            } else if (alert.getRiskScore() >= 60) {
                alert.setSeverity("HIGH");
            } else if (alert.getRiskScore() >= 40) {
                alert.setSeverity("MEDIUM");
            } else {
                alert.setSeverity("LOW");
            }
        }

        alerts.put(alert.getAlertId(), alert);
        return alert;
    }

    /**
     * Get alert by ID.
     */
    public FraudAlert getAlert(String alertId) {
        return alerts.get(alertId);
    }

    /**
     * Get all alerts.
     */
    public List<FraudAlert> getAllAlerts() {
        return new ArrayList<>(alerts.values());
    }

    /**
     * Delete alert by ID.
     */
    public boolean deleteAlert(String alertId) {
        return alerts.remove(alertId) != null;
    }

    /**
     * Update alert status.
     */
    public boolean updateAlertStatus(String alertId, String status) {
        FraudAlert alert = alerts.get(alertId);
        if (alert != null) {
            alert.setStatus(status);
            return true;
        }
        return false;
    }

    /**
     * Resolve alert.
     */
    public boolean resolveAlert(String alertId, String resolution) {
        FraudAlert alert = alerts.get(alertId);
        if (alert != null) {
            alert.setStatus("RESOLVED");
            alert.setResolution(resolution);
            return true;
        }
        return false;
    }

    /**
     * Get alert count.
     */
    public int getAlertCount() {
        return alerts.size();
    }

    /**
     * Get alerts by status.
     */
    public List<FraudAlert> getAlertsByStatus(String status) {
        List<FraudAlert> result = new ArrayList<>();
        for (FraudAlert alert : alerts.values()) {
            if (status.equals(alert.getStatus())) {
                result.add(alert);
            }
        }
        return result;
    }

    /**
     * Clear all alerts.
     */
    public void clear() {
        alerts.clear();
    }
}
