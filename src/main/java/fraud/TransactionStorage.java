package fraud;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * In-memory storage for transactions with CRUD operations.
 * Thread-safe implementation using ConcurrentHashMap.
 */
public class TransactionStorage {
    private final Map<String, Transaction> transactions = new ConcurrentHashMap<>();
    private final Deque<String> transactionOrder = new ConcurrentLinkedDeque<>();
    private static final int MAX_TRANSACTIONS = 10000;

    /**
     * Create a new transaction
     */
    public Transaction addTransaction(Transaction transaction) {
        if (transaction.getTransactionId() == null) {
            transaction.setTransactionId("txn-" + UUID.randomUUID().toString().substring(0, 8));
        }
        if (transaction.getTimestamp() == null) {
            transaction.setTimestamp(java.time.Instant.now());
        }

        transactions.put(transaction.getTransactionId(), transaction);
        transactionOrder.addLast(transaction.getTransactionId());

        // Cleanup old transactions if we exceed the limit
        while (transactionOrder.size() > MAX_TRANSACTIONS) {
            String oldId = transactionOrder.pollFirst();
            if (oldId != null) {
                transactions.remove(oldId);
            }
        }

        return transaction;
    }

    /**
     * Get transaction by ID
     */
    public Transaction getTransaction(String transactionId) {
        return transactions.get(transactionId);
    }

    /**
     * Get all transactions
     */
    public List<Transaction> getAllTransactions() {
        List<Transaction> result = new ArrayList<>();
        for (String id : transactionOrder) {
            Transaction txn = transactions.get(id);
            if (txn != null) {
                result.add(txn);
            }
        }
        return result;
    }

    /**
     * Get transactions by user ID
     */
    public List<Transaction> getTransactionsByUser(String userId) {
        List<Transaction> result = new ArrayList<>();
        for (Transaction txn : transactions.values()) {
            if (userId.equals(txn.getUserId())) {
                result.add(txn);
            }
        }
        return result;
    }

    /**
     * Update transaction
     */
    public boolean updateTransaction(String transactionId, Transaction updated) {
        Transaction existing = transactions.get(transactionId);
        if (existing == null) {
            return false;
        }

        if (updated.getAmount() > 0) {
            existing.setAmount(updated.getAmount());
        }
        if (updated.getLocation() != null) {
            existing.setLocation(updated.getLocation());
        }
        if (updated.getMerchantId() != null) {
            existing.setMerchantId(updated.getMerchantId());
        }

        return true;
    }

    /**
     * Delete transaction
     */
    public boolean deleteTransaction(String transactionId) {
        Transaction removed = transactions.remove(transactionId);
        if (removed != null) {
            transactionOrder.remove(transactionId);
            return true;
        }
        return false;
    }

    /**
     * Get transaction count
     */
    public int getTransactionCount() {
        return transactions.size();
    }

    /**
     * Get fraud pattern statistics
     */
    public Map<String, Object> getPatternStats() {
        Map<String, Object> stats = new LinkedHashMap<>();

        // Hourly distribution
        int[] hourlyDistribution = new int[24];
        int[] dailyDistribution = new int[7];
        Map<String, Integer> amountRanges = new LinkedHashMap<>();
        amountRanges.put("0-100", 0);
        amountRanges.put("101-500", 0);
        amountRanges.put("501-1000", 0);
        amountRanges.put("1001-5000", 0);
        amountRanges.put("5001-10000", 0);
        amountRanges.put("10000+", 0);

        for (Transaction txn : transactions.values()) {
            if (txn.getTimestamp() != null) {
                java.time.Instant ts = txn.getTimestamp();
                java.time.ZoneId zone = java.time.ZoneId.systemDefault();
                java.time.LocalDateTime local = ts.atZone(zone).toLocalDateTime();

                hourlyDistribution[local.getHour()]++;
                dailyDistribution[local.getDayOfWeek().getValue() - 1]++;
            }

            // Amount ranges
            double amount = txn.getAmount();
            if (amount <= 100) {
                amountRanges.put("0-100", amountRanges.get("0-100") + 1);
            } else if (amount <= 500) {
                amountRanges.put("101-500", amountRanges.get("101-500") + 1);
            } else if (amount <= 1000) {
                amountRanges.put("501-1000", amountRanges.get("501-1000") + 1);
            } else if (amount <= 5000) {
                amountRanges.put("1001-5000", amountRanges.get("1001-5000") + 1);
            } else if (amount <= 10000) {
                amountRanges.put("5001-10000", amountRanges.get("5001-10000") + 1);
            } else {
                amountRanges.put("10000+", amountRanges.get("10000+") + 1);
            }
        }

        stats.put("hourlyDistribution", hourlyDistribution);
        stats.put("dailyDistribution", dailyDistribution);
        stats.put("amountRanges", amountRanges);

        return stats;
    }

    /**
     * Get country distribution
     */
    public Map<String, Integer> getCountryDistribution() {
        Map<String, Integer> countryMap = new LinkedHashMap<>();
        for (Transaction txn : transactions.values()) {
            if (txn.getLocation() != null && txn.getLocation().getCountry() != null) {
                String country = txn.getLocation().getCountry();
                countryMap.put(country, countryMap.getOrDefault(country, 0) + 1);
            }
        }
        return countryMap;
    }

    /**
     * Clear all transactions
     */
    public void clear() {
        transactions.clear();
        transactionOrder.clear();
    }
}
