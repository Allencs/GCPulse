package com.gcpulse.model;

import lombok.Data;
import lombok.Builder;
import java.util.List;

/**
 * JVM 启动参数
 */
@Data
@Builder
public class JVMArguments {
    
    private List<String> gcArguments;        // GC 相关参数
    private List<String> memoryArguments;    // 内存相关参数
    private List<String> performanceArguments;  // 性能相关参数
    private List<String> otherArguments;     // 其他参数
    private List<String> allArguments;       // 所有参数
}

