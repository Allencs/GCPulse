package com.gcpulse.model;

import lombok.Data;
import lombok.Builder;

/**
 * GC 原因统计
 */
@Data
@Builder
public class GCCause {
    
    private String cause;           // GC 原因
    private int count;              // 发生次数
    private double avgTime;         // 平均时间 (ms)
    private double maxTime;         // 最大时间 (ms)
    private double minTime;         // 最小时间 (ms)
    private double totalTime;       // 总时间 (ms)
    private double percentage;      // 占总GC时间的百分比
    
    public String getAvgTimeFormatted() {
        return String.format("%.3f ms", avgTime);
    }
    
    public String getMaxTimeFormatted() {
        return String.format("%.3f ms", maxTime);
    }
    
    public String getTotalTimeFormatted() {
        if (totalTime >= 1000) {
            return String.format("%.3f s", totalTime / 1000.0);
        }
        return String.format("%.3f ms", totalTime);
    }
    
    public String getPercentageFormatted() {
        return String.format("%.2f%%", percentage);
    }
}

