package com.gcpulse.model;

import lombok.Data;
import lombok.Builder;

/**
 * AI诊断请求
 */
@Data
@Builder
public class AIDiagnosisRequest {
    
    private String apiUrl;                    // OpenAI API地址
    private String apiKey;                    // OpenAI API密钥
    private String model;                     // 模型名称，默认 gpt-4
    private String gcLogContent;              // GC日志内容（原始日志）
    private String collectorType;             // GC收集器类型
    private Integer eventCount;               // GC事件数量
    
    // 新增：结构化优化上下文（优先使用）
    private OptimizationContext optimizationContext;  // 结构化的GC分析数据
    private boolean useStructuredData;        // 是否使用结构化数据（默认false保持向后兼容）
}

