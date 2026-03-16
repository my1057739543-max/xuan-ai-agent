package com.xuan.xuanopenagent.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class TimeGetTool {

    @Tool(description = "Get current date time with timezone")
    public Map<String, String> getCurrentDateTime() {
        ZonedDateTime now = ZonedDateTime.now();
        Map<String, String> result = new LinkedHashMap<>();
        result.put("iso", now.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        result.put("zone", now.getZone().getId());
        result.put("formatted", now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z")));
        return result;
    }
}