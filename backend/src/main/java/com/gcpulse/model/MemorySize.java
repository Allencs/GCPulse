package com.gcpulse.model;

import lombok.Data;
import lombok.Builder;

/**
 * JVM内存大小信息
 */
@Data
@Builder
public class MemorySize {
    
    // 堆内存
    private MemoryRegion heap;
    
    // 元空间
    private MemoryRegion metaspace;
    
    // 总内存
    private MemoryRegion total;
    
    @Data
    @Builder
    public static class MemoryRegion {
        private long allocated;     // 分配的内存（字节）
        private long peak;          // 峰值使用（字节）
        
        // 格式化显示（MB或GB）
        public String getAllocatedFormatted() {
            return formatBytes(allocated);
        }
        
        public String getPeakFormatted() {
            return formatBytes(peak);
        }
        
        private String formatBytes(long bytes) {
            if (bytes >= 1024 * 1024 * 1024) {
                return String.format("%.3f GB", bytes / (1024.0 * 1024 * 1024));
            } else {
                return String.format("%.3f MB", bytes / (1024.0 * 1024));
            }
        }
    }
}

