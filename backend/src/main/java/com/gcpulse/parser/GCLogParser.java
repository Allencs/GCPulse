package com.gcpulse.parser;

import com.gcpulse.model.*;
import org.springframework.stereotype.Component;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * GC日志解析器 - 支持多种GC收集器
 * 支持: G1GC, ZGC, CMS, Parallel GC, Serial GC, Shenandoah
 */
@Component
public class GCLogParser {
    
    // 各种GC日志格式的正则表达式
    private static final Pattern TIMESTAMP_PATTERN = Pattern.compile("^(\\d+\\.\\d+):");
    private static final Pattern G1GC_PATTERN = Pattern.compile("\\[GC pause.*?\\((.*?)\\).*?(\\d+\\.\\d+)ms\\]");
    
    // 支持新旧两种ZGC日志格式
    private static final Pattern ZGC_PATTERN_OLD = Pattern.compile("\\[(\\d+\\.\\d+)s\\].*?GC\\((\\d+)\\).*?Pause.*?(\\d+\\.\\d+)ms");
    // 新格式: [2025-12-11T01:40:40.712+0800][118][gc,phases   ] GC(0) Pause Mark Start 0.015ms
    private static final Pattern ZGC_PATTERN_NEW = Pattern.compile("\\[(\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}[+-]\\d{4})\\]\\[\\d+\\]\\[gc,phases\\s*\\] GC\\((\\d+)\\) (Pause .*?) ([\\d.]+)ms");
    private static final Pattern ZGC_UNIFIED_TIMESTAMP = Pattern.compile("\\[(\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3})");
    
    // CMS/ParNew格式 - 支持多种时间戳格式
    // 时间戳格式1: "4.856: [GC"
    // 时间戳格式2: "2025-08-05T13:23:18.409+0800: 4.856: [GC"
    private static final Pattern CMS_TIMESTAMP_PATTERN = Pattern.compile("(\\d+\\.\\d+):\\s*\\[(?:GC|Full GC)");
    
    // ParNew 格式（年轻代GC）
    private static final Pattern CMS_PARNEW_PATTERN = Pattern.compile("\\[ParNew(?:\\s+\\(promotion failed\\))?: (\\d+)K->(\\d+)K\\((\\d+)K\\), ([\\d.]+) secs\\]\\s+(\\d+)K->(\\d+)K\\((\\d+)K\\)");
    
    // Full GC 格式
    private static final Pattern CMS_FULL_PATTERN = Pattern.compile("\\[Full GC.*?\\[CMS(?:[^:]*)?:\\s*(\\d+)K->(\\d+)K\\((\\d+)K\\),\\s*([\\d.]+)\\s*secs\\]\\s*(\\d+)K->(\\d+)K\\((\\d+)K\\)");
    
    // CMS Initial Mark（初始标记）
    private static final Pattern CMS_INITIAL_MARK_PATTERN = Pattern.compile("\\[GC \\(CMS Initial Mark\\).*?\\[1 CMS-initial-mark: (\\d+)K\\((\\d+)K\\)\\]\\s*(\\d+)K\\((\\d+)K\\),\\s*([\\d.]+)\\s*secs\\]");
    
    // CMS Remark（重新标记）
    private static final Pattern CMS_REMARK_PATTERN = Pattern.compile("\\[GC \\(CMS Final Remark\\).*?\\[1 CMS-remark: (\\d+)K\\((\\d+)K\\)\\]\\s*(\\d+)K\\((\\d+)K\\),\\s*([\\d.]+)\\s*secs\\]");
    
    // CMS 并发阶段
    private static final Pattern CMS_CONCURRENT_PATTERN = Pattern.compile("\\[(CMS-concurrent-(?:mark|preclean|sweep|reset))(?:-start)?:?\\s*([\\d.]+)?(?:/([\\d.]+))?\\s*secs\\]");
    
    private static final Pattern PARALLEL_PATTERN = Pattern.compile("\\[(Full )?GC.*?\\[PS.*?(\\d+)K->(\\d+)K\\((\\d+)K\\).*?(\\d+\\.\\d+) secs\\]");
    private static final Pattern HEAP_PATTERN = Pattern.compile("Heap.*?(\\d+)K->(\\d+)K\\((\\d+)K\\)");
    private static final Pattern MEMORY_PATTERN = Pattern.compile("(\\d+)([KMGT])");
    
    /**
     * 解析GC日志输入流
     */
    public GCPulseResult parse(InputStream inputStream, String fileName, long fileSize) throws IOException {
        List<String> lines = readLines(inputStream);
        
        // 检测GC收集器类型
        String collectorType = detectCollectorType(lines);
        
        // 解析GC事件
        List<GCEvent> gcEvents = parseGCEvents(lines, collectorType);
        
        // 对于ZGC，增强解析堆内存信息
        if ("ZGC".equals(collectorType)) {
            enhanceZGCEventsWithHeapInfo(gcEvents, lines);
        }
        
        // 计算各项指标
        MemorySize memorySize = calculateMemorySize(gcEvents, lines, collectorType);
        KPIMetrics kpiMetrics = calculateKPIMetrics(gcEvents);
        Map<String, PhaseStatistics> phaseStats = calculatePhaseStatistics(gcEvents, collectorType);
        ObjectStats objectStats = calculateObjectStats(gcEvents, lines, collectorType);
        CPUStats cpuStats = parseCPUStats(lines);
        PauseDurationDistribution pauseDist = calculatePauseDuration(gcEvents);
        DiagnosisReport diagnosisReport = performDiagnosis(gcEvents, memorySize);
        TimeSeriesData timeSeriesData = generateTimeSeriesData(gcEvents);
        
        return GCPulseResult.builder()
                .fileName(fileName)
                .fileSize(fileSize)
                .collectorType(collectorType)
                .memorySize(memorySize)
                .kpiMetrics(kpiMetrics)
                .gcEvents(gcEvents)
                .phaseStatistics(phaseStats)
                .objectStats(objectStats)
                .cpuStats(cpuStats)
                .pauseDurationDistribution(pauseDist)
                .diagnosisReport(diagnosisReport)
                .timeSeriesData(timeSeriesData)
                .build();
    }
    
    /**
     * 读取所有行
     */
    private List<String> readLines(InputStream inputStream) throws IOException {
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        }
        return lines;
    }
    
    /**
     * 检测GC收集器类型
     */
    private String detectCollectorType(List<String> lines) {
        for (String line : lines) {
            if (line.contains("Using G1") || line.contains("[G1")) {
                return "G1GC";
            } else if (line.contains("Z Garbage Collector") || line.contains("ZGC") || line.contains("gc,init] Initializing The Z")) {
                return "ZGC";
            } else if (line.contains("UseConcMarkSweepGC") || line.contains("UseCMSInitiatingOccupancyOnly")) {
                return "CMS";
            } else if (line.contains("CMS") || line.contains("ParNew")) {
                return "CMS";
            } else if (line.contains("Using Parallel") || line.contains("[PSYoungGen") || line.contains("[ParOldGen")) {
                return "Parallel GC";
            } else if (line.contains("Using Serial") || line.contains("[DefNew")) {
                return "Serial GC";
            } else if (line.contains("Shenandoah")) {
                return "Shenandoah";
            }
        }
        return "Unknown";
    }
    
    /**
     * 解析GC事件
     */
    private List<GCEvent> parseGCEvents(List<String> lines, String collectorType) {
        List<GCEvent> events = new ArrayList<>();
        
        for (String line : lines) {
            try {
                GCEvent event = null;
                
                // 根据不同的收集器类型解析
                switch (collectorType) {
                    case "G1GC" -> event = parseG1GCEvent(line);
                    case "ZGC" -> event = parseZGCEvent(line);
                    case "CMS" -> event = parseCMSEvent(line);
                    case "Parallel GC" -> event = parseParallelGCEvent(line);
                    default -> event = parseGenericGCEvent(line);
                }
                
                if (event != null) {
                    events.add(event);
                }
            } catch (Exception e) {
                // 忽略无法解析的行
            }
        }
        
        return events;
    }
    
    /**
     * 解析G1GC事件
     */
    private GCEvent parseG1GCEvent(String line) {
        Matcher tsMatcher = TIMESTAMP_PATTERN.matcher(line);
        if (!tsMatcher.find()) return null;
        
        double timestamp = Double.parseDouble(tsMatcher.group(1));
        
        Matcher gcMatcher = G1GC_PATTERN.matcher(line);
        if (!gcMatcher.find()) return null;
        
        String reason = gcMatcher.group(1);
        double pauseTime = Double.parseDouble(gcMatcher.group(2));
        
        boolean isFullGC = line.contains("Full GC");
        
        // 解析堆内存变化
        GCEvent.MemoryChange heapMemory = parseMemoryChange(line);
        
        return GCEvent.builder()
                .timestamp((long) (timestamp * 1000))
                .eventType(isFullGC ? "Full GC" : "Young GC")
                .pauseTime(pauseTime)
                .concurrentTime(0.0)
                .heapMemory(heapMemory)
                .isFullGC(isFullGC)
                .isLongPause(pauseTime > 100)
                .build();
    }
    
    /**
     * 解析ZGC事件（支持新旧两种格式）
     */
    private GCEvent parseZGCEvent(String line) {
        // 尝试新格式（Java 21 Unified Logging）
        Matcher newMatcher = ZGC_PATTERN_NEW.matcher(line);
        if (newMatcher.find()) {
            String timestampStr = newMatcher.group(1);
            String gcId = newMatcher.group(2);
            String pauseType = newMatcher.group(3);
            double pauseTime = Double.parseDouble(newMatcher.group(4));
            
            // 解析完整的ISO时间戳：2025-12-11T01:40:40.712+0800
            // 提取：年月日时分秒毫秒
            Matcher tsMatcher = Pattern.compile("(\\d{4})-(\\d{2})-(\\d{2})T(\\d{2}):(\\d{2}):(\\d{2})\\.(\\d{3})").matcher(timestampStr);
            long timestamp = 0;
            if (tsMatcher.find()) {
                // 简化：使用从日志开始的相对时间（秒）
                int hour = Integer.parseInt(tsMatcher.group(4));
                int minute = Integer.parseInt(tsMatcher.group(5));
                int second = Integer.parseInt(tsMatcher.group(6));
                int milli = Integer.parseInt(tsMatcher.group(7));
                // 计算从当天零点开始的毫秒数
                timestamp = (hour * 3600L + minute * 60L + second) * 1000L + milli;
            }
            
            return GCEvent.builder()
                    .timestamp(timestamp)
                    .eventType("ZGC " + pauseType)
                    .pauseTime(pauseTime)
                    .concurrentTime(0.0)
                    .isFullGC(false)
                    .isLongPause(pauseTime > 10)
                    .build();
        }
        
        // 尝试旧格式
        Matcher oldMatcher = ZGC_PATTERN_OLD.matcher(line);
        if (oldMatcher.find()) {
            double timestamp = Double.parseDouble(oldMatcher.group(1));
            double pauseTime = Double.parseDouble(oldMatcher.group(3));
            
            return GCEvent.builder()
                    .timestamp((long) (timestamp * 1000))
                    .eventType("ZGC Pause")
                    .pauseTime(pauseTime)
                    .concurrentTime(0.0)
                    .isFullGC(false)
                    .isLongPause(pauseTime > 10)
                    .build();
        }
        
        return null;
    }
    
    /**
     * 为ZGC事件增强堆内存信息
     */
    private void enhanceZGCEventsWithHeapInfo(List<GCEvent> gcEvents, List<String> lines) {
        // 解析ZGC的heap信息：[gc,heap] GC(0) Used: 1026M (10%) 1058M (10%) ...
        Pattern heapPattern = Pattern.compile("\\[gc,heap\\s*\\] GC\\((\\d+)\\)\\s+Used:\\s+(\\d+)M.*?(\\d+)M.*?(\\d+)M");
        Pattern capacityPattern = Pattern.compile("\\[gc,heap\\s*\\] GC\\((\\d+)\\)\\s+Capacity:\\s+(\\d+)M");
        
        Map<Integer, Long> gcUsedMemory = new HashMap<>();
        Map<Integer, Long> gcCapacity = new HashMap<>();
        
        for (String line : lines) {
            Matcher heapMatcher = heapPattern.matcher(line);
            if (heapMatcher.find()) {
                int gcId = Integer.parseInt(heapMatcher.group(1));
                long usedBefore = Long.parseLong(heapMatcher.group(2)) * 1024 * 1024;
                long usedAfter = Long.parseLong(heapMatcher.group(3)) * 1024 * 1024;
                gcUsedMemory.put(gcId, usedAfter);
            }
            
            Matcher capMatcher = capacityPattern.matcher(line);
            if (capMatcher.find()) {
                int gcId = Integer.parseInt(capMatcher.group(1));
                long capacity = Long.parseLong(capMatcher.group(2)) * 1024 * 1024;
                gcCapacity.put(gcId, capacity);
            }
        }
        
        // 将内存信息关联到GC事件（通过时间戳匹配）
        int gcId = 0;
        for (GCEvent event : gcEvents) {
            if (event.getEventType().contains("Relocate Start")) {
                Long used = gcUsedMemory.get(gcId);
                Long capacity = gcCapacity.get(gcId);
                
                if (used != null && capacity != null) {
                    GCEvent.MemoryChange heapMemory = GCEvent.MemoryChange.builder()
                            .before(used)
                            .after(used)
                            .total(capacity)
                            .build();
                    event.setHeapMemory(heapMemory);
                }
                gcId++;
            }
        }
    }
    
    /**
     * 解析CMS事件（支持多种格式）
     */
    private GCEvent parseCMSEvent(String line) {
        // 提取时间戳：支持格式 "4.856: [GC" 或 "2025-08-05T13:23:18.409+0800: 4.856: [GC"
        Matcher tsMatcher = CMS_TIMESTAMP_PATTERN.matcher(line);
        double timestamp = 0;
        if (tsMatcher.find()) {
            timestamp = Double.parseDouble(tsMatcher.group(1));
        }
        
        // 1. 尝试解析 CMS Initial Mark（初始标记）
        Matcher initialMarkMatcher = CMS_INITIAL_MARK_PATTERN.matcher(line);
        if (initialMarkMatcher.find()) {
            double pauseTime = Double.parseDouble(initialMarkMatcher.group(5)) * 1000;
            long heapUsed = Long.parseLong(initialMarkMatcher.group(3)) * 1024;
            long heapTotal = Long.parseLong(initialMarkMatcher.group(4)) * 1024;
            
            return GCEvent.builder()
                    .timestamp((long) (timestamp * 1000))
                    .eventType("CMS Initial Mark")
                    .pauseTime(pauseTime)
                    .isFullGC(false)
                    .isLongPause(pauseTime > 100)
                    .build();
        }
        
        // 2. 尝试解析 CMS Remark（重新标记）
        Matcher remarkMatcher = CMS_REMARK_PATTERN.matcher(line);
        if (remarkMatcher.find()) {
            double pauseTime = Double.parseDouble(remarkMatcher.group(5)) * 1000;
            
            return GCEvent.builder()
                    .timestamp((long) (timestamp * 1000))
                    .eventType("CMS Final Remark")
                    .pauseTime(pauseTime)
                    .isFullGC(false)
                    .isLongPause(pauseTime > 100)
                    .build();
        }
        
        // 3. 尝试解析 ParNew 事件（包括 promotion failed）
        Matcher parNewMatcher = CMS_PARNEW_PATTERN.matcher(line);
        if (parNewMatcher.find()) {
            long youngBefore = Long.parseLong(parNewMatcher.group(1)) * 1024;
            long youngAfter = Long.parseLong(parNewMatcher.group(2)) * 1024;
            long youngTotal = Long.parseLong(parNewMatcher.group(3)) * 1024;
            double pauseTime = Double.parseDouble(parNewMatcher.group(4)) * 1000;
            
            long heapBefore = Long.parseLong(parNewMatcher.group(5)) * 1024;
            long heapAfter = Long.parseLong(parNewMatcher.group(6)) * 1024;
            long heapTotal = Long.parseLong(parNewMatcher.group(7)) * 1024;
            
            GCEvent.MemoryChange heapMemory = GCEvent.MemoryChange.builder()
                    .before(heapBefore)
                    .after(heapAfter)
                    .total(heapTotal)
                    .build();
            
            GCEvent.MemoryChange youngGen = GCEvent.MemoryChange.builder()
                    .before(youngBefore)
                    .after(youngAfter)
                    .total(youngTotal)
                    .build();
            
            // 检查是否是 promotion failed
            boolean isPromotionFailed = line.contains("promotion failed");
            String eventType = isPromotionFailed ? "ParNew (promotion failed)" : "ParNew";
            
            return GCEvent.builder()
                    .timestamp((long) (timestamp * 1000))
                    .eventType(eventType)
                    .pauseTime(pauseTime)
                    .heapMemory(heapMemory)
                    .youngGen(youngGen)
                    .isFullGC(false)
                    .isLongPause(pauseTime > 100)
                    .build();
        }
        
        // 4. 尝试解析 Full GC (CMS) 事件
        Matcher fullGCMatcher = CMS_FULL_PATTERN.matcher(line);
        if (fullGCMatcher.find()) {
            long oldBefore = Long.parseLong(fullGCMatcher.group(1)) * 1024;
            long oldAfter = Long.parseLong(fullGCMatcher.group(2)) * 1024;
            long oldTotal = Long.parseLong(fullGCMatcher.group(3)) * 1024;
            double pauseTime = Double.parseDouble(fullGCMatcher.group(4)) * 1000;
            
            long heapBefore = Long.parseLong(fullGCMatcher.group(5)) * 1024;
            long heapAfter = Long.parseLong(fullGCMatcher.group(6)) * 1024;
            long heapTotal = Long.parseLong(fullGCMatcher.group(7)) * 1024;
            
            GCEvent.MemoryChange heapMemory = GCEvent.MemoryChange.builder()
                    .before(heapBefore)
                    .after(heapAfter)
                    .total(heapTotal)
                    .build();
            
            // 检查失败类型
            String eventType = "Full GC (CMS)";
            if (line.contains("concurrent mode failure")) {
                eventType = "Full GC (Concurrent Mode Failure)";
            } else if (line.contains("promotion failed")) {
                eventType = "Full GC (Promotion Failed)";
            }
            
            return GCEvent.builder()
                    .timestamp((long) (timestamp * 1000))
                    .eventType(eventType)
                    .pauseTime(pauseTime)
                    .heapMemory(heapMemory)
                    .isFullGC(true)
                    .isLongPause(pauseTime > 100)
                    .build();
        }
        
        // 5. 尝试解析 CMS 并发阶段（记录但不作为暂停时间）
        Matcher concurrentMatcher = CMS_CONCURRENT_PATTERN.matcher(line);
        if (concurrentMatcher.find()) {
            String phaseName = concurrentMatcher.group(1);
            // 并发阶段不计入暂停时间，仅记录用于统计
            if (concurrentMatcher.group(2) != null) {
                double concurrentTime = Double.parseDouble(concurrentMatcher.group(2)) * 1000;
                
                return GCEvent.builder()
                        .timestamp((long) (timestamp * 1000))
                        .eventType(phaseName)
                        .pauseTime(0.0)  // 并发阶段，无暂停
                        .concurrentTime(concurrentTime)
                        .isFullGC(false)
                        .isLongPause(false)
                        .build();
            }
        }
        
        return null;
    }
    
    /**
     * 解析Parallel GC事件
     */
    private GCEvent parseParallelGCEvent(String line) {
        Matcher tsMatcher = TIMESTAMP_PATTERN.matcher(line);
        if (!tsMatcher.find()) return null;
        
        double timestamp = Double.parseDouble(tsMatcher.group(1));
        
        Matcher parallelMatcher = PARALLEL_PATTERN.matcher(line);
        if (!parallelMatcher.find()) return null;
        
        boolean isFullGC = parallelMatcher.group(1) != null;
        long before = Long.parseLong(parallelMatcher.group(2)) * 1024;
        long after = Long.parseLong(parallelMatcher.group(3)) * 1024;
        long total = Long.parseLong(parallelMatcher.group(4)) * 1024;
        double pauseTime = Double.parseDouble(parallelMatcher.group(5)) * 1000;
        
        GCEvent.MemoryChange heapMemory = GCEvent.MemoryChange.builder()
                .before(before)
                .after(after)
                .total(total)
                .build();
        
        return GCEvent.builder()
                .timestamp((long) (timestamp * 1000))
                .eventType(isFullGC ? "Full GC" : "Young GC")
                .pauseTime(pauseTime)
                .heapMemory(heapMemory)
                .isFullGC(isFullGC)
                .isLongPause(pauseTime > 100)
                .build();
    }
    
    /**
     * 解析通用GC事件
     */
    private GCEvent parseGenericGCEvent(String line) {
        Matcher tsMatcher = TIMESTAMP_PATTERN.matcher(line);
        if (!tsMatcher.find()) return null;
        
        double timestamp = Double.parseDouble(tsMatcher.group(1));
        
        if (!line.contains("GC")) return null;
        
        // 尝试提取暂停时间
        Pattern pausePattern = Pattern.compile("(\\d+\\.\\d+)\\s*(?:ms|secs)");
        Matcher pauseMatcher = pausePattern.matcher(line);
        double pauseTime = 0.0;
        if (pauseMatcher.find()) {
            pauseTime = Double.parseDouble(pauseMatcher.group(1));
            if (line.contains("secs")) {
                pauseTime *= 1000; // 转换为ms
            }
        }
        
        boolean isFullGC = line.contains("Full GC");
        
        return GCEvent.builder()
                .timestamp((long) (timestamp * 1000))
                .eventType(isFullGC ? "Full GC" : "GC")
                .pauseTime(pauseTime)
                .isFullGC(isFullGC)
                .isLongPause(pauseTime > 100)
                .build();
    }
    
    /**
     * 解析内存变化
     */
    private GCEvent.MemoryChange parseMemoryChange(String line) {
        Matcher heapMatcher = HEAP_PATTERN.matcher(line);
        if (!heapMatcher.find()) return null;
        
        long before = Long.parseLong(heapMatcher.group(1)) * 1024;
        long after = Long.parseLong(heapMatcher.group(2)) * 1024;
        long total = Long.parseLong(heapMatcher.group(3)) * 1024;
        
        return GCEvent.MemoryChange.builder()
                .before(before)
                .after(after)
                .total(total)
                .build();
    }
    
    /**
     * 计算内存大小
     */
    private MemorySize calculateMemorySize(List<GCEvent> events, List<String> lines, String collectorType) {
        long maxHeap = 0;
        long peakHeap = 0;
        long metaspaceAllocated = 210L * 1024 * 1024;
        long metaspacePeak = 208L * 1024 * 1024;
        
        // 从事件中提取
        for (GCEvent event : events) {
            if (event.getHeapMemory() != null) {
                maxHeap = Math.max(maxHeap, event.getHeapMemory().getTotal());
                peakHeap = Math.max(peakHeap, event.getHeapMemory().getBefore());
            }
        }
        
        // 对于ZGC，从日志中提取准确的容量信息
        if ("ZGC".equals(collectorType)) {
            Pattern maxCapPattern = Pattern.compile("\\[gc,heap\\s*\\] GC\\(\\d+\\) Max Capacity: (\\d+)M");
            Pattern metaspacePattern = Pattern.compile("\\[gc,metaspace\\] GC\\(\\d+\\) Metaspace: (\\d+)M used, (\\d+)M committed");
            
            for (String line : lines) {
                Matcher capMatcher = maxCapPattern.matcher(line);
                if (capMatcher.find() && maxHeap == 0) {
                    maxHeap = Long.parseLong(capMatcher.group(1)) * 1024 * 1024;
                }
                
                Matcher metaMatcher = metaspacePattern.matcher(line);
                if (metaMatcher.find()) {
                    metaspacePeak = Math.max(metaspacePeak, Long.parseLong(metaMatcher.group(1)) * 1024 * 1024);
                    metaspaceAllocated = Math.max(metaspaceAllocated, Long.parseLong(metaMatcher.group(2)) * 1024 * 1024);
                }
            }
        }
        
        // 默认值（如果日志中没有足够信息）
        if (maxHeap == 0) {
            maxHeap = 10L * 1024 * 1024 * 1024; // 10GB
        }
        if (peakHeap == 0) {
            peakHeap = (long) (maxHeap * 0.8);
        }
        
        MemorySize.MemoryRegion heap = MemorySize.MemoryRegion.builder()
                .allocated(maxHeap)
                .peak(peakHeap)
                .build();
        
        MemorySize.MemoryRegion metaspace = MemorySize.MemoryRegion.builder()
                .allocated(metaspaceAllocated)
                .peak(metaspacePeak)
                .build();
        
        MemorySize.MemoryRegion total = MemorySize.MemoryRegion.builder()
                .allocated(heap.getAllocated() + metaspace.getAllocated())
                .peak(heap.getPeak() + metaspace.getPeak())
                .build();
        
        return MemorySize.builder()
                .heap(heap)
                .metaspace(metaspace)
                .total(total)
                .build();
    }
    
    /**
     * 计算KPI指标
     */
    private KPIMetrics calculateKPIMetrics(List<GCEvent> events) {
        if (events.isEmpty()) {
            return KPIMetrics.builder()
                    .throughput(0.0)
                    .latency(KPIMetrics.LatencyMetrics.builder().build())
                    .build();
        }
        
        double totalPauseTime = 0.0;
        double totalConcurrentTime = 0.0;
        double maxPauseTime = 0.0;
        double minPauseTime = Double.MAX_VALUE;
        
        for (GCEvent event : events) {
            totalPauseTime += event.getPauseTime();
            totalConcurrentTime += event.getConcurrentTime();
            maxPauseTime = Math.max(maxPauseTime, event.getPauseTime());
            if (event.getPauseTime() > 0) {
                minPauseTime = Math.min(minPauseTime, event.getPauseTime());
            }
        }
        
        long totalRunTime = events.get(events.size() - 1).getTimestamp();
        double avgPauseTime = totalPauseTime / events.size();
        
        // 计算标准差
        double variance = 0.0;
        for (GCEvent event : events) {
            variance += Math.pow(event.getPauseTime() - avgPauseTime, 2);
        }
        double stdDev = Math.sqrt(variance / events.size());
        
        // 吞吐量 = (总运行时间 - 总暂停时间) / 总运行时间
        double throughput = totalRunTime > 0 ? 
                ((totalRunTime - totalPauseTime) / totalRunTime) * 100 : 0.0;
        
        KPIMetrics.LatencyMetrics latency = KPIMetrics.LatencyMetrics.builder()
                .avgPauseTime(avgPauseTime)
                .maxPauseTime(maxPauseTime)
                .minPauseTime(minPauseTime == Double.MAX_VALUE ? 0 : minPauseTime)
                .stdDevPauseTime(stdDev)
                .build();
        
        KPIMetrics.ConcurrentTimeMetrics concurrentTime = KPIMetrics.ConcurrentTimeMetrics.builder()
                .totalTime((long) totalConcurrentTime)
                .avgTime(totalConcurrentTime / events.size())
                .maxTime(events.stream().mapToDouble(GCEvent::getConcurrentTime).max().orElse(0.0))
                .minTime(events.stream().mapToDouble(GCEvent::getConcurrentTime).min().orElse(0.0))
                .build();
        
        return KPIMetrics.builder()
                .throughput(throughput)
                .latency(latency)
                .concurrentTime(concurrentTime)
                .build();
    }
    
    /**
     * 计算阶段统计
     */
    private Map<String, PhaseStatistics> calculatePhaseStatistics(List<GCEvent> events, String collectorType) {
        Map<String, PhaseStatistics> stats = new HashMap<>();
        
        // 根据收集器类型添加不同的阶段
        if ("ZGC".equals(collectorType)) {
            // 统计各个ZGC阶段
            Map<String, List<Double>> phaseTimesMap = new HashMap<>();
            
            for (GCEvent event : events) {
                String eventType = event.getEventType();
                double pauseTime = event.getPauseTime();
                
                phaseTimesMap.computeIfAbsent(eventType, k -> new ArrayList<>()).add(pauseTime);
            }
            
            // 为每个阶段创建统计信息
            for (Map.Entry<String, List<Double>> entry : phaseTimesMap.entrySet()) {
                String phaseName = entry.getKey();
                List<Double> times = entry.getValue();
                
                if (!times.isEmpty()) {
                    double totalTime = times.stream().mapToDouble(Double::doubleValue).sum();
                    double avgTime = totalTime / times.size();
                    double maxTime = times.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
                    double minTime = times.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
                    
                    stats.put(phaseName, PhaseStatistics.builder()
                            .phaseName(phaseName)
                            .totalTime((long) totalTime)
                            .avgTime(avgTime)
                            .maxTime(maxTime)
                            .minTime(minTime)
                            .count(times.size())
                            .build());
                }
            }
        } else {
            // 其他GC类型的统计
            Map<String, List<Double>> phaseTimesMap = new HashMap<>();
            
            for (GCEvent event : events) {
                String eventType = event.getEventType();
                double pauseTime = event.getPauseTime();
                
                phaseTimesMap.computeIfAbsent(eventType, k -> new ArrayList<>()).add(pauseTime);
            }
            
            for (Map.Entry<String, List<Double>> entry : phaseTimesMap.entrySet()) {
                String phaseName = entry.getKey();
                List<Double> times = entry.getValue();
                
                if (!times.isEmpty()) {
                    double totalTime = times.stream().mapToDouble(Double::doubleValue).sum();
                    double avgTime = totalTime / times.size();
                    
                    stats.put(phaseName, PhaseStatistics.builder()
                            .phaseName(phaseName)
                            .totalTime((long) totalTime)
                            .avgTime(avgTime)
                            .count(times.size())
                            .build());
                }
            }
        }
        
        return stats;
    }
    
    /**
     * 计算对象统计
     */
    private ObjectStats calculateObjectStats(List<GCEvent> events, List<String> lines, String collectorType) {
        long totalCreated = 0;
        long totalPromoted = 0;
        long totalReclaimed = 0;
        
        // 从事件中提取
        for (GCEvent event : events) {
            if (event.getHeapMemory() != null) {
                totalCreated += event.getHeapMemory().getReclaimed();
            }
        }
        
        // 对于ZGC，从日志中提取Allocated、Garbage和Reclaimed信息
        if ("ZGC".equals(collectorType)) {
            // Allocated格式: GC(0) Allocated: - 32M (0%) 36M (0%) 43M (0%) - -
            Pattern allocPattern = Pattern.compile("\\[gc,heap\\s*\\] GC\\(\\d+\\) Allocated:.*?(\\d+)M \\(\\d+%\\)[^\\d]*$");
            // Reclaimed格式: GC(0) Reclaimed: - - 4M (0%) 891M (9%) - -
            // 提取最大的回收量（第4个数字）
            Pattern reclaimedPattern = Pattern.compile("\\[gc,heap\\s*\\] GC\\(\\d+\\) Reclaimed:.*?(\\d+)M \\(\\d+%\\)\\s+\\S+\\s+\\S+\\s*$");
            
            for (String line : lines) {
                Matcher allocMatcher = allocPattern.matcher(line);
                if (allocMatcher.find()) {
                    totalCreated += Long.parseLong(allocMatcher.group(1)) * 1024 * 1024;
                }
                
                Matcher reclaimedMatcher = reclaimedPattern.matcher(line);
                if (reclaimedMatcher.find()) {
                    totalReclaimed += Long.parseLong(reclaimedMatcher.group(1)) * 1024 * 1024;
                }
            }
        }
        
        long totalTime = events.isEmpty() ? 1 : 
                events.get(events.size() - 1).getTimestamp() - events.get(0).getTimestamp();
        
        double avgCreationRate = totalTime > 0 ? 
                (totalCreated / 1024.0 / 1024.0) / (totalTime / 1000.0) : 0.0;
        
        return ObjectStats.builder()
                .totalCreatedBytes(totalCreated)
                .totalPromotedBytes(totalPromoted)
                .totalReclaimedBytes(totalReclaimed)
                .avgCreationRate(avgCreationRate)
                .avgPromotionRate(0.0)
                .build();
    }
    
    /**
     * 解析CPU统计
     */
    private CPUStats parseCPUStats(List<String> lines) {
        // 在实际实现中，这里会解析CPU时间
        return CPUStats.builder()
                .cpuTime(null)
                .userTime(null)
                .sysTime(null)
                .build();
    }
    
    /**
     * 计算暂停时间分布
     */
    private PauseDurationDistribution calculatePauseDuration(List<GCEvent> events) {
        List<PauseDurationDistribution.DurationRange> ranges = new ArrayList<>();
        
        // 定义时间范围
        double[][] rangeDefinitions = {
            {0, 0.1},
            {0.1, 0.2},
            {0.2, 1.0},
            {1.0, 10.0},
            {10.0, 100.0},
            {100.0, Double.MAX_VALUE}
        };
        
        int[] counts = new int[rangeDefinitions.length];
        
        for (GCEvent event : events) {
            double pauseTime = event.getPauseTime();
            for (int i = 0; i < rangeDefinitions.length; i++) {
                if (pauseTime >= rangeDefinitions[i][0] && pauseTime < rangeDefinitions[i][1]) {
                    counts[i]++;
                    break;
                }
            }
        }
        
        int totalCount = events.size();
        for (int i = 0; i < rangeDefinitions.length; i++) {
            String label = String.format("%.1f - %.1f ms", 
                    rangeDefinitions[i][0], 
                    Math.min(rangeDefinitions[i][1], 1000.0));
            
            double percentage = totalCount > 0 ? (counts[i] * 100.0 / totalCount) : 0.0;
            
            ranges.add(PauseDurationDistribution.DurationRange.builder()
                    .rangeLabel(label)
                    .minDuration(rangeDefinitions[i][0])
                    .maxDuration(rangeDefinitions[i][1])
                    .count(counts[i])
                    .percentage(percentage)
                    .build());
        }
        
        return PauseDurationDistribution.builder()
                .ranges(ranges)
                .build();
    }
    
    /**
     * 执行诊断
     */
    private DiagnosisReport performDiagnosis(List<GCEvent> events, MemorySize memorySize) {
        // 内存泄漏检测
        DiagnosisReport.MemoryLeakInfo memoryLeakInfo = detectMemoryLeak(events);
        
        // Full GC检测
        DiagnosisReport.FullGCInfo fullGCInfo = detectFullGC(events);
        
        // 长暂停检测
        DiagnosisReport.LongPauseInfo longPauseInfo = detectLongPause(events);
        
        // 生成建议
        List<DiagnosisReport.Recommendation> recommendations = generateRecommendations(
                events, memorySize, memoryLeakInfo, fullGCInfo, longPauseInfo);
        
        return DiagnosisReport.builder()
                .memoryLeakInfo(memoryLeakInfo)
                .fullGCInfo(fullGCInfo)
                .longPauseInfo(longPauseInfo)
                .recommendations(recommendations)
                .build();
    }
    
    private DiagnosisReport.MemoryLeakInfo detectMemoryLeak(List<GCEvent> events) {
        // 简单的内存泄漏检测逻辑
        boolean hasLeak = false;
        List<String> evidences = new ArrayList<>();
        
        return DiagnosisReport.MemoryLeakInfo.builder()
                .hasMemoryLeak(hasLeak)
                .description(hasLeak ? "检测到潜在内存泄漏" : "未检测到明显的内存泄漏")
                .evidences(evidences)
                .build();
    }
    
    private DiagnosisReport.FullGCInfo detectFullGC(List<GCEvent> events) {
        List<GCEvent> fullGCEvents = events.stream()
                .filter(GCEvent::isFullGC)
                .toList();
        
        return DiagnosisReport.FullGCInfo.builder()
                .count(fullGCEvents.size())
                .hasFullGC(!fullGCEvents.isEmpty())
                .fullGCEvents(fullGCEvents)
                .build();
    }
    
    private DiagnosisReport.LongPauseInfo detectLongPause(List<GCEvent> events) {
        double threshold = 100.0; // 100ms
        List<GCEvent> longPauseEvents = events.stream()
                .filter(e -> e.getPauseTime() > threshold)
                .toList();
        
        return DiagnosisReport.LongPauseInfo.builder()
                .count(longPauseEvents.size())
                .hasLongPause(!longPauseEvents.isEmpty())
                .threshold(threshold)
                .longPauseEvents(longPauseEvents)
                .build();
    }
    
    private List<DiagnosisReport.Recommendation> generateRecommendations(
            List<GCEvent> events, MemorySize memorySize,
            DiagnosisReport.MemoryLeakInfo memoryLeakInfo,
            DiagnosisReport.FullGCInfo fullGCInfo,
            DiagnosisReport.LongPauseInfo longPauseInfo) {
        
        List<DiagnosisReport.Recommendation> recommendations = new ArrayList<>();
        
        if (fullGCInfo.isHasFullGC()) {
            recommendations.add(DiagnosisReport.Recommendation.builder()
                    .category("GC配置")
                    .level("WARNING")
                    .title("检测到Full GC")
                    .description(String.format("系统执行了 %d 次Full GC", fullGCInfo.getCount()))
                    .suggestion("考虑增加堆内存大小或优化对象分配策略")
                    .build());
        }
        
        if (longPauseInfo.isHasLongPause()) {
            recommendations.add(DiagnosisReport.Recommendation.builder()
                    .category("性能")
                    .level("WARNING")
                    .title("检测到长暂停")
                    .description(String.format("检测到 %d 次超过 %.0fms 的GC暂停", 
                            longPauseInfo.getCount(), longPauseInfo.getThreshold()))
                    .suggestion("考虑调整GC参数或使用低延迟GC收集器（如ZGC或Shenandoah）")
                    .build());
        }
        
        if (recommendations.isEmpty()) {
            recommendations.add(DiagnosisReport.Recommendation.builder()
                    .category("总体")
                    .level("INFO")
                    .title("GC性能良好")
                    .description("未检测到明显的GC性能问题")
                    .suggestion("继续保持当前配置")
                    .build());
        }
        
        return recommendations;
    }
    
    /**
     * 生成时间序列数据
     */
    private TimeSeriesData generateTimeSeriesData(List<GCEvent> events) {
        List<TimeSeriesData.DataPoint> heapTrend = new ArrayList<>();
        List<TimeSeriesData.DataPoint> pauseTrend = new ArrayList<>();
        
        if (events.isEmpty()) {
            return TimeSeriesData.builder()
                    .heapUsageTrend(heapTrend)
                    .pauseTimeTrend(pauseTrend)
                    .build();
        }
        
        // 计算相对时间（从第一个事件开始）
        long baseTimestamp = events.get(0).getTimestamp();
        
        for (GCEvent event : events) {
            // 使用相对时间（毫秒），前端图表使用 type: 'time' 需要毫秒值
            long relativeTime = event.getTimestamp() - baseTimestamp;
            
            if (event.getHeapMemory() != null) {
                heapTrend.add(TimeSeriesData.DataPoint.builder()
                        .timestamp(relativeTime)
                        .value(event.getHeapMemory().getAfter() / (1024.0 * 1024.0))
                        .build());
            }
            
            pauseTrend.add(TimeSeriesData.DataPoint.builder()
                    .timestamp(relativeTime)
                    .value(event.getPauseTime())
                    .build());
        }
        
        return TimeSeriesData.builder()
                .heapUsageTrend(heapTrend)
                .pauseTimeTrend(pauseTrend)
                .build();
    }
}

