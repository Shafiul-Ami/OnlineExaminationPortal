package com.onlineexam.db;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Small JDBC helper. Rows come back as maps with camelCase keys (question_text -> questionText)
 * so they can be returned straight to the frontend as JSON.
 */
public final class Db {
    private static String url;
    private static String user;
    private static String password;

    static {
        // Inside Tomcat the driver in WEB-INF/lib must be loaded explicitly
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new ExceptionInInitializerError("PostgreSQL driver not found in WEB-INF/lib");
        }
    }

    private Db() {}

    public static void configure(String url, String user, String password) {
        Db.url = url;
        Db.user = user;
        Db.password = password;
    }

    public static Connection connect() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    @FunctionalInterface
    public interface TxWork<T> {
        T run(Connection c) throws Exception;
    }

    /** Runs work inside a transaction, rolling back on any exception. */
    public static <T> T tx(TxWork<T> work) throws Exception {
        try (Connection c = connect()) {
            c.setAutoCommit(false);
            try {
                T result = work.run(c);
                c.commit();
                return result;
            } catch (Exception e) {
                c.rollback();
                throw e;
            }
        }
    }

    // ---- convenience overloads that open their own connection ----

    public static List<Map<String, Object>> query(String sql, Object... params) throws SQLException {
        try (Connection c = connect()) {
            return query(c, sql, params);
        }
    }

    public static Map<String, Object> one(String sql, Object... params) throws SQLException {
        try (Connection c = connect()) {
            return one(c, sql, params);
        }
    }

    public static int update(String sql, Object... params) throws SQLException {
        try (Connection c = connect()) {
            return update(c, sql, params);
        }
    }

    // ---- variants that reuse a connection (for transactions) ----

    public static List<Map<String, Object>> query(Connection c, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = prepare(c, sql, params); ResultSet rs = ps.executeQuery()) {
            List<Map<String, Object>> rows = new ArrayList<>();
            ResultSetMetaData md = rs.getMetaData();
            int cols = md.getColumnCount();
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= cols; i++) row.put(camel(md.getColumnLabel(i)), convert(rs.getObject(i)));
                rows.add(row);
            }
            return rows;
        }
    }

    public static Map<String, Object> one(Connection c, String sql, Object... params) throws SQLException {
        List<Map<String, Object>> rows = query(c, sql, params);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public static int update(Connection c, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = prepare(c, sql, params)) {
            return ps.executeUpdate();
        }
    }

    /** Executes an INSERT ... RETURNING id and returns the id. */
    public static long insert(Connection c, String sql, Object... params) throws SQLException {
        Map<String, Object> row = one(c, sql, params);
        return ((Number) row.get("id")).longValue();
    }

    public static long insert(String sql, Object... params) throws SQLException {
        try (Connection c = connect()) {
            return insert(c, sql, params);
        }
    }

    public static long count(String sql, Object... params) throws SQLException {
        Map<String, Object> row = one(sql, params);
        Object v = row == null ? null : row.values().iterator().next();
        return v == null ? 0 : ((Number) v).longValue();
    }

    private static PreparedStatement prepare(Connection c, String sql, Object... params) throws SQLException {
        PreparedStatement ps = c.prepareStatement(sql);
        for (int i = 0; i < params.length; i++) ps.setObject(i + 1, params[i]);
        return ps;
    }

    private static Object convert(Object v) {
        if (v instanceof Timestamp t) return t.toInstant().toString();
        if (v instanceof BigDecimal b) return b.doubleValue();
        if (v instanceof java.sql.Date d) return d.toString();
        return v;
    }

    private static String camel(String col) {
        StringBuilder sb = new StringBuilder();
        boolean up = false;
        for (char ch : col.toCharArray()) {
            if (ch == '_') { up = true; continue; }
            sb.append(up ? Character.toUpperCase(ch) : ch);
            up = false;
        }
        return sb.toString();
    }
}
