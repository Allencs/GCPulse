package com.gcpulse.parser;

import com.gcpulse.model.GCEvent;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parallel GC日志解析器
 * 支持PSYoungGen和ParOldGen详细统计
 */
@Component
public class ParallelGCLogParser extends AbstractGCLogParser {
    
    // Parallel GC Pattern
    private static final Pattern PARALLEL_PATTERN = Pattern.compile("\\[(Full )?GC.*?\\[PS.*?(\\d+)K->(\\d+)K\\((\\d+)K\\).*?(\\d+\\.\\d+) secs\\]");
    private static final Pattern PARALLEL_YOUNG_PATTERN = Pattern.compile("\\[PSYoungGen: (\\d+)K->(\\d+)K\\((\\d+)K\\)\\]");
    private static final Pattern PARALLEL_OLD_PATTERN = Pattern.compile("\\[ParOldGen: (\\d+)K->(\\d+)K\\((\\d+)K\\)\\]");
    private static final Pattern PARALLEL_FULL_GC_PATTERN = Pattern.compile("(\\d+\\.\\d+): \\[Full GC.*?\\[PSYoungGen: (\\d+)K->(\\d+)K\\((\\d+)K\\)\\].*?\\[ParOldGen: (\\d+)K->(\\d+)K\\((\\d+)K\\)\\]\\s*(\\d+)K->(\\d+)K\\((\\d+)K\\).*?(\\d+\\.\\d+)\\s*secs\\]");
    
    @Override
    public String getGCType() {
        return "Parallel GC";
    }
    
    @Override
    public boolean canParse(List<String> lines) {
        for (String line : lines) {
            if (line.contains("Using Parallel") || 
                line.contains("[PSYoungGen") || 
                line.contains("[ParOldGen")) {
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
                GCEvent event = parseParallelGCEvent(line);
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
     * 解析Parallel GC事件
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
            
            // 提取Metaspace信息
            GCEvent.MemoryChange metaspace = extractMetaspaceInfo(line);
            
            return GCEvent.builder()
                    .timestamp((long) (timestamp * 1000))
                    .eventType("Full GC (Parallel)")
                    .gcCause("Full GC (Parallel)")
                    .pauseTime(pauseTime)
                    .heapMemory(heapMemory)
                    .youngGen(youngGen)
                    .oldGen(oldGen)
                    .metaspace(metaspace)
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
        
        // 提取Metaspace信息
        GCEvent.MemoryChange metaspace = extractMetaspaceInfo(line);
        
        String eventType = isFullGC ? "Full GC (Parallel)" : "Young GC (Parallel)";
        return GCEvent.builder()
                .timestamp((long) (timestamp * 1000))
                .eventType(eventType)
                .gcCause(eventType)
                .pauseTime(pauseTime)
                .heapMemory(heapMemory)
                .youngGen(youngGen)
                .oldGen(oldGen)
                .metaspace(metaspace)
                .isFullGC(isFullGC)
                .isLongPause(pauseTime > 100)
                .build();
    }
}

