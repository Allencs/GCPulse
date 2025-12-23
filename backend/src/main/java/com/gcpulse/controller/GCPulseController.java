package com.gcpulse.controller;

import com.gcpulse.model.GCPulseResult;
import com.gcpulse.service.GCPulseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

/**
 * GC分析REST API控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/gc")
@RequiredArgsConstructor
public class GCPulseController {
    
    private final GCPulseService gcPulseService;
    
    /**
     * 上传并分析GC日志
     */
    @PostMapping("/analyze")
    public ResponseEntity<?> analyzeGCLog(@RequestParam("file") MultipartFile file) {
        try {
            log.info("接收到GC日志上传请求: {}", file.getOriginalFilename());
            
            // 验证文件
            if (!gcPulseService.isValidGCLogFile(file)) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("无效的文件格式，请上传GC日志文件（.log或.txt）"));
            }
            
            // 检查文件大小（最大500MB）
            if (file.getSize() > 500 * 1024 * 1024) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("文件大小超过限制（最大500MB）"));
            }
            
            // 分析GC日志
            GCPulseResult result = gcPulseService.analyzeGCLog(file);
            
            return ResponseEntity.ok(createSuccessResponse(result));
            
        } catch (Exception e) {
            log.error("GC日志分析失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("分析失败: " + e.getMessage()));
        }
    }
    
    /**
     * 健康检查
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "GCPulse Platform");
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }
    
    /**
     * 获取支持的GC收集器类型
     */
    @GetMapping("/collectors")
    public ResponseEntity<Map<String, Object>> getSupportedCollectors() {
        Map<String, Object> response = new HashMap<>();
        response.put("collectors", new String[]{
                "G1GC",
                "ZGC",
                "CMS",
                "Parallel GC",
                "Serial GC",
                "Shenandoah"
        });
        return ResponseEntity.ok(response);
    }
    
    private Map<String, Object> createSuccessResponse(Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", data);
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }
    
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", message);
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }
}

