package fraud;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * HTTP handler for static files (HTML, CSS, JS).
 */
public class FraudHttpHandler implements HttpHandler {

    private static final Map<String, String> CONTENT_TYPES = Map.of(
            ".html", "text/html",
            ".css", "text/css",
            ".js", "application/javascript",
            ".json", "application/json",
            ".png", "image/png",
            ".jpg", "image/jpeg",
            ".svg", "image/svg+xml");

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();

        if (path.equals("/") || path.isEmpty()) {
            path = "/index.html";
        }

        // Remove leading slash for file lookup
        String filePath = "src/main/resources/static" + path;
        Path resolvedPath = Path.of(filePath).toAbsolutePath();

        // Security check - ensure path is within static directory
        if (!resolvedPath
                .startsWith(Path.of(System.getProperty("user.dir"), "src/main/resources/static").toAbsolutePath())) {
            sendError(exchange, 403, "Forbidden");
            return;
        }

        try {
            if (Files.exists(resolvedPath) && Files.isRegularFile(resolvedPath)) {
                byte[] content = Files.readAllBytes(resolvedPath);
                String ext = getFileExtension(path);
                String contentType = CONTENT_TYPES.getOrDefault(ext, "application/octet-stream");

                exchange.getResponseHeaders().set("Content-Type", contentType);
                exchange.getResponseHeaders().set("Cache-Control", "no-cache");
                exchange.sendResponseHeaders(200, content.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(content);
                }
            } else {
                sendError(exchange, 404, "Not Found: " + path);
            }
        } catch (IOException e) {
            sendError(exchange, 500, "Internal Server Error: " + e.getMessage());
        }
    }

    private String getFileExtension(String path) {
        int lastDot = path.lastIndexOf('.');
        return lastDot > 0 ? path.substring(lastDot) : "";
    }

    private void sendError(HttpExchange exchange, int code, String message) throws IOException {
        exchange.sendResponseHeaders(code, message.length());
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(message.getBytes());
        }
    }
}
