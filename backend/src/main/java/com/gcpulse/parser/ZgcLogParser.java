package com.gcpulse.parser;

import com.gcpulse.model.GCEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ZGC日志解析器
 * 支持JDK 11+ ZGC Unified Logging格式
 * 支持分代模式(ZGenerational)和非分代模式(legacy single-generation)
 * 
 * 分代模式特点：
 * - 包含 Minor Collection (Young GC) 和 Major Collection (Old GC)
 * - GC日志中包含 "GC Workers for Young Generation" 和 "GC Workers for Old Generation"
 * - 统计信息区分 Minor Collection 和 Major Collection
 */
@Slf4j
@Component
public class ZgcLogParser extends AbstractGCLogParser {
    
    // ZGC GC开始标记（同时支持分代和非分代）
    private static final Pattern ZGC_START_PATTERN = Pattern.compile("\\[(\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}[+-]\\d{4})\\]\\[\\d+\\]\\[gc,start\\s*\\]\\s*GC\\((\\d+)\\)\\s*(.+?)\\s*$");
    
    // 分代模式检测
    private static final Pattern ZGC_GENERATIONAL_PATTERN = Pattern.compile("GC Workers for (Young|Old) Generation");
    
    // ZGC Pause阶段
    private static final Pattern ZGC_PAUSE_PATTERN = Pattern.compile("\\[(\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}[+-]\\d{4})\\]\\[\\d+\\]\\[gc,phases\\s*\\]\\s*GC\\((\\d+)\\)\\s*(Pause .+?)\\s+([\\d.]+)ms");
    
    // ZGC Concurrent阶段
    private static final Pattern ZGC_CONCURRENT_PATTERN = Pattern.compile("\\[(\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}[+-]\\d{4})\\]\\[\\d+\\]\\[gc,phases\\s*\\]\\s*GC\\((\\d+)\\)\\s*(Concurrent .+?)\\s+([\\d.]+)ms");
    
    // ZGC堆内存统计表格 - Used行
    private static final Pattern ZGC_HEAP_USED_PATTERN = Pattern.compile("\\[gc,heap\\s*\\]\\s*GC\\((\\d+)\\)\\s+Used:\\s+(\\d+)M\\s+\\(\\d+%\\)\\s+(\\d+)M\\s+\\(\\d+%\\)\\s+(\\d+)M\\s+\\(\\d+%\\)\\s+(\\d+)M\\s+\\(\\d+%\\)\\s+(\\d+)M\\s+\\(\\d+%\\)\\s+(\\d+)M\\s+\\(\\d+%\\)");
    
    // ZGC堆内存统计表格 - Live行
    private static final Pattern ZGC_HEAP_LIVE_PATTERN = Pattern.compile("\\[gc,heap\\s*\\]\\s*GC\\((\\d+)\\)\\s+Live:\\s+-\\s+(\\d+)M\\s+\\(\\d+%\\)\\s+(\\d+)M\\s+\\(\\d+%\\)\\s+(\\d+)M\\s+\\(\\d+%\\)");
    
    // ZGC堆内存统计表格 - Allocated行
    private static final Pattern ZGC_HEAP_ALLOCATED_PATTERN = Pattern.compile("\\[gc,heap\\s*\\]\\s*GC\\((\\d+)\\)\\s+Allocated:\\s+-\\s+(\\d+)M\\s+\\(\\d+%\\)\\s+(\\d+)M\\s+\\(\\d+%\\)\\s+(\\d+)M\\s+\\(\\d+%\\)");
    
    // ZGC堆内存统计表格 - Garbage行
    private static final Pattern ZGC_HEAP_GARBAGE_PATTERN = Pattern.compile("\\[gc,heap\\s*\\]\\s*GC\\((\\d+)\\)\\s+Garbage:\\s+-\\s+(\\d+)M\\s+\\(\\d+%\\)\\s+(\\d+)M\\s+\\(\\d+%\\)\\s+(\\d+)M\\s+\\(\\d+%\\)");
    
    // ZGC堆内存统计表格 - Reclaimed行
    private static final Pattern ZGC_HEAP_RECLAIMED_PATTERN = Pattern.compile("\\[gc,heap\\s*\\]\\s*GC\\((\\d+)\\)\\s+Reclaimed:\\s+-\\s+-\\s+(\\d+)M\\s+\\(\\d+%\\)\\s+(\\d+)M\\s+\\(\\d+%\\)");
    
    // ZGC堆内存统计表格 - Capacity行
    private static final Pattern ZGC_HEAP_CAPACITY_PATTERN = Pattern.compile("\\[gc,heap\\s*\\]\\s*GC\\((\\d+)\\)\\s+Capacity:\\s+(\\d+)M");
    
    // ZGC GC汇总行
    private static final Pattern ZGC_SUMMARY_PATTERN = Pattern.compile("\\[gc\\s*\\]\\s*GC\\((\\d+)\\)\\s*.*?\\s+(\\d+)M\\s*\\((\\d+)%\\)->(\\d+)M\\s*\\((\\d+)%\\)");
    
    // ZGC Metaspace
    private static final Pattern ZGC_METASPACE_PATTERN = Pattern.compile("\\[gc,metaspace\\]\\s*GC\\((\\d+)\\)\\s*Metaspace:\\s+(\\d+)M\\s+used,\\s+(\\d+)M\\s+committed,\\s+(\\d+)M\\s+reserved");
    
    @Override
    public String getGCType() {
        return "ZGC";
    }
    
    @Override
    public boolean canParse(List<String> lines) {
        for (String line : lines) {
            if (line.contains("Z Garbage Collector") || 
                line.contains("ZGC") || 
                line.contains("gc,init] Initializing The Z")) {
                return true;
            }
        }
        return false;
    }
    
    @Override
    public List<GCEvent> parseGCEvents(List<String> lines) {
        // 首先检测是否为分代模式
        boolean isGenerational = detectGenerationalMode(lines);
        log.info("ZGC模式检测: {}", isGenerational ? "分代模式(Generational)" : "非分代模式(Single-Generation)");
        
        List<GCEvent> events = new ArrayList<>();
        Map<Integer, ZGCEventData> gcDataMap = new HashMap<>();
        
        // 第一遍：收集所有GC事件的基本信息
        for (String line : lines) {
            try {
                // 检测GC开始
                Matcher startMatcher = ZGC_START_PATTERN.matcher(line);
                if (startMatcher.find()) {
                    String timestampStr = startMatcher.group(1);
                    int gcId = Integer.parseInt(startMatcher.group(2));
                    String gcCause = startMatcher.group(3);
                    
                    ZGCEventData data = gcDataMap.computeIfAbsent(gcId, k -> new ZGCEventData());
                    data.gcId = gcId;
                    data.timestamp = parseAbsoluteTimestamp(timestampStr);
                    data.gcCause = gcCause;
                }
                
                // 收集Pause阶段时间
                Matcher pauseMatcher = ZGC_PAUSE_PATTERN.matcher(line);
                if (pauseMatcher.find()) {
                    int gcId = Integer.parseInt(pauseMatcher.group(2));
                    String phase = pauseMatcher.group(3);
                    double pauseTime = Double.parseDouble(pauseMatcher.group(4));
                    
                    ZGCEventData data = gcDataMap.computeIfAbsent(gcId, k -> new ZGCEventData());
                    data.pausePhases.put(phase, pauseTime);
                    data.totalPauseTime += pauseTime;
                }
                
                // 收集Concurrent阶段时间
                Matcher concurrentMatcher = ZGC_CONCURRENT_PATTERN.matcher(line);
                if (concurrentMatcher.find()) {
                    int gcId = Integer.parseInt(concurrentMatcher.group(2));
                    String phase = concurrentMatcher.group(3);
                    double concurrentTime = Double.parseDouble(concurrentMatcher.group(4));
                    
                    ZGCEventData data = gcDataMap.computeIfAbsent(gcId, k -> new ZGCEventData());
                    data.concurrentPhases.put(phase, concurrentTime);
                    data.totalConcurrentTime += concurrentTime;
                }
                
            } catch (Exception e) {
                log.debug("解析ZGC基本信息失败: {}", e.getMessage());
            }
        }
        
        // 第二遍：收集详细的堆内存统计信息
        for (String line : lines) {
            try {
                // Used: Mark Start / Mark End / Relocate Start / Relocate End / High / Low
                Matcher usedMatcher = ZGC_HEAP_USED_PATTERN.matcher(line);
                if (usedMatcher.find()) {
                    int gcId = Integer.parseInt(usedMatcher.group(1));
                    ZGCEventData data = gcDataMap.get(gcId);
                    if (data != null) {
                        data.usedMarkStart = Long.parseLong(usedMatcher.group(2)) * 1024 * 1024;
                        data.usedMarkEnd = Long.parseLong(usedMatcher.group(3)) * 1024 * 1024;
                        data.usedRelocateStart = Long.parseLong(usedMatcher.group(4)) * 1024 * 1024;
                        data.usedRelocateEnd = Long.parseLong(usedMatcher.group(5)) * 1024 * 1024;
                        data.usedHigh = Long.parseLong(usedMatcher.group(6)) * 1024 * 1024;
                        data.usedLow = Long.parseLong(usedMatcher.group(7)) * 1024 * 1024;
                    }
                    continue;
                }
                
                // Live: - / Mark End / Relocate Start / Relocate End
                Matcher liveMatcher = ZGC_HEAP_LIVE_PATTERN.matcher(line);
                if (liveMatcher.find()) {
                    int gcId = Integer.parseInt(liveMatcher.group(1));
                    ZGCEventData data = gcDataMap.get(gcId);
                    if (data != null) {
                        data.liveMarkEnd = Long.parseLong(liveMatcher.group(2)) * 1024 * 1024;
                        data.liveRelocateStart = Long.parseLong(liveMatcher.group(3)) * 1024 * 1024;
                        data.liveRelocateEnd = Long.parseLong(liveMatcher.group(4)) * 1024 * 1024;
                    }
                    continue;
                }
                
                // Allocated: - / Mark End / Relocate Start / Relocate End
                Matcher allocatedMatcher = ZGC_HEAP_ALLOCATED_PATTERN.matcher(line);
                if (allocatedMatcher.find()) {
                    int gcId = Integer.parseInt(allocatedMatcher.group(1));
                    ZGCEventData data = gcDataMap.get(gcId);
                    if (data != null) {
                        data.allocatedMarkEnd = Long.parseLong(allocatedMatcher.group(2)) * 1024 * 1024;
                        data.allocatedRelocateStart = Long.parseLong(allocatedMatcher.group(3)) * 1024 * 1024;
                        data.allocatedRelocateEnd = Long.parseLong(allocatedMatcher.group(4)) * 1024 * 1024;
                    }
                    continue;
                }
                
                // Garbage: - / Mark End / Relocate Start / Relocate End
                Matcher garbageMatcher = ZGC_HEAP_GARBAGE_PATTERN.matcher(line);
                if (garbageMatcher.find()) {
                    int gcId = Integer.parseInt(garbageMatcher.group(1));
                    ZGCEventData data = gcDataMap.get(gcId);
                    if (data != null) {
                        data.garbageMarkEnd = Long.parseLong(garbageMatcher.group(2)) * 1024 * 1024;
                        data.garbageRelocateStart = Long.parseLong(garbageMatcher.group(3)) * 1024 * 1024;
                        data.garbageRelocateEnd = Long.parseLong(garbageMatcher.group(4)) * 1024 * 1024;
                    }
                    continue;
                }
                
                // Reclaimed: - / - / Relocate Start / Relocate End
                Matcher reclaimedMatcher = ZGC_HEAP_RECLAIMED_PATTERN.matcher(line);
                if (reclaimedMatcher.find()) {
                    int gcId = Integer.parseInt(reclaimedMatcher.group(1));
                    ZGCEventData data = gcDataMap.get(gcId);
                    if (data != null) {
                        data.reclaimedRelocateStart = Long.parseLong(reclaimedMatcher.group(2)) * 1024 * 1024;
                        data.reclaimedRelocateEnd = Long.parseLong(reclaimedMatcher.group(3)) * 1024 * 1024;
                    }
                    continue;
                }
                
                // Capacity
                Matcher capacityMatcher = ZGC_HEAP_CAPACITY_PATTERN.matcher(line);
                if (capacityMatcher.find()) {
                    int gcId = Integer.parseInt(capacityMatcher.group(1));
                    ZGCEventData data = gcDataMap.get(gcId);
                    if (data != null) {
                        data.capacity = Long.parseLong(capacityMatcher.group(2)) * 1024 * 1024;
                    }
                    continue;
                }
                
                // Metaspace
                Matcher metaspaceMatcher = ZGC_METASPACE_PATTERN.matcher(line);
                if (metaspaceMatcher.find()) {
                    int gcId = Integer.parseInt(metaspaceMatcher.group(1));
                    ZGCEventData data = gcDataMap.get(gcId);
                    if (data != null) {
                        long metaUsed = Long.parseLong(metaspaceMatcher.group(2)) * 1024 * 1024;
                        long metaCommitted = Long.parseLong(metaspaceMatcher.group(3)) * 1024 * 1024;
                        data.metaspace = GCEvent.MemoryChange.builder()
                            .before(metaUsed)
                            .after(metaUsed)
                            .total(metaCommitted)
                            .build();
                    }
                    continue;
                }
                
            } catch (Exception e) {
                log.debug("解析ZGC堆内存统计失败: {}", e.getMessage());
            }
        }
        
        // 第三遍：组装完整的GC事件
        List<Integer> sortedGcIds = new ArrayList<>(gcDataMap.keySet());
        Collections.sort(sortedGcIds);
        
        for (Integer gcId : sortedGcIds) {
            ZGCEventData data = gcDataMap.get(gcId);
            if (data == null || data.timestamp == 0) {
                continue;
            }
            
            try {
                // 构建堆内存变化
                GCEvent.MemoryChange heapMemory = null;
                if (data.usedMarkStart > 0 && data.usedRelocateEnd > 0 && data.capacity > 0) {
                    heapMemory = GCEvent.MemoryChange.builder()
                            .before(data.usedMarkStart)
                            .after(data.usedRelocateEnd)
                            .total(data.capacity)
                            .build();
                }
                
                // 判断事件类型
                String eventType;
                boolean isMinorGC = false;
                if (isGenerational) {
                    // 分代模式：根据gcCause判断是Minor还是Major
                    if (data.gcCause != null && 
                        (data.gcCause.contains("Minor") || 
                         data.gcCause.toLowerCase().contains("young"))) {
                        eventType = "ZGC Minor Collection";
                        isMinorGC = true;
                    } else if (data.gcCause != null && 
                               (data.gcCause.contains("Major") || 
                                data.gcCause.toLowerCase().contains("old"))) {
                        eventType = "ZGC Major Collection";
                        isMinorGC = false;
                    } else {
                        // 默认视为Minor Collection（年轻代GC更频繁）
                        eventType = "ZGC Minor Collection";
                        isMinorGC = true;
                    }
                } else {
                    // 非分代模式：统一称为ZGC Cycle
                    eventType = "ZGC Cycle";
                    isMinorGC = false; // 非分代模式不区分
                }
                
                GCEvent event = GCEvent.builder()
                        .timestamp(data.timestamp)
                        .eventType(eventType)
                        .gcCause(data.gcCause != null ? data.gcCause : "ZGC")
                        .pauseTime(data.totalPauseTime)
                        .concurrentTime(data.totalConcurrentTime)
                        .heapMemory(heapMemory)
                        .metaspace(data.metaspace)
                        .isFullGC(!isMinorGC && isGenerational)  // 分代模式下，Major Collection视为Full GC
                        .isLongPause(data.totalPauseTime > 10)
                        .build();
                
                events.add(event);
                
            } catch (Exception e) {
                log.error("构建ZGC事件失败: {}", e.getMessage());
            }
        }
        
        log.info("解析到 {} 个ZGC事件", events.size());
        return events;
    }
    
    /**
     * 检测ZGC是否为分代模式
     */
    private boolean detectGenerationalMode(List<String> lines) {
        for (String line : lines) {
            // 检查分代模式的特征标记
            if (line.contains("GC Workers for Young Generation") || 
                line.contains("GC Workers for Old Generation") ||
                line.contains("Minor Collection") ||
                line.contains("Major Collection")) {
                return true;
            }
            // 检查非分代模式的明确标记
            if (line.contains("legacy single-generation mode")) {
                return false;
            }
        }
        return false; // 默认为非分代模式
    }
    
    /**
     * ZGC事件数据结构
     */
    private static class ZGCEventData {
        int gcId;
        long timestamp;
        String gcCause;
        
        // 阶段时间
        Map<String, Double> pausePhases = new HashMap<>();
        Map<String, Double> concurrentPhases = new HashMap<>();
        double totalPauseTime = 0.0;
        double totalConcurrentTime = 0.0;
        
        // 堆内存统计（来自gc,heap表格）
        long usedMarkStart;
        long usedMarkEnd;
        long usedRelocateStart;
        long usedRelocateEnd;
        long usedHigh;
        long usedLow;
        
        long liveMarkEnd;
        long liveRelocateStart;
        long liveRelocateEnd;
        
        long allocatedMarkEnd;
        long allocatedRelocateStart;
        long allocatedRelocateEnd;
        
        long garbageMarkEnd;
        long garbageRelocateStart;
        long garbageRelocateEnd;
        
        long reclaimedRelocateStart;
        long reclaimedRelocateEnd;
        
        long capacity;
        
        GCEvent.MemoryChange metaspace;
    }
}

