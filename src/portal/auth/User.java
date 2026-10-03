package portal.auth;

import java.util.LinkedHashMap;
import java.util.Map;

public record User(long id, String name, String email, Role role) {
    public Map<String, Object> toJson() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("name", name);
        m.put("email", email);
        m.put("role", role.name());
        return m;
    }
}
