package com.gcpulse.model;

import lombok.Data;
import lombok.Builder;
import java.util.List;
import java.util.Map;

/**
 * AI优化上下文 - 汇总所有关键信息供AI分析
 * 基于JVM专家角度提取的关键诊断数据
 */
@Data
@Builder
public class OptimizationContext {
    
    // ========== 基础信息 ==========
    /** GC收集器类型 */
    private String collectorType;
    
    /** JVM参数配置 */
    private JVMConfiguration jvmConfiguration;
    
    // ========== 性能指标 ==========
    /** 关键性能指标 */
    private PerformanceMetrics performanceMetrics;
    
    // ========== 内存分析 ==========
    /** 内存使用情况 */
    private MemoryAnalysis memoryAnalysis;
    
    // ========== GC行为分析 ==========
    /** GC行为模式 */
    private GCBehaviorPattern gcBehaviorPattern;
    
    // ========== 问题诊断 ==========
    /** 检测到的问题 */
    private DetectedIssues detectedIssues;
    
    // ========== 趋势分析 ==========
    /** 趋势信息 */
    private TrendAnalysis trendAnalysis;
    
    /**
     * JVM配置信息
     */
    @Data
    @Builder
    public static class JVMConfiguration {
        /** 堆大小配置（Xms, Xmx） */
        private String heapSize;
        /** 新生代配置（Xmn, NewRatio） */
        private String youngGenSize;
        /** GC相关参数 */
        private List<String> gcArguments;
        /** 内存参数 */
        private List<String> memoryArguments;
        /** 其他关键参数 */
        private List<String> otherArguments;
        /** 是否存在明显的配置问题 */
        private List<String> configurationIssues;
    }
    
    /**
     * 性能指标
     */
    @Data
    @Builder
    public static class PerformanceMetrics {
        /** 吞吐量（%） */
        private double throughput;
        /** 平均GC暂停时间（ms） */
        private double avgPauseTime;
        /** 最大GC暂停时间（ms） */
        private double maxPauseTime;
        /** P95暂停时间（ms） */
        private double p95PauseTime;
        /** P99暂停时间（ms） */
        private double p99PauseTime;
        /** GC频率（次/分钟） */
        private double gcFrequency;
        /** 总GC次数 */
        private int totalGCCount;
        /** 性能评级 */
        private String performanceRating; // EXCELLENT, GOOD, FAIR, POOR, CRITICAL
    }
    
    /**
     * 内存分析
     */
    @Data
    @Builder
    public static class MemoryAnalysis {
        /** 堆内存配置大小 */
        private String heapMaxSize;
        /** 平均堆使用率（%） */
        private double avgHeapUsage;
        /** 最高堆使用率（%） */
        private double maxHeapUsage;
        /** 平均GC后堆使用 */
        private String avgHeapAfterGC;
        /** Young Gen平均占比（%） */
        private double avgYoungGenRatio;
        /** Old Gen平均占比（%） */
        private double avgOldGenRatio;
        /** Metaspace使用情况 */
        private String metaspaceUsage;
        /** 是否存在内存泄漏风险 */
        private boolean memoryLeakRisk;
        /** 内存泄漏证据 */
        private List<String> memoryLeakEvidences;
        /** 平均内存回收效率（%） */
        private double avgReclamationRate;
    }
    
    /**
     * GC行为模式
     */
    @Data
    @Builder
    public static class GCBehaviorPattern {
        /** Minor GC频率 */
        private double minorGCFrequency;
        /** Major GC频率 */
        private double majorGCFrequency;
        /** Full GC次数 */
        private int fullGCCount;
        /** 是否有连续Full GC */
        private boolean hasConsecutiveFullGC;
        /** 最大连续Full GC次数 */
        private int maxConsecutiveFullGC;
        /** GC原因分布 */
        private Map<String, Integer> gcCauseDistribution;
        /** 对象晋升模式 */
        private PromotionPattern promotionPattern;
        /** 是否存在晋升失败 */
        private boolean hasPromotionFailure;
        /** GC类型分布 */
        private Map<String, Integer> gcTypeDistribution;
    }
    
    /**
     * 对象晋升模式
     */
    @Data
    @Builder
    public static class PromotionPattern {
        /** 平均晋升速率（MB/s） */
        private double avgPromotionRate;
        /** 最大晋升速率（MB/s） */
        private double maxPromotionRate;
        /** 平均晋升年龄 */
        private Integer avgTenuringAge;
        /** 期望晋升阈值 */
        private Integer desiredSurvivorSize;
    }
    
    /**
     * 检测到的问题
     */
    @Data
    @Builder
    public static class DetectedIssues {
        /** Full GC过多 */
        private boolean excessiveFullGC;
        /** GC暂停时间过长 */
        private boolean longPauses;
        /** 最长暂停时间（ms） */
        private double maxPauseDuration;
        /** GC频率过高 */
        private boolean highGCFrequency;
        /** 内存碎片化 */
        private boolean memoryFragmentation;
        /** 对象晋升过快 */
        private boolean rapidPromotion;
        /** Metaspace增长异常 */
        private boolean abnormalMetaspaceGrowth;
        /** 安全点时间过长 */
        private boolean longSafePointTime;
        /** 严重程度 */
        private String severity; // LOW, MEDIUM, HIGH, CRITICAL
        /** 问题摘要 */
        private List<String> issueSummary;
    }
    
    /**
     * 趋势分析
     */
    @Data
    @Builder
    public static class TrendAnalysis {
        /** 堆使用趋势 */
        private String heapUsageTrend; // STABLE, INCREASING, DECREASING, FLUCTUATING
        /** GC暂停时间趋势 */
        private String pauseTimeTrend;
        /** GC频率趋势 */
        private String gcFrequencyTrend;
        /** Old Gen增长趋势 */
        private String oldGenGrowthTrend;
        /** 是否趋于稳定 */
        private boolean isStable;
        /** 预测问题 */
        private List<String> predictedIssues;
    }
    
    /**
     * 从GCPulseResult构建优化上下文
     */
    public static OptimizationContext fromGCPulseResult(GCPulseResult result) {
        return OptimizationContext.builder()
                .collectorType(result.getCollectorType())
                .jvmConfiguration(buildJVMConfiguration(result))
                .performanceMetrics(buildPerformanceMetrics(result))
                .memoryAnalysis(buildMemoryAnalysis(result))
                .gcBehaviorPattern(buildGCBehaviorPattern(result))
                .detectedIssues(buildDetectedIssues(result))
                .trendAnalysis(buildTrendAnalysis(result))
                .build();
    }
    
    private static JVMConfiguration buildJVMConfiguration(GCPulseResult result) {
        JVMArguments jvmArgs = result.getJvmArguments();
        MemorySize memSize = result.getMemorySize();
        List<String> issues = new java.util.ArrayList<>();
        
        // 检查配置问题
        if (memSize != null && memSize.getHeap() != null) {
            if (memSize.getHeap().getAllocated() == 0) {
                issues.add("未明确设置最大堆大小（-Xmx）");
            }
        }
        
        String heapSize = memSize != null && memSize.getHeap() != null ? 
                String.format("Xmx=%s", memSize.getHeap().getAllocatedFormatted()) : "未知";
        
        String youngGenSize = "默认配置"; // MemorySize doesn't track youngGen separately
        
        return JVMConfiguration.builder()
                .heapSize(heapSize)
                .youngGenSize(youngGenSize)
                .gcArguments(jvmArgs != null ? jvmArgs.getGcArguments() : List.of())
                .memoryArguments(jvmArgs != null ? jvmArgs.getMemoryArguments() : List.of())
                .otherArguments(jvmArgs != null ? jvmArgs.getOtherArguments() : List.of())
                .configurationIssues(issues)
                .build();
    }
    
    private static PerformanceMetrics buildPerformanceMetrics(GCPulseResult result) {
        KPIMetrics kpi = result.getKpiMetrics();
        List<GCEvent> events = result.getGcEvents();
        
        // 计算P95和P99暂停时间
        List<Double> pauseTimes = events.stream()
                .map(GCEvent::getPauseTime)
                .filter(d -> d != null && d > 0)
                .sorted()
                .toList();
        
        double p95 = calculatePercentile(pauseTimes, 0.95);
        double p99 = calculatePercentile(pauseTimes, 0.99);
        
        // 计算GC频率（假设日志时间跨度）
        double gcFrequency = 0.0;
        if (!events.isEmpty() && events.size() > 1) {
            long firstTime = events.get(0).getTimestamp();
            long lastTime = events.get(events.size() - 1).getTimestamp();
            if (lastTime > firstTime) {
                double durationMinutes = (lastTime - firstTime) / 60000.0;
                gcFrequency = events.size() / durationMinutes;
            }
        }
        
        // 性能评级
        String rating = calculatePerformanceRating(kpi, p99);
        
        return PerformanceMetrics.builder()
                .throughput(kpi != null ? kpi.getThroughput() : 0.0)
                .avgPauseTime(kpi != null && kpi.getLatency() != null ? kpi.getLatency().getAvgPauseTime() : 0.0)
                .maxPauseTime(kpi != null && kpi.getLatency() != null ? kpi.getLatency().getMaxPauseTime() : 0.0)
                .p95PauseTime(p95)
                .p99PauseTime(p99)
                .gcFrequency(gcFrequency)
                .totalGCCount(events.size())
                .performanceRating(rating)
                .build();
    }
    
    private static MemoryAnalysis buildMemoryAnalysis(GCPulseResult result) {
        List<GCEvent> events = result.getGcEvents();
        MemorySize memSize = result.getMemorySize();
        DiagnosisReport diagnosis = result.getDiagnosisReport();
        
        // 计算平均堆使用率
        double avgHeapUsage = events.stream()
                .filter(e -> e.getHeapMemory() != null && e.getHeapMemory().getTotal() > 0)
                .mapToDouble(e -> (e.getHeapMemory().getAfter() * 100.0) / e.getHeapMemory().getTotal())
                .average().orElse(0.0);
        
        double maxHeapUsage = events.stream()
                .filter(e -> e.getHeapMemory() != null && e.getHeapMemory().getTotal() > 0)
                .mapToDouble(e -> (e.getHeapMemory().getAfter() * 100.0) / e.getHeapMemory().getTotal())
                .max().orElse(0.0);
        
        long avgHeapAfter = (long) events.stream()
                .filter(e -> e.getHeapMemory() != null)
                .mapToLong(e -> e.getHeapMemory().getAfter())
                .average().orElse(0.0);
        
        // 计算回收效率
        double avgReclamationRate = events.stream()
                .filter(e -> e.getHeapMemory() != null)
                .filter(e -> e.getHeapMemory().getBefore() > e.getHeapMemory().getAfter())
                .mapToDouble(e -> {
                    long reclaimed = e.getHeapMemory().getBefore() - e.getHeapMemory().getAfter();
                    return (reclaimed * 100.0) / e.getHeapMemory().getBefore();
                })
                .average().orElse(0.0);
        
        // 内存泄漏检测
        boolean memoryLeakRisk = diagnosis != null && 
                diagnosis.getMemoryLeakInfo() != null && 
                diagnosis.getMemoryLeakInfo().isHasMemoryLeak();
        
        List<String> evidences = memoryLeakRisk && diagnosis.getMemoryLeakInfo().getEvidences() != null ?
                diagnosis.getMemoryLeakInfo().getEvidences() : List.of();
        
        String maxHeapStr = "未知";
        if (memSize != null && memSize.getHeap() != null) {
            maxHeapStr = memSize.getHeap().getAllocatedFormatted();
        }
        
        return MemoryAnalysis.builder()
                .heapMaxSize(maxHeapStr)
                .avgHeapUsage(avgHeapUsage)
                .maxHeapUsage(maxHeapUsage)
                .avgHeapAfterGC(formatBytes(avgHeapAfter))
                .avgYoungGenRatio(0.0) // 可以后续计算
                .avgOldGenRatio(0.0)
                .metaspaceUsage("需要从日志提取")
                .memoryLeakRisk(memoryLeakRisk)
                .memoryLeakEvidences(evidences)
                .avgReclamationRate(avgReclamationRate)
                .build();
    }
    
    private static GCBehaviorPattern buildGCBehaviorPattern(GCPulseResult result) {
        List<GCEvent> events = result.getGcEvents();
        Map<String, GCCause> gcCauses = result.getGcCauses();
        DiagnosisReport diagnosis = result.getDiagnosisReport();
        TenuringSummary tenuring = result.getTenuringSummary();
        
        // 统计GC类型
        Map<String, Integer> gcTypeDistribution = new java.util.HashMap<>();
        events.forEach(e -> {
            String type = e.getEventType() != null ? e.getEventType() : "Unknown";
            gcTypeDistribution.merge(type, 1, Integer::sum);
        });
        
        // GC原因分布
        Map<String, Integer> gcCauseDistribution = new java.util.HashMap<>();
        if (gcCauses != null) {
            gcCauses.forEach((cause, gcCause) -> 
                    gcCauseDistribution.put(cause, gcCause.getCount()));
        }
        
        // Full GC信息
        int fullGCCount = diagnosis != null && diagnosis.getFullGCInfo() != null ?
                diagnosis.getFullGCInfo().getCount() : 0;
        
        boolean hasConsecutiveFullGC = diagnosis != null && 
                diagnosis.getConsecutiveFullGCInfo() != null &&
                diagnosis.getConsecutiveFullGCInfo().isHasConsecutiveFullGC();
        
        int maxConsecutiveFullGC = hasConsecutiveFullGC ?
                diagnosis.getConsecutiveFullGCInfo().getMaxConsecutiveCount() : 0;
        
        // 晋升模式
        PromotionPattern promotionPattern = null;
        if (tenuring != null) {
            // TenuringSummary没有desiredSurvivorSize字段，设为null
            promotionPattern = PromotionPattern.builder()
                    .avgTenuringAge(tenuring.getMaxTenuringThreshold())
                    .desiredSurvivorSize(null)
                    .build();
        }
        
        return GCBehaviorPattern.builder()
                .minorGCFrequency(0.0) // 需要计算
                .majorGCFrequency(0.0)
                .fullGCCount(fullGCCount)
                .hasConsecutiveFullGC(hasConsecutiveFullGC)
                .maxConsecutiveFullGC(maxConsecutiveFullGC)
                .gcCauseDistribution(gcCauseDistribution)
                .promotionPattern(promotionPattern)
                .hasPromotionFailure(gcCauseDistribution.containsKey("Promotion Failed"))
                .gcTypeDistribution(gcTypeDistribution)
                .build();
    }
    
    private static DetectedIssues buildDetectedIssues(GCPulseResult result) {
        DiagnosisReport diagnosis = result.getDiagnosisReport();
        KPIMetrics kpi = result.getKpiMetrics();
        
        boolean excessiveFullGC = diagnosis != null && diagnosis.getFullGCInfo() != null &&
                diagnosis.getFullGCInfo().getCount() > 5;
        
        boolean longPauses = diagnosis != null && diagnosis.getLongPauseInfo() != null &&
                diagnosis.getLongPauseInfo().isHasLongPause();
        
        double maxPause = kpi != null && kpi.getLatency() != null ? 
                kpi.getLatency().getMaxPauseTime() : 0.0;
        
        List<String> issueSummary = new java.util.ArrayList<>();
        if (excessiveFullGC) issueSummary.add("检测到过多Full GC");
        if (longPauses) issueSummary.add("存在长时间GC暂停");
        if (diagnosis != null && diagnosis.getMemoryLeakInfo() != null && 
            diagnosis.getMemoryLeakInfo().isHasMemoryLeak()) {
            issueSummary.add("疑似存在内存泄漏");
        }
        
        String severity = calculateSeverity(excessiveFullGC, longPauses, maxPause);
        
        return DetectedIssues.builder()
                .excessiveFullGC(excessiveFullGC)
                .longPauses(longPauses)
                .maxPauseDuration(maxPause)
                .highGCFrequency(false) // 需要计算
                .memoryFragmentation(false)
                .rapidPromotion(false)
                .abnormalMetaspaceGrowth(false)
                .longSafePointTime(false)
                .severity(severity)
                .issueSummary(issueSummary)
                .build();
    }
    
    private static TrendAnalysis buildTrendAnalysis(GCPulseResult result) {
        List<GCEvent> events = result.getGcEvents();
        
        // 简单的趋势分析
        String heapUsageTrend = analyzeTrend(events, e -> {
            if (e.getHeapMemory() != null && e.getHeapMemory().getTotal() > 0) {
                return (e.getHeapMemory().getAfter() * 100.0) / e.getHeapMemory().getTotal();
            }
            return null;
        });
        
        String pauseTimeTrend = analyzeTrend(events, GCEvent::getPauseTime);
        
        return TrendAnalysis.builder()
                .heapUsageTrend(heapUsageTrend)
                .pauseTimeTrend(pauseTimeTrend)
                .gcFrequencyTrend("STABLE")
                .oldGenGrowthTrend("STABLE")
                .isStable(heapUsageTrend.equals("STABLE"))
                .predictedIssues(List.of())
                .build();
    }
    
    // ========== 辅助方法 ==========
    
    private static String formatBytes(Long bytes) {
        if (bytes == null || bytes == 0) return "未知";
        if (bytes >= 1024 * 1024 * 1024) {
            return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
        } else if (bytes >= 1024 * 1024) {
            return String.format("%.2f MB", bytes / (1024.0 * 1024));
        } else if (bytes >= 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        }
        return bytes + " B";
    }
    
    private static double calculatePercentile(List<Double> sortedValues, double percentile) {
        if (sortedValues.isEmpty()) return 0.0;
        int index = (int) Math.ceil(sortedValues.size() * percentile) - 1;
        index = Math.max(0, Math.min(index, sortedValues.size() - 1));
        return sortedValues.get(index);
    }
    
    private static String calculatePerformanceRating(KPIMetrics kpi, double p99) {
        if (kpi == null) return "UNKNOWN";
        
        double throughput = kpi.getThroughput();
        double maxPause = kpi.getLatency() != null ? kpi.getLatency().getMaxPauseTime() : 0.0;
        
        if (throughput >= 99.0 && maxPause < 100 && p99 < 50) return "EXCELLENT";
        if (throughput >= 95.0 && maxPause < 500 && p99 < 200) return "GOOD";
        if (throughput >= 90.0 && maxPause < 1000) return "FAIR";
        if (throughput >= 80.0) return "POOR";
        return "CRITICAL";
    }
    
    private static String calculateSeverity(boolean excessiveFullGC, boolean longPauses, double maxPause) {
        if ((excessiveFullGC && longPauses) || maxPause > 5000) return "CRITICAL";
        if (excessiveFullGC || maxPause > 1000) return "HIGH";
        if (longPauses || maxPause > 500) return "MEDIUM";
        return "LOW";
    }
    
    private static <T> String analyzeTrend(List<GCEvent> events, java.util.function.Function<GCEvent, T> extractor) {
        if (events.size() < 10) return "INSUFFICIENT_DATA";
        
        List<Double> values = events.stream()
                .map(extractor)
                .filter(v -> v instanceof Number)
                .map(v -> ((Number) v).doubleValue())
                .toList();
        
        if (values.size() < 10) return "INSUFFICIENT_DATA";
        
        // 简单线性趋势分析
        int mid = values.size() / 2;
        double firstHalf = values.subList(0, mid).stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double secondHalf = values.subList(mid, values.size()).stream().mapToDouble(Double::doubleValue).average().orElse(0);
        
        double change = ((secondHalf - firstHalf) / firstHalf) * 100;
        
        if (Math.abs(change) < 10) return "STABLE";
        if (change > 10) return "INCREASING";
        return "DECREASING";
    }
}

