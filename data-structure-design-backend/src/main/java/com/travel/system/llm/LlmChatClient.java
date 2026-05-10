package com.travel.system.llm;

import com.travel.system.dto.AgentChatMessage;

import java.util.List;

public interface LlmChatClient {
    String chat(String systemPrompt, List<AgentChatMessage> messages) throws Exception;
}
