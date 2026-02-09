package fraud;

import java.util.List;

/**
 * Fraud decision result from the rule engine.
 */
public class FraudDecision {
    private final String transactionId;
    private final boolean fraud;
    private final double riskScore;
    private final List<String> reasons;
    private final List<String> triggeredRules;

    public FraudDecision(String transactionId, boolean fraud, double riskScore,
            List<String> reasons, List<String> triggeredRules) {
        this.transactionId = transactionId;
        this.fraud = fraud;
        this.riskScore = riskScore;
        this.reasons = reasons;
        this.triggeredRules = triggeredRules;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public boolean isFraud() {
        return fraud;
    }

    public double getRiskScore() {
        return riskScore;
    }

    public List<String> getReasons() {
        return reasons;
    }

    public List<String> getTriggeredRules() {
        return triggeredRules;
    }

    public static FraudDecision approve(String transactionId) {
        return new FraudDecision(transactionId, false, 0.0,
                List.of("Transaction passed all checks"), List.of());
    }

    public static FraudDecision review(String transactionId, double riskScore,
            List<String> reasons, List<String> triggeredRules) {
        return new FraudDecision(transactionId, riskScore > 50, riskScore, reasons, triggeredRules);
    }
}
