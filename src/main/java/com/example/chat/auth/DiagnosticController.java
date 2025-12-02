package com.example.chat.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/debug")
@RequiredArgsConstructor
public class DiagnosticController {

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    @GetMapping("/db-info")
    public Map<String, Object> getDbInfo() {
        Map<String, Object> info = new HashMap<>();

        try {
            Connection conn = dataSource.getConnection();
            DatabaseMetaData meta = conn.getMetaData();

            info.put("database_url", meta.getURL());
            info.put("database_product", meta.getDatabaseProductName());
            info.put("database_version", meta.getDatabaseProductVersion());
            info.put("driver_name", meta.getDriverName());

            // Check database type
            String url = meta.getURL().toLowerCase();
            if (url.contains("h2")) {
                info.put("database_type", "H2 (IN-MEMORY - DATA LOST ON RESTART)");
            } else if (url.contains("postgresql")) {
                info.put("database_type", "PostgreSQL");
            } else {
                info.put("database_type", "Unknown");
            }

            // Try to get user count
            try {
                Integer userCount = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM users", Integer.class);
                info.put("user_count", userCount);
            } catch (Exception e) {
                info.put("user_count_error", e.getMessage());
            }

            conn.close();
            info.put("status", "success");

        } catch (Exception e) {
            info.put("status", "error");
            info.put("error", e.getMessage());
            e.printStackTrace();
        }

        return info;
    }

    @GetMapping("/test-connection")
    public Map<String, Object> testConnection() {
        Map<String, Object> result = new HashMap<>();

        try {
            // Test connection
            Connection conn = dataSource.getConnection();
            result.put("connection_test", "SUCCESS");
            result.put("url", conn.getMetaData().getURL());
            conn.close();

            // Test query
            jdbcTemplate.execute("SELECT 1");
            result.put("query_test", "SUCCESS");

            // Check users table exists
            try {
                Integer tableExists = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM information_schema.tables " +
                                "WHERE table_schema = 'public' AND table_name = 'users'",
                        Integer.class);
                result.put("users_table_exists", tableExists > 0);
            } catch (Exception e) {
                result.put("users_table_error", e.getMessage());
            }

        } catch (Exception e) {
            result.put("error", e.getMessage());
        }

        return result;
    }
}