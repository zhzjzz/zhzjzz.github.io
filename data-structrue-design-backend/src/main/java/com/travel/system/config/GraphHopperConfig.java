package com.travel.system.config;

import com.graphhopper.GraphHopper;
import com.graphhopper.config.CHProfile;
import com.graphhopper.config.Profile;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(GraphHopperProperties.class)
public class GraphHopperConfig {

    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(prefix = "routing.graphhopper", name = "enabled", havingValue = "true")
    public GraphHopper graphHopper(GraphHopperProperties properties) {
        GraphHopper hopper = new GraphHopper();
        hopper.setOSMFile(properties.getOsmFile());
        hopper.setGraphHopperLocation(properties.getGraphLocation());
        hopper.setProfiles(new Profile(properties.getProfile()).setVehicle("car").setWeighting("fastest"));
        hopper.getCHPreparationHandler().setCHProfiles(new CHProfile(properties.getProfile()));
        hopper.importOrLoad();
        return hopper;
    }
}
