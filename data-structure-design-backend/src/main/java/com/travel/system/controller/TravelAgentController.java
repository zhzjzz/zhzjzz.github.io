package com.travel.system.controller;

import com.travel.system.dto.AgentChatRequest;
import com.travel.system.dto.AgentChatResponse;
import com.travel.system.dto.AgentStatusResponse;
import com.travel.system.service.TravelAgentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/agent")
@Tag(name = "旅游服务 Agent", description = "面向游客和校园参观者的大模型问答接口")
public class TravelAgentController {
    private final TravelAgentService travelAgentService;

    public TravelAgentController(TravelAgentService travelAgentService) {
        this.travelAgentService = travelAgentService;
    }

    @PostMapping("/chat")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "旅游服务 Agent 对话", description = "通过后端代理调用 SiliconFlow Chat Completions，避免前端暴露 API Key")
    public AgentChatResponse chat(@RequestBody AgentChatRequest request) {
        try {
            return travelAgentService.chat(request);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        }
    }

    @GetMapping("/status")
    @Operation(summary = "旅游服务 Agent 配置状态", description = "返回后端是否读取到 SiliconFlow 配置，不返回 API Key 明文")
    public AgentStatusResponse status() {
        return travelAgentService.status();
    }
}
