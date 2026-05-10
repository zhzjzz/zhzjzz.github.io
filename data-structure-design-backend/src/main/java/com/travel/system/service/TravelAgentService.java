package com.travel.system.service;

import com.travel.system.config.LlmProperties;
import com.travel.system.dto.AgentChatMessage;
import com.travel.system.dto.AgentChatRequest;
import com.travel.system.dto.AgentChatResponse;
import com.travel.system.llm.LlmChatClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class TravelAgentService {
    private static final String PROVIDER = "SiliconFlow";
    private static final int MAX_HISTORY_MESSAGES = 8;
    private static final Set<String> ALLOWED_HISTORY_ROLES = Set.of("user", "assistant");

    private static final String SYSTEM_PROMPT = """
            你是个性化旅游系统中的旅游服务Agent。

            你面向游客和校园参观者，提供：
            1. 旅游目的地推荐；
            2. 景点介绍；
            3. 路线规划解释；
            4. 周边设施查询建议；
            5. 美食推荐；
            6. 旅游日记创作建议；
            7. 攻略解析与行程建议。

            你回答时要友好、简洁、实用。
            涉及路线、距离、热度、评分时，应说明这些信息来自系统数据或算法结果。

            如果用户询问具体路线，你需要提示系统会结合有向图、拥塞度、交通工具权限和最短路径算法生成路线。
            如果用户询问推荐结果，你需要说明系统会综合热度、评分、兴趣偏好和距离等因素计算。
            不要编造不存在的真实数据。
            当系统数据不足时，应说明需要用户在系统中选择目的地、当前位置或目标地点后再计算。
            """;

    private final LlmChatClient llmChatClient;
    private final LlmProperties properties;

    public TravelAgentService(LlmChatClient llmChatClient, LlmProperties properties) {
        this.llmChatClient = llmChatClient;
        this.properties = properties;
    }

    public AgentChatResponse chat(AgentChatRequest request) {
        String userMessage = request == null ? "" : normalize(request.getMessage());
        if (userMessage.isBlank()) {
            throw new IllegalArgumentException("问题不能为空");
        }

        if (!isConfigured()) {
            return fallbackResponse(userMessage);
        }

        List<AgentChatMessage> messages = sanitizeHistory(request.getHistory());
        messages.add(new AgentChatMessage("user", userMessage));

        try {
            String reply = llmChatClient.chat(SYSTEM_PROMPT, messages);
            return new AgentChatResponse(reply, PROVIDER, properties.getModel().trim(), false);
        } catch (Exception exception) {
            return fallbackResponse(userMessage);
        }
    }

    private boolean isConfigured() {
        return properties.getApiKey() != null && !properties.getApiKey().isBlank()
                && properties.getModel() != null && !properties.getModel().isBlank();
    }

    private List<AgentChatMessage> sanitizeHistory(List<AgentChatMessage> history) {
        List<AgentChatMessage> sanitized = new ArrayList<>();
        if (history == null || history.isEmpty()) {
            return sanitized;
        }

        int start = Math.max(0, history.size() - MAX_HISTORY_MESSAGES);
        for (AgentChatMessage message : history.subList(start, history.size())) {
            String role = normalize(message.getRole()).toLowerCase();
            String content = normalize(message.getContent());
            if (ALLOWED_HISTORY_ROLES.contains(role) && !content.isBlank()) {
                sanitized.add(new AgentChatMessage(role, content));
            }
        }
        return sanitized;
    }

    private AgentChatResponse fallbackResponse(String userMessage) {
        String reply = "我可以作为旅游服务 Agent 帮你分析这个问题。当前 SiliconFlow API 密钥或模型还未配置，"
                + "所以暂时使用本地安全回复：如果你问路线，系统需要你先选择当前位置和目标地点，之后会结合有向图、拥塞度、交通工具权限和最短路径算法生成；"
                + "如果你问推荐，系统会综合热度、评分、兴趣偏好和距离等系统数据或算法结果计算。"
                + "你刚才的问题是：“" + userMessage + "”。";
        return new AgentChatResponse(reply, PROVIDER, properties.getModel(), true);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
