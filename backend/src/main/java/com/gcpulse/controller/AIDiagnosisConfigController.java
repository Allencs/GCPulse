package com.gcpulse.controller;

import com.gcpulse.config.AIDiagnosisConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * AI诊断配置控制器
 * 用于前端获取后端配置信息
 */
@RestController
@RequestMapping("/api/ai")
public class AIDiagnosisConfigController {
    
    @Autowired
    private AIDiagnosisConfig aiConfig;
    
    /**
     * 获取AI诊断配置（不包含敏感信息如API Key）
     */
    @GetMapping("/config")
    public Map<String, Object> getConfig() {
        Map<String, Object> config = new HashMap<>();
        
        // 是否已配置API Key
        boolean hasApiKey = aiConfig.getApiKey() != null && !aiConfig.getApiKey().trim().isEmpty();
        config.put("hasApiKey", hasApiKey);
        
        // 是否已配置API URL
        boolean hasApiUrl = aiConfig.getApiUrl() != null && !aiConfig.getApiUrl().trim().isEmpty();
        config.put("hasApiUrl", hasApiUrl);
        config.put("apiUrl", hasApiUrl ? aiConfig.getApiUrl() : "");
        
        // 默认模型
        boolean hasDefaultModel = aiConfig.getDefaultModel() != null && !aiConfig.getDefaultModel().trim().isEmpty();
        config.put("hasDefaultModel", hasDefaultModel);
        config.put("defaultModel", hasDefaultModel ? aiConfig.getDefaultModel() : "");
        
        return config;
    }
}

