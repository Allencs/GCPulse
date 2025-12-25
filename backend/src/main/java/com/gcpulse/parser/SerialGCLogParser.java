package com.gcpulse.parser;

import com.gcpulse.model.GCEvent;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Serial GC日志解析器
 * 支持DefNew和Tenured详细统计
 */
@Component
public class SerialGCLogParser extends AbstractGCLogParser {
    
    // Serial GC Pattern
    private static final Pattern SERIAL_PATTERN = Pattern.compile("(\\d+\\.\\d+): \\[GC.*?\\[DefNew: (\\d+)K->(\\d+)K\\((\\d+)K\\),\\s*([\\d.]+)\\s*secs\\]\\s*(\\d+)K->(\\d+)K\\((\\d+)K\\)");
    private static final Pattern SERIAL_FULL_GC_PATTERN = Pattern.compile("(\\d+\\.\\d+): \\[Full GC.*?\\[Tenured: (\\d+)K->(\\d+)K\\((\\d+)K\\),\\s*([\\d.]+)\\s*secs\\]\\s*(\\d+)K->(\\d+)K\\((\\d+)K\\)");
    
    @Override
    public String getGCType() {
        return "Serial GC";
    }
    
    @Override
    public boolean canParse(List<String> lines) {
        for (String line : lines) {
            if (line.contains("Using Serial") || 
                line.contains("[DefNew")) {
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
                GCEvent event = parseSerialGCEvent(line);
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
     * 解析Serial GC事件
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
            
            // 提取Metaspace信息
            GCEvent.MemoryChange metaspace = extractMetaspaceInfo(line);
            
            return GCEvent.builder()
                    .timestamp((long) (timestamp * 1000))
                    .eventType("Full GC (Serial)")
                    .gcCause("Full GC (Serial)")
                    .pauseTime(pauseTime)
                    .heapMemory(heapMemory)
                    .oldGen(oldGen)
                    .metaspace(metaspace)
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
            
            // 提取Metaspace信息
            GCEvent.MemoryChange metaspace = extractMetaspaceInfo(line);
            
            return GCEvent.builder()
                    .timestamp((long) (timestamp * 1000))
                    .eventType("Young GC (DefNew)")
                    .gcCause("Young GC (DefNew)")
                    .pauseTime(pauseTime)
                    .heapMemory(heapMemory)
                    .youngGen(youngGen)
                    .metaspace(metaspace)
                    .isFullGC(false)
                    .isLongPause(pauseTime > 100)
                    .build();
        }
        
        return null;
    }
}

