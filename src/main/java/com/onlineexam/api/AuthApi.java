package com.onlineexam.api;

import java.util.Map;
import java.util.regex.Pattern;
import com.onlineexam.auth.Passwords;
import com.onlineexam.auth.Sessions;
import com.onlineexam.auth.User;
import com.onlineexam.db.Db;
import com.onlineexam.http.ApiException;
import com.onlineexam.http.Request;
import com.onlineexam.http.Router;

public final class AuthApi {
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private AuthApi() {}

    public static void register(Router r) {
        r.post("/api/auth/register", AuthApi::registerStudent);
        r.post("/api/auth/login", AuthApi::login);
        r.post("/api/auth/logout", req -> { Sessions.end(req); return null; });
        r.get("/api/auth/me", req -> {
            req.user = Sessions.userFor(req.cookie(Sessions.COOKIE));
            return Map.of("user", req.user == null ? false : req.user.toJson());
        });
    }

    private static Object registerStudent(Request req) throws Exception {
        String name = req.str("name", true, 100);
        String email = req.str("email", true, 150).toLowerCase();
        String password = req.str("password", true, 200);
        if (!EMAIL.matcher(email).matches()) throw ApiException.badRequest("Please enter a valid email address");
        if (password.length() < 6) throw ApiException.badRequest("Password must be at least 6 characters");
        if (Db.one("select id from users where email = ?", email) != null) {
            throw new ApiException(409, "An account with this email already exists");
        }
        long id = Db.insert("insert into users(name, email, password_hash, role) values (?, ?, ?, 'STUDENT') returning id",
                name, email, Passwords.hash(password));
        Sessions.start(req, id);
        return Map.of("user", new User(id, name, email, com.onlineexam.auth.Role.STUDENT).toJson());
    }

    private static Object login(Request req) throws Exception {
        String email = req.str("email", true, 150).toLowerCase();
        String password = req.str("password", true, 200);
        Map<String, Object> row = Db.one("select id, name, email, role, password_hash from users where email = ?", email);
        if (row == null || !Passwords.verify(password, (String) row.get("passwordHash"))) {
            throw new ApiException(401, "Invalid email or password");
        }
        long id = ((Number) row.get("id")).longValue();
        Sessions.start(req, id);
        User u = new User(id, (String) row.get("name"), (String) row.get("email"),
                com.onlineexam.auth.Role.valueOf((String) row.get("role")));
        return Map.of("user", u.toJson());
    }
}
