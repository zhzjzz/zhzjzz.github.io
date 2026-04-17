package com.travel.system.config;

import com.graphhopper.GraphHopper;
import com.graphhopper.config.CHProfile;
import com.graphhopper.config.Profile;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Files;
import java.nio.file.Path;

@Configuration
@EnableConfigurationProperties(GraphHopperProperties.class)
public class GraphHopperConfig {
    private static final String PROFILE_CAR = "car";
    private static final String PROFILE_BIKE = "bike";
    private static final String PROFILE_FOOT = "foot";
    private static final String PROFILE_PUBLIC_TRANSPORT = "public_transport";

    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(prefix = "routing.graphhopper", name = "enabled", havingValue = "true")
    public GraphHopper graphHopper(GraphHopperProperties properties) {
        GraphHopper hopper = new GraphHopper();
        hopper.setOSMFile(resolveBackendRelativePath(properties.getOsmFile()));
        hopper.setGraphHopperLocation(resolveBackendRelativePath(properties.getGraphLocation()));
        hopper.setProfiles(
                new Profile(PROFILE_CAR).setVehicle(PROFILE_CAR).setWeighting("fastest"),
                new Profile(PROFILE_BIKE).setVehicle(PROFILE_BIKE).setWeighting("fastest"),
                new Profile(PROFILE_FOOT).setVehicle(PROFILE_FOOT).setWeighting("fastest"),
                // TODO: 接入 GTFS 后替换为真实公共交通 profile；当前 OSM-only 场景下按步行近似。
                new Profile(PROFILE_PUBLIC_TRANSPORT).setVehicle(PROFILE_FOOT).setWeighting("fastest")
        );
        hopper.getCHPreparationHandler().setCHProfiles(
                new CHProfile(PROFILE_CAR),
                new CHProfile(PROFILE_BIKE),
                new CHProfile(PROFILE_FOOT),
                new CHProfile(PROFILE_PUBLIC_TRANSPORT)
        );
        hopper.importOrLoad();
        return hopper;
    }

    private String resolveBackendRelativePath(String rawPath) {
        Path configured = Path.of(rawPath);
        if (configured.isAbsolute()) {
            return configured.normalize().toString();
        }

        Path userDir = Path.of(System.getProperty("user.dir"));
        Path backendDir = userDir;
        if (!Files.exists(userDir.resolve("src"))
                && Files.exists(userDir.resolve("data-structrue-design-backend").resolve("src"))) {
            backendDir = userDir.resolve("data-structrue-design-backend");
        }
        return backendDir.resolve(configured).normalize().toString();
    }
}
