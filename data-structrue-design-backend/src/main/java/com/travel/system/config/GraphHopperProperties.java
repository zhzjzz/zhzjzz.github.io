package com.travel.system.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Setter
@Getter
@ConfigurationProperties(prefix = "routing.graphhopper")
public class GraphHopperProperties {
    private boolean enabled = true;
    private String osmFile;
    private String graphLocation;
    private String profile;
    private String vehicle;

}
