package fraud;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

/**
 * Pure Java Fraud Detection Application using built-in HttpServer.
 * No Spring Boot, no external dependencies required.
 */
public class FraudDetectionApplication {

    public static void main(String[] args) throws IOException {
        int port = 8080;
        if (args.length > 0) {
            port = Integer.parseInt(args[0]);
        }

        System.out.println("Starting Fraud Detection System on port " + port + "...");

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        // Create context for static files (HTML, CSS, JS)
        server.createContext("/", new FraudHttpHandler());

        // Create context for API endpoints
        server.createContext("/api", new ApiHttpHandler());

        // Thread pool for handling requests
        server.setExecutor(Executors.newFixedThreadPool(10));

        server.start();
        System.out.println("Fraud Detection System started successfully!");
        System.out.println("Dashboard available at: http://localhost:" + port + "/");
        System.out.println("API endpoints available at: http://localhost:" + port + "/api");
    }
}
