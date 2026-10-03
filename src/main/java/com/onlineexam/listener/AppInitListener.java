package com.onlineexam.listener;

import com.onlineexam.db.Db;
import com.onlineexam.db.Schema;
import com.onlineexam.util.AppConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;

@WebListener
public class AppInitListener implements ServletContextListener {

    /** App starts -> create the database/tables if needed, then the admin account and sample questions. */
    @Override
    public void contextInitialized(ServletContextEvent event) {
        ServletContext ctx = event.getServletContext();
        Db.configure(AppConfig.DB_URL, AppConfig.DB_USER, AppConfig.DB_PASSWORD);
        try {
            Schema.ensureDatabase(AppConfig.DB_URL, AppConfig.DB_USER, AppConfig.DB_PASSWORD, msg -> ctx.log("OnlineExaminationPortal: " + msg));
            Schema.migrate();
            Schema.seedAdmin(AppConfig.ADMIN_NAME, AppConfig.ADMIN_EMAIL, AppConfig.ADMIN_PASSWORD);
            if (AppConfig.SEED_SAMPLE) Schema.seedSampleData();
            ctx.log("OnlineExaminationPortal: database is ready (" + AppConfig.DB_URL + ")");
        } catch (Exception e) {
            // Don't crash Tomcat - the pages still load and API calls will show the DB error
            ctx.log("OnlineExaminationPortal: could not prepare the database - check app.properties", e);
        }
    }

    /** App stops -> unregister the PostgreSQL driver (fixes the memory-leak WARNING in Tomcat's log). */
    @Override
    public void contextDestroyed(ServletContextEvent event) {
        Enumeration<Driver> drivers = DriverManager.getDrivers();
        while (drivers.hasMoreElements()) {
            Driver driver = drivers.nextElement();
            if (driver.getClass().getClassLoader() == getClass().getClassLoader()) {   // only OUR app's driver
                try {
                    DriverManager.deregisterDriver(driver);
                } catch (SQLException ignored) {
                    // shutting down anyway
                }
            }
        }
    }
}
