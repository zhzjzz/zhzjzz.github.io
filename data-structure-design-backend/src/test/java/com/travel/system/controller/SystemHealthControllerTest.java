package com.travel.system.controller;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SystemHealthControllerTest {

    @Test
    void versionReportsRouteInnovationBackendCapability() {
        SystemHealthController controller = new SystemHealthController();

        SystemHealthController.VersionResponse response = controller.version();

        assertThat(response.app()).isEqualTo("拾迹成行");
        assertThat(response.routeInnovation()).isTrue();
        assertThat(response.commit()).isNotBlank();
        assertThat(response.buildTime()).isNotBlank();
    }
}
