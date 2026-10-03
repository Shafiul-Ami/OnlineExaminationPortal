package com.onlineexam.http;

import com.onlineexam.api.AdminApi;
import com.onlineexam.api.AuthApi;
import com.onlineexam.api.CatalogApi;
import com.onlineexam.api.ExamApi;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/** One servlet for the whole REST API: every /api/* request goes through the router. */
@WebServlet(urlPatterns = "/api/*", loadOnStartup = 1)
public class ApiServlet extends HttpServlet {
    private final Router router = new Router();

    @Override
    public void init() {
        AuthApi.register(router);
        CatalogApi.register(router);
        ExamApi.register(router);
        AdminApi.register(router);
    }

    /** Overriding service() lets the router handle GET, POST, PUT and DELETE in one place. */
    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        router.handle(new Request(req, resp));
    }
}
