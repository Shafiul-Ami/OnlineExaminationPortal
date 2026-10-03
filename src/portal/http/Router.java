package portal.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import portal.auth.Role;
import portal.auth.Sessions;

/** Tiny REST router: matches METHOD + /path/{param}, enforces roles, writes JSON responses. */
public class Router implements HttpHandler {

    @FunctionalInterface
    public interface Handler {
        Object handle(Request req) throws Exception;
    }

    private record Route(String method, String[] parts, Role role, boolean authOnly, Handler handler) {}

    private final List<Route> routes = new ArrayList<>();

    /** Public route. */
    public Router get(String p, Handler h) { return add("GET", p, null, false, h); }
    public Router post(String p, Handler h) { return add("POST", p, null, false, h); }

    /** Route for any logged-in user. */
    public Router authed(String method, String p, Handler h) { return add(method, p, null, true, h); }

    /** Route restricted to one role. */
    public Router role(Role role, String method, String p, Handler h) { return add(method, p, role, true, h); }

    private Router add(String method, String pattern, Role role, boolean authOnly, Handler h) {
        routes.add(new Route(method, split(pattern), role, authOnly, h));
        return this;
    }

    private static String[] split(String path) {
        String trimmed = path.replaceAll("^/+|/+$", "");
        return trimmed.isEmpty() ? new String[0] : trimmed.split("/");
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        Request req = new Request(ex);
        int status = 200;
        Object result;
        try {
            result = dispatch(req);
            if (result == null) result = Map.of("ok", true);
        } catch (ApiException e) {
            status = e.status;
            result = Map.of("error", e.getMessage());
        } catch (SQLException e) {
            // 23505 = unique violation, 23503 = foreign key violation
            if ("23505".equals(e.getSQLState())) {
                status = 409;
                result = Map.of("error", "That item already exists");
            } else if ("23503".equals(e.getSQLState())) {
                status = 409;
                result = Map.of("error", "This item is linked to other records");
            } else {
                status = 500;
                result = Map.of("error", "Database error");
                System.err.println("[DB] " + req.method + " " + req.path + ": " + e.getMessage());
            }
        } catch (Exception e) {
            status = 500;
            result = Map.of("error", "Internal server error");
            System.err.println("[ERR] " + req.method + " " + req.path);
            e.printStackTrace();
        }
        send(ex, status, Json.stringify(result));
    }

    private Object dispatch(Request req) throws Exception {
        String[] parts = split(req.path);
        boolean pathMatched = false;
        for (Route r : routes) {
            Map<String, String> params = match(r.parts, parts);
            if (params == null) continue;
            pathMatched = true;
            if (!r.method.equals(req.method)) continue;

            req.pathParams = params;
            req.user = Sessions.userFor(req.cookie(Sessions.COOKIE));
            if (r.authOnly && req.user == null) throw ApiException.unauthorized();
            if (r.role != null && req.user.role() != r.role) throw ApiException.forbidden();
            return r.handler.handle(req);
        }
        if (pathMatched) throw new ApiException(405, "Method not allowed");
        throw ApiException.notFound("Endpoint");
    }

    private static Map<String, String> match(String[] pattern, String[] actual) {
        if (pattern.length != actual.length) return null;
        Map<String, String> params = new HashMap<>();
        for (int i = 0; i < pattern.length; i++) {
            String p = pattern[i];
            if (p.startsWith("{") && p.endsWith("}")) params.put(p.substring(1, p.length() - 1), actual[i]);
            else if (!p.equals(actual[i])) return null;
        }
        return params;
    }

    private static void send(HttpExchange ex, int status, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.getResponseHeaders().set("Cache-Control", "no-store");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }
}
