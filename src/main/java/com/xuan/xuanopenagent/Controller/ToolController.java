package com.xuan.xuanopenagent.controller;

import com.xuan.xuanopenagent.service.AgentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tools")
public class ToolController {

    private final AgentService agentService;

    public ToolController(AgentService agentService) {
        this.agentService = agentService;
    }

    @GetMapping
    public Map<String, List<String>> listTools() {
        return Map.of("tools", agentService.listTools());
    }
}