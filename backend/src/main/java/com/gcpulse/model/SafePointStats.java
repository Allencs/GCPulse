package com.gcpulse.model;

import lombok.Data;
import lombok.Builder;
import java.util.List;

/**
 * 安全点统计
 */
@Data
@Builder
public class SafePointStats {
    
    private long totalCount;          // 总次数
    private double avgDuration;       // 平均持续时间 (ms)
    private double maxDuration;       // 最大持续时间 (ms)
    private double minDuration;       // 最小持续时间 (ms)
    private double totalDuration;     // 总持续时间 (ms)
    private double avgTimeToSafePoint;  // 平均到达安全点时间 (ms)
    private List<SafePointEvent> longSafePoints;  // 长安全点事件列表
    
    public String getAvgDurationFormatted() {
        return String.format("%.3f ms", avgDuration);
    }
    
    public String getMaxDurationFormatted() {
        return String.format("%.3f ms", maxDuration);
    }
    
    public String getTotalDurationFormatted() {
        if (totalDuration >= 1000) {
            return String.format("%.3f s", totalDuration / 1000.0);
        }
        return String.format("%.3f ms", totalDuration);
    }
    
    @Data
    @Builder
    public static class SafePointEvent {
        private long timestamp;       // 时间戳
        private double duration;      // 持续时间 (ms)
        private String operation;     // 操作类型
    }
}

