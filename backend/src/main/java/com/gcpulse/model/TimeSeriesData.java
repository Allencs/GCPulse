package com.gcpulse.model;

import lombok.Data;
import lombok.Builder;
import java.util.List;

/**
 * 时间序列数据（用于图表展示）
 */
@Data
@Builder
public class TimeSeriesData {
    
    // 堆内存使用趋势
    private List<DataPoint> heapUsageTrend;
    
    // GC暂停时间趋势
    private List<DataPoint> pauseTimeTrend;
    
    // GC吞吐量趋势
    private List<DataPoint> throughputTrend;
    
    // 对象创建速率趋势
    private List<DataPoint> allocationRateTrend;
    
    @Data
    @Builder
    public static class DataPoint {
        private long timestamp;           // 时间戳（ms）
        private double value;             // 值
        private String label;             // 标签（可选）
    }
}

