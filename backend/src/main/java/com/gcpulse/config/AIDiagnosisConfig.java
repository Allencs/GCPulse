package com.gcpulse.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * AI诊断配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "ai.diagnosis")
public class AIDiagnosisConfig {
    
    /**
     * API地址（支持OpenAI官方或兼容服务，如OpenRouter）
     */
    private String apiUrl = "https://api.openai.com/v1";
    
    /**
     * API Key
     */
    private String apiKey;
    
    /**
     * 默认模型
     */
    private String defaultModel = "gpt-4o";
    
    /**
     * 超时时间（秒）
     */
    private int timeoutSeconds = 60;
    
    /**
     * 发送给AI的最大日志长度
     */
    private int maxLogLength = 15000;
}

