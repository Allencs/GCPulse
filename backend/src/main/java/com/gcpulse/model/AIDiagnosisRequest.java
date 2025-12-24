package com.gcpulse.model;

import lombok.Data;
import lombok.Builder;

/**
 * AI诊断请求
 */
@Data
@Builder
public class AIDiagnosisRequest {
    
    private String apiUrl;          // OpenAI API地址
    private String apiKey;          // OpenAI API密钥
    private String model;           // 模型名称，默认 gpt-4
    private String gcLogContent;    // GC日志内容
    private String collectorType;   // GC收集器类型
    private Integer eventCount;     // GC事件数量
}

