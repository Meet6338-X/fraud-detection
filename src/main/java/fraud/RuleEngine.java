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

        // Amount rule - flag amounts over $5000 as high risk
        if (enabledRules.getOrDefault("amount_rule", true)) {
            double amount = transaction.getAmount();
            if (amount > 10000) {
                riskScore += 60;
                reasons.add("Critically high transaction amount: $" + String.format("%.2f", amount));
                triggeredRules.add("amount_rule");
            } else if (amount > 5000) {
                riskScore += 40;
                reasons.add("Very high transaction amount: $" + String.format("%.2f", amount));
                triggeredRules.add("amount_rule");
            } else if (amount > 2000) {
                riskScore += 15;
                reasons.add("High transaction amount: $" + String.format("%.2f", amount));
                triggeredRules.add("amount_rule");
            }
        }

        // Location rule - flag unusual countries
        if (enabledRules.getOrDefault("location_rule", true)) {
            Location location = transaction.getLocation();
            if (location != null && location.getCountry() != null) {
                String country = location.getCountry().toUpperCase();
                // Flag high-risk countries
                if (country.equals("RU") || country.equals("CN") || country.equals("KP") || 
                    country.equals("IR") || country.equals("SY") || country.equals("BY")) {
                    riskScore += 35;
                    reasons.add("High-risk country: " + country);
                    triggeredRules.add("location_rule");
                } else if (country.equals("IN") || country.equals("BR") || country.equals("NG")) {
                    riskScore += 15;
                    reasons.add("Moderate-risk country: " + country);
                    triggeredRules.add("location_rule");
                }
            }
        }

        // New account rule - flag unknown users (simplified check)
        if (enabledRules.getOrDefault("new_account_rule", true)) {
            String userId = transaction.getUserId();
            if (userId != null) {
                // Check for suspicious user IDs
                if (userId.toLowerCase().contains("unknown") || 
                    userId.toLowerCase().contains("test") ||
                    userId.toLowerCase().contains("guest")) {
                    riskScore += 20;
                    reasons.add("Unknown or suspicious user ID: " + userId);
                    triggeredRules.add("new_account_rule");
                }
            }
        }

        // Velocity rule - flag multiple rapid transactions (simplified)
        if (enabledRules.getOrDefault("velocity_rule", true)) {
            // In a real system, this would check transaction history
            // For now, flag transactions from certain merchants
            String merchant = transaction.getMerchantId();
            if (merchant != null && merchant.toLowerCase().contains("test")) {
                riskScore += 15;
                reasons.add("Test merchant detected");
                triggeredRules.add("velocity_rule");
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
