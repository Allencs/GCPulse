package com.gcpulse.model;

import lombok.Data;
import lombok.Builder;

/**
 * AI诊断响应
 */
@Data
@Builder
public class AIDiagnosisResponse {
    
    private boolean success;            // 是否成功
    private String diagnosis;           // 诊断结果（Markdown格式）
    private String error;               // 错误信息
    private long processTime;           // 处理时间（毫秒）
}

