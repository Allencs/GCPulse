package com.gcpulse.model;

import lombok.Data;
import lombok.Builder;
import java.util.List;
import java.util.Map;

/**
 * GC分析结果主数据模型
 */
@Data
@Builder
public class GCPulseResult {
    
    // 文件基本信息
    private String fileName;
    private long fileSize;
    private String collectorType;
    
    // JVM内存大小
    private MemorySize memorySize;
    
    // 关键性能指标
    private KPIMetrics kpiMetrics;
    
    // GC事件列表
    private List<GCEvent> gcEvents;
    
    // GC阶段统计
    private Map<String, PhaseStatistics> phaseStatistics;
    
    // 对象统计
    private ObjectStats objectStats;
    
    // CPU统计
    private CPUStats cpuStats;
    
    // 暂停时间分布
    private PauseDurationDistribution pauseDurationDistribution;
    
    // 问题诊断
    private DiagnosisReport diagnosisReport;
    
    // 时间序列数据（用于图表）
    private TimeSeriesData timeSeriesData;
    
    // 新增企业级功能
    private JVMArguments jvmArguments;              // JVM 启动参数
    private TenuringSummary tenuringSummary;        // 老年代晋升总结
    private Map<String, GCCause> gcCauses;          // GC 原因统计
    private SafePointStats safePointStats;          // 安全点统计
    private StringDeduplicationStats stringDedup;   // 字符串去重统计
}

