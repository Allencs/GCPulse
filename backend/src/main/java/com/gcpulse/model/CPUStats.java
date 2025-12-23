package com.gcpulse.model;

import lombok.Data;
import lombok.Builder;

/**
 * CPU统计信息
 */
@Data
@Builder
public class CPUStats {
    
    private Double cpuTime;           // CPU总时间（秒）
    private Double userTime;          // 用户态时间（秒）
    private Double sysTime;           // 系统态时间（秒）
    
    // 是否有CPU数据
    public boolean hasCPUData() {
        return cpuTime != null && cpuTime > 0;
    }
    
    public String getCpuTimeFormatted() {
        return cpuTime != null ? String.format("%.3f s", cpuTime) : "n/a";
    }
    
    public String getUserTimeFormatted() {
        return userTime != null ? String.format("%.3f s", userTime) : "n/a";
    }
    
    public String getSysTimeFormatted() {
        return sysTime != null ? String.format("%.3f s", sysTime) : "n/a";
    }
}

