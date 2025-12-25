package com.gcpulse.parser;

import com.gcpulse.model.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * GC日志解析器抽象基类
 * 提供通用的解析方法和模板方法模式
 */
public abstract class AbstractGCLogParser {
    
    // 通用正则表达式
    protected static final Pattern TIMESTAMP_PATTERN = Pattern.compile("^(\\d+\\.\\d+):");
    protected static final Pattern HEAP_PATTERN = Pattern.compile("Heap.*?(\\d+)K->(\\d+)K\\((\\d+)K\\)");
    protected static final Pattern MEMORY_PATTERN = Pattern.compile("(\\d+)([KMGT])");
    
    // Metaspace 相关模式
    protected static final Pattern METASPACE_PATTERN = Pattern.compile("\\[Metaspace:\\s*(\\d+)K->(\\d+)K\\((\\d+)K\\)\\]");
    protected static final Pattern METASPACE_UNIFIED_PATTERN = Pattern.compile("\\[gc,metaspace\\s*\\]\\s*GC\\((\\d+)\\)\\s*Metaspace:\\s*([\\d.]+)M\\s*used,\\s*([\\d.]+)M\\s*committed");
    
    /**
     * 获取支持的GC类型
     */
    public abstract String getGCType();
    
    /**
     * 检测是否是当前解析器支持的日志格式
     */
    public abstract boolean canParse(List<String> lines);
    
    /**
     * 解析GC事件（由子类实现具体逻辑）
     */
    public abstract List<GCEvent> parseGCEvents(List<String> lines);
    
    /**
     * 解析内存单位并转换为字节
     */
    protected double parseMemoryUnit(String value, String unit) {
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
     * 从日志行中提取Metaspace信息
     */
    protected GCEvent.MemoryChange extractMetaspaceInfo(String line) {
        Matcher metaspaceMatcher = METASPACE_PATTERN.matcher(line);
        if (metaspaceMatcher.find()) {
            long metaBefore = Long.parseLong(metaspaceMatcher.group(1)) * 1024;
            long metaAfter = Long.parseLong(metaspaceMatcher.group(2)) * 1024;
            long metaTotal = Long.parseLong(metaspaceMatcher.group(3)) * 1024;
            
            return GCEvent.MemoryChange.builder()
                .before(metaBefore)
                .after(metaAfter)
                .total(metaTotal)
                .build();
        }
        return null;
    }
    
    /**
     * 解析内存变化
     */
    protected GCEvent.MemoryChange parseMemoryChange(String line) {
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
     * 解析绝对时间戳（ISO 8601格式）
     * 例如：2025-08-05T13:23:18.409+0800
     */
    protected long parseAbsoluteTimestamp(String timestampStr) {
        try {
            java.time.format.DateTimeFormatter formatter = 
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXX");
            java.time.ZonedDateTime zonedDateTime = java.time.ZonedDateTime.parse(timestampStr, formatter);
            return zonedDateTime.toInstant().toEpochMilli();
        } catch (Exception e) {
            return 0;
        }
    }
    
    /**
     * 提取GC原因
     */
    protected String extractGCCause(String eventType) {
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
}

