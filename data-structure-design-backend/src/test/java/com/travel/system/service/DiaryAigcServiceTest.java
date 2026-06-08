package com.travel.system.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DiaryAigcServiceTest {

    @Test
    void imageApiEndpointAcceptsOpenAiCompatibleBaseUrl() {
        assertThat(DiaryAigcService.imageApiEndpoint("https://api.siliconflow.cn/v1"))
                .isEqualTo("https://api.siliconflow.cn/v1/images/generations");
    }

    @Test
    void imageApiEndpointKeepsExplicitGenerationEndpoint() {
        assertThat(DiaryAigcService.imageApiEndpoint("https://api.siliconflow.cn/v1/images/generations"))
                .isEqualTo("https://api.siliconflow.cn/v1/images/generations");
    }
}
