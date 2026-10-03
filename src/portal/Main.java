package portal;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.concurrent.Executors;
import portal.api.AdminApi;
import portal.api.AuthApi;
import portal.api.CatalogApi;
import portal.api.ExamApi;
import portal.db.Db;
import portal.db.Schema;
import portal.http.Router;
import portal.http.StaticHandler;

/** Entry point: prepares the database, then serves the REST API under /api and the frontend from web/. */
public class Main {
    public static void main(String[] args) throws Exception {
        Config cfg = new Config(Path.of("config.properties"));

        String host = cfg.get("db.host", "localhost");
        int dbPort = cfg.getInt("db.port", 5432);
        String dbName = cfg.get("db.name", "exam_portal");
        String dbUser = cfg.get("db.user", "postgres");
        String dbPass = cfg.get("db.password", "");

        Schema.ensureDatabase(host, dbPort, dbName, dbUser, dbPass);
        Db.configure(host, dbPort, dbName, dbUser, dbPass);
        Schema.migrate();
        Schema.seedAdmin(cfg.get("admin.name", "Administrator"),
                cfg.get("admin.email", "admin@exam.com"),
                cfg.get("admin.password", "Admin@123"));
        if (Boolean.parseBoolean(cfg.get("seed.sample", "true"))) Schema.seedSampleData();

        Router api = new Router();
        AuthApi.register(api);
        CatalogApi.register(api);
        ExamApi.register(api);
        AdminApi.register(api);

        int port = cfg.getInt("server.port", 8080);
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/api/", api);
        server.createContext("/", new StaticHandler(Path.of("web")));
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        server.start();

        System.out.println("""

                  ExamSphere is running  ->  http://localhost:%d
                  Press Ctrl+C to stop.
                """.formatted(port));
    }
}
