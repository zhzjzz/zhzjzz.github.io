package com.travel.system.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "routing.graphhopper")
public class GraphHopperProperties {
    private boolean enabled;
    private String osmFile;
    private String graphLocation;
    private String profile;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getOsmFile() {
        return osmFile;
    }

    public void setOsmFile(String osmFile) {
        this.osmFile = osmFile;
    }

    public String getGraphLocation() {
        return graphLocation;
    }

    public void setGraphLocation(String graphLocation) {
        this.graphLocation = graphLocation;
    }

    public String getProfile() {
        return profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }
}
