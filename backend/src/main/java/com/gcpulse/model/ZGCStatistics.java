package com.gcpulse.model;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.Map;

/**
 * ZGC统计信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ZGCStatistics {
    
    // MMU (Minimum Mutator Utilization)
    private Map<String, Double> mmuPercentages;  // 如 "2ms" -> 99.5
    
    // 系统负载
    private String systemLoad;  // 如 "3.95/4.22/4.13"
    
    // 标记统计
    private Integer markStripes;
    private Integer proactiveFlushes;
    private Integer terminateFlushes;
    private Integer completions;
    private Integer continuations;
    private String markStackUsage;
    
    // NMethod统计
    private Integer nmethodsRegistered;
    private Integer nmethodsUnregistered;
    
    // 引用统计
    private ReferenceStats softReferences;
    private ReferenceStats weakReferences;
    private ReferenceStats finalReferences;
    private ReferenceStats phantomReferences;
    
    // 页面统计
    private PageStats smallPages;
    private PageStats mediumPages;
    private PageStats largePages;
    
    // Forwarding使用
    private String forwardingUsage;
    
    /**
     * 引用统计
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReferenceStats {
        private Integer encountered;
        private Integer discovered;
        private Integer enqueued;
    }
    
    /**
     * 页面统计
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PageStats {
        private Integer count;
        private String size;
        private String emptySize;
        private String relocatedSize;
        private Integer inPlace;
    }
}

