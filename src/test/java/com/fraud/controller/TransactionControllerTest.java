package com.fraud.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fraud.model.FraudDecision;
import com.fraud.model.Transaction;
import com.fraud.service.TransactionAnalysisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for TransactionController.
 */
@ExtendWith(MockitoExtension.class)
class TransactionControllerTest {

    @Mock
    private TransactionAnalysisService analysisService;

    @InjectMocks
    private TransactionController controller;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void testAnalyzeTransaction_Success() throws Exception {
        // Arrange
        Transaction transaction = createTestTransaction();
        FraudDecision decision = FraudDecision.approve("txn-123", 25.0);
        when(analysisService.analyzeTransaction(any(Transaction.class))).thenReturn(decision);

        // Act & Assert
        mockMvc.perform(post("/api/v1/transactions/analyze")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(transaction)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").value("txn-123"))
                .andExpect(jsonPath("$.decision").value("APPROVED"))
                .andExpect(jsonPath("$.riskScore").value(25.0));
    }

    @Test
    void testAnalyzeTransaction_Blocked() throws Exception {
        // Arrange
        Transaction transaction = createTestTransaction();
        FraudDecision decision = FraudDecision.block("txn-123", 85.0,
                List.of("High amount detected"), List.of("AMOUNT_RULE"));
        when(analysisService.analyzeTransaction(any(Transaction.class))).thenReturn(decision);

        // Act & Assert
        mockMvc.perform(post("/api/v1/transactions/analyze")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(transaction)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decision").value("BLOCKED"))
                .andExpect(jsonPath("$.riskScore").value(85.0))
                .andExpect(jsonPath("$.reasons[0]").value("High amount detected"));
    }

    @Test
    void testAnalyzeTransaction_Review() throws Exception {
        // Arrange
        Transaction transaction = createTestTransaction();
        FraudDecision decision = FraudDecision.review("txn-123", 60.0,
                List.of("Unusual location"), List.of("LOCATION_RULE"));
        when(analysisService.analyzeTransaction(any(Transaction.class))).thenReturn(decision);

        // Act & Assert
        mockMvc.perform(post("/api/v1/transactions/analyze")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(transaction)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decision").value("REVIEW"))
                .andExpect(jsonPath("$.triggeredRules[0]").value("LOCATION_RULE"));
    }

    @Test
    void testHealthEndpoint() throws Exception {
        // Arrange
        when(analysisService.getStats())
                .thenReturn(new TransactionAnalysisService.RuleEngineStats(4, 4));

        // Act & Assert
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.rules.enabled").value(4))
                .andExpect(jsonPath("$.rules.total").value(4));
    }

    @Test
    void testBatchAnalysis() throws Exception {
        // Arrange
        List<Transaction> transactions = List.of(
                createTestTransaction(),
                createTestTransaction());

        Map<String, FraudDecision> decisions = Map.of(
                "txn-123", FraudDecision.approve("txn-123", 20.0),
                "txn-124", FraudDecision.approve("txn-124", 30.0));

        when(analysisService.analyzeTransaction(any(Transaction.class)))
                .thenReturn(FraudDecision.approve("txn-123", 20.0))
                .thenReturn(FraudDecision.approve("txn-124", 30.0));

        // Act & Assert
        mockMvc.perform(post("/api/v1/transactions/analyze/batch")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(transactions)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.txn-123").exists())
                .andExpect(jsonPath("$.txn-124").exists());
    }

    private Transaction createTestTransaction() {
        return new Transaction.Builder()
                .transactionId("txn-123")
                .userId("user-456")
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .timestamp(Instant.now())
                .merchantId("merchant-789")
                .build();
    }
}
