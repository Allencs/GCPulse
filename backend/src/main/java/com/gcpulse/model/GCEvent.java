package com.gcpulse.model;

import lombok.Data;
import lombok.Builder;

/**
 * GC事件
 */
@Data
@Builder
public class GCEvent {
    
    private long timestamp;           // 时间戳（相对于JVM启动）
    private String eventType;         // 事件类型（Young GC, Full GC, etc）
    private String gcCause;           // GC原因（G1 Evacuation Pause, Metadata GC Threshold, etc）
    private double pauseTime;         // 暂停时间（ms）
    private double concurrentTime;    // 并发时间（ms）
    
    // 内存变化
    private MemoryChange heapMemory;
    private MemoryChange youngGen;
    private MemoryChange oldGen;
    private MemoryChange metaspace;
    
    // 是否为Full GC
    private boolean isFullGC;
    
    // 是否为长暂停
    private boolean isLongPause;
    
    @Data
    @Builder
    public static class MemoryChange {
        private long before;          // GC前内存大小（字节）
        private long after;           // GC后内存大小（字节）
        private long total;           // 总内存大小（字节）
        
        public long getReclaimed() {
            return before - after;
        }
        
        public double getUsageBeforePercent() {
            return total > 0 ? (before * 100.0 / total) : 0;
        }
        
        public double getUsageAfterPercent() {
            return total > 0 ? (after * 100.0 / total) : 0;
        }
    }
}

