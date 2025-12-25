package com.gcpulse.controller;

import com.gcpulse.model.AIDiagnosisRequest;
import com.gcpulse.model.AIDiagnosisResponse;
import com.gcpulse.service.AIDiagnosisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

/**
 * AI诊断Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = "*")
public class AIDiagnosisController {
    
    @Autowired
    private AIDiagnosisService aiDiagnosisService;
    
    /**
     * 执行AI诊断
     */
    @PostMapping("/diagnose")
    public AIDiagnosisResponse diagnose(
            @RequestParam(value = "apiUrl", required = false) String apiUrl,
            @RequestParam(value = "apiKey", required = false) String apiKey,
            @RequestParam(value = "model", required = false) String model,
            @RequestParam(value = "collectorType", required = false, defaultValue = "Unknown") String collectorType,
            @RequestParam(value = "eventCount", required = false, defaultValue = "0") Integer eventCount,
            @RequestParam("file") MultipartFile file) {
        
        try {
            // 读取GC日志文件内容
            String gcLogContent = new String(file.getBytes(), StandardCharsets.UTF_8);
            
            // 处理空字符串，转换为 null 以使用后端配置
            String finalApiUrl = (apiUrl != null && !apiUrl.trim().isEmpty()) ? apiUrl : null;
            String finalApiKey = (apiKey != null && !apiKey.trim().isEmpty()) ? apiKey : null;
            String finalModel = (model != null && !model.trim().isEmpty()) ? model : null;
            
            log.info("AI诊断请求参数:");
            log.info("  - apiUrl: {}", finalApiUrl != null ? finalApiUrl : "[使用后端配置]");
            log.info("  - apiKey: {}", finalApiKey != null ? "***" : "[使用后端配置]");
            log.info("  - model: {}", finalModel != null ? finalModel : "[使用后端配置]");
            log.info("  - collectorType: {}", collectorType);
            log.info("  - eventCount: {}", eventCount);
            
            // 构建请求
            AIDiagnosisRequest request = AIDiagnosisRequest.builder()
                    .apiUrl(finalApiUrl)
                    .apiKey(finalApiKey)
                    .model(finalModel)
                    .gcLogContent(gcLogContent)
                    .collectorType(collectorType)
                    .eventCount(eventCount)
                    .build();
            
            // 执行诊断
            return aiDiagnosisService.diagnose(request);
            
        } catch (Exception e) {
            e.printStackTrace();
            return AIDiagnosisResponse.builder()
                    .success(false)
                    .error("读取日志文件失败: " + e.getMessage())
                    .processTime(0)
                    .build();
        }
    }
}

