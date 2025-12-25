package com.gcpulse.parser;

import com.gcpulse.model.GCEvent;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * CMS日志解析器
 * 支持ParNew、CMS各个阶段、Full GC等
 */
@Component
public class CmsLogParser extends AbstractGCLogParser {
    
    // CMS/ParNew格式 - 支持多种时间戳格式
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
    
    @Override
    public String getGCType() {
        return "CMS";
    }
    
    @Override
    public boolean canParse(List<String> lines) {
        for (String line : lines) {
            if (line.contains("UseConcMarkSweepGC") || 
                line.contains("UseCMSInitiatingOccupancyOnly") ||
                line.contains("CMS") || 
                line.contains("ParNew")) {
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
                GCEvent event = parseCMSEvent(line);
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
     * 从日志行中提取CMS GC Cause
     */
    private String extractCMSGCCause(String line) {
        if (line.contains("(Allocation Failure)")) {
            return "Allocation Failure";
        } else if (line.contains("(Metadata GC Threshold)")) {
            return "Metadata GC Threshold";
        } else if (line.contains("(System.gc())")) {
            return "System.gc()";
        } else if (line.contains("(GCLocker Initiated GC)")) {
            return "GCLocker Initiated GC";
        } else if (line.contains("promotion failed")) {
            return "Promotion Failed";
        } else if (line.contains("concurrent mode failure")) {
            return "Concurrent Mode Failure";
        }
        return "Unknown";
    }
    
    /**
     * 解析CMS事件（支持多种格式）
     */
    private GCEvent parseCMSEvent(String line) {
        // 提取时间戳：优先使用绝对时间，否则使用相对时间
        long timestamp = 0;
        
        // 尝试提取绝对时间戳
        Matcher absoluteTsMatcher = CMS_ABSOLUTE_TIMESTAMP_PATTERN.matcher(line);
        if (absoluteTsMatcher.find()) {
            String absoluteTimestamp = absoluteTsMatcher.group(1);
            timestamp = parseAbsoluteTimestamp(absoluteTimestamp);
        } else {
            // 使用相对时间戳
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
                    .gcCause("CMS Initial Mark")
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
                    .gcCause("CMS Final Remark")
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
            
            // 计算老年代内存：Old = Heap - Young
            long oldBefore = heapBefore - youngBefore;
            long oldAfter = heapAfter - youngAfter;
            long oldTotal = heapTotal - youngTotal;
            
            GCEvent.MemoryChange oldGen = GCEvent.MemoryChange.builder()
                    .before(oldBefore)
                    .after(oldAfter)
                    .total(oldTotal)
                    .build();
            
            // 检查是否是 promotion failed
            boolean isPromotionFailed = line.contains("promotion failed");
            // 统一使用 "Young GC" 作为事件类型，以便于统计模块识别
            String eventType = isPromotionFailed ? "Young GC (promotion failed)" : "Young GC";
            String gcCause = isPromotionFailed ? "Promotion Failed" : extractCMSGCCause(line);
            
            // 提取Metaspace信息
            GCEvent.MemoryChange metaspace = extractMetaspaceInfo(line);
            
            return GCEvent.builder()
                    .timestamp(timestamp)
                    .eventType(eventType)
                    .gcCause(gcCause)
                    .pauseTime(pauseTime)
                    .heapMemory(heapMemory)
                    .youngGen(youngGen)
                    .oldGen(oldGen)
                    .metaspace(metaspace)
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
            
            GCEvent.MemoryChange oldGen = GCEvent.MemoryChange.builder()
                    .before(oldBefore)
                    .after(oldAfter)
                    .total(oldTotal)
                    .build();
            
            // 检查失败类型
            String eventType = "Full GC";
            String gcCause = "CMS";
            if (line.contains("concurrent mode failure")) {
                eventType = "Full GC";
                gcCause = "Concurrent Mode Failure";
            } else if (line.contains("promotion failed")) {
                eventType = "Full GC";
                gcCause = "Promotion Failed";
            }
            
            // 提取Metaspace信息
            GCEvent.MemoryChange metaspace = extractMetaspaceInfo(line);
            
            return GCEvent.builder()
                    .timestamp(timestamp)
                    .eventType(eventType)
                    .gcCause(gcCause)
                    .pauseTime(pauseTime)
                    .heapMemory(heapMemory)
                    .oldGen(oldGen)
                    .metaspace(metaspace)
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
                        .timestamp(timestamp)
                        .eventType(phaseName)
                        .gcCause(phaseName)
                        .pauseTime(0.0)  // 并发阶段，无暂停
                        .concurrentTime(concurrentTime)
                        .isFullGC(false)
                        .isLongPause(false)
                        .build();
            }
        }
        
        return null;
    }
}

