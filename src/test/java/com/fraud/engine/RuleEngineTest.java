package com.fraud.engine;

import com.fraud.config.AppConfig;
import com.fraud.engine.rules.AmountRule;
import com.fraud.engine.rules.LocationRule;
import com.fraud.engine.rules.NewAccountRule;
import com.fraud.engine.rules.VelocityRule;
import com.fraud.model.FraudDecision;
import com.fraud.model.Transaction;
import com.fraud.model.UserProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.fraud.cache.UserProfileCache;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the RuleEngine.
 */
@ExtendWith(MockitoExtension.class)
class RuleEngineTest {

    @Mock
    private UserProfileCache userProfileCache;

    private RuleEngine ruleEngine;
    private AppConfig appConfig;

    @BeforeEach
    void setUp() {
        appConfig = createTestAppConfig();

        List<FraudRule> rules = List.of(
                new AmountRule(),
                new LocationRule(),
                new NewAccountRule(),
                new VelocityRule(userProfileCache));

        ruleEngine = new RuleEngine(rules, appConfig);
    }

    private AppConfig createTestAppConfig() {
        AppConfig config = new AppConfig();
        AppConfig.RuleConfig ruleConfig = new AppConfig.RuleConfig();
        ruleConfig.setHighRiskThreshold(75.0);
        ruleConfig.setMediumRiskThreshold(50.0);
        ruleConfig.setMaxTransactionsPerWindow(10);
        config.setRules(ruleConfig);
        return config;
    }

    @Test
    void testNormalTransaction_Approved() {
        // Arrange
        Transaction transaction = createNormalTransaction();
        when(userProfileCache.incrementTransactionCount(anyString())).thenReturn(1L);

        // Act
        FraudDecision decision = ruleEngine.evaluate(transaction, null);

        // Assert
        assertEquals(FraudDecision.Decision.APPROVED, decision.getDecision());
        assertTrue(decision.getRiskScore() < 50);
        assertTrue(decision.getTriggeredRules().isEmpty());
    }

    @Test
    void testHighAmountTransaction_Blocked() {
        // Arrange
        Transaction transaction = createNormalTransaction();
        transaction.setAmount(new BigDecimal("15000")); // Above max threshold
        when(userProfileCache.incrementTransactionCount(anyString())).thenReturn(1L);

        // Act
        FraudDecision decision = ruleEngine.evaluate(transaction, null);

        // Assert
        assertEquals(FraudDecision.Decision.BLOCKED, decision.getDecision());
        assertTrue(decision.getRiskScore() >= 75);
        assertTrue(decision.getTriggeredRules().contains("AMOUNT_RULE"));
    }

    @Test
    void testNewUserWithHighAmount_Review() {
        // Arrange
        Transaction transaction = createNormalTransaction();
        transaction.setAmount(new BigDecimal("2000"));
        UserProfile newUser = createNewUserProfile();
        when(userProfileCache.incrementTransactionCount(anyString())).thenReturn(1L);

        // Act
        FraudDecision decision = ruleEngine.evaluate(transaction, newUser);

        // Assert
        assertTrue(decision.getRiskScore() > 0);
        assertTrue(decision.getTriggeredRules().contains("NEW_ACCOUNT_RULE"));
    }

    @Test
    void testHighVelocityTransaction_Blocked() {
        // Arrange
        Transaction transaction = createNormalTransaction();
        when(userProfileCache.incrementTransactionCount(anyString())).thenReturn(15L); // Above max

        // Act
        FraudDecision decision = ruleEngine.evaluate(transaction, null);

        // Assert
        assertEquals(FraudDecision.Decision.BLOCKED, decision.getDecision());
        assertTrue(decision.getTriggeredRules().contains("VELOCITY_RULE"));
    }

    @Test
    void testTransactionFromHighRiskCountry_Blocked() {
        // Arrange
        Transaction transaction = createNormalTransaction();
        Transaction.Location location = new Transaction.Location();
        location.setCountry("XX"); // High risk
        transaction.setLocation(location);
        when(userProfileCache.incrementTransactionCount(anyString())).thenReturn(1L);

        // Act
        FraudDecision decision = ruleEngine.evaluate(transaction, null);

        // Assert
        assertEquals(FraudDecision.Decision.BLOCKED, decision.getDecision());
        assertTrue(decision.getTriggeredRules().contains("LOCATION_RULE"));
    }

    @Test
    void testTransactionWithNoLocation_TriggersLocationRule() {
        // Arrange
        Transaction transaction = createNormalTransaction();
        transaction.setLocation(null);
        when(userProfileCache.incrementTransactionCount(anyString())).thenReturn(1L);

        // Act
        FraudDecision decision = ruleEngine.evaluate(transaction, null);

        // Assert
        assertTrue(decision.getTriggeredRules().contains("LOCATION_RULE"));
    }

    @Test
    void testProcessingTimeIsRecorded() {
        // Arrange
        Transaction transaction = createNormalTransaction();
        when(userProfileCache.incrementTransactionCount(anyString())).thenReturn(1L);

        // Act
        FraudDecision decision = ruleEngine.evaluate(transaction, null);

        // Assert
        assertTrue(decision.getProcessingTimeMs() >= 0);
    }

    @Test
    void testMultipleRulesTriggered_CumulativeRisk() {
        // Arrange
        Transaction transaction = createNormalTransaction();
        transaction.setAmount(new BigDecimal("9000")); // High amount
        Transaction.Location location = new Transaction.Location();
        location.setCountry("NK"); // High risk country
        transaction.setLocation(location);
        when(userProfileCache.incrementTransactionCount(anyString())).thenReturn(8L); // Suspicious velocity

        // Act
        FraudDecision decision = ruleEngine.evaluate(transaction, null);

        // Assert
        assertTrue(decision.getTriggeredRules().size() > 1);
        assertTrue(decision.getRiskScore() > 50);
    }

    private Transaction createNormalTransaction() {
        return new Transaction.Builder()
                .transactionId("txn-123")
                .userId("user-456")
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .timestamp(Instant.now())
                .merchantId("merchant-789")
                .merchantCategory("RETAIL")
                .ipAddress("192.168.1.1")
                .deviceId("device-abc")
                .location(new Transaction.Location("US", "New York",
                        new Transaction.Coordinates(40.7128, -74.0060)))
                .metadata(Map.of())
                .build();
    }

    private UserProfile createNewUserProfile() {
        return new UserProfile.Builder()
                .userId("user-456")
                .accountCreatedAt(Instant.now().minusSeconds(7 * 24 * 60 * 60)) // 7 days ago
                .accountStatus("ACTIVE")
                .riskLevel("LOW")
                .averageTransactionAmount(new BigDecimal("50.00"))
                .typicalLocations(List.of("US"))
                .typicalMerchants(List.of("merchant-789"))
                .typicalDeviceIds(List.of("device-abc"))
                .transactionCount24h(2)
                .totalAmount24h(new BigDecimal("100.00"))
                .flaggedTransactionsCount(0)
                .build();
    }
}
