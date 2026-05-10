package com.travel.system.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AgentChatRequest {
    private String message;
    private List<AgentChatMessage> history = new ArrayList<>();
}
