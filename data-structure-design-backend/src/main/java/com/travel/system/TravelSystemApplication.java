package com.travel.system;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TravelSystemApplication {
    /**
     * 应用入口方法，负责在 Spring Boot 启动前补齐 SQLite 数据库地址配置，然后启动后端服务。
     */
    public static void main(String[] args) {
        loadDotenv();
        configureSqliteUrlIfMissing();
        SpringApplication.run(TravelSystemApplication.class, args);
    }

    private static void loadDotenv() {
        List<Path> candidates = List.of(
                Path.of(".env"),
                Path.of("data-structure-design-backend", ".env"));

        for (Path envPath : candidates) {
            if (Files.isRegularFile(envPath)) {
                loadDotenvFile(envPath);
            }
        }
    }

    private static void loadDotenvFile(Path envPath) {
        try {
            for (String line : Files.readAllLines(envPath)) {
                loadDotenvLine(line);
            }
        } catch (IOException ignored) {
            // Shell or deployment environment variables can still provide configuration.
        }
    }

    private static void loadDotenvLine(String line) {
        String trimmed = line.trim();
        if (trimmed.isEmpty() || trimmed.startsWith("#")) {
            return;
        }

        int separator = trimmed.indexOf('=');
        if (separator <= 0) {
            return;
        }

        String key = trimmed.substring(0, separator).trim();
        String value = stripQuotes(trimmed.substring(separator + 1).trim());
        if (hasText(key) && System.getProperty(key) == null && System.getenv(key) == null) {
            System.setProperty(key, value);
        }
    }

    /**

     * 检查运行环境中的数据库连接配置；如果没有显式配置 spring.datasource.url，则按项目 data 目录中的 tourism_system.gpkg 自动生成 SQLite JDBC 地址。

     */
    private static void configureSqliteUrlIfMissing() {
        if (hasText(System.getenv("SQLITE_URL")) || hasText(System.getProperty("SQLITE_URL"))) {
            return;
        }

        List<Path> candidates = List.of(
                Path.of("data", "tourism_system.gpkg"),
                Path.of("data-structure-design-backend", "data", "tourism_system.gpkg"));

        for (Path candidate : candidates) {
            Path absolutePath = candidate.toAbsolutePath().normalize();
            if (Files.isRegularFile(absolutePath)) {
                System.setProperty("SQLITE_URL", "jdbc:sqlite:" + absolutePath.toString().replace('\\', '/'));
                return;
            }
        }
    }

    /**

     * 判断字符串是否包含非空白字符，用于区分“未配置”和“配置为空”的场景。

     */
    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String stripQuotes(String value) {
        if (value.length() >= 2
                && ((value.startsWith("\"") && value.endsWith("\""))
                || (value.startsWith("'") && value.endsWith("'")))) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }
}
