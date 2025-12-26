package com.gcpulse.model;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * ZGC初始化配置信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ZGCInitConfig {
    
    // 版本信息
    private String version;
    
    // ZGC模式
    private String mode;  // "Generational" 或 "Single-Generation"
    
    // CPU信息
    private Integer cpuTotal;
    private Integer cpuAvailable;
    
    // 内存信息
    private String totalMemory;  // 如 "16384M"
    
    // NUMA支持
    private Boolean numaSupport;
    
    // 大页支持
    private Boolean largePageSupport;
    
    // GC工作线程
    private Integer gcWorkers;
    private String gcWorkersMode;  // "static" 或 "dynamic"
    
    // 地址空间配置
    private String addressSpaceType;
    private String addressSpaceSize;
    
    // 堆容量配置
    private String minCapacity;
    private String initialCapacity;
    private String maxCapacity;
    
    // 页大小
    private String mediumPageSize;
    
    // 其他配置
    private Boolean preTouch;
    private Boolean uncommit;
    private String uncommitDelay;
    
    // Runtime Workers
    private Integer runtimeWorkers;
}

