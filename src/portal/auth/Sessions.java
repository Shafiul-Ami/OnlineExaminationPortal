package portal.auth;

import java.sql.SQLException;
import java.util.Map;
import portal.db.Db;
import portal.http.Request;

/** Database-backed login sessions, carried in an HttpOnly cookie. */
public final class Sessions {
    public static final String COOKIE = "exam_sid";
    private static final int TTL_DAYS = 7;

    private Sessions() {}

    public static void start(Request req, long userId) throws SQLException {
        String token = Passwords.randomToken();
        Db.update("insert into sessions(token, user_id, expires_at) values (?, ?, now() + make_interval(days => ?))",
                token, userId, TTL_DAYS);
        req.ex.getResponseHeaders().add("Set-Cookie",
                COOKIE + "=" + token + "; Path=/; HttpOnly; SameSite=Lax; Max-Age=" + (TTL_DAYS * 86400));
    }

    public static void end(Request req) throws SQLException {
        String token = req.cookie(COOKIE);
        if (token != null) Db.update("delete from sessions where token = ?", token);
        req.ex.getResponseHeaders().add("Set-Cookie", COOKIE + "=; Path=/; HttpOnly; SameSite=Lax; Max-Age=0");
    }

    public static User userFor(String token) throws SQLException {
        if (token == null || token.isBlank()) return null;
        Map<String, Object> row = Db.one("""
                select u.id, u.name, u.email, u.role
                from sessions s join users u on u.id = s.user_id
                where s.token = ? and s.expires_at > now()
                """, token);
        if (row == null) return null;
        return new User(((Number) row.get("id")).longValue(), (String) row.get("name"),
                (String) row.get("email"), Role.valueOf((String) row.get("role")));
    }
}
