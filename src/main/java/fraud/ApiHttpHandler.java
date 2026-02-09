package fraud;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

/**
 * API handler for all /api/* endpoints.
 */
public class ApiHttpHandler implements HttpHandler {

    private final TransactionStorage transactionStorage;
    private final AlertStorage alertStorage;
    private final RuleEngine ruleEngine;
    private final JsonHelper jsonHelper;

    public ApiHttpHandler() {
        this.transactionStorage = new TransactionStorage();
        this.alertStorage = new AlertStorage();
        this.ruleEngine = new RuleEngine();
        this.jsonHelper = new JsonHelper();

        // Initialize with sample transactions
        initializeSampleData();
    }

    private void initializeSampleData() {
        // Add sample transactions
        String[] users = { "user-001", "user-002", "user-003", "user-004", "user-005" };
        String[] cities = { "New York", "Los Angeles", "Chicago", "Houston", "Miami" };
        String[] countries = { "US", "US", "US", "US", "US" };
        String[] merchants = { "Amazon", "Walmart", "Target", "BestBuy", "Costco" };

        Random random = new Random(42);
        for (int i = 0; i < 100; i++) {
            Transaction txn = new Transaction();
            txn.setTransactionId("TXN-" + String.format("%04d", i + 1));
            txn.setUserId(users[random.nextInt(users.length)]);
            txn.setAmount(10 + random.nextDouble() * 990);
            txn.setCurrency("USD");
            txn.setMerchantId(merchants[random.nextInt(merchants.length)]);
            txn.setTimestamp(Instant.now().minusSeconds(random.nextInt(604800))); // Last week
            txn.setLocation(
                    new Location(cities[random.nextInt(cities.length)], countries[random.nextInt(countries.length)]));

            transactionStorage.addTransaction(txn);
        }
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        System.out.println("API Request: " + method + " " + path);

        try {
            if (path.equals("/api/transactions") && method.equals("GET")) {
                handleGetTransactions(exchange);
            } else if (path.equals("/api/transactions") && method.equals("POST")) {
                handleCreateTransaction(exchange);
            } else if (path.startsWith("/api/transactions/") && method.equals("GET")) {
                String id = path.substring("/api/transactions/".length());
                handleGetTransaction(exchange, id);
            } else if (path.startsWith("/api/transactions/") && method.equals("DELETE")) {
                String id = path.substring("/api/transactions/".length());
                handleDeleteTransaction(exchange, id);
            } else if (path.equals("/api/transactions/analyze") && method.equals("POST")) {
                handleAnalyzeTransaction(exchange);
            } else if (path.equals("/api/alerts") && method.equals("GET")) {
                handleGetAlerts(exchange);
            } else if (path.startsWith("/api/alerts/") && method.equals("DELETE")) {
                String id = path.substring("/api/alerts/".length());
                handleDeleteAlert(exchange, id);
            } else if (path.equals("/api/rules") && method.equals("GET")) {
                handleGetRules(exchange);
            } else if (path.equals("/api/rules") && method.equals("PUT")) {
                handleUpdateRule(exchange);
            } else if (path.equals("/api/stats/patterns") && method.equals("GET")) {
                handleGetPatternStats(exchange);
            } else if (path.equals("/api/stats/geography") && method.equals("GET")) {
                handleGetGeographyStats(exchange);
            } else if (path.equals("/api/stats") && method.equals("GET")) {
                handleGetStats(exchange);
            } else {
                sendJsonResponse(exchange, 404, Map.of("error", "Not found: " + path));
            }
        } catch (Exception e) {
            System.err.println("Error handling request: " + e.getMessage());
            e.printStackTrace();
            sendJsonResponse(exchange, 500, Map.of("error", e.getMessage()));
        }
    }

    private void handleGetTransactions(HttpExchange exchange) {
        List<Transaction> transactions = transactionStorage.getAllTransactions();
        sendJsonResponse(exchange, 200, transactions);
    }

    private void handleCreateTransaction(HttpExchange exchange) throws IOException {
        String body = readRequestBody(exchange);
        Transaction txn = jsonHelper.fromJson(body, Transaction.class);

        if (txn.getTransactionId() == null || txn.getTransactionId().isEmpty()) {
            txn.setTransactionId("TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        }
        txn.setTimestamp(Instant.now());

        transactionStorage.addTransaction(txn);

        // Analyze the transaction
        FraudDecision decision = ruleEngine.analyze(txn);
        if (decision.isFraud()) {
            FraudAlert alert = new FraudAlert();
            alert.setAlertId("ALT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            alert.setTransactionId(txn.getTransactionId());
            alert.setUserId(txn.getUserId());
            alert.setAmount(txn.getAmount());
            alert.setRiskScore(decision.getRiskScore());
            alert.setReasons(decision.getReasons());
            alert.setTimestamp(Instant.now());
            alert.setStatus("NEW");
            alertStorage.addAlert(alert);
        }

        sendJsonResponse(exchange, 201, txn);
    }

    private void handleGetTransaction(HttpExchange exchange, String id) {
        Transaction txn = transactionStorage.getTransaction(id);
        if (txn != null) {
            sendJsonResponse(exchange, 200, txn);
        } else {
            sendJsonResponse(exchange, 404, Map.of("error", "Transaction not found: " + id));
        }
    }

    private void handleDeleteTransaction(HttpExchange exchange, String id) {
        boolean deleted = transactionStorage.deleteTransaction(id);
        if (deleted) {
            sendJsonResponse(exchange, 200, Map.of("message", "Transaction deleted: " + id));
        } else {
            sendJsonResponse(exchange, 404, Map.of("error", "Transaction not found: " + id));
        }
    }

    private void handleAnalyzeTransaction(HttpExchange exchange) throws IOException {
        String body = readRequestBody(exchange);
        Transaction txn = jsonHelper.fromJson(body, Transaction.class);

        FraudDecision decision = ruleEngine.analyze(txn);

        Map<String, Object> response = new HashMap<>();
        response.put("decision", decision);
        response.put("timestamp", Instant.now().toString());

        sendJsonResponse(exchange, 200, response);
    }

    private void handleGetAlerts(HttpExchange exchange) {
        List<FraudAlert> alerts = alertStorage.getAllAlerts();
        sendJsonResponse(exchange, 200, alerts);
    }

    private void handleDeleteAlert(HttpExchange exchange, String id) {
        boolean deleted = alertStorage.deleteAlert(id);
        if (deleted) {
            sendJsonResponse(exchange, 200, Map.of("message", "Alert deleted: " + id));
        } else {
            sendJsonResponse(exchange, 404, Map.of("error", "Alert not found: " + id));
        }
    }

    private void handleGetRules(HttpExchange exchange) {
        List<Map<String, Object>> rules = new ArrayList<>();
        for (Map.Entry<String, Boolean> entry : ruleEngine.getEnabledRules().entrySet()) {
            Map<String, Object> rule = new HashMap<>();
            rule.put("name", entry.getKey());
            rule.put("enabled", entry.getValue());
            rules.add(rule);
        }
        sendJsonResponse(exchange, 200, rules);
    }

    private void handleUpdateRule(HttpExchange exchange) throws IOException {
        String body = readRequestBody(exchange);
        @SuppressWarnings("unchecked")
        Map<String, Object> request = jsonHelper.fromJson(body, Map.class);

        String name = (String) request.get("name");
        Boolean enabled = (Boolean) request.get("enabled");

        if (name != null && enabled != null) {
            ruleEngine.setRuleEnabled(name, enabled);
            sendJsonResponse(exchange, 200, Map.of("message", "Rule updated: " + name));
        } else {
            sendJsonResponse(exchange, 400, Map.of("error", "Invalid request: name and enabled required"));
        }
    }

    private void handleGetPatternStats(HttpExchange exchange) {
        List<Transaction> transactions = transactionStorage.getAllTransactions();

        // Hourly distribution
        Map<Integer, Integer> hourlyDist = new HashMap<>();
        for (int i = 0; i < 24; i++)
            hourlyDist.put(i, 0);

        // Amount range distribution
        Map<String, Integer> amountRanges = new HashMap<>();
        amountRanges.put("$0-$100", 0);
        amountRanges.put("$100-$500", 0);
        amountRanges.put("$500-$1000", 0);
        amountRanges.put("$1000+", 0);

        // Day of week distribution
        String[] days = { "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday" };
        Map<String, Integer> dayDist = new HashMap<>();
        for (String day : days)
            dayDist.put(day, 0);

        int fraudCount = 0;
        for (Transaction txn : transactions) {
            // Hourly
            int hour = txn.getTimestamp().atZone(java.time.ZoneId.systemDefault()).getHour();
            hourlyDist.merge(hour, 1, Integer::sum);

            // Amount range
            if (txn.getAmount() < 100)
                amountRanges.merge("$0-$100", 1, Integer::sum);
            else if (txn.getAmount() < 500)
                amountRanges.merge("$100-$500", 1, Integer::sum);
            else if (txn.getAmount() < 1000)
                amountRanges.merge("$500-$1000", 1, Integer::sum);
            else
                amountRanges.merge("$1000+", 1, Integer::sum);

            // Day of week
            String day = txn.getTimestamp().atZone(java.time.ZoneId.systemDefault()).getDayOfWeek().toString();
            dayDist.merge(day, 1, Integer::sum);
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("hourlyDistribution", hourlyDist);
        stats.put("amountRanges", amountRanges);
        stats.put("dayOfWeek", dayDist);
        stats.put("totalTransactions", transactions.size());
        stats.put("fraudCount", fraudCount);

        sendJsonResponse(exchange, 200, stats);
    }

    private void handleGetGeographyStats(HttpExchange exchange) {
        List<Transaction> transactions = transactionStorage.getAllTransactions();

        Map<String, Integer> countryFraud = new HashMap<>();
        Map<String, Integer> cityFraud = new HashMap<>();

        for (Transaction txn : transactions) {
            if (txn.getLocation() != null) {
                String country = txn.getLocation().getCountry() != null ? txn.getLocation().getCountry() : "Unknown";
                String city = txn.getLocation().getCity() != null ? txn.getLocation().getCity() : "Unknown";

                countryFraud.merge(country, 1, Integer::sum);
                cityFraud.merge(city + ", " + country, 1, Integer::sum);
            }
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("byCountry", countryFraud);
        stats.put("byCity", cityFraud);

        sendJsonResponse(exchange, 200, stats);
    }

    private void handleGetStats(HttpExchange exchange) {
        List<Transaction> transactions = transactionStorage.getAllTransactions();
        List<FraudAlert> alerts = alertStorage.getAllAlerts();

        double totalAmount = transactions.stream().mapToDouble(Transaction::getAmount).sum();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalTransactions", transactions.size());
        stats.put("totalAlerts", alerts.size());
        stats.put("totalAmount", totalAmount);
        stats.put("enabledRules", ruleEngine.getEnabledRules().size());

        sendJsonResponse(exchange, 200, stats);
    }

    private String readRequestBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private void sendJsonResponse(HttpExchange exchange, int status, Object data) {
        try {
            String json = jsonHelper.toJson(data);
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        } catch (IOException e) {
            System.err.println("Error sending response: " + e.getMessage());
        }
    }
}
