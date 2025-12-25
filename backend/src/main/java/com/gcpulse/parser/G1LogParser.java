package com.gcpulse.parser;

import com.gcpulse.model.GCEvent;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * G1GC日志解析器
 * 支持JDK 8传统格式和JDK 9+ Unified Logging格式
 */
@Component
public class G1LogParser extends AbstractGCLogParser {
    
    // G1GC日志格式枚举
    private enum G1LogFormat {
        JDK8_TRADITIONAL,  // JDK 8 传统格式
        JDK9_UNIFIED       // JDK 9+ 统一日志格式
    }
    
    // JDK 8 G1GC Pattern
    private static final Pattern G1GC_JDK8_PAUSE_PATTERN = Pattern.compile("(?:\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}[+-]\\d{4}:\\s*)?(\\d+\\.\\d+):\\s*\\[GC pause\\s+((?:\\([^)]+\\)\\s*)+)");
    private static final Pattern G1GC_JDK8_EDEN_PATTERN = Pattern.compile("\\[Eden:\\s*([\\d.]+)([BKMGT])\\(([\\d.]+)([BKMGT])\\)->([\\d.]+)([BKMGT])\\(([\\d.]+)([BKMGT])\\)\\s*Survivors?:\\s*([\\d.]+)([BKMGT])->([\\d.]+)([BKMGT])\\s*Heap:\\s*([\\d.]+)([BKMGT])\\(([\\d.]+)([BKMGT])\\)->([\\d.]+)([BKMGT])\\(([\\d.]+)([BKMGT])\\)\\]");
    private static final Pattern G1GC_JDK8_TIME_PATTERN = Pattern.compile(",\\s*([\\d.]+)\\s*secs\\]");
    private static final Pattern G1GC_JDK8_METASPACE_HEAP_PATTERN = Pattern.compile("Metaspace\\s+used\\s+(\\d+)K,\\s+capacity\\s+(\\d+)K,\\s+committed\\s+(\\d+)K,\\s+reserved\\s+(\\d+)K");
    private static final Pattern G1GC_JDK8_ABSOLUTE_TIMESTAMP = Pattern.compile("(\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}[+-]\\d{4}):");
    
    // JDK 9+ G1GC Pattern
    private static final Pattern G1GC_UNIFIED_START_PATTERN = Pattern.compile("\\[(\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}[+-]\\d{4})\\]\\[info\\s*\\]\\[gc,start\\s*\\]\\s*GC\\((\\d+)\\)\\s*Pause\\s+(\\w+)\\s*\\((.*?)\\)\\s*\\((.*?)\\)");
    private static final Pattern G1GC_UNIFIED_END_PATTERN = Pattern.compile("\\[(\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}[+-]\\d{4})\\]\\[info\\s*\\]\\[gc\\s*\\]\\s*GC\\((\\d+)\\)\\s*Pause\\s+(\\w+)\\s*\\((.*?)\\)\\s*\\((.*?)\\)\\s*([\\d.]+)M->([\\d.]+)M\\(([\\d.]+)M\\)\\s*([\\d.]+)ms");
    private static final Pattern G1GC_UNIFIED_HEAP_REGIONS_PATTERN = Pattern.compile("\\[.*?\\]\\[info\\s*\\]\\[gc,heap\\s*\\]\\s*GC\\((\\d+)\\)\\s*Eden regions:\\s*(\\d+)->(\\d+)\\((\\d+)\\)");
    private static final Pattern G1GC_UNIFIED_SURVIVOR_PATTERN = Pattern.compile("\\[.*?\\]\\[info\\s*\\]\\[gc,heap\\s*\\]\\s*GC\\((\\d+)\\)\\s*Survivor regions:\\s*(\\d+)->(\\d+)\\((\\d+)\\)");
    
    @Override
    public String getGCType() {
        return "G1GC";
    }
    
    @Override
    public boolean canParse(List<String> lines) {
        for (String line : lines) {
            if (line.contains("Using G1") || line.contains("[G1") || 
                line.contains("UseG1GC") || line.contains("-XX:+UseG1GC") ||
                line.contains("GC pause (G1 Evacuation Pause)") ||
                line.contains("GC concurrent-mark") || line.contains("GC remark")) {
                return true;
            }
        }
        return false;
    }
    
    @Override
    public List<GCEvent> parseGCEvents(List<String> lines) {
        G1LogFormat format = detectG1LogFormat(lines);
        
        if (format == G1LogFormat.JDK9_UNIFIED) {
            return parseG1UnifiedLogging(lines);
        } else {
            return parseG1JDK8MultiLine(lines);
        }
    }
    
    /**
     * 检测G1日志格式
     */
    private G1LogFormat detectG1LogFormat(List<String> lines) {
        for (String line : lines) {
            if (line.matches("\\[\\d{4}-\\d{2}-\\d{2}T.*?\\]\\[.*?\\]\\[gc.*?\\].*")) {
                System.out.println("检测到JDK 9+ G1 Unified Logging格式");
                return G1LogFormat.JDK9_UNIFIED;
            }
            if (line.matches(".*\\d+\\.\\d+:\\s*\\[GC pause.*") || 
                line.matches(".*\\d{4}-\\d{2}-\\d{2}T.*?:\\s*\\d+\\.\\d+:\\s*\\[GC pause.*")) {
                System.out.println("检测到JDK 8 G1传统格式");
                return G1LogFormat.JDK8_TRADITIONAL;
            }
        }
        System.out.println("未检测到明确的G1格式，默认使用JDK 8传统格式");
        return G1LogFormat.JDK8_TRADITIONAL;
    }
    
    /**
     * 解析JDK 8 G1GC事件（多行模式）
     */
    private List<GCEvent> parseG1JDK8MultiLine(List<String> lines) {
        List<GCEvent> events = new ArrayList<>();
        StringBuilder currentEvent = new StringBuilder();
        boolean inGCEvent = false;
        int gcStartLineCount = 0;
        
        for (String line : lines) {
            try {
                if (line.contains(": [GC pause") || line.contains(": [Full GC")) {
                    gcStartLineCount++;
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
                    if (line.trim().startsWith("[Times:")) {
                        GCEvent event = parseG1JDK8SingleEvent(currentEvent.toString());
                        if (event != null) {
                            events.add(event);
                        }
                        currentEvent = new StringBuilder();
                        inGCEvent = false;
                    } else if (line.trim().equals("}") && currentEvent.toString().contains("Heap after GC")) {
                        // 继续读取
                    } else if (line.trim().equals("}")) {
                        GCEvent event = parseG1JDK8SingleEvent(currentEvent.toString());
                        if (event != null) {
                            events.add(event);
                        }
                        currentEvent = new StringBuilder();
                        inGCEvent = false;
                    } else if (line.contains("Heap before GC")) {
                        String eventStr = currentEvent.toString();
                        int lastNewlineIndex = eventStr.lastIndexOf("\n");
                        if (lastNewlineIndex > 0) {
                            eventStr = eventStr.substring(0, lastNewlineIndex);
                        }
                        GCEvent event = parseG1JDK8SingleEvent(eventStr);
                        if (event != null) {
                            events.add(event);
                        }
                        currentEvent = new StringBuilder();
                        inGCEvent = false;
                    }
                }
            } catch (Exception e) {
                System.err.println("解析G1 JDK8事件失败: " + e.getMessage());
            }
        }
        
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
        
        System.out.println("解析到 " + events.size() + " 个G1 GC事件（JDK 8传统格式），检测到 " + gcStartLineCount + " 个GC开始行");
        return events;
    }
    
    /**
     * 解析单个JDK 8 G1GC事件
     */
    private GCEvent parseG1JDK8SingleEvent(String eventText) {
        if (!eventText.contains("[GC pause") && !eventText.contains("[Full GC")) {
            return null;
        }
        
        long timestamp = 0;
        String gcType = "Young GC";
        String gcCause = "Unknown";
        boolean isFullGC = false;
        
        Matcher absTimeMatcher = G1GC_JDK8_ABSOLUTE_TIMESTAMP.matcher(eventText);
        Matcher pauseMatcher = G1GC_JDK8_PAUSE_PATTERN.matcher(eventText);
        
        double pauseTime = 0.0;
        
        if (pauseMatcher.find()) {
            double relativeTime = Double.parseDouble(pauseMatcher.group(1));
            timestamp = (long) (relativeTime * 1000);
            
            String allParentheses = pauseMatcher.group(2);
            if (allParentheses != null) {
                Pattern firstParenPattern = Pattern.compile("\\(([^)]+)\\)");
                Matcher firstParenMatcher = firstParenPattern.matcher(allParentheses);
                if (firstParenMatcher.find()) {
                    gcCause = firstParenMatcher.group(1).trim();
                }
            }
        } else {
            return null;
        }
        
        if (absTimeMatcher.find()) {
            String isoTimestamp = absTimeMatcher.group(1);
            long absoluteTimestamp = parseAbsoluteTimestamp(isoTimestamp);
            if (absoluteTimestamp > 0) {
                timestamp = absoluteTimestamp;
            }
        }
        
        if (eventText.contains("Full GC")) {
            gcType = "Full GC";
            isFullGC = true;
        } else if (eventText.contains("(mixed)")) {
            gcType = "Mixed GC";
        } else if (eventText.contains("(young)")) {
            gcType = "Young GC";
        }
        
        Matcher timeMatcher = G1GC_JDK8_TIME_PATTERN.matcher(eventText);
        while (timeMatcher.find()) {
            pauseTime = Double.parseDouble(timeMatcher.group(1)) * 1000;
        }
        
        GCEvent.MemoryChange heapMemory = null;
        GCEvent.MemoryChange youngGen = null;
        GCEvent.MemoryChange oldGen = null;
        GCEvent.MemoryChange metaspace = null;
        
        Matcher edenMatcher = G1GC_JDK8_EDEN_PATTERN.matcher(eventText);
        if (edenMatcher.find()) {
            double edenBefore = parseMemoryUnit(edenMatcher.group(1), edenMatcher.group(2));
            double edenAfter = parseMemoryUnit(edenMatcher.group(5), edenMatcher.group(6));
            double edenTotal = parseMemoryUnit(edenMatcher.group(7), edenMatcher.group(8));
            
            double survivorsBefore = parseMemoryUnit(edenMatcher.group(9), edenMatcher.group(10));
            double survivorsAfter = parseMemoryUnit(edenMatcher.group(11), edenMatcher.group(12));
            
            double heapBefore = parseMemoryUnit(edenMatcher.group(13), edenMatcher.group(14));
            double heapAfter = parseMemoryUnit(edenMatcher.group(17), edenMatcher.group(18));
            double heapTotal = parseMemoryUnit(edenMatcher.group(19), edenMatcher.group(20));
            
            heapMemory = GCEvent.MemoryChange.builder()
                .before((long) heapBefore)
                .after((long) heapAfter)
                .total((long) heapTotal)
                .build();
            
            long youngBefore = (long) (edenBefore + survivorsBefore);
            long youngAfter = (long) (edenAfter + survivorsAfter);
            long youngTotal = (long) edenTotal;
            
            youngGen = GCEvent.MemoryChange.builder()
                .before(youngBefore)
                .after(youngAfter)
                .total(youngTotal)
                .build();
            
            long oldBefore = (long) (heapBefore - youngBefore);
            long oldAfter = (long) (heapAfter - youngAfter);
            long oldTotal = (long) (heapTotal - youngTotal);
            
            if (oldBefore >= 0 && oldAfter >= 0 && oldTotal > 0) {
                oldGen = GCEvent.MemoryChange.builder()
                    .before(oldBefore)
                    .after(oldAfter)
                    .total(oldTotal)
                    .build();
            }
        }
        
        if (heapMemory == null) {
            heapMemory = parseMemoryChange(eventText);
        }
        
        metaspace = extractMetaspaceInfo(eventText);
        
        if (metaspace == null) {
            Matcher heapMetaMatcher = G1GC_JDK8_METASPACE_HEAP_PATTERN.matcher(eventText);
            if (heapMetaMatcher.find()) {
                long metaUsed = Long.parseLong(heapMetaMatcher.group(1)) * 1024;
                long metaCommitted = Long.parseLong(heapMetaMatcher.group(3)) * 1024;
                
                metaspace = GCEvent.MemoryChange.builder()
                    .before(metaUsed)
                    .after(metaUsed)
                    .total(metaCommitted)
                    .build();
            }
        }
        
        return GCEvent.builder()
                .timestamp(timestamp)
                .eventType(gcType)
                .gcCause(gcCause)
                .pauseTime(pauseTime)
                .concurrentTime(0.0)
                .heapMemory(heapMemory)
                .youngGen(youngGen)
                .oldGen(oldGen)
                .metaspace(metaspace)
                .isFullGC(isFullGC)
                .isLongPause(pauseTime > 100)
                .build();
    }
    
    /**
     * 解析JDK 9+ G1 Unified Logging格式
     */
    private List<GCEvent> parseG1UnifiedLogging(List<String> lines) {
        List<GCEvent> events = new ArrayList<>();
        Map<String, GCEventData> gcEventMap = new HashMap<>();
        
        for (String line : lines) {
            try {
                Matcher endMatcher = G1GC_UNIFIED_END_PATTERN.matcher(line);
                if (endMatcher.find()) {
                    String timestampStr = endMatcher.group(1);
                    String gcId = endMatcher.group(2);
                    String pauseType = endMatcher.group(3);
                    String gcCause = endMatcher.group(5);
                    double heapBefore = Double.parseDouble(endMatcher.group(6));
                    double heapAfter = Double.parseDouble(endMatcher.group(7));
                    double heapTotal = Double.parseDouble(endMatcher.group(8));
                    double pauseTime = Double.parseDouble(endMatcher.group(9));
                    
                    long timestamp = parseAbsoluteTimestamp(timestampStr);
                    
                    String gcType = "Young GC";
                    boolean isFullGC = false;
                    if (pauseType.equalsIgnoreCase("Full")) {
                        gcType = "Full GC";
                        isFullGC = true;
                    } else if (pauseType.equalsIgnoreCase("Mixed")) {
                        gcType = "Mixed GC";
                    }
                    
                    GCEvent.MemoryChange heapMemory = GCEvent.MemoryChange.builder()
                        .before((long) (heapBefore * 1024 * 1024))
                        .after((long) (heapAfter * 1024 * 1024))
                        .total((long) (heapTotal * 1024 * 1024))
                        .build();
                    
                    GCEvent event = GCEvent.builder()
                            .timestamp(timestamp)
                            .eventType(gcType)
                            .gcCause(gcCause)
                            .pauseTime(pauseTime)
                            .concurrentTime(0.0)
                            .heapMemory(heapMemory)
                            .isFullGC(isFullGC)
                            .isLongPause(pauseTime > 100)
                            .build();
                    
                    events.add(event);
                    
                    GCEventData data = new GCEventData();
                    data.event = event;
                    gcEventMap.put(gcId, data);
                }
            } catch (Exception e) {
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
}

