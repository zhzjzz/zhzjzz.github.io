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

    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(prefix = "routing.graphhopper", name = "enabled", havingValue = "true")
    public GraphHopper graphHopper(GraphHopperProperties properties) {
        GraphHopper hopper = new GraphHopper();
        hopper.setOSMFile(resolveBackendRelativePath(properties.getOsmFile()));
        hopper.setGraphHopperLocation(resolveBackendRelativePath(properties.getGraphLocation()));
        hopper.setProfiles(new Profile(properties.getProfile()).setVehicle(properties.getVehicle()).setWeighting("fastest"));
        hopper.getCHPreparationHandler().setCHProfiles(new CHProfile(properties.getProfile()));
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
