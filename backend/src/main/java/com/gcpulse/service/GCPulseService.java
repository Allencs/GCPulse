package com.gcpulse.service;

import com.gcpulse.model.GCPulseResult;
import com.gcpulse.parser.GCLogParser;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;

/**
 * GC分析服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GCPulseService {
    
    private final GCLogParser gcLogParser;
    
    /**
     * 分析上传的GC日志文件
     */
    public GCPulseResult analyzeGCLog(MultipartFile file) throws IOException {
        log.info("开始分析GC日志文件: {}, 大小: {} bytes", 
                file.getOriginalFilename(), file.getSize());
        
        long startTime = System.currentTimeMillis();
        
        try (InputStream inputStream = file.getInputStream()) {
            GCPulseResult result = gcLogParser.parse(
                    inputStream, 
                    file.getOriginalFilename(),
                    file.getSize()
            );
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("GC日志分析完成，耗时: {}ms, 检测到的收集器: {}, GC事件数: {}", 
                    duration, result.getCollectorType(), 
                    result.getGcEvents() != null ? result.getGcEvents().size() : 0);
            
            return result;
        } catch (Exception e) {
            log.error("分析GC日志文件失败: {}", file.getOriginalFilename(), e);
            throw new RuntimeException("分析GC日志失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 验证文件格式
     */
    public boolean isValidGCLogFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return false;
        }
        
        String filename = file.getOriginalFilename();
        if (filename == null) {
            return false;
        }
        
        // 检查文件扩展名
        String lowerFilename = filename.toLowerCase();
        return lowerFilename.endsWith(".log") || 
               lowerFilename.endsWith(".txt") || 
               lowerFilename.contains("gc");
    }
}

