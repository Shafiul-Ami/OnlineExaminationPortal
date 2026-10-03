package portal.http;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import portal.auth.User;

/** Wraps an HttpExchange with convenient accessors for query, path params, JSON body and the logged-in user. */
public class Request {
    private static final int MAX_BODY = 2 * 1024 * 1024;

    public final HttpExchange ex;
    public final String method;
    public final String path;
    public final Map<String, String> query;
    public Map<String, String> pathParams = Map.of();
    public User user;
    private Map<String, Object> body;

    public Request(HttpExchange ex) {
        this.ex = ex;
        this.method = ex.getRequestMethod().toUpperCase();
        this.path = ex.getRequestURI().getPath();
        this.query = parseQuery(ex.getRequestURI().getRawQuery());
    }

    private static Map<String, String> parseQuery(String raw) {
        Map<String, String> m = new HashMap<>();
        if (raw == null || raw.isEmpty()) return m;
        for (String pair : raw.split("&")) {
            int eq = pair.indexOf('=');
            String k = eq < 0 ? pair : pair.substring(0, eq);
            String v = eq < 0 ? "" : pair.substring(eq + 1);
            m.put(URLDecoder.decode(k, StandardCharsets.UTF_8), URLDecoder.decode(v, StandardCharsets.UTF_8));
        }
        return m;
    }

    public String cookie(String name) {
        List<String> headers = ex.getRequestHeaders().get("Cookie");
        if (headers == null) return null;
        for (String h : headers) {
            for (String part : h.split(";")) {
                String p = part.trim();
                if (p.startsWith(name + "=")) return p.substring(name.length() + 1);
            }
        }
        return null;
    }

    public long pathLong(String name) {
        try {
            return Long.parseLong(pathParams.get(name));
        } catch (NumberFormatException e) {
            throw ApiException.badRequest("Invalid " + name);
        }
    }

    public Long queryLong(String name) {
        String v = query.get(name);
        if (v == null || v.isBlank()) return null;
        try {
            return Long.parseLong(v);
        } catch (NumberFormatException e) {
            throw ApiException.badRequest("Invalid " + name);
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> body() {
        if (body != null) return body;
        try (InputStream in = ex.getRequestBody()) {
            byte[] bytes = in.readNBytes(MAX_BODY + 1);
            if (bytes.length > MAX_BODY) throw new ApiException(413, "Request body too large");
            String text = new String(bytes, StandardCharsets.UTF_8).trim();
            if (text.isEmpty()) return body = new HashMap<>();
            Object parsed = Json.parse(text);
            if (!(parsed instanceof Map)) throw ApiException.badRequest("Expected a JSON object");
            return body = (Map<String, Object>) parsed;
        } catch (IOException e) {
            throw ApiException.badRequest("Could not read request body");
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest(e.getMessage());
        }
    }

    // ---- typed body accessors ----

    public String str(String key, boolean required, int maxLen) {
        Object v = body().get(key);
        String s = v == null ? null : String.valueOf(v).trim();
        if (s == null || s.isEmpty()) {
            if (required) throw ApiException.badRequest(label(key) + " is required");
            return null;
        }
        if (s.length() > maxLen) throw ApiException.badRequest(label(key) + " must be at most " + maxLen + " characters");
        return s;
    }

    public Long num(String key, boolean required) {
        Object v = body().get(key);
        if (v == null || (v instanceof String s && s.isBlank())) {
            if (required) throw ApiException.badRequest(label(key) + " is required");
            return null;
        }
        if (v instanceof Number n) return n.longValue();
        try {
            return Long.parseLong(String.valueOf(v).trim());
        } catch (NumberFormatException e) {
            throw ApiException.badRequest(label(key) + " must be a number");
        }
    }

    private static String label(String key) {
        String spaced = key.replaceAll("([A-Z])", " $1").toLowerCase();
        return Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1);
    }
}
