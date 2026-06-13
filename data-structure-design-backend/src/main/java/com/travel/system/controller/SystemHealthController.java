package com.travel.system.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/health")
public class SystemHealthController {

    @GetMapping("/version")
    public VersionResponse version() {
        return new VersionResponse(
                "拾迹成行",
                commit(),
                Instant.now().toString(),
                true
        );
    }

    private String commit() {
        String value = firstText(
                System.getProperty("git.commit"),
                System.getenv("GIT_COMMIT"),
                System.getenv("COMMIT_SHA"),
                getClass().getPackage().getImplementationVersion()
        );
        return value == null ? "local" : value;
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    public record VersionResponse(String app, String commit, String buildTime, boolean routeInnovation) {
    }
}
