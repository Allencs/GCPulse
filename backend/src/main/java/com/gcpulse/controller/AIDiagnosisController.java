package com.gcpulse.controller;

import com.gcpulse.model.AIDiagnosisRequest;
import com.gcpulse.model.AIDiagnosisResponse;
import com.gcpulse.model.GCPulseResult;
import com.gcpulse.model.OptimizationContext;
import com.gcpulse.service.AIDiagnosisService;
import com.gcpulse.service.DiagnosisExportService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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
    
    @Autowired
    private DiagnosisExportService exportService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    /**
     * 执行AI诊断（基于原始日志）
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
                    .useStructuredData(false) // 使用原始日志
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
    
    /**
     * 智能优化建议（基于结构化分析结果） - 新增端点
     */
    @PostMapping("/optimize")
    public AIDiagnosisResponse getOptimizationSuggestions(
            @RequestParam(value = "apiUrl", required = false) String apiUrl,
            @RequestParam(value = "apiKey", required = false) String apiKey,
            @RequestParam(value = "model", required = false) String model,
            @RequestBody String analysisResultJson) {
        
        try {
            log.info("收到AI优化建议请求（结构化数据模式）");
            
            // 解析GCPulseResult
            GCPulseResult analysisResult = objectMapper.readValue(analysisResultJson, GCPulseResult.class);
            
            // 构建优化上下文
            OptimizationContext context = OptimizationContext.fromGCPulseResult(analysisResult);
            
            // 处理API配置
            String finalApiUrl = (apiUrl != null && !apiUrl.trim().isEmpty()) ? apiUrl : null;
            String finalApiKey = (apiKey != null && !apiKey.trim().isEmpty()) ? apiKey : null;
            String finalModel = (model != null && !model.trim().isEmpty()) ? model : null;
            
            log.info("AI优化建议请求参数:");
            log.info("  - apiUrl: {}", finalApiUrl != null ? finalApiUrl : "[使用后端配置]");
            log.info("  - apiKey: {}", finalApiKey != null ? "***" : "[使用后端配置]");
            log.info("  - model: {}", finalModel != null ? finalModel : "[使用后端配置]");
            log.info("  - collectorType: {}", context.getCollectorType());
            log.info("  - 使用结构化数据模式");
            
            // 构建请求
            AIDiagnosisRequest request = AIDiagnosisRequest.builder()
                    .apiUrl(finalApiUrl)
                    .apiKey(finalApiKey)
                    .model(finalModel)
                    .collectorType(context.getCollectorType())
                    .useStructuredData(true) // 使用结构化数据
                    .optimizationContext(context)
                    .build();
            
            // 执行诊断
            return aiDiagnosisService.diagnose(request);
            
        } catch (Exception e) {
            log.error("AI优化建议失败: {}", e.getMessage(), e);
            return AIDiagnosisResponse.builder()
                    .success(false)
                    .error("优化建议生成失败: " + e.getMessage())
                    .processTime(0)
                    .build();
        }
    }
    
    /**
     * 导出诊断报告为HTML格式
     */
    @PostMapping("/export/html")
    public ResponseEntity<byte[]> exportToHtml(
            @RequestParam(value = "renderedHtml", required = false) String renderedHtml,
            @RequestParam("diagnosis") String diagnosis,
            @RequestParam(value = "collectorType", required = false) String collectorType,
            @RequestParam(value = "eventCount", required = false, defaultValue = "0") Integer eventCount) {
        
        try {
            log.info("导出HTML格式诊断报告，使用渲染HTML: {}", renderedHtml != null ? "是" : "否");
            byte[] htmlBytes = exportService.exportToHtml(renderedHtml, diagnosis, collectorType, eventCount);
            
            String fileName = "GCPulse_AI_Diagnosis_" + 
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".html";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_HTML);
            headers.setContentDispositionFormData("attachment", fileName);
            headers.setContentLength(htmlBytes.length);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(htmlBytes);
                    
        } catch (Exception e) {
            log.error("导出HTML失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 导出诊断报告为Markdown格式
     */
    @PostMapping("/export/markdown")
    public ResponseEntity<byte[]> exportToMarkdown(
            @RequestParam("diagnosis") String diagnosis,
            @RequestParam(value = "collectorType", required = false) String collectorType,
            @RequestParam(value = "eventCount", required = false, defaultValue = "0") Integer eventCount) {
        
        try {
            log.info("导出Markdown格式诊断报告");
            byte[] markdownBytes = exportService.exportToMarkdown(diagnosis, collectorType, eventCount);
            
            String fileName = "GCPulse_AI_Diagnosis_" + 
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".md";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_PLAIN);
            headers.setContentDispositionFormData("attachment", fileName);
            headers.setContentLength(markdownBytes.length);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(markdownBytes);
                    
        } catch (Exception e) {
            log.error("导出Markdown失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}

