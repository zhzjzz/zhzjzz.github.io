package com.travel.system;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TravelSystemApplication {
    public static void main(String[] args) {
        configureSqliteUrlIfMissing();
        SpringApplication.run(TravelSystemApplication.class, args);
    }

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

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
