package com.gcpulse.model;

import lombok.Data;
import lombok.Builder;

/**
 * 关键性能指标
 */
@Data
@Builder
public class KPIMetrics {
    
    // 吞吐量（应用运行时间占比）
    private double throughput;
    
    // 延迟指标
    private LatencyMetrics latency;
    
    // 并发时间指标
    private ConcurrentTimeMetrics concurrentTime;
    
    @Data
    @Builder
    public static class LatencyMetrics {
        private double avgPauseTime;      // 平均暂停时间（ms）
        private double maxPauseTime;      // 最大暂停时间（ms）
        private double minPauseTime;      // 最小暂停时间（ms）
        private double stdDevPauseTime;   // 暂停时间标准差（ms）
    }
    
    @Data
    @Builder
    public static class ConcurrentTimeMetrics {
        private long totalTime;           // 总时间（ms）
        private double avgTime;           // 平均时间（ms）
        private double maxTime;           // 最大时间（ms）
        private double minTime;           // 最小时间（ms）
        private double stdDevTime;        // 标准差（ms）
    }
}

