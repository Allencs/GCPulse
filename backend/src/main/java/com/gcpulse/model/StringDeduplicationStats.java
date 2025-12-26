package com.gcpulse.model;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 字符串去重统计
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StringDeduplicationStats {
    
    private long totalInspected;         // 检查的字符串总数
    private long totalDeduplicated;      // 去重的字符串总数
    private long bytesSaved;             // 节省的字节数
    private double deduplicationRate;    // 去重率 (%)
    private double avgDeduplicationTime; // 平均去重时间 (ms)
    private long totalDeduplicationTime; // 总去重时间 (ms)
    
    public String getBytesSavedFormatted() {
        if (bytesSaved >= 1024L * 1024 * 1024) {
            return String.format("%.3f GB", bytesSaved / (1024.0 * 1024 * 1024));
        } else if (bytesSaved >= 1024 * 1024) {
            return String.format("%.3f MB", bytesSaved / (1024.0 * 1024));
        } else {
            return String.format("%.3f KB", bytesSaved / 1024.0);
        }
    }
    
    public String getDeduplicationRateFormatted() {
        return String.format("%.2f%%", deduplicationRate);
    }
}

