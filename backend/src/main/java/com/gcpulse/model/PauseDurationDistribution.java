package com.gcpulse.model;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

/**
 * GC暂停时间分布
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PauseDurationDistribution {
    
    private List<DurationRange> ranges;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DurationRange {
        private String rangeLabel;        // 范围标签（如 "0-0.1ms"）
        private double minDuration;       // 最小时间（ms）
        private double maxDuration;       // 最大时间（ms）
        private int count;                // 该范围内的GC次数
        private double percentage;        // 占总GC次数的百分比
    }
}

