package portal.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/** Serves the frontend from the web/ folder, guarding against path traversal. */
public class StaticHandler implements HttpHandler {
    private static final Map<String, String> TYPES = Map.of(
            "html", "text/html; charset=utf-8",
            "css", "text/css; charset=utf-8",
            "js", "text/javascript; charset=utf-8",
            "svg", "image/svg+xml",
            "png", "image/png",
            "jpg", "image/jpeg",
            "ico", "image/x-icon",
            "json", "application/json; charset=utf-8");

    private final Path root;

    public StaticHandler(Path root) {
        this.root = root.toAbsolutePath().normalize();
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        if (!ex.getRequestMethod().equalsIgnoreCase("GET") && !ex.getRequestMethod().equalsIgnoreCase("HEAD")) {
            sendText(ex, 405, "Method not allowed");
            return;
        }
        String uriPath = ex.getRequestURI().getPath();
        Path file = root.resolve(uriPath.replaceFirst("^/+", "")).normalize();
        if (!file.startsWith(root)) {
            sendText(ex, 403, "Forbidden");
            return;
        }
        if (Files.isDirectory(file)) file = file.resolve("index.html");
        if (!Files.exists(file) && !uriPath.contains(".")) file = Path.of(file + ".html"); // pretty URLs: /login -> login.html
        if (!Files.isRegularFile(file)) {
            Path notFound = root.resolve("404.html");
            if (Files.isRegularFile(notFound)) {
                write(ex, 404, TYPES.get("html"), Files.readAllBytes(notFound));
            } else {
                sendText(ex, 404, "Not found");
            }
            return;
        }
        String name = file.getFileName().toString();
        String ext = name.contains(".") ? name.substring(name.lastIndexOf('.') + 1).toLowerCase() : "";
        write(ex, 200, TYPES.getOrDefault(ext, "application/octet-stream"), Files.readAllBytes(file));
    }

    private static void write(HttpExchange ex, int status, String type, byte[] bytes) throws IOException {
        ex.getResponseHeaders().set("Content-Type", type);
        ex.getResponseHeaders().set("Cache-Control", "no-cache");
        ex.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        boolean head = ex.getRequestMethod().equalsIgnoreCase("HEAD");
        ex.sendResponseHeaders(status, head ? -1 : bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            if (!head) os.write(bytes);
        }
    }

    private static void sendText(HttpExchange ex, int status, String text) throws IOException {
        write(ex, status, "text/plain; charset=utf-8", text.getBytes(StandardCharsets.UTF_8));
    }
}
