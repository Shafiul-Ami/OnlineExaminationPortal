package com.onlineexam.http;

import com.onlineexam.auth.User;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/** Wraps the servlet request/response with convenient accessors for query, path params, JSON body and the logged-in user. */
public class Request {
    private static final int MAX_BODY = 2 * 1024 * 1024;

    public final HttpServletRequest raw;
    public final HttpServletResponse resp;
    public final String method;
    /** Path inside the web app, e.g. /api/auth/login (the context path is not included). */
    public final String path;
    public final Map<String, String> query;
    public Map<String, String> pathParams = Map.of();
    public User user;
    private Map<String, Object> body;

    public Request(HttpServletRequest raw, HttpServletResponse resp) {
        this.raw = raw;
        this.resp = resp;
        this.method = raw.getMethod().toUpperCase();
        this.path = raw.getServletPath() + (raw.getPathInfo() == null ? "" : raw.getPathInfo());
        this.query = parseQuery(raw.getQueryString());
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
        Cookie[] cookies = raw.getCookies();
        if (cookies == null) return null;
        for (Cookie c : cookies) {
            if (c.getName().equals(name)) return c.getValue();
        }
        return null;
    }

    /** Adds a raw Set-Cookie header (lets us set SameSite, which the Cookie class can't). */
    public void setCookieHeader(String value) {
        resp.addHeader("Set-Cookie", value);
    }

    /** Cookie path: the web app's context path, e.g. /OnlineExaminationPortal (or / when deployed as ROOT). */
    public String cookiePath() {
        String ctx = raw.getContextPath();
        return ctx == null || ctx.isEmpty() ? "/" : ctx;
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
        try (InputStream in = raw.getInputStream()) {
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
