package com.xuan.xuanopenagent.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class TerminateTool {

    @Tool(description = "Terminate current task with reason and final answer")
    public Map<String, String> terminateTask(String reason, String finalAnswer) {
        Map<String, String> result = new LinkedHashMap<>();
        result.put("reason", reason == null || reason.isBlank() ? "task_completed" : reason);
        result.put("finalAnswer", finalAnswer == null || finalAnswer.isBlank() ? "任务已完成。" : finalAnswer);
        return result;
    }
}