package com.gcpulse.parser;

import com.gcpulse.model.GCEvent;
import com.gcpulse.model.GCPulseResult;
import com.gcpulse.model.JVMArguments;
import com.gcpulse.model.ZGCInitConfig;
import com.gcpulse.model.ZGCStatistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ZGC日志解析器测试
 */
@SpringBootTest
public class ZgcLogParserTest {
    
    @Autowired
    private GCLogParser gcLogParser;
    
    @Test
    public void testZgcLogParsing() throws IOException {
        String logFilePath = "/Users/hb26933/Desktop/gc日志/for_test/jdk21-zgc-simple.log";
        
        try (FileInputStream fis = new FileInputStream(logFilePath)) {
            GCPulseResult result = gcLogParser.parse(fis, "jdk21-zgc-simple.log", 0L);
            
            // 验证基本信息
            assertNotNull(result);
            assertEquals("ZGC", result.getCollectorType());
            
            // 验证GC事件
            List<GCEvent> gcEvents = result.getGcEvents();
            assertNotNull(gcEvents);
            assertTrue(gcEvents.size() > 0, "应该至少解析到1个GC事件");
            
            System.out.println("==== GC事件信息 ====");
            System.out.println("GC事件总数: " + gcEvents.size());
            if (!gcEvents.isEmpty()) {
                GCEvent firstEvent = gcEvents.get(0);
                System.out.println("第一个GC事件类型: " + firstEvent.getEventType());
                System.out.println("第一个GC事件暂停时间: " + firstEvent.getPauseTime() + " ms");
                System.out.println("第一个GC事件并发时间: " + firstEvent.getConcurrentTime() + " ms");
            }
            
            // 验证JVM启动参数
            JVMArguments jvmArgs = result.getJvmArguments();
            assertNotNull(jvmArgs);
            assertNotNull(jvmArgs.getAllArguments());
            assertTrue(jvmArgs.getAllArguments().size() > 0, "应该至少解析到1个JVM参数");
            
            System.out.println("\n==== JVM启动参数 ====");
            System.out.println("参数总数: " + jvmArgs.getAllArguments().size());
            System.out.println("GC相关参数: " + jvmArgs.getGcArguments());
            System.out.println("内存相关参数: " + jvmArgs.getMemoryArguments());
            
            // 验证ZGC初始化配置
            ZGCInitConfig zgcConfig = result.getZgcInitConfig();
            assertNotNull(zgcConfig, "应该解析到ZGC初始化配置");
            
            System.out.println("\n==== ZGC初始化配置 ====");
            System.out.println("版本: " + zgcConfig.getVersion());
            System.out.println("模式: " + zgcConfig.getMode());
            System.out.println("CPU总数: " + zgcConfig.getCpuTotal());
            System.out.println("CPU可用: " + zgcConfig.getCpuAvailable());
            System.out.println("总内存: " + zgcConfig.getTotalMemory());
            System.out.println("NUMA支持: " + zgcConfig.getNumaSupport());
            System.out.println("大页支持: " + zgcConfig.getLargePageSupport());
            System.out.println("GC工作线程: " + zgcConfig.getGcWorkers());
            System.out.println("GC工作线程模式: " + zgcConfig.getGcWorkersMode());
            System.out.println("最小堆容量: " + zgcConfig.getMinCapacity());
            System.out.println("初始堆容量: " + zgcConfig.getInitialCapacity());
            System.out.println("最大堆容量: " + zgcConfig.getMaxCapacity());
            
            // 验证ZGC统计信息
            ZGCStatistics zgcStats = result.getZgcStatistics();
            assertNotNull(zgcStats, "应该解析到ZGC统计信息");
            
            System.out.println("\n==== ZGC统计信息 ====");
            if (zgcStats.getMmuPercentages() != null && !zgcStats.getMmuPercentages().isEmpty()) {
                System.out.println("MMU (Minimum Mutator Utilization):");
                zgcStats.getMmuPercentages().forEach((k, v) -> 
                    System.out.println("  " + k + ": " + v + "%"));
            }
            System.out.println("系统负载: " + zgcStats.getSystemLoad());
            System.out.println("标记条纹: " + zgcStats.getMarkStripes());
            System.out.println("NMethods注册: " + zgcStats.getNmethodsRegistered());
            System.out.println("NMethods注销: " + zgcStats.getNmethodsUnregistered());
            
            if (zgcStats.getSoftReferences() != null) {
                System.out.println("\n软引用统计:");
                System.out.println("  遇到: " + zgcStats.getSoftReferences().getEncountered());
                System.out.println("  发现: " + zgcStats.getSoftReferences().getDiscovered());
                System.out.println("  入队: " + zgcStats.getSoftReferences().getEnqueued());
            }
            
            if (zgcStats.getSmallPages() != null) {
                System.out.println("\n小页面统计:");
                System.out.println("  数量: " + zgcStats.getSmallPages().getCount());
                System.out.println("  大小: " + zgcStats.getSmallPages().getSize());
                System.out.println("  重定位: " + zgcStats.getSmallPages().getRelocatedSize());
            }
            
            // 验证KPI指标
            assertNotNull(result.getKpiMetrics());
            System.out.println("\n==== KPI指标 ====");
            System.out.println("吞吐量: " + result.getKpiMetrics().getThroughput() + "%");
            if (result.getKpiMetrics().getLatency() != null) {
                System.out.println("平均暂停时间: " + result.getKpiMetrics().getLatency().getAvgPauseTime() + " ms");
                System.out.println("最大暂停时间: " + result.getKpiMetrics().getLatency().getMaxPauseTime() + " ms");
            }
        }
    }
}

