package com.gcpulse.parser;

import com.gcpulse.model.GCEvent;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ZGC日志解析器
 * 支持旧格式和新格式（Java 21 Unified Logging）
 * 支持分代模式（Generational ZGC）
 */
@Component
public class ZgcLogParser extends AbstractGCLogParser {
    
    // ZGC Pattern - 旧格式
    private static final Pattern ZGC_PATTERN_OLD = Pattern.compile("\\[(\\d+\\.\\d+)s\\].*?GC\\((\\d+)\\).*?Pause.*?(\\d+\\.\\d+)ms");
    
    // ZGC Pattern - 新格式
    private static final Pattern ZGC_PATTERN_NEW = Pattern.compile("\\[(\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}[+-]\\d{4})\\]\\[\\d+\\]\\[gc,phases\\s*\\] GC\\((\\d+)\\) (Pause .*?) ([\\d.]+)ms");
    private static final Pattern ZGC_UNIFIED_TIMESTAMP = Pattern.compile("\\[(\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3})");
    
    // ZGC 分代模式 - 年轻代和老年代统计
    private static final Pattern ZGC_GENERATIONAL_YOUNG_PATTERN = Pattern.compile("\\[gc,heap\\s*\\]\\s*GC\\((\\d+)\\)\\s*Young:\\s*([\\d.]+)([KMGT])->([\\d.]+)([KMGT])\\(([\\d.]+)([KMGT])\\)");
    private static final Pattern ZGC_GENERATIONAL_OLD_PATTERN = Pattern.compile("\\[gc,heap\\s*\\]\\s*GC\\((\\d+)\\)\\s*Old:\\s*([\\d.]+)([KMGT])->([\\d.]+)([KMGT])\\(([\\d.]+)([KMGT])\\)");
    
    // ZGC 堆内存统计
    private static final Pattern ZGC_HEAP_PATTERN = Pattern.compile("\\[gc\\s*\\]\\s*GC\\((\\d+)\\)\\s*.*?([\\d.]+)M->([\\d.]+)M\\(([\\d.]+)M\\)");
    
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
        List<GCEvent> events = new ArrayList<>();
        
        for (String line : lines) {
            try {
                GCEvent event = parseZGCEvent(line);
                if (event != null) {
                    events.add(event);
                }
            } catch (Exception e) {
                // 忽略无法解析的行
            }
        }
        
        // 为ZGC事件增强堆内存信息
        enhanceZGCEventsWithHeapInfo(events, lines);
        
        return events;
    }
    
    /**
     * 解析单个ZGC事件（支持新旧两种格式）
     */
    private GCEvent parseZGCEvent(String line) {
        // 尝试新格式（Java 21 Unified Logging）
        Matcher newMatcher = ZGC_PATTERN_NEW.matcher(line);
        if (newMatcher.find()) {
            String timestampStr = newMatcher.group(1);
            String gcId = newMatcher.group(2);
            String pauseType = newMatcher.group(3);
            double pauseTime = Double.parseDouble(newMatcher.group(4));
            
            Matcher tsMatcher = Pattern.compile("(\\d{4})-(\\d{2})-(\\d{2})T(\\d{2}):(\\d{2}):(\\d{2})\\.(\\d{3})").matcher(timestampStr);
            long timestamp = 0;
            if (tsMatcher.find()) {
                int hour = Integer.parseInt(tsMatcher.group(4));
                int minute = Integer.parseInt(tsMatcher.group(5));
                int second = Integer.parseInt(tsMatcher.group(6));
                int milli = Integer.parseInt(tsMatcher.group(7));
                timestamp = (hour * 3600L + minute * 60L + second) * 1000L + milli;
            }
            
            return GCEvent.builder()
                    .timestamp(timestamp)
                    .eventType("ZGC " + pauseType)
                    .gcCause(pauseType)
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
                    .gcCause("ZGC Pause")
                    .pauseTime(pauseTime)
                    .concurrentTime(0.0)
                    .isFullGC(false)
                    .isLongPause(pauseTime > 10)
                    .build();
        }
        
        return null;
    }
    
    /**
     * 为ZGC事件增强堆内存信息（包括分代模式支持）
     */
    private void enhanceZGCEventsWithHeapInfo(List<GCEvent> gcEvents, List<String> lines) {
        Pattern heapPattern = Pattern.compile("\\[gc,heap\\s*\\] GC\\((\\d+)\\)\\s+Used:\\s+(\\d+)M.*?(\\d+)M.*?(\\d+)M");
        Pattern capacityPattern = Pattern.compile("\\[gc,heap\\s*\\] GC\\((\\d+)\\)\\s+Capacity:\\s+(\\d+)M");
        
        Map<Integer, Long> gcUsedMemory = new HashMap<>();
        Map<Integer, Long> gcCapacity = new HashMap<>();
        Map<Integer, GCEvent.MemoryChange> gcYoungGen = new HashMap<>();
        Map<Integer, GCEvent.MemoryChange> gcOldGen = new HashMap<>();
        Map<Integer, GCEvent.MemoryChange> gcHeapMemory = new HashMap<>();
        Map<Integer, GCEvent.MemoryChange> gcMetaspace = new HashMap<>();
        
        for (String line : lines) {
            // 解析传统的堆内存信息
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
            
            // 解析ZGC分代模式的年轻代信息
            Matcher youngMatcher = ZGC_GENERATIONAL_YOUNG_PATTERN.matcher(line);
            if (youngMatcher.find()) {
                try {
                    int gcId = Integer.parseInt(youngMatcher.group(1));
                    double youngBefore = parseMemoryUnit(youngMatcher.group(2), youngMatcher.group(3));
                    double youngAfter = parseMemoryUnit(youngMatcher.group(4), youngMatcher.group(5));
                    double youngTotal = parseMemoryUnit(youngMatcher.group(6), youngMatcher.group(7));
                    
                    GCEvent.MemoryChange youngGen = GCEvent.MemoryChange.builder()
                            .before((long) youngBefore)
                            .after((long) youngAfter)
                            .total((long) youngTotal)
                            .build();
                    gcYoungGen.put(gcId, youngGen);
                } catch (Exception e) {
                    System.err.println("解析ZGC年轻代信息失败: " + e.getMessage());
                }
            }
            
            // 解析ZGC分代模式的老年代信息
            Matcher oldMatcher = ZGC_GENERATIONAL_OLD_PATTERN.matcher(line);
            if (oldMatcher.find()) {
                try {
                    int gcId = Integer.parseInt(oldMatcher.group(1));
                    double oldBefore = parseMemoryUnit(oldMatcher.group(2), oldMatcher.group(3));
                    double oldAfter = parseMemoryUnit(oldMatcher.group(4), oldMatcher.group(5));
                    double oldTotal = parseMemoryUnit(oldMatcher.group(6), oldMatcher.group(7));
                    
                    GCEvent.MemoryChange oldGen = GCEvent.MemoryChange.builder()
                            .before((long) oldBefore)
                            .after((long) oldAfter)
                            .total((long) oldTotal)
                            .build();
                    gcOldGen.put(gcId, oldGen);
                } catch (Exception e) {
                    System.err.println("解析ZGC老年代信息失败: " + e.getMessage());
                }
            }
            
            // 解析ZGC的堆内存总量信息
            Matcher heapMemMatcher = ZGC_HEAP_PATTERN.matcher(line);
            if (heapMemMatcher.find()) {
                try {
                    int gcId = Integer.parseInt(heapMemMatcher.group(1));
                    long heapBefore = (long) (Double.parseDouble(heapMemMatcher.group(2)) * 1024 * 1024);
                    long heapAfter = (long) (Double.parseDouble(heapMemMatcher.group(3)) * 1024 * 1024);
                    long heapTotal = (long) (Double.parseDouble(heapMemMatcher.group(4)) * 1024 * 1024);
                    
                    GCEvent.MemoryChange heapMem = GCEvent.MemoryChange.builder()
                            .before(heapBefore)
                            .after(heapAfter)
                            .total(heapTotal)
                            .build();
                    gcHeapMemory.put(gcId, heapMem);
                } catch (Exception e) {
                    System.err.println("解析ZGC堆内存信息失败: " + e.getMessage());
                }
            }
            
            // 解析ZGC的Metaspace信息
            Matcher metaMatcher = METASPACE_UNIFIED_PATTERN.matcher(line);
            if (metaMatcher.find()) {
                try {
                    int gcId = Integer.parseInt(metaMatcher.group(1));
                    long metaUsed = (long) (Double.parseDouble(metaMatcher.group(2)) * 1024 * 1024);
                    long metaCommitted = (long) (Double.parseDouble(metaMatcher.group(3)) * 1024 * 1024);
                    
                    GCEvent.MemoryChange metaMem = GCEvent.MemoryChange.builder()
                            .before(metaUsed)
                            .after(metaUsed)
                            .total(metaCommitted)
                            .build();
                    gcMetaspace.put(gcId, metaMem);
                } catch (Exception e) {
                    System.err.println("解析ZGC Metaspace信息失败: " + e.getMessage());
                }
            }
        }
        
        // 将内存信息关联到GC事件
        int gcId = 0;
        for (GCEvent event : gcEvents) {
            if (event.getEventType().contains("Relocate Start") || event.getEventType().contains("ZGC")) {
                // 优先使用解析的堆内存信息
                if (gcHeapMemory.containsKey(gcId)) {
                    event.setHeapMemory(gcHeapMemory.get(gcId));
                } else {
                    // 兼容旧格式
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
                }
                
                // 设置年轻代信息
                if (gcYoungGen.containsKey(gcId)) {
                    event.setYoungGen(gcYoungGen.get(gcId));
                }
                
                // 设置老年代信息
                if (gcOldGen.containsKey(gcId)) {
                    event.setOldGen(gcOldGen.get(gcId));
                }
                
                // 设置Metaspace信息
                if (gcMetaspace.containsKey(gcId)) {
                    event.setMetaspace(gcMetaspace.get(gcId));
                }
                
                gcId++;
            }
        }
    }
}

