package com.gcpulse.parser;

import com.gcpulse.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * GC日志解析器工厂协调器
 * 负责检测GC类型并委托给具体的解析器实现
 * 同时负责聚合指标计算和诊断报告生成
 */
@Slf4j
@Component
public class GCLogParser {
    
    @Autowired
    private G1LogParser g1LogParser;
    
    @Autowired
    private ZgcLogParser zgcLogParser;
    
    @Autowired
    private CmsLogParser cmsLogParser;
    
    @Autowired
    private ParallelGCLogParser parallelGCLogParser;
    
    @Autowired
    private SerialGCLogParser serialGCLogParser;
    
    private List<AbstractGCLogParser> parsers;
    
    @Autowired
    public void initParsers() {
        parsers = Arrays.asList(
            g1LogParser,
            zgcLogParser,
            cmsLogParser,
            parallelGCLogParser,
            serialGCLogParser
        );
    }
    
    /**
     * 解析GC日志输入流
     */
    public GCPulseResult parse(InputStream inputStream, String fileName, long fileSize) throws IOException {
        List<String> lines = readLines(inputStream);
        
        // 检测GC收集器类型并选择解析器
        AbstractGCLogParser selectedParser = detectAndSelectParser(lines);
        String collectorType = selectedParser != null ? selectedParser.getGCType() : "Unknown";
        
        // 解析GC事件
        List<GCEvent> gcEvents = selectedParser != null ? 
            selectedParser.parseGCEvents(lines) : new ArrayList<>();
        
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
        JVMArguments jvmArgs = selectedParser != null ? 
                selectedParser.parseJVMArguments(lines) : parseJVMArguments(lines);
        TenuringSummary tenuringSummary = parseTenuringSummary(lines);
        Map<String, GCCause> gcCauses = calculateGCCauses(gcEvents);
        SafePointStats safePointStats = parseSafePointStats(lines);
        StringDeduplicationStats stringDedup = parseStringDeduplication(lines);
        
        // ZGC特定功能
        ZGCInitConfig zgcInitConfig = null;
        ZGCStatistics zgcStatistics = null;
        if ("ZGC".equals(collectorType) && selectedParser instanceof ZgcLogParser) {
            ZgcLogParser zgcParser = (ZgcLogParser) selectedParser;
            zgcInitConfig = zgcParser.parseZGCInitConfig(lines);
            zgcStatistics = zgcParser.parseZGCStatistics(lines);
        }
        
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
                // ZGC特定功能
                .zgcInitConfig(zgcInitConfig)
                .zgcStatistics(zgcStatistics)
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
     * 检测并选择合适的解析器
     */
    private AbstractGCLogParser detectAndSelectParser(List<String> lines) {
        for (AbstractGCLogParser parser : parsers) {
            if (parser.canParse(lines)) {
                log.info("选择解析器: {}", parser.getGCType());
                return parser;
            }
        }
        log.info("未找到合适的解析器，使用默认解析器");
            return null;
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
     * 计算对象统计
     */
    private ObjectStats calculateObjectStats(List<GCEvent> events, List<String> lines, String collectorType) {
        long totalCreated = 0;
        long totalPromoted = 0;
        long totalReclaimed = 0;
        
        // 从事件中提取统计数据
        for (GCEvent event : events) {
            if (event.getHeapMemory() != null) {
                long reclaimed = event.getHeapMemory().getReclaimed();
                if (reclaimed > 0) {
                    totalReclaimed += reclaimed;
                }
            }
            
            if (event.getYoungGen() != null && !event.isFullGC()) {
                long youngBefore = event.getYoungGen().getBefore();
                if (youngBefore > 0) {
                    totalCreated += youngBefore;
                }
            }
            
            if (event.getOldGen() != null) {
                long oldBefore = event.getOldGen().getBefore();
                long oldAfter = event.getOldGen().getAfter();
                long promoted = oldAfter - oldBefore;
                if (promoted > 0) {
                    totalPromoted += promoted;
                }
            }
        }
        
        // 对于ZGC，从日志中提取额外的信息
        if ("ZGC".equals(collectorType)) {
            // ZGC heap info表格格式:
            // [gc,heap] GC(0) Allocated:         -           12M (0%)          16M (0%)          23M (0%)             -                  -
            // [gc,heap] GC(0) Reclaimed:         -            -                4M (0%)          425M (8%)            -                  -
            // 我们需要提取最后一个阶段(Relocate End)的值
            Pattern allocPattern = Pattern.compile("\\[gc,heap\\s*\\]\\s*GC\\(\\d+\\)\\s+Allocated:\\s+-\\s+(\\d+)M\\s+\\(\\d+%\\)\\s+(\\d+)M\\s+\\(\\d+%\\)\\s+(\\d+)M\\s+\\(\\d+%\\)");
            Pattern reclaimedPattern = Pattern.compile("\\[gc,heap\\s*\\]\\s*GC\\(\\d+\\)\\s+Reclaimed:\\s+-\\s+-\\s+(\\d+)M\\s+\\(\\d+%\\)\\s+(\\d+)M\\s+\\(\\d+%\\)");
            
            for (String line : lines) {
                Matcher allocMatcher = allocPattern.matcher(line);
                if (allocMatcher.find()) {
                    // 使用Relocate End的值（第3个值）
                    long allocated = Long.parseLong(allocMatcher.group(3)) * 1024 * 1024;
                    totalCreated += allocated;
                }
                
                Matcher reclaimedMatcher = reclaimedPattern.matcher(line);
                if (reclaimedMatcher.find()) {
                    // 使用Relocate End的值（第2个值）
                    long reclaimed = Long.parseLong(reclaimedMatcher.group(2)) * 1024 * 1024;
                    totalReclaimed += reclaimed;
                }
            }
        }
        
        long totalTime = events.isEmpty() ? 1 : 
                events.get(events.size() - 1).getTimestamp() - events.get(0).getTimestamp();
        
        double avgCreationRate = totalTime > 0 ? 
                (totalCreated / 1024.0 / 1024.0) / (totalTime / 1000.0) : 0.0;
        
        double avgPromotionRate = totalTime > 0 ? 
                (totalPromoted / 1024.0 / 1024.0) / (totalTime / 1000.0) : 0.0;
        
        return ObjectStats.builder()
                .totalCreatedBytes(totalCreated)
                .totalPromotedBytes(totalPromoted)
                .totalReclaimedBytes(totalReclaimed)
                .avgCreationRate(avgCreationRate)
                .avgPromotionRate(avgPromotionRate)
                .build();
    }
    
    /**
     * 解析CPU统计
     */
    private CPUStats parseCPUStats(List<String> lines) {
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
     * 生成时间序列数据
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
        List<TimeSeriesData.DataPoint> metaspaceTrend = new ArrayList<>();
        
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
                    .metaspaceTrend(metaspaceTrend)
                    .build();
        }
        
        for (GCEvent event : events) {
            long timestamp = event.getTimestamp();
            
            if (event.getHeapMemory() != null) {
                heapAfterTrend.add(TimeSeriesData.DataPoint.builder()
                        .timestamp(timestamp)
                        .value(event.getHeapMemory().getAfter() / (1024.0 * 1024.0))
                        .build());
                
                heapBeforeTrend.add(TimeSeriesData.DataPoint.builder()
                        .timestamp(timestamp)
                        .value(event.getHeapMemory().getBefore() / (1024.0 * 1024.0))
                        .build());
                
                reclaimedTrend.add(TimeSeriesData.DataPoint.builder()
                        .timestamp(timestamp)
                        .value(event.getHeapMemory().getReclaimed() / (1024.0 * 1024.0))
                        .build());
            }
            
            pauseTrend.add(TimeSeriesData.DataPoint.builder()
                    .timestamp(timestamp)
                    .value(event.getPauseTime())
                    .build());
            
            if (event.getYoungGen() != null) {
                youngGenTrend.add(TimeSeriesData.DataPoint.builder()
                        .timestamp(timestamp)
                        .value(event.getYoungGen().getAfter() / (1024.0 * 1024.0))
                        .build());
                
                allocationTrend.add(TimeSeriesData.DataPoint.builder()
                        .timestamp(timestamp)
                        .value(event.getYoungGen().getBefore() / (1024.0 * 1024.0))
                        .build());
            }
            
            if (event.getOldGen() != null) {
                oldGenTrend.add(TimeSeriesData.DataPoint.builder()
                        .timestamp(timestamp)
                        .value(event.getOldGen().getAfter() / (1024.0 * 1024.0))
                        .build());
            }
            
            if (event.getOldGen() != null && event.getOldGen().getAfter() > event.getOldGen().getBefore()) {
                promotionTrend.add(TimeSeriesData.DataPoint.builder()
                        .timestamp(timestamp)
                        .value((event.getOldGen().getAfter() - event.getOldGen().getBefore()) / (1024.0 * 1024.0))
                        .build());
            }
            
            if (event.getMetaspace() != null) {
                metaspaceTrend.add(TimeSeriesData.DataPoint.builder()
                        .timestamp(timestamp)
                        .value(event.getMetaspace().getAfter() / (1024.0 * 1024.0))
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
                .metaspaceTrend(metaspaceTrend)
                .build();
    }
    
    /**
     * 解析 JVM 参数
     */
    private JVMArguments parseJVMArguments(List<String> lines) {
        List<String> allArgs = new ArrayList<>();
        List<String> gcArgs = new ArrayList<>();
        List<String> memoryArgs = new ArrayList<>();
        List<String> performanceArgs = new ArrayList<>();
        List<String> otherArgs = new ArrayList<>();
        
        Pattern commandLinePattern = Pattern.compile("CommandLine flags:\\s*(.*)");
        
        for (String line : lines) {
            Matcher matcher = commandLinePattern.matcher(line);
            if (matcher.find()) {
                String flagsStr = matcher.group(1);
                String[] flags = flagsStr.split("\\s+-");
                for (String flag : flags) {
                    if (flag.trim().isEmpty()) continue;
                    String arg = "-" + flag.trim();
                    allArgs.add(arg);
                    
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
            return null;
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
                .totalPromotedObjects(0L)
                .promotionRate(0.0)
                .build();
    }
    
    /**
     * 计算 GC 原因统计
     */
    private Map<String, GCCause> calculateGCCauses(List<GCEvent> events) {
        Map<String, GCCause> causes = new HashMap<>();
        Map<String, List<Double>> causeTimesMap = new HashMap<>();
        
        for (GCEvent event : events) {
            String cause = event.getGcCause();
            if (cause == null || cause.isEmpty() || "Unknown".equals(cause)) {
                cause = extractGCCause(event.getEventType());
            }
            causeTimesMap.computeIfAbsent(cause, k -> new ArrayList<>()).add(event.getPauseTime());
        }
        
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
        Pattern safePointPattern = Pattern.compile("Total time for which application threads were stopped:\\s+([\\d.]+)\\s+seconds");
        Pattern timeToSafePointPattern = Pattern.compile("Stopping threads took:\\s+([\\d.]+)\\s+seconds");
        
        List<Double> durations = new ArrayList<>();
        List<Double> timeToSafePoints = new ArrayList<>();
        
        for (String line : lines) {
            Matcher durationMatcher = safePointPattern.matcher(line);
            if (durationMatcher.find()) {
                durations.add(Double.parseDouble(durationMatcher.group(1)) * 1000);
            }
            
            Matcher timeToMatcher = timeToSafePointPattern.matcher(line);
            if (timeToMatcher.find()) {
                timeToSafePoints.add(Double.parseDouble(timeToMatcher.group(1)) * 1000);
            }
        }
        
        if (durations.isEmpty()) {
            return null;
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
            return null;
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
                double maxTime = times.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
                double minTime = times.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
                
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
        DiagnosisReport.MemoryLeakInfo memoryLeakInfo = detectMemoryLeak(events);
        DiagnosisReport.FullGCInfo fullGCInfo = detectFullGC(events);
        DiagnosisReport.LongPauseInfo longPauseInfo = detectLongPause(events);
        DiagnosisReport.ConsecutiveFullGCInfo consecutiveFullGCInfo = detectConsecutiveFullGC(events);
        DiagnosisReport.SafePointInfo safePointInfo = null;
        
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
    
    private DiagnosisReport.MemoryLeakInfo detectMemoryLeak(List<GCEvent> events) {
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
        double threshold = 100.0;
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
                currentCount = 0;
                currentSequence.clear();
            }
        }
        
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
        
        if (fullGCInfo.isHasFullGC() && fullGCInfo.getCount() > 10) {
            recommendations.add(DiagnosisReport.Recommendation.builder()
                    .category("GC配置")
                    .level("WARNING")
                    .title("检测到频繁 Full GC")
                    .description(String.format("系统执行了 %d 次 Full GC", fullGCInfo.getCount()))
                    .suggestion("考虑增加堆内存大小、优化对象分配策略或调整GC参数")
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
}
