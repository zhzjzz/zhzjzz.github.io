package com.travel.system.service;

import com.travel.system.config.LlmProperties;
import com.travel.system.dto.AgentChatMessage;
import com.travel.system.dto.AgentChatRequest;
import com.travel.system.dto.AgentChatResponse;
import com.travel.system.llm.LlmChatClient;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TravelAgentServiceTest {

    @Test
    void forwardsUserMessageWithTravelAgentPrompt() {
        RecordingLlmChatClient client = new RecordingLlmChatClient("可以，我会基于系统数据给你推荐。");
        LlmProperties properties = new LlmProperties();
        properties.setApiKey("test-key");
        properties.setModel("Qwen/Qwen2.5-7B-Instruct");
        TravelAgentService service = new TravelAgentService(client, properties);

        AgentChatRequest request = new AgentChatRequest();
        request.setMessage("帮我推荐一个适合校园参观的路线");
        request.setHistory(List.of(new AgentChatMessage("assistant", "你好，我是旅游服务 Agent。")));

        AgentChatResponse response = service.chat(request);

        assertThat(response.isFallback()).isFalse();
        assertThat(response.getReply()).isEqualTo("可以，我会基于系统数据给你推荐。");
        assertThat(response.getProvider()).isEqualTo("SiliconFlow");
        assertThat(response.getModel()).isEqualTo("Qwen/Qwen2.5-7B-Instruct");
        assertThat(client.systemPrompt).contains("不要编造不存在的真实数据");
        assertThat(client.systemPrompt).contains("有向图、拥塞度、交通工具权限和最短路径算法");
        assertThat(client.messages)
                .extracting(AgentChatMessage::getContent)
                .containsExactly("你好，我是旅游服务 Agent。", "帮我推荐一个适合校园参观的路线");
    }

    @Test
    void returnsFallbackWhenApiIsNotConfigured() {
        TravelAgentService service = new TravelAgentService(new RecordingLlmChatClient("unused"), new LlmProperties());
        AgentChatRequest request = new AgentChatRequest();
        request.setMessage("附近有什么美食？");

        AgentChatResponse response = service.chat(request);

        assertThat(response.isFallback()).isTrue();
        assertThat(response.getReply()).contains("SiliconFlow");
        assertThat(response.getReply()).contains("系统数据");
    }

    @Test
    void rejectsBlankMessage() {
        TravelAgentService service = new TravelAgentService(new RecordingLlmChatClient("unused"), new LlmProperties());
        AgentChatRequest request = new AgentChatRequest();
        request.setMessage("  ");

        assertThatThrownBy(() -> service.chat(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("问题不能为空");
    }

    private static class RecordingLlmChatClient implements LlmChatClient {
        private final String reply;
        private String systemPrompt;
        private List<AgentChatMessage> messages = new ArrayList<>();

        private RecordingLlmChatClient(String reply) {
            this.reply = reply;
        }

        @Override
        public String chat(String systemPrompt, List<AgentChatMessage> messages) {
            this.systemPrompt = systemPrompt;
            this.messages = new ArrayList<>(messages);
            return reply;
        }
    }
}
