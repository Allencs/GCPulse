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
    
    // G1GC日志格式枚举
    private enum G1LogFormat {
        JDK8_TRADITIONAL,  // JDK 8 传统格式
        JDK9_UNIFIED       // JDK 9+ 统一日志格式 (Unified Logging)
    }
    
    // JDK 8 G1GC Pattern（传统格式）
    private static final Pattern G1GC_JDK8_PAUSE_PATTERN = Pattern.compile("(\\d+\\.\\d+):\\s*\\[GC pause\\s*\\((.*?)\\)\\s*\\((.*?)\\)");
    private static final Pattern G1GC_JDK8_EDEN_PATTERN = Pattern.compile("\\[Eden:\\s*([\\d.]+)([BKMGT])\\(([\\d.]+)([BKMGT])\\)->([\\d.]+)([BKMGT])\\(([\\d.]+)([BKMGT])\\)\\s*Survivors?:\\s*([\\d.]+)([BKMGT])->([\\d.]+)([BKMGT])\\s*Heap:\\s*([\\d.]+)([BKMGT])\\(([\\d.]+)([BKMGT])\\)->([\\d.]+)([BKMGT])\\(([\\d.]+)([BKMGT])\\)\\]");
    private static final Pattern G1GC_JDK8_TIME_PATTERN = Pattern.compile(",\\s*([\\d.]+)\\s*secs\\]");
    private static final Pattern G1GC_JDK8_ABSOLUTE_TIMESTAMP = Pattern.compile("(\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}[+-]\\d{4}):");
    
    // JDK 9+ G1GC Pattern（Unified Logging格式）
    private static final Pattern G1GC_UNIFIED_START_PATTERN = Pattern.compile("\\[(\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}[+-]\\d{4})\\]\\[info\\s*\\]\\[gc,start\\s*\\]\\s*GC\\((\\d+)\\)\\s*Pause\\s+(\\w+)\\s*\\((.*?)\\)\\s*\\((.*?)\\)");
    private static final Pattern G1GC_UNIFIED_END_PATTERN = Pattern.compile("\\[(\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}[+-]\\d{4})\\]\\[info\\s*\\]\\[gc\\s*\\]\\s*GC\\((\\d+)\\)\\s*Pause\\s+(\\w+)\\s*\\((.*?)\\)\\s*\\((.*?)\\)\\s*([\\d.]+)M->([\\d.]+)M\\(([\\d.]+)M\\)\\s*([\\d.]+)ms");
    private static final Pattern G1GC_UNIFIED_HEAP_REGIONS_PATTERN = Pattern.compile("\\[.*?\\]\\[info\\s*\\]\\[gc,heap\\s*\\]\\s*GC\\((\\d+)\\)\\s*Eden regions:\\s*(\\d+)->(\\d+)\\((\\d+)\\)");
    private static final Pattern G1GC_UNIFIED_SURVIVOR_PATTERN = Pattern.compile("\\[.*?\\]\\[info\\s*\\]\\[gc,heap\\s*\\]\\s*GC\\((\\d+)\\)\\s*Survivor regions:\\s*(\\d+)->(\\d+)\\((\\d+)\\)");
    
    // 支持新旧两种ZGC日志格式
    private static final Pattern ZGC_PATTERN_OLD = Pattern.compile("\\[(\\d+\\.\\d+)s\\].*?GC\\((\\d+)\\).*?Pause.*?(\\d+\\.\\d+)ms");
    // 新格式: [2025-12-11T01:40:40.712+0800][118][gc,phases   ] GC(0) Pause Mark Start 0.015ms
    private static final Pattern ZGC_PATTERN_NEW = Pattern.compile("\\[(\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}[+-]\\d{4})\\]\\[\\d+\\]\\[gc,phases\\s*\\] GC\\((\\d+)\\) (Pause .*?) ([\\d.]+)ms");
    private static final Pattern ZGC_UNIFIED_TIMESTAMP = Pattern.compile("\\[(\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3})");
    
    // CMS/ParNew格式 - 支持多种时间戳格式
    // 时间戳格式1: "4.856: [GC"
    // 时间戳格式2: "2025-08-05T13:23:18.409+0800: 4.856: [GC"
    private static final Pattern CMS_TIMESTAMP_PATTERN = Pattern.compile("(\\d+\\.\\d+):\\s*\\[(?:GC|Full GC)");
    private static final Pattern CMS_ABSOLUTE_TIMESTAMP_PATTERN = Pattern.compile("(\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}[+-]\\d{4}):");
    
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
    
    // Parallel GC Pattern - 增强版，分别识别 PSYoungGen 和 ParOldGen
    private static final Pattern PARALLEL_PATTERN = Pattern.compile("\\[(Full )?GC.*?\\[PS.*?(\\d+)K->(\\d+)K\\((\\d+)K\\).*?(\\d+\\.\\d+) secs\\]");
    private static final Pattern PARALLEL_YOUNG_PATTERN = Pattern.compile("\\[PSYoungGen: (\\d+)K->(\\d+)K\\((\\d+)K\\)\\]");
    private static final Pattern PARALLEL_OLD_PATTERN = Pattern.compile("\\[ParOldGen: (\\d+)K->(\\d+)K\\((\\d+)K\\)\\]");
    private static final Pattern PARALLEL_FULL_GC_PATTERN = Pattern.compile("(\\d+\\.\\d+): \\[Full GC.*?\\[PSYoungGen: (\\d+)K->(\\d+)K\\((\\d+)K\\)\\].*?\\[ParOldGen: (\\d+)K->(\\d+)K\\((\\d+)K\\)\\]\\s*(\\d+)K->(\\d+)K\\((\\d+)K\\).*?(\\d+\\.\\d+)\\s*secs\\]");
    
    // Serial GC Pattern - 增强版，识别 DefNew 和 Tenured
    private static final Pattern SERIAL_PATTERN = Pattern.compile("(\\d+\\.\\d+): \\[GC.*?\\[DefNew: (\\d+)K->(\\d+)K\\((\\d+)K\\),\\s*([\\d.]+)\\s*secs\\]\\s*(\\d+)K->(\\d+)K\\((\\d+)K\\)");
    private static final Pattern SERIAL_FULL_GC_PATTERN = Pattern.compile("(\\d+\\.\\d+): \\[Full GC.*?\\[Tenured: (\\d+)K->(\\d+)K\\((\\d+)K\\),\\s*([\\d.]+)\\s*secs\\]\\s*(\\d+)K->(\\d+)K\\((\\d+)K\\)");
    
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
        Map<String, PhaseStatistics> phaseStats = calculatePhaseStatisticsEnhanced(gcEvents, collectorType);
        ObjectStats objectStats = calculateObjectStats(gcEvents, lines, collectorType);
        CPUStats cpuStats = parseCPUStats(lines);
        PauseDurationDistribution pauseDist = calculatePauseDuration(gcEvents);
        DiagnosisReport diagnosisReport = performDiagnosisEnhanced(gcEvents, memorySize);
        TimeSeriesData timeSeriesData = generateTimeSeriesData(gcEvents);
        
        // 企业级功能
        JVMArguments jvmArgs = parseJVMArguments(lines);
        TenuringSummary tenuringSummary = parseTenuringSummary(lines);
        Map<String, GCCause> gcCauses = calculateGCCauses(gcEvents);
        SafePointStats safePointStats = parseSafePointStats(lines);
        StringDeduplicationStats stringDedup = parseStringDeduplication(lines);
        
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
                // 企业级功能
                .jvmArguments(jvmArgs)
                .tenuringSummary(tenuringSummary)
                .gcCauses(gcCauses)
                .safePointStats(safePointStats)
                .stringDedup(stringDedup)
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
     * 检测G1日志格式（JDK 8传统格式 vs JDK 9+ Unified Logging格式）
     */
    private G1LogFormat detectG1LogFormat(List<String> lines) {
        for (String line : lines) {
            // 检测JDK 9+ Unified Logging特征：[timestamp][level][tags]
            if (line.matches("\\[\\d{4}-\\d{2}-\\d{2}T.*?\\]\\[.*?\\]\\[gc.*?\\].*")) {
                System.out.println("检测到JDK 9+ G1 Unified Logging格式");
                return G1LogFormat.JDK9_UNIFIED;
            }
            // 检测JDK 8传统格式特征：timestamp: [GC pause
            if (line.matches(".*\\d+\\.\\d+:\\s*\\[GC pause.*")) {
                System.out.println("检测到JDK 8 G1传统格式");
                return G1LogFormat.JDK8_TRADITIONAL;
            }
        }
        // 默认返回传统格式
        System.out.println("未检测到明确的G1格式，默认使用JDK 8传统格式");
        return G1LogFormat.JDK8_TRADITIONAL;
    }
    
    /**
     * 解析GC事件
     */
    private List<GCEvent> parseGCEvents(List<String> lines, String collectorType) {
        List<GCEvent> events = new ArrayList<>();
        
        // 对于G1GC，检测日志格式
        G1LogFormat g1Format = null;
        if ("G1GC".equals(collectorType)) {
            g1Format = detectG1LogFormat(lines);
        }
        
        // 如果是G1GC（任何格式），需要收集多行信息
        if ("G1GC".equals(collectorType)) {
            if (g1Format == G1LogFormat.JDK9_UNIFIED) {
                events = parseG1UnifiedLogging(lines);
            } else {
                events = parseG1JDK8MultiLine(lines);
            }
        } else {
            // 其他格式逐行解析
            for (String line : lines) {
                try {
                    GCEvent event = null;
                    
                    // 根据不同的收集器类型解析
                    switch (collectorType) {
                        case "ZGC" -> event = parseZGCEvent(line);
                        case "CMS" -> event = parseCMSEvent(line);
                        case "Parallel GC" -> event = parseParallelGCEvent(line);
                        case "Serial GC" -> event = parseSerialGCEvent(line);
                        default -> event = parseGenericGCEvent(line);
                    }
                    
                    if (event != null) {
                        events.add(event);
                    }
                } catch (Exception e) {
                    // 忽略无法解析的行
                }
            }
        }
        
        return events;
    }
    
    /**
     * 解析JDK 8 G1GC事件（传统格式，多行模式）
     * 格式示例：
     * 2025-12-24T20:01:41.755-0800: 0.561: [GC pause (G1 Evacuation Pause) (young)
     * [Eden: 24.0M(24.0M)->0.0B(33.0M) Survivors: 0.0B->3072.0K Heap: 24.0M(256.0M)->4191.5K(256.0M)]
     * [Times: user=0.01 sys=0.00, real=0.00 secs], 0.0034420 secs]
     */
    private List<GCEvent> parseG1JDK8MultiLine(List<String> lines) {
        List<GCEvent> events = new ArrayList<>();
        StringBuilder currentEvent = new StringBuilder();
        boolean inGCEvent = false;
        
        for (String line : lines) {
            try {
                // 检查是否是GC事件的开始
                if (line.contains(": [GC pause") || line.contains(": [Full GC")) {
                    // 如果之前有未完成的事件，先处理它
                    if (inGCEvent && currentEvent.length() > 0) {
                        GCEvent event = parseG1JDK8SingleEvent(currentEvent.toString());
                        if (event != null) {
                            events.add(event);
                        }
                        currentEvent = new StringBuilder();
                    }
                    inGCEvent = true;
                    currentEvent.append(line).append("\n");
                } else if (inGCEvent) {
                    currentEvent.append(line).append("\n");
                    // 检查事件是否结束（遇到"}"或者下一个Heap before GC）
                    if (line.trim().equals("}") || line.contains("Heap before GC")) {
                        GCEvent event = parseG1JDK8SingleEvent(currentEvent.toString());
                        if (event != null) {
                            events.add(event);
                        }
                        currentEvent = new StringBuilder();
                        inGCEvent = false;
                    }
                }
            } catch (Exception e) {
                // 忽略解析错误
                System.err.println("解析G1 JDK8事件失败: " + e.getMessage());
            }
        }
        
        // 处理最后一个未完成的事件
        if (inGCEvent && currentEvent.length() > 0) {
            try {
                GCEvent event = parseG1JDK8SingleEvent(currentEvent.toString());
                if (event != null) {
                    events.add(event);
                }
            } catch (Exception e) {
                System.err.println("解析最后一个G1 JDK8事件失败: " + e.getMessage());
            }
        }
        
        System.out.println("解析到 " + events.size() + " 个G1 GC事件（JDK 8传统格式）");
        return events;
    }
    
    /**
     * 解析单个JDK 8 G1GC事件（已收集完整的多行文本）
     */
    private GCEvent parseG1JDK8SingleEvent(String eventText) {
        // 检查是否是GC pause行
        if (!eventText.contains("[GC pause") && !eventText.contains("[Full GC")) {
            return null;
        }
        
        long timestamp = 0;
        String gcType = "Young GC";
        boolean isFullGC = false;
        
        // 尝试解析绝对时间戳（如果存在）
        Matcher absTimeMatcher = G1GC_JDK8_ABSOLUTE_TIMESTAMP.matcher(eventText);
        if (absTimeMatcher.find()) {
            String isoTimestamp = absTimeMatcher.group(1);
            timestamp = parseAbsoluteTimestamp(isoTimestamp);
        } else {
            // 解析相对时间戳
            Matcher relTimeMatcher = G1GC_JDK8_PAUSE_PATTERN.matcher(eventText);
            if (relTimeMatcher.find()) {
                double relativeTime = Double.parseDouble(relTimeMatcher.group(1));
                timestamp = (long) (relativeTime * 1000);
            } else {
                return null;
            }
        }
        
        // 识别GC类型
        if (eventText.contains("Full GC")) {
            gcType = "Full GC";
            isFullGC = true;
        } else if (eventText.contains("(mixed)")) {
            gcType = "Mixed GC";
        } else if (eventText.contains("(young)")) {
            gcType = "Young GC";
        }
        
        // 提取暂停时间（秒）- 查找最后的", X.XXXX secs]"
        double pauseTime = 0.0;
        Matcher timeMatcher = G1GC_JDK8_TIME_PATTERN.matcher(eventText);
        while (timeMatcher.find()) {
            // 使用最后一个匹配的时间（总暂停时间）
            pauseTime = Double.parseDouble(timeMatcher.group(1)) * 1000; // 转换为毫秒
        }
        
        // 解析Eden内存变化（用于构建heapMemory）
        GCEvent.MemoryChange heapMemory = null;
        Matcher edenMatcher = G1GC_JDK8_EDEN_PATTERN.matcher(eventText);
        if (edenMatcher.find()) {
            // 提取Heap信息：Heap: 24.0M(256.0M)->4191.5K(256.0M)
            double heapBefore = parseMemoryUnit(edenMatcher.group(13), edenMatcher.group(14));
            double heapAfter = parseMemoryUnit(edenMatcher.group(17), edenMatcher.group(18));
            double heapTotal = parseMemoryUnit(edenMatcher.group(19), edenMatcher.group(20));
            
            heapMemory = GCEvent.MemoryChange.builder()
                .before((long) heapBefore)
                .after((long) heapAfter)
                .total((long) heapTotal)
                .build();
        }
        
        // 如果没有匹配到详细的内存信息，尝试简单解析
        if (heapMemory == null) {
            heapMemory = parseMemoryChange(eventText);
        }
        
        return GCEvent.builder()
                .timestamp(timestamp)
                .eventType(gcType)
                .pauseTime(pauseTime)
                .concurrentTime(0.0)
                .heapMemory(heapMemory)
                .isFullGC(isFullGC)
                .isLongPause(pauseTime > 100)
                .build();
    }
    
    /**
     * 解析内存单位并转换为字节
     */
    private double parseMemoryUnit(String value, String unit) {
        double val = Double.parseDouble(value);
        return switch (unit) {
            case "B" -> val;
            case "K" -> val * 1024;
            case "M" -> val * 1024 * 1024;
            case "G" -> val * 1024 * 1024 * 1024;
            case "T" -> val * 1024 * 1024 * 1024 * 1024;
            default -> val;
        };
    }
    
    /**
     * 解析JDK 9+ G1 Unified Logging格式
     * 格式示例：
     * [2025-12-24T11:27:59.533+0000][info ][gc,start] GC(0) Pause Young (Normal) (G1 Evacuation Pause)
     * [2025-12-24T11:27:59.535+0000][info ][gc     ] GC(0) Pause Young (Normal) (G1 Evacuation Pause) 23M->16M(260M) 2.554ms
     * [2025-12-24T11:27:59.535+0000][info ][gc,heap] GC(0) Eden regions: 11->0(7)
     */
    private List<GCEvent> parseG1UnifiedLogging(List<String> lines) {
        List<GCEvent> events = new ArrayList<>();
        Map<String, GCEventData> gcEventMap = new HashMap<>();
        
        for (String line : lines) {
            try {
                // 匹配GC结束行（包含完整信息）
                Matcher endMatcher = G1GC_UNIFIED_END_PATTERN.matcher(line);
                if (endMatcher.find()) {
                    String timestampStr = endMatcher.group(1);
                    String gcId = endMatcher.group(2);
                    String pauseType = endMatcher.group(3); // Young, Mixed, Full
                    String gcCause = endMatcher.group(5);   // G1 Evacuation Pause
                    double heapBefore = Double.parseDouble(endMatcher.group(6));
                    double heapAfter = Double.parseDouble(endMatcher.group(7));
                    double heapTotal = Double.parseDouble(endMatcher.group(8));
                    double pauseTime = Double.parseDouble(endMatcher.group(9));
                    
                    // 解析时间戳
                    long timestamp = parseAbsoluteTimestamp(timestampStr);
                    
                    // 确定GC类型
                    String gcType = "Young GC";
                    boolean isFullGC = false;
                    if (pauseType.equalsIgnoreCase("Full")) {
                        gcType = "Full GC";
                        isFullGC = true;
                    } else if (pauseType.equalsIgnoreCase("Mixed")) {
                        gcType = "Mixed GC";
                    }
                    
                    // 构建内存变化（MB转字节）
                    GCEvent.MemoryChange heapMemory = GCEvent.MemoryChange.builder()
                        .before((long) (heapBefore * 1024 * 1024))
                        .after((long) (heapAfter * 1024 * 1024))
                        .total((long) (heapTotal * 1024 * 1024))
                        .build();
                    
                    // 创建GC事件
                    GCEvent event = GCEvent.builder()
                            .timestamp(timestamp)
                .eventType(gcType)
                .pauseTime(pauseTime)
                .concurrentTime(0.0)
                .heapMemory(heapMemory)
                .isFullGC(isFullGC)
                            .isLongPause(pauseTime > 100)
                            .build();
                    
                    events.add(event);
                    
                    // 保存到map用于后续增强
                    GCEventData data = new GCEventData();
                    data.event = event;
                    gcEventMap.put(gcId, data);
                }
            } catch (Exception e) {
                // 忽略无法解析的行
                System.err.println("解析G1 Unified Logging失败: " + e.getMessage());
            }
        }
        
        System.out.println("解析到 " + events.size() + " 个G1 GC事件（Unified Logging格式）");
        return events;
    }
    
    /**
     * 用于临时存储GC事件数据
     */
    private static class GCEventData {
        GCEvent event;
        Integer edenBefore;
        Integer edenAfter;
        Integer survivorBefore;
        Integer survivorAfter;
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
        // 提取时间戳：优先使用绝对时间，否则使用相对时间
        long timestamp = 0;
        
        // 尝试提取绝对时间戳：2025-08-05T13:23:18.409+0800
        Matcher absoluteTsMatcher = CMS_ABSOLUTE_TIMESTAMP_PATTERN.matcher(line);
        if (absoluteTsMatcher.find()) {
            String absoluteTimestamp = absoluteTsMatcher.group(1);
            timestamp = parseAbsoluteTimestamp(absoluteTimestamp);
        } else {
            // 使用相对时间戳：4.856
        Matcher tsMatcher = CMS_TIMESTAMP_PATTERN.matcher(line);
        if (tsMatcher.find()) {
                double relativeTimestamp = Double.parseDouble(tsMatcher.group(1));
                timestamp = (long) (relativeTimestamp * 1000);
            }
        }
        
        // 1. 尝试解析 CMS Initial Mark（初始标记）
        Matcher initialMarkMatcher = CMS_INITIAL_MARK_PATTERN.matcher(line);
        if (initialMarkMatcher.find()) {
            double pauseTime = Double.parseDouble(initialMarkMatcher.group(5)) * 1000;
            
            return GCEvent.builder()
                    .timestamp(timestamp)
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
                    .timestamp(timestamp)
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
                    .timestamp(timestamp)
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
                    .timestamp(timestamp)
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
     * 解析Parallel GC事件 - 增强版
     * 支持: PSYoungGen和ParOldGen详细统计
     */
    private GCEvent parseParallelGCEvent(String line) {
        Matcher tsMatcher = TIMESTAMP_PATTERN.matcher(line);
        if (!tsMatcher.find()) return null;
        
        double timestamp = Double.parseDouble(tsMatcher.group(1));
        
        // 尝试解析Full GC（包含PSYoungGen和ParOldGen）
        Matcher fullGCMatcher = PARALLEL_FULL_GC_PATTERN.matcher(line);
        if (fullGCMatcher.find()) {
            long youngBefore = Long.parseLong(fullGCMatcher.group(2)) * 1024;
            long youngAfter = Long.parseLong(fullGCMatcher.group(3)) * 1024;
            long youngTotal = Long.parseLong(fullGCMatcher.group(4)) * 1024;
            
            long oldBefore = Long.parseLong(fullGCMatcher.group(5)) * 1024;
            long oldAfter = Long.parseLong(fullGCMatcher.group(6)) * 1024;
            long oldTotal = Long.parseLong(fullGCMatcher.group(7)) * 1024;
            
            long heapBefore = Long.parseLong(fullGCMatcher.group(8)) * 1024;
            long heapAfter = Long.parseLong(fullGCMatcher.group(9)) * 1024;
            long heapTotal = Long.parseLong(fullGCMatcher.group(10)) * 1024;
            
            double pauseTime = Double.parseDouble(fullGCMatcher.group(11)) * 1000;
            
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
            
            GCEvent.MemoryChange oldGen = GCEvent.MemoryChange.builder()
                    .before(oldBefore)
                    .after(oldAfter)
                    .total(oldTotal)
                    .build();
            
            return GCEvent.builder()
                    .timestamp((long) (timestamp * 1000))
                    .eventType("Full GC (Parallel)")
                    .pauseTime(pauseTime)
                    .heapMemory(heapMemory)
                    .youngGen(youngGen)
                    .oldGen(oldGen)
                    .isFullGC(true)
                    .isLongPause(pauseTime > 100)
                    .build();
        }
        
        // 尝试解析普通Young GC
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
        
        // 尝试提取PSYoungGen详情
        GCEvent.MemoryChange youngGen = null;
        Matcher youngMatcher = PARALLEL_YOUNG_PATTERN.matcher(line);
        if (youngMatcher.find()) {
            long youngBefore = Long.parseLong(youngMatcher.group(1)) * 1024;
            long youngAfter = Long.parseLong(youngMatcher.group(2)) * 1024;
            long youngTotal = Long.parseLong(youngMatcher.group(3)) * 1024;
            
            youngGen = GCEvent.MemoryChange.builder()
                    .before(youngBefore)
                    .after(youngAfter)
                    .total(youngTotal)
                    .build();
        }
        
        // 尝试提取ParOldGen详情
        GCEvent.MemoryChange oldGen = null;
        Matcher oldMatcher = PARALLEL_OLD_PATTERN.matcher(line);
        if (oldMatcher.find()) {
            long oldBefore = Long.parseLong(oldMatcher.group(1)) * 1024;
            long oldAfter = Long.parseLong(oldMatcher.group(2)) * 1024;
            long oldTotal = Long.parseLong(oldMatcher.group(3)) * 1024;
            
            oldGen = GCEvent.MemoryChange.builder()
                    .before(oldBefore)
                    .after(oldAfter)
                    .total(oldTotal)
                    .build();
        }
        
        return GCEvent.builder()
                .timestamp((long) (timestamp * 1000))
                .eventType(isFullGC ? "Full GC (Parallel)" : "Young GC (Parallel)")
                .pauseTime(pauseTime)
                .heapMemory(heapMemory)
                .youngGen(youngGen)
                .oldGen(oldGen)
                .isFullGC(isFullGC)
                .isLongPause(pauseTime > 100)
                .build();
    }
    
    /**
     * 解析Serial GC事件 - 增强版
     * 支持: DefNew和Tenured详细统计
     */
    private GCEvent parseSerialGCEvent(String line) {
        // 尝试解析Full GC（包含Tenured）
        Matcher fullGCMatcher = SERIAL_FULL_GC_PATTERN.matcher(line);
        if (fullGCMatcher.find()) {
            double timestamp = Double.parseDouble(fullGCMatcher.group(1));
            
            long tenuredBefore = Long.parseLong(fullGCMatcher.group(2)) * 1024;
            long tenuredAfter = Long.parseLong(fullGCMatcher.group(3)) * 1024;
            long tenuredTotal = Long.parseLong(fullGCMatcher.group(4)) * 1024;
            double pauseTime = Double.parseDouble(fullGCMatcher.group(5)) * 1000;
            
            long heapBefore = Long.parseLong(fullGCMatcher.group(6)) * 1024;
            long heapAfter = Long.parseLong(fullGCMatcher.group(7)) * 1024;
            long heapTotal = Long.parseLong(fullGCMatcher.group(8)) * 1024;
            
            GCEvent.MemoryChange heapMemory = GCEvent.MemoryChange.builder()
                    .before(heapBefore)
                    .after(heapAfter)
                    .total(heapTotal)
                    .build();
            
            GCEvent.MemoryChange oldGen = GCEvent.MemoryChange.builder()
                    .before(tenuredBefore)
                    .after(tenuredAfter)
                    .total(tenuredTotal)
                    .build();
            
            return GCEvent.builder()
                    .timestamp((long) (timestamp * 1000))
                    .eventType("Full GC (Serial)")
                    .pauseTime(pauseTime)
                    .heapMemory(heapMemory)
                    .oldGen(oldGen)
                    .isFullGC(true)
                    .isLongPause(pauseTime > 100)
                    .build();
        }
        
        // 尝试解析Young GC（DefNew）
        Matcher serialMatcher = SERIAL_PATTERN.matcher(line);
        if (serialMatcher.find()) {
            double timestamp = Double.parseDouble(serialMatcher.group(1));
            
            long defNewBefore = Long.parseLong(serialMatcher.group(2)) * 1024;
            long defNewAfter = Long.parseLong(serialMatcher.group(3)) * 1024;
            long defNewTotal = Long.parseLong(serialMatcher.group(4)) * 1024;
            double pauseTime = Double.parseDouble(serialMatcher.group(5)) * 1000;
            
            long heapBefore = Long.parseLong(serialMatcher.group(6)) * 1024;
            long heapAfter = Long.parseLong(serialMatcher.group(7)) * 1024;
            long heapTotal = Long.parseLong(serialMatcher.group(8)) * 1024;
            
            GCEvent.MemoryChange heapMemory = GCEvent.MemoryChange.builder()
                    .before(heapBefore)
                    .after(heapAfter)
                    .total(heapTotal)
                    .build();
            
            GCEvent.MemoryChange youngGen = GCEvent.MemoryChange.builder()
                    .before(defNewBefore)
                    .after(defNewAfter)
                    .total(defNewTotal)
                    .build();
            
            return GCEvent.builder()
                    .timestamp((long) (timestamp * 1000))
                    .eventType("Young GC (DefNew)")
                    .pauseTime(pauseTime)
                    .heapMemory(heapMemory)
                    .youngGen(youngGen)
                    .isFullGC(false)
                    .isLongPause(pauseTime > 100)
                    .build();
        }
        
        return null;
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
     * 生成时间序列数据 - 增强版（支持多视图）
     */
    private TimeSeriesData generateTimeSeriesData(List<GCEvent> events) {
        List<TimeSeriesData.DataPoint> heapAfterTrend = new ArrayList<>();
        List<TimeSeriesData.DataPoint> heapBeforeTrend = new ArrayList<>();
        List<TimeSeriesData.DataPoint> pauseTrend = new ArrayList<>();
        List<TimeSeriesData.DataPoint> reclaimedTrend = new ArrayList<>();
        List<TimeSeriesData.DataPoint> youngGenTrend = new ArrayList<>();
        List<TimeSeriesData.DataPoint> oldGenTrend = new ArrayList<>();
        List<TimeSeriesData.DataPoint> allocationTrend = new ArrayList<>();
        List<TimeSeriesData.DataPoint> promotionTrend = new ArrayList<>();
        
        if (events.isEmpty()) {
            return TimeSeriesData.builder()
                    .heapUsageTrend(heapAfterTrend)
                    .heapBeforeGCTrend(heapBeforeTrend)
                    .pauseTimeTrend(pauseTrend)
                    .reclaimedBytesTrend(reclaimedTrend)
                    .youngGenTrend(youngGenTrend)
                    .oldGenTrend(oldGenTrend)
                    .allocationTrend(allocationTrend)
                    .promotionTrend(promotionTrend)
                    .build();
        }
        
        // 直接使用事件的时间戳（可能是绝对时间或相对时间）
        for (GCEvent event : events) {
            // 使用事件的timestamp（毫秒），前端图表使用 type: 'time' 需要毫秒值
            long timestamp = event.getTimestamp();
            
            // Heap After GC
            if (event.getHeapMemory() != null) {
                heapAfterTrend.add(TimeSeriesData.DataPoint.builder()
                        .timestamp(timestamp)
                        .value(event.getHeapMemory().getAfter() / (1024.0 * 1024.0))
                        .build());
                
                // Heap Before GC
                heapBeforeTrend.add(TimeSeriesData.DataPoint.builder()
                        .timestamp(timestamp)
                        .value(event.getHeapMemory().getBefore() / (1024.0 * 1024.0))
                        .build());
                
                // Reclaimed Bytes
                reclaimedTrend.add(TimeSeriesData.DataPoint.builder()
                        .timestamp(timestamp)
                        .value(event.getHeapMemory().getReclaimed() / (1024.0 * 1024.0))
                        .build());
            }
            
            // GC Duration (Pause Time)
            pauseTrend.add(TimeSeriesData.DataPoint.builder()
                    .timestamp(timestamp)
                    .value(event.getPauseTime())
                    .build());
            
            // Young Gen
            if (event.getYoungGen() != null) {
                youngGenTrend.add(TimeSeriesData.DataPoint.builder()
                        .timestamp(timestamp)
                        .value(event.getYoungGen().getAfter() / (1024.0 * 1024.0))
                        .build());
                
                // Allocation (Young Gen created)
                allocationTrend.add(TimeSeriesData.DataPoint.builder()
                        .timestamp(timestamp)
                        .value(event.getYoungGen().getBefore() / (1024.0 * 1024.0))
                        .build());
            }
            
            // Old Gen
            if (event.getOldGen() != null) {
                oldGenTrend.add(TimeSeriesData.DataPoint.builder()
                        .timestamp(timestamp)
                        .value(event.getOldGen().getAfter() / (1024.0 * 1024.0))
                        .build());
            }
            
            // Promotion (increase in old gen)
            if (event.getOldGen() != null && event.getOldGen().getAfter() > event.getOldGen().getBefore()) {
                promotionTrend.add(TimeSeriesData.DataPoint.builder()
                        .timestamp(timestamp)
                        .value((event.getOldGen().getAfter() - event.getOldGen().getBefore()) / (1024.0 * 1024.0))
                        .build());
            }
        }
        
        return TimeSeriesData.builder()
                .heapUsageTrend(heapAfterTrend)
                .heapBeforeGCTrend(heapBeforeTrend)
                .pauseTimeTrend(pauseTrend)
                .reclaimedBytesTrend(reclaimedTrend)
                .youngGenTrend(youngGenTrend)
                .oldGenTrend(oldGenTrend)
                .allocationTrend(allocationTrend)
                .promotionTrend(promotionTrend)
                .build();
    }
    
    /**
     * ======================== 企业级功能方法 ========================
     */
    
    /**
     * 解析 JVM 参数
     */
    private JVMArguments parseJVMArguments(List<String> lines) {
        List<String> allArgs = new ArrayList<>();
        List<String> gcArgs = new ArrayList<>();
        List<String> memoryArgs = new ArrayList<>();
        List<String> performanceArgs = new ArrayList<>();
        List<String> otherArgs = new ArrayList<>();
        
        // 查找 CommandLine flags 行
        Pattern commandLinePattern = Pattern.compile("CommandLine flags:\\s*(.*)");
        
        for (String line : lines) {
            Matcher matcher = commandLinePattern.matcher(line);
            if (matcher.find()) {
                String flagsStr = matcher.group(1);
                // 解析参数
                String[] flags = flagsStr.split("\\s+-");
                for (String flag : flags) {
                    if (flag.trim().isEmpty()) continue;
                    String arg = "-" + flag.trim();
                    allArgs.add(arg);
                    
                    // 分类
                    if (isGCArg(arg)) {
                        gcArgs.add(arg);
                    } else if (isMemoryArg(arg)) {
                        memoryArgs.add(arg);
                    } else if (isPerformanceArg(arg)) {
                        performanceArgs.add(arg);
                    } else {
                        otherArgs.add(arg);
                    }
                }
            }
        }
        
        return JVMArguments.builder()
                .allArguments(allArgs)
                .gcArguments(gcArgs)
                .memoryArguments(memoryArgs)
                .performanceArguments(performanceArgs)
                .otherArguments(otherArgs)
                .build();
    }
    
    private boolean isGCArg(String arg) {
        return arg.contains("GC") || arg.contains("gc") || 
               arg.startsWith("-XX:+Use") && arg.contains("GC");
    }
    
    private boolean isMemoryArg(String arg) {
        return arg.contains("Heap") || arg.contains("heap") || 
               arg.contains("Xms") || arg.contains("Xmx") || 
               arg.contains("Metaspace") || arg.contains("NewSize");
    }
    
    private boolean isPerformanceArg(String arg) {
        return arg.contains("Parallel") || arg.contains("Thread") || 
               arg.contains("Concurrent") || arg.contains("concurrent");
    }
    
    /**
     * 解析老年代晋升总结
     */
    private TenuringSummary parseTenuringSummary(List<String> lines) {
        // 解析 Tenuring threshold 和 age distribution
        Pattern thresholdPattern = Pattern.compile("Desired survivor size.*?new threshold (\\d+)");
        Pattern agePattern = Pattern.compile("- age\\s+(\\d+):\\s+(\\d+) bytes");
        
        List<Integer> thresholds = new ArrayList<>();
        Map<Integer, Long> ageDistribution = new HashMap<>();
        
        for (String line : lines) {
            Matcher thresholdMatcher = thresholdPattern.matcher(line);
            if (thresholdMatcher.find()) {
                thresholds.add(Integer.parseInt(thresholdMatcher.group(1)));
            }
            
            Matcher ageMatcher = agePattern.matcher(line);
            if (ageMatcher.find()) {
                int age = Integer.parseInt(ageMatcher.group(1));
                long bytes = Long.parseLong(ageMatcher.group(2));
                ageDistribution.merge(age, bytes, Long::sum);
            }
        }
        
        if (thresholds.isEmpty() && ageDistribution.isEmpty()) {
            return null;  // 没有相关数据
        }
        
        Integer maxThreshold = thresholds.isEmpty() ? null : Collections.max(thresholds);
        Integer avgThreshold = thresholds.isEmpty() ? null : 
                (int) thresholds.stream().mapToInt(Integer::intValue).average().orElse(0);
        
        long totalSurvived = ageDistribution.values().stream().mapToLong(Long::longValue).sum();
        
        return TenuringSummary.builder()
                .maxTenuringThreshold(maxThreshold)
                .avgTenuringThreshold(avgThreshold)
                .ageDistribution(ageDistribution)
                .totalSurvivedObjects(totalSurvived)
                .totalPromotedObjects(0L)  // 需要从其他地方提取
                .promotionRate(0.0)
                .build();
    }
    
    /**
     * 计算 GC 原因统计
     */
    private Map<String, GCCause> calculateGCCauses(List<GCEvent> events) {
        Map<String, GCCause> causes = new HashMap<>();
        
        // 从事件类型中提取原因
        Map<String, List<Double>> causeTimesMap = new HashMap<>();
        
        for (GCEvent event : events) {
            String cause = extractGCCause(event.getEventType());
            causeTimesMap.computeIfAbsent(cause, k -> new ArrayList<>()).add(event.getPauseTime());
        }
        
        // 计算总时间用于百分比
        double totalGCTime = events.stream().mapToDouble(GCEvent::getPauseTime).sum();
        
        for (Map.Entry<String, List<Double>> entry : causeTimesMap.entrySet()) {
            String cause = entry.getKey();
            List<Double> times = entry.getValue();
            
            if (!times.isEmpty()) {
                double totalTime = times.stream().mapToDouble(Double::doubleValue).sum();
                double avgTime = totalTime / times.size();
                double maxTime = times.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
                double minTime = times.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
                double percentage = totalGCTime > 0 ? (totalTime / totalGCTime) * 100.0 : 0.0;
                
                causes.put(cause, GCCause.builder()
                        .cause(cause)
                        .count(times.size())
                        .avgTime(avgTime)
                        .maxTime(maxTime)
                        .minTime(minTime)
                        .totalTime(totalTime)
                        .percentage(percentage)
                        .build());
            }
        }
        
        return causes;
    }
    
    private String extractGCCause(String eventType) {
        // 从事件类型中提取原因
        if (eventType.contains("Allocation Failure")) {
            return "Allocation Failure";
        } else if (eventType.contains("GCLocker")) {
            return "GCLocker Initiated GC";
        } else if (eventType.contains("System.gc()")) {
            return "System.gc()";
        } else if (eventType.contains("Metadata GC")) {
            return "Metadata GC Threshold";
        } else if (eventType.contains("Ergonomics")) {
            return "Ergonomics";
        } else if (eventType.contains("CMS")) {
            return "CMS";
        } else if (eventType.contains("promotion failed")) {
            return "Promotion Failed";
        } else if (eventType.contains("concurrent mode failure")) {
            return "Concurrent Mode Failure";
        } else {
            return eventType.replaceAll("\\(.*?\\)", "").trim();
        }
    }
    
    /**
     * 解析安全点统计
     */
    private SafePointStats parseSafePointStats(List<String> lines) {
        // 解析安全点相关日志
        Pattern safePointPattern = Pattern.compile("Total time for which application threads were stopped:\\s+([\\d.]+)\\s+seconds");
        Pattern timeToSafePointPattern = Pattern.compile("Stopping threads took:\\s+([\\d.]+)\\s+seconds");
        
        List<Double> durations = new ArrayList<>();
        List<Double> timeToSafePoints = new ArrayList<>();
        
        for (String line : lines) {
            Matcher durationMatcher = safePointPattern.matcher(line);
            if (durationMatcher.find()) {
                durations.add(Double.parseDouble(durationMatcher.group(1)) * 1000);  // 转换为 ms
            }
            
            Matcher timeToMatcher = timeToSafePointPattern.matcher(line);
            if (timeToMatcher.find()) {
                timeToSafePoints.add(Double.parseDouble(timeToMatcher.group(1)) * 1000);
            }
        }
        
        if (durations.isEmpty()) {
            return null;  // 没有安全点数据
        }
        
        double totalDuration = durations.stream().mapToDouble(Double::doubleValue).sum();
        double avgDuration = totalDuration / durations.size();
        double maxDuration = durations.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
        double minDuration = durations.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
        double avgTimeToSafePoint = timeToSafePoints.isEmpty() ? 0.0 : 
                timeToSafePoints.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        
        return SafePointStats.builder()
                .totalCount(durations.size())
                .avgDuration(avgDuration)
                .maxDuration(maxDuration)
                .minDuration(minDuration)
                .totalDuration(totalDuration)
                .avgTimeToSafePoint(avgTimeToSafePoint)
                .longSafePoints(new ArrayList<>())
                .build();
    }
    
    /**
     * 解析字符串去重统计
     */
    private StringDeduplicationStats parseStringDeduplication(List<String> lines) {
        // 解析字符串去重相关日志
        Pattern dedupPattern = Pattern.compile("\\[String Deduplication.*?inspected:(\\d+).*?deduplicated:(\\d+).*?saved:(\\d+)");
        
        long totalInspected = 0;
        long totalDeduplicated = 0;
        long bytesSaved = 0;
        
        for (String line : lines) {
            Matcher matcher = dedupPattern.matcher(line);
            if (matcher.find()) {
                totalInspected += Long.parseLong(matcher.group(1));
                totalDeduplicated += Long.parseLong(matcher.group(2));
                bytesSaved += Long.parseLong(matcher.group(3));
            }
        }
        
        if (totalInspected == 0) {
            return null;  // 没有字符串去重数据
        }
        
        double deduplicationRate = totalInspected > 0 ? 
                (totalDeduplicated * 100.0 / totalInspected) : 0.0;
        
        return StringDeduplicationStats.builder()
                .totalInspected(totalInspected)
                .totalDeduplicated(totalDeduplicated)
                .bytesSaved(bytesSaved)
                .deduplicationRate(deduplicationRate)
                .avgDeduplicationTime(0.0)
                .totalDeduplicationTime(0L)
                .build();
    }
    
    /**
     * 增强的阶段统计（包含标准差）
     */
    private Map<String, PhaseStatistics> calculatePhaseStatisticsEnhanced(List<GCEvent> events, String collectorType) {
        Map<String, PhaseStatistics> stats = new HashMap<>();
        
        // 收集各阶段时间数据
        Map<String, List<Double>> phaseTimesMap = new HashMap<>();
        
        for (GCEvent event : events) {
            String eventType = event.getEventType();
            double pauseTime = event.getPauseTime();
            
            phaseTimesMap.computeIfAbsent(eventType, k -> new ArrayList<>()).add(pauseTime);
        }
        
        // 为每个阶段创建统计信息（包含标准差）
        for (Map.Entry<String, List<Double>> entry : phaseTimesMap.entrySet()) {
            String phaseName = entry.getKey();
            List<Double> times = entry.getValue();
            
            if (!times.isEmpty()) {
                double totalTime = times.stream().mapToDouble(Double::doubleValue).sum();
                double avgTime = totalTime / times.size();
                double maxTime = times.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
                double minTime = times.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
                
                // 计算标准差
                double variance = times.stream()
                        .mapToDouble(t -> Math.pow(t - avgTime, 2))
                        .average()
                        .orElse(0.0);
                double stdDev = Math.sqrt(variance);
                
                stats.put(phaseName, PhaseStatistics.builder()
                        .phaseName(phaseName)
                        .totalTime((long) totalTime)
                        .avgTime(avgTime)
                        .maxTime(maxTime)
                        .minTime(minTime)
                        .stdDevTime(stdDev)
                        .count(times.size())
                        .build());
            }
        }
        
        return stats;
    }
    
    /**
     * 增强的诊断（包含连续 Full GC 检测）
     */
    private DiagnosisReport performDiagnosisEnhanced(List<GCEvent> events, MemorySize memorySize) {
        // 内存泄漏检测
        DiagnosisReport.MemoryLeakInfo memoryLeakInfo = detectMemoryLeak(events);
        
        // Full GC检测
        DiagnosisReport.FullGCInfo fullGCInfo = detectFullGC(events);
        
        // 长暂停检测
        DiagnosisReport.LongPauseInfo longPauseInfo = detectLongPause(events);
        
        // 连续 Full GC 检测
        DiagnosisReport.ConsecutiveFullGCInfo consecutiveFullGCInfo = detectConsecutiveFullGC(events);
        
        // 安全点信息（如果有数据）
        DiagnosisReport.SafePointInfo safePointInfo = null;  // 可以从 SafePointStats 计算
        
        // 生成建议
        List<DiagnosisReport.Recommendation> recommendations = generateRecommendationsEnhanced(
                events, memorySize, memoryLeakInfo, fullGCInfo, longPauseInfo, consecutiveFullGCInfo);
        
        return DiagnosisReport.builder()
                .memoryLeakInfo(memoryLeakInfo)
                .fullGCInfo(fullGCInfo)
                .longPauseInfo(longPauseInfo)
                .consecutiveFullGCInfo(consecutiveFullGCInfo)
                .safePointInfo(safePointInfo)
                .recommendations(recommendations)
                .build();
    }
    
    /**
     * 检测连续 Full GC
     */
    private DiagnosisReport.ConsecutiveFullGCInfo detectConsecutiveFullGC(List<GCEvent> events) {
        List<DiagnosisReport.ConsecutiveFullGCInfo.ConsecutiveFullGCSequence> sequences = new ArrayList<>();
        int maxConsecutiveCount = 0;
        int currentCount = 0;
        List<GCEvent> currentSequence = new ArrayList<>();
        
        for (GCEvent event : events) {
            if (event.isFullGC()) {
                currentCount++;
                currentSequence.add(event);
            } else {
                if (currentCount >= 2) {  // 至少2次连续才记录
                    long startTs = currentSequence.get(0).getTimestamp();
                    long endTs = currentSequence.get(currentSequence.size() - 1).getTimestamp();
                    double totalDuration = currentSequence.stream()
                            .mapToDouble(GCEvent::getPauseTime).sum();
                    
                    sequences.add(DiagnosisReport.ConsecutiveFullGCInfo.ConsecutiveFullGCSequence.builder()
                            .count(currentCount)
                            .startTimestamp(startTs)
                            .endTimestamp(endTs)
                            .totalDuration(totalDuration)
                            .events(new ArrayList<>(currentSequence))
                            .build());
                    
                    maxConsecutiveCount = Math.max(maxConsecutiveCount, currentCount);
                }
                currentCount = 0;
                currentSequence.clear();
            }
        }
        
        // 处理结尾的连续 Full GC
        if (currentCount >= 2) {
            long startTs = currentSequence.get(0).getTimestamp();
            long endTs = currentSequence.get(currentSequence.size() - 1).getTimestamp();
            double totalDuration = currentSequence.stream()
                    .mapToDouble(GCEvent::getPauseTime).sum();
            
            sequences.add(DiagnosisReport.ConsecutiveFullGCInfo.ConsecutiveFullGCSequence.builder()
                    .count(currentCount)
                    .startTimestamp(startTs)
                    .endTimestamp(endTs)
                    .totalDuration(totalDuration)
                    .events(new ArrayList<>(currentSequence))
                    .build());
            
            maxConsecutiveCount = Math.max(maxConsecutiveCount, currentCount);
        }
        
        String severity = determineSeverity(maxConsecutiveCount);
        
        return DiagnosisReport.ConsecutiveFullGCInfo.builder()
                .hasConsecutiveFullGC(!sequences.isEmpty())
                .maxConsecutiveCount(maxConsecutiveCount)
                .sequences(sequences)
                .severity(severity)
                .build();
    }
    
    private String determineSeverity(int consecutiveCount) {
        if (consecutiveCount >= 10) return "CRITICAL";
        if (consecutiveCount >= 5) return "HIGH";
        if (consecutiveCount >= 3) return "MEDIUM";
        return "LOW";
    }
    
    /**
     * 增强的建议生成
     */
    private List<DiagnosisReport.Recommendation> generateRecommendationsEnhanced(
            List<GCEvent> events, MemorySize memorySize,
            DiagnosisReport.MemoryLeakInfo memoryLeakInfo,
            DiagnosisReport.FullGCInfo fullGCInfo,
            DiagnosisReport.LongPauseInfo longPauseInfo,
            DiagnosisReport.ConsecutiveFullGCInfo consecutiveFullGCInfo) {
        
        List<DiagnosisReport.Recommendation> recommendations = new ArrayList<>();
        
        // 连续 Full GC 警告（最高优先级）
        if (consecutiveFullGCInfo.isHasConsecutiveFullGC()) {
            String level = switch (consecutiveFullGCInfo.getSeverity()) {
                case "CRITICAL" -> "CRITICAL";
                case "HIGH" -> "WARNING";
                default -> "INFO";
            };
            
            recommendations.add(DiagnosisReport.Recommendation.builder()
                    .category("严重问题")
                    .level(level)
                    .title("检测到连续 Full GC")
                    .description(String.format("检测到最多 %d 次连续 Full GC，严重程度：%s", 
                            consecutiveFullGCInfo.getMaxConsecutiveCount(), 
                            consecutiveFullGCInfo.getSeverity()))
                    .suggestion("立即检查应用程序是否存在内存泄漏，考虑增加堆内存大小，或优化对象生命周期管理")
                    .build());
        }
        
        // Full GC 警告
        if (fullGCInfo.isHasFullGC() && fullGCInfo.getCount() > 10) {
            recommendations.add(DiagnosisReport.Recommendation.builder()
                    .category("GC配置")
                    .level("WARNING")
                    .title("检测到频繁 Full GC")
                    .description(String.format("系统执行了 %d 次 Full GC", fullGCInfo.getCount()))
                    .suggestion("考虑增加堆内存大小、优化对象分配策略或调整GC参数")
                    .build());
        }
        
        // 长暂停警告
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
     * 解析绝对时间戳（ISO 8601格式）
     * 例如：2025-08-05T13:23:18.409+0800
     * 
     * @param timestampStr ISO 8601格式的时间戳字符串
     * @return Unix毫秒时间戳
     */
    private long parseAbsoluteTimestamp(String timestampStr) {
        try {
            // 解析格式：2025-08-05T13:23:18.409+0800
            // 使用 XX 来匹配 +0800 这样的时区格式（没有冒号）
            java.time.format.DateTimeFormatter formatter = 
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXX");
            java.time.ZonedDateTime zonedDateTime = java.time.ZonedDateTime.parse(timestampStr, formatter);
            return zonedDateTime.toInstant().toEpochMilli();
        } catch (Exception e) {
            // 如果解析失败，返回0
            return 0;
        }
    }
}

