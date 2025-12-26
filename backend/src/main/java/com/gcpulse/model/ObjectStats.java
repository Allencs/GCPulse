package com.gcpulse.model;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 对象统计信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ObjectStats {
    
    private long totalCreatedBytes;       // 总创建字节数
    private long totalPromotedBytes;      // 总晋升字节数
    private long totalReclaimedBytes;     // 总回收字节数
    private double avgCreationRate;       // 平均创建速率（MB/s）
    private double avgPromotionRate;      // 平均晋升速率（MB/s）
    
    public String getTotalCreatedFormatted() {
        return formatBytes(totalCreatedBytes);
    }
    
    public String getTotalPromotedFormatted() {
        if (totalPromotedBytes <= 0) {
            return "n/a";
        }
        return formatBytes(totalPromotedBytes);
    }
    
    public String getTotalReclaimedFormatted() {
        if (totalReclaimedBytes <= 0) {
            return "N/A";
        }
        return formatBytes(totalReclaimedBytes);
    }
    
    private String formatBytes(long bytes) {
        if (bytes >= 1024L * 1024 * 1024 * 1024) {
            return String.format("%.3f TB", bytes / (1024.0 * 1024 * 1024 * 1024));
        } else if (bytes >= 1024 * 1024 * 1024) {
            return String.format("%.3f GB", bytes / (1024.0 * 1024 * 1024));
        } else {
            return String.format("%.3f MB", bytes / (1024.0 * 1024));
        }
    }
}

