package com.gcpulse.model;

import lombok.Data;
import lombok.Builder;
import java.util.Map;

/**
 * 老年代晋升总结
 */
@Data
@Builder
public class TenuringSummary {
    
    private Integer maxTenuringThreshold;    // 最大晋升阈值
    private Integer avgTenuringThreshold;    // 平均晋升阈值
    private Map<Integer, Long> ageDistribution;  // 年龄分布 (age -> count)
    private long totalSurvivedObjects;       // 总存活对象数
    private long totalPromotedObjects;       // 总晋升对象数
    private double promotionRate;            // 晋升率 (%)
    
    public String getPromotionRateFormatted() {
        return String.format("%.3f%%", promotionRate);
    }
}

