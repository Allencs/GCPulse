package com.gcpulse.model;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * GC阶段统计信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PhaseStatistics {
    
    private String phaseName;         // 阶段名称
    private long totalTime;           // 总时间（ms）
    private double avgTime;           // 平均时间（ms）
    private double maxTime;           // 最大时间（ms）
    private double minTime;           // 最小时间（ms）
    private double stdDevTime;        // 标准差（ms）
    private int count;                // 执行次数
    
    // 计算百分比
    public double getPercentage(long totalDuration) {
        return totalDuration > 0 ? (totalTime * 100.0 / totalDuration) : 0;
    }
}

