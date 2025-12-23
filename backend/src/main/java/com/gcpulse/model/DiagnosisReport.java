package com.gcpulse.model;

import lombok.Data;
import lombok.Builder;
import java.util.List;

/**
 * 诊断报告
 */
@Data
@Builder
public class DiagnosisReport {
    
    // 内存泄漏检测
    private MemoryLeakInfo memoryLeakInfo;
    
    // Full GC信息
    private FullGCInfo fullGCInfo;
    
    // 长暂停信息
    private LongPauseInfo longPauseInfo;
    
    // 连续 Full GC 信息
    private ConsecutiveFullGCInfo consecutiveFullGCInfo;
    
    // 安全点信息
    private SafePointInfo safePointInfo;
    
    // 优化建议
    private List<Recommendation> recommendations;
    
    @Data
    @Builder
    public static class MemoryLeakInfo {
        private boolean hasMemoryLeak;
        private String description;
        private List<String> evidences;
    }
    
    @Data
    @Builder
    public static class FullGCInfo {
        private int count;
        private boolean hasFullGC;
        private List<GCEvent> fullGCEvents;
    }
    
    @Data
    @Builder
    public static class LongPauseInfo {
        private int count;
        private boolean hasLongPause;
        private double threshold;         // 长暂停阈值（ms）
        private List<GCEvent> longPauseEvents;
    }
    
    @Data
    @Builder
    public static class ConsecutiveFullGCInfo {
        private boolean hasConsecutiveFullGC;
        private int maxConsecutiveCount;    // 最大连续次数
        private List<ConsecutiveFullGCSequence> sequences;  // 连续 Full GC 序列
        private String severity;            // 严重程度 (LOW, MEDIUM, HIGH, CRITICAL)
        
        @Data
        @Builder
        public static class ConsecutiveFullGCSequence {
            private int count;              // 连续次数
            private long startTimestamp;    // 开始时间戳
            private long endTimestamp;      // 结束时间戳
            private double totalDuration;   // 总持续时间 (ms)
            private List<GCEvent> events;   // Full GC 事件列表
        }
    }
    
    @Data
    @Builder
    public static class SafePointInfo {
        private long totalTime;           // 总时间（ms）
        private double avgTime;           // 平均时间（ms）
        private double percentage;        // 占总运行时间的百分比
    }
    
    @Data
    @Builder
    public static class Recommendation {
        private String category;          // 类别（内存、GC配置、性能等）
        private String level;             // 级别（INFO, WARNING, CRITICAL）
        private String title;             // 标题
        private String description;       // 描述
        private String suggestion;        // 建议
    }
}

