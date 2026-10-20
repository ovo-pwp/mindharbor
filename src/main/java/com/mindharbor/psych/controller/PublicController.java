package com.mindharbor.psych.controller;

import com.mindharbor.psych.common.R;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/** 公开接口：容器健康检查与前端运行时配置 */
@RestController
@RequestMapping("/api/public")
public class PublicController {

    @Value("${app.demo-accounts:true}")
    private boolean demoAccounts;

    @GetMapping("/health")
    public R<Map<String, Object>> health() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("status", "UP");
        data.put("time", LocalDateTime.now().toString());
        return R.ok(data);
    }

    /**
     * 前端运行时配置。
     * demoAccounts 控制登录页是否展示演示账号，公开部署时应为 false。
     */
    @GetMapping("/config")
    public R<Map<String, Object>> config() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("demoAccounts", demoAccounts);
        data.put("appName", "心屿");
        return R.ok(data);
    }
}
