package fraud;

import java.util.*;

/**
 * Main fraud detection rule engine that evaluates all rules and makes
 * decisions.
 */
public class RuleEngine {

    private final Map<String, Boolean> enabledRules;
    private final Map<String, Integer> ruleThresholds;

    // Risk score thresholds
    private double highRiskThreshold = 75.0;
    private double mediumRiskThreshold = 50.0;

    public RuleEngine() {
        this.enabledRules = new HashMap<>();
        this.ruleThresholds = new HashMap<>();
        initializeDefaultRules();
    }

    private void initializeDefaultRules() {
        enabledRules.put("amount_rule", true);
        enabledRules.put("velocity_rule", true);
        enabledRules.put("location_rule", true);
        enabledRules.put("new_account_rule", true);

        ruleThresholds.put("amount_rule", 1000); // $1000 threshold
        ruleThresholds.put("velocity_rule", 5); // 5 transactions per minute
        ruleThresholds.put("location_rule", 500); // 500km distance threshold
        ruleThresholds.put("new_account_rule", 7); // 7 days for new account
    }

    /**
     * Analyze a transaction for fraud.
     */
    public FraudDecision analyze(Transaction transaction) {
        List<String> reasons = new ArrayList<>();
        List<String> triggeredRules = new ArrayList<>();
        double riskScore = 0;

        // Amount rule
        if (enabledRules.getOrDefault("amount_rule", true)) {
            if (transaction.getAmount() > ruleThresholds.getOrDefault("amount_rule", 1000)) {
                riskScore += 30;
                reasons.add(
                        "Transaction amount $" + String.format("%.2f", transaction.getAmount()) + " exceeds threshold");
                triggeredRules.add("amount_rule");
            }
        }

        // Velocity rule (random for demo)
        if (enabledRules.getOrDefault("velocity_rule", true)) {
            if (Math.random() < 0.1) { // 10% chance of triggering
                riskScore += 25;
                reasons.add("High transaction velocity detected");
                triggeredRules.add("velocity_rule");
            }
        }

        // Location rule
        if (enabledRules.getOrDefault("location_rule", true)) {
            if (Math.random() < 0.05) { // 5% chance of triggering
                riskScore += 35;
                reasons.add("Unusual location detected");
                triggeredRules.add("location_rule");
            }
        }

        // New account rule
        if (enabledRules.getOrDefault("new_account_rule", true)) {
            if (Math.random() < 0.08) { // 8% chance of triggering
                riskScore += 20;
                reasons.add("New account with high-risk transaction");
                triggeredRules.add("new_account_rule");
            }
        }

        // Cap risk score at 100
        riskScore = Math.min(riskScore, 100);

        boolean isFraud = riskScore >= 50;

        return new FraudDecision(
                transaction.getTransactionId(),
                isFraud,
                riskScore,
                reasons.isEmpty() ? List.of("Transaction passed all checks") : reasons,
                triggeredRules);
    }

    /**
     * Analyze a transaction for fraud (full method name).
     */
    public FraudDecision analyzeTransaction(Transaction transaction) {
        return analyze(transaction);
    }

    /**
     * Get all enabled rules.
     */
    public Map<String, Boolean> getEnabledRules() {
        return new HashMap<>(enabledRules);
    }

    /**
     * Get count of enabled rules.
     */
    public int getEnabledRulesCount() {
        int count = 0;
        for (Boolean enabled : enabledRules.values()) {
            if (enabled)
                count++;
        }
        return count;
    }

    /**
     * Get total rule count.
     */
    public int getTotalRulesCount() {
        return enabledRules.size();
    }

    /**
     * Enable a rule by name.
     */
    public boolean setRuleEnabled(String ruleName, boolean enabled) {
        String key = ruleName.toLowerCase().replace(" ", "_");
        if (enabledRules.containsKey(key)) {
            enabledRules.put(key, enabled);
            return true;
        }
        return false;
    }

    /**
     * Set threshold for a rule.
     */
    public boolean setRuleThreshold(String ruleName, int threshold) {
        String key = ruleName.toLowerCase().replace(" ", "_");
        if (ruleThresholds.containsKey(key)) {
            ruleThresholds.put(key, threshold);
            return true;
        }
        return false;
    }

    /**
     * Get rule threshold.
     */
    public int getRuleThreshold(String ruleName) {
        String key = ruleName.toLowerCase().replace(" ", "_");
        return ruleThresholds.getOrDefault(key, 0);
    }
}
