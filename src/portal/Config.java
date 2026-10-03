package portal;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Reads config.properties; any key can be overridden by an environment variable (db.password -> DB_PASSWORD). */
public final class Config {
    private final Properties props = new Properties();

    public Config(Path file) throws IOException {
        if (Files.exists(file)) {
            try (Reader r = Files.newBufferedReader(file)) {
                props.load(r);
            }
        } else {
            System.out.println("[config] " + file + " not found, using defaults and environment variables");
        }
    }

    public String get(String key, String def) {
        String env = System.getenv(key.toUpperCase().replace('.', '_'));
        if (env != null && !env.isBlank()) return env;
        String v = props.getProperty(key);
        return v == null || v.isBlank() ? def : v.trim();
    }

    public int getInt(String key, int def) {
        return Integer.parseInt(get(key, String.valueOf(def)));
    }
}
