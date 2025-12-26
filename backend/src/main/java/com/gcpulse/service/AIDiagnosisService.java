package com.gcpulse.service;

import com.gcpulse.config.AIDiagnosisConfig;
import com.gcpulse.model.AIDiagnosisRequest;
import com.gcpulse.model.AIDiagnosisResponse;
import com.gcpulse.model.OptimizationContext;
import com.theokanning.openai.client.OpenAiApi;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import com.theokanning.openai.service.OpenAiService;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.jackson.JacksonConverterFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

/**
 * AI诊断服务
 */
@Slf4j
@Service
public class AIDiagnosisService {
    
    @Autowired
    private AIDiagnosisConfig aiConfig;
    
    /**
     * 执行AI诊断
     */
    public AIDiagnosisResponse diagnose(AIDiagnosisRequest request) {
        long startTime = System.currentTimeMillis();
        
        try {
            // 使用配置中的API Key（如果请求中没有提供）
            String apiKey = request.getApiKey();
            if (apiKey == null || apiKey.trim().isEmpty()) {
                apiKey = aiConfig.getApiKey();
            }
            
            // 验证API Key
            if (apiKey == null || apiKey.trim().isEmpty()) {
                return AIDiagnosisResponse.builder()
                        .success(false)
                        .error("API Key未配置，请在请求中提供或在配置文件中设置 ai.diagnosis.api-key")
                        .processTime(System.currentTimeMillis() - startTime)
                        .build();
            }
            
            // Lambda 中使用，必须是 final
            final String finalApiKey = apiKey;
            
            // 验证输入数据：根据模式验证不同的数据
            if (request.isUseStructuredData()) {
                // 使用结构化数据模式：需要OptimizationContext
                if (request.getOptimizationContext() == null) {
                    return AIDiagnosisResponse.builder()
                            .success(false)
                            .error("结构化数据不能为空")
                            .processTime(System.currentTimeMillis() - startTime)
                            .build();
                }
            } else {
                // 使用原始日志模式：需要gcLogContent
                if (request.getGcLogContent() == null || request.getGcLogContent().trim().isEmpty()) {
                    return AIDiagnosisResponse.builder()
                            .success(false)
                            .error("GC日志内容不能为空")
                            .processTime(System.currentTimeMillis() - startTime)
                            .build();
                }
            }
            
            // 使用配置中的API地址（如果请求中没有提供）
            String apiUrl = request.getApiUrl();
            if (apiUrl == null || apiUrl.trim().isEmpty()) {
                apiUrl = aiConfig.getApiUrl();
            }
            
            // 使用配置中的默认模型（如果请求中没有提供）
            String model = request.getModel();
            if (model == null || model.trim().isEmpty()) {
                model = aiConfig.getDefaultModel();
            }
            
            // 创建OpenAI服务
            Duration timeout = Duration.ofSeconds(aiConfig.getTimeoutSeconds());
            
            // 支持自定义API地址（如OpenRouter）
            if (apiUrl != null && !apiUrl.trim().isEmpty() && 
                !apiUrl.equals("https://api.openai.com/v1")) {
                
                log.info("使用自定义API地址(OpenRouter): {}", apiUrl);
                
                // 对于 OpenRouter，直接使用 OkHttp 发送请求
                return diagnoseWithCustomApi(apiUrl, finalApiKey, model, request, startTime, timeout);
                
            } else {
                // 使用官方OpenAI API
                log.info("使用OpenAI官方API");
                OpenAiService service = new OpenAiService(finalApiKey, timeout);
            
                // 构建对话消息
                List<ChatMessage> messages = new ArrayList<>();
                
                // 系统消息：定义AI的角色和任务
                messages.add(new ChatMessage(ChatMessageRole.SYSTEM.value(), 
                        buildSystemPrompt(request.isUseStructuredData())));
                
                // 用户消息：提供GC日志和上下文信息
                messages.add(new ChatMessage(ChatMessageRole.USER.value(), buildUserPrompt(request)));
                
                log.info("使用AI模型: {}", model);
                
                ChatCompletionRequest chatRequest = ChatCompletionRequest.builder()
                        .model(model)
                        .messages(messages)
                        .temperature(0.7)
                        .maxTokens(2000)
                        .build();
                
                // 调用API
                String diagnosis = service.createChatCompletion(chatRequest)
                        .getChoices()
                        .get(0)
                        .getMessage()
                        .getContent();
                
                // 清理资源
                service.shutdownExecutor();
                
                return AIDiagnosisResponse.builder()
                        .success(true)
                        .diagnosis(diagnosis)
                        .processTime(System.currentTimeMillis() - startTime)
                        .build();
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            return AIDiagnosisResponse.builder()
                    .success(false)
                    .error("AI诊断失败: " + e.getMessage())
                    .processTime(System.currentTimeMillis() - startTime)
                    .build();
        }
    }
    
    /**
     * 构建系统提示词
     */
    private String buildSystemPrompt(boolean useStructuredData) {
        if (useStructuredData) {
            return """
                    你是一位资深的Java性能调优专家和JVM内存管理专家，拥有15年以上的大规模生产环境GC调优经验。
                    你精通各种GC收集器（Serial、Parallel、CMS、G1、ZGC、Shenandoah）的内部实现原理和最佳实践。
                    
                    你将收到已经解析和分析过的结构化GC数据，包括：
                    - JVM配置参数
                    - 性能指标（吞吐量、延迟、P95/P99）
                    - 内存使用分析
                    - GC行为模式
                    - 检测到的问题
                    - 趋势分析
                    
                    你的任务是基于这些结构化数据提供深度的、可操作的优化建议。
                    
                    请按照以下格式返回Markdown格式的专业分析报告：
                    
                    ## 🎯 执行摘要
                    用2-3句话总结最关键的发现和建议优先级。
                    
                    ## 📊 性能评估
                    
                    ### 当前性能等级
                    基于吞吐量、延迟等指标给出综合评分和等级。
                    
                    ### 关键指标分析
                    - **吞吐量**: 当前值、行业基准对比、改进空间
                    - **延迟**: P50/P95/P99分析、SLA达成情况
                    - **GC频率**: 是否正常、是否需要优化
                    
                    ## 🔍 深度诊断
                    
                    ### 1. 内存配置分析
                    - 堆大小是否合理（基于使用率和业务需求）
                    - 新生代/老年代比例是否优化
                    - Metaspace配置评估
                    
                    ### 2. GC行为模式
                    - GC类型分布是否健康
                    - Full GC触发原因分析
                    - 对象晋升模式评估
                    - 是否存在异常模式（如连续Full GC、晋升失败）
                    
                    ### 3. 潜在风险识别
                    - 内存泄漏风险评估及证据
                    - 性能退化趋势预测
                    - 容量规划建议
                    
                    ## 💡 优化建议路线图
                    
                    ### 🔴 紧急优化（P0 - 立即执行）
                    列出需要立即处理的Critical问题，提供：
                    - 问题描述
                    - 具体的JVM参数调整建议（给出完整参数）
                    - 预期效果
                    - 实施风险评估
                    
                    ### 🟠 重要优化（P1 - 本周完成）
                    列出需要尽快处理的重要问题，提供具体操作步骤。
                    
                    ### 🟡 性能提升（P2 - 计划内优化）
                    列出可以进一步提升性能的优化点。
                    
                    ### 🟢 长期优化（P3 - 架构层面）
                    如果需要，提供架构层面的改进建议（如更换GC收集器）。
                    
                    ## ⚙️ 推荐JVM配置
                    
                    根据分析结果，提供完整的推荐JVM启动参数配置：
                    ```bash
                    # 推荐配置
                    -Xms<size> -Xmx<size>
                    -XX:+Use<GC>
                    ...（其他关键参数）
                    ```
                    
                    说明每个参数的作用和设置理由。
                    
                    ## 📈 监控建议
                    
                    - 需要持续监控的关键指标
                    - 告警阈值建议
                    - 建议的监控工具
                    
                    ## ⚠️ 注意事项
                    
                    - 参数调整的注意事项
                    - 需要进行的测试验证
                    - 回滚方案
                    
                    请确保建议具体、可操作，并考虑生产环境的稳定性。
                    """;
        } else {
            // 原有的提示词（向后兼容）
            return """
                    你是一位专业的Java GC（垃圾回收）性能调优专家。你的任务是分析GC日志并提供专业的诊断报告。
                    
                    请按照以下格式返回Markdown格式的分析报告：
                    
                    ## 📊 GC日志分析摘要
                    简要概述GC日志的关键发现（2-3句话）
                    
                    ## 🔍 详细分析
                    
                    ### 1. GC行为模式
                    - 分析GC的频率、类型分布
                    - 识别异常的GC模式
                    
                    ### 2. 性能指标
                    - 暂停时间分析
                    - 吞吐量评估
                    - 内存使用趋势
                    
                    ### 3. 潜在问题
                    列出发现的性能问题或风险点
                    
                    ## 💡 优化建议
                    
                    ### 高优先级
                    1. 最重要的优化建议
                    2. 第二重要的建议
                    
                    ### 中等优先级
                    1. 次要的改进建议
                    
                    ### 配置建议
                    提供具体的JVM参数调整建议（如果适用）
                    
                    ## ⚠️ 注意事项
                    列出需要特别关注的点或警告
                    
                    请确保分析专业、准确，并提供可操作的建议。
                    """;
        }
    }
    
    /**
     * 构建用户提示词
     */
    private String buildUserPrompt(AIDiagnosisRequest request) {
        // 如果使用结构化数据，构建更精准的提示
        if (request.isUseStructuredData() && request.getOptimizationContext() != null) {
            return buildStructuredPrompt(request.getOptimizationContext());
        }
        
        // 否则使用原有的日志内容方式（向后兼容）
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("请分析以下GC日志：\n\n");
        prompt.append("**GC收集器类型**: ").append(request.getCollectorType()).append("\n");
        prompt.append("**GC事件数量**: ").append(request.getEventCount()).append("\n\n");
        prompt.append("**GC日志内容**:\n```\n");
        
        // 限制日志长度，避免超过token限制
        String logContent = request.getGcLogContent();
        int maxLength = aiConfig.getMaxLogLength();
        
        if (logContent.length() > maxLength) {
            // 取前半部分和后半部分
            int halfLength = maxLength / 2;
            prompt.append(logContent, 0, halfLength);
            prompt.append("\n\n...[中间部分省略]...\n\n");
            prompt.append(logContent.substring(logContent.length() - halfLength));
        } else {
            prompt.append(logContent);
        }
        
        prompt.append("\n```\n\n");
        prompt.append("请基于以上日志提供专业的GC性能分析和优化建议。");
        
        return prompt.toString();
    }
    
    /**
     * 构建基于结构化数据的提示词
     */
    private String buildStructuredPrompt(OptimizationContext context) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("# GC性能分析数据\n\n");
        prompt.append("以下是已经解析和分析过的GC数据，请基于这些结构化信息提供深度优化建议。\n\n");
        
        // 1. 基础信息
        prompt.append("## 基础配置\n\n");
        prompt.append("**GC收集器**: ").append(context.getCollectorType()).append("\n\n");
        
        // 2. JVM配置
        if (context.getJvmConfiguration() != null) {
            OptimizationContext.JVMConfiguration jvmConfig = context.getJvmConfiguration();
            prompt.append("### JVM配置\n\n");
            prompt.append("- **堆内存配置**: ").append(jvmConfig.getHeapSize()).append("\n");
            prompt.append("- **新生代配置**: ").append(jvmConfig.getYoungGenSize()).append("\n");
            
            if (!jvmConfig.getGcArguments().isEmpty()) {
                prompt.append("- **GC参数**: ").append(String.join(", ", jvmConfig.getGcArguments())).append("\n");
            }
            if (!jvmConfig.getMemoryArguments().isEmpty()) {
                prompt.append("- **内存参数**: ").append(String.join(", ", jvmConfig.getMemoryArguments())).append("\n");
            }
            if (!jvmConfig.getConfigurationIssues().isEmpty()) {
                prompt.append("\n**⚠️ 配置问题**:\n");
                jvmConfig.getConfigurationIssues().forEach(issue -> 
                    prompt.append("- ").append(issue).append("\n"));
            }
            prompt.append("\n");
        }
        
        // 3. 性能指标
        if (context.getPerformanceMetrics() != null) {
            OptimizationContext.PerformanceMetrics perf = context.getPerformanceMetrics();
            prompt.append("## 性能指标\n\n");
            prompt.append(String.format("- **吞吐量**: %.2f%%\n", perf.getThroughput()));
            prompt.append(String.format("- **平均GC暂停**: %.2f ms\n", perf.getAvgPauseTime()));
            prompt.append(String.format("- **最大GC暂停**: %.2f ms\n", perf.getMaxPauseTime()));
            prompt.append(String.format("- **P95暂停时间**: %.2f ms\n", perf.getP95PauseTime()));
            prompt.append(String.format("- **P99暂停时间**: %.2f ms\n", perf.getP99PauseTime()));
            prompt.append(String.format("- **GC频率**: %.2f 次/分钟\n", perf.getGcFrequency()));
            prompt.append(String.format("- **总GC次数**: %d\n", perf.getTotalGCCount()));
            prompt.append(String.format("- **性能等级**: %s\n\n", perf.getPerformanceRating()));
        }
        
        // 4. 内存分析
        if (context.getMemoryAnalysis() != null) {
            OptimizationContext.MemoryAnalysis mem = context.getMemoryAnalysis();
            prompt.append("## 内存使用分析\n\n");
            prompt.append(String.format("- **堆最大值**: %s\n", mem.getHeapMaxSize()));
            prompt.append(String.format("- **平均堆使用率**: %.2f%%\n", mem.getAvgHeapUsage()));
            prompt.append(String.format("- **最大堆使用率**: %.2f%%\n", mem.getMaxHeapUsage()));
            prompt.append(String.format("- **GC后平均堆使用**: %s\n", mem.getAvgHeapAfterGC()));
            prompt.append(String.format("- **平均回收效率**: %.2f%%\n", mem.getAvgReclamationRate()));
            
            if (mem.isMemoryLeakRisk()) {
                prompt.append("\n**⚠️ 内存泄漏风险**:\n");
                if (mem.getMemoryLeakEvidences() != null && !mem.getMemoryLeakEvidences().isEmpty()) {
                    mem.getMemoryLeakEvidences().forEach(evidence -> 
                        prompt.append("- ").append(evidence).append("\n"));
                }
            }
            prompt.append("\n");
        }
        
        // 5. GC行为模式
        if (context.getGcBehaviorPattern() != null) {
            OptimizationContext.GCBehaviorPattern gcBehavior = context.getGcBehaviorPattern();
            prompt.append("## GC行为模式\n\n");
            prompt.append(String.format("- **Full GC次数**: %d\n", gcBehavior.getFullGCCount()));
            
            if (gcBehavior.isHasConsecutiveFullGC()) {
                prompt.append(String.format("- **⚠️ 连续Full GC**: 是（最多连续%d次）\n", gcBehavior.getMaxConsecutiveFullGC()));
            }
            
            if (gcBehavior.isHasPromotionFailure()) {
                prompt.append("- **⚠️ 晋升失败**: 检测到\n");
            }
            
            if (gcBehavior.getGcCauseDistribution() != null && !gcBehavior.getGcCauseDistribution().isEmpty()) {
                prompt.append("\n**GC原因分布**:\n");
                gcBehavior.getGcCauseDistribution().forEach((cause, count) ->
                    prompt.append(String.format("- %s: %d次\n", cause, count)));
            }
            
            if (gcBehavior.getGcTypeDistribution() != null && !gcBehavior.getGcTypeDistribution().isEmpty()) {
                prompt.append("\n**GC类型分布**:\n");
                gcBehavior.getGcTypeDistribution().forEach((type, count) ->
                    prompt.append(String.format("- %s: %d次\n", type, count)));
            }
            
            if (gcBehavior.getPromotionPattern() != null) {
                OptimizationContext.PromotionPattern promotion = gcBehavior.getPromotionPattern();
                prompt.append("\n**对象晋升模式**:\n");
                if (promotion.getAvgTenuringAge() != null) {
                    prompt.append(String.format("- 平均晋升年龄: %d\n", promotion.getAvgTenuringAge()));
                }
            }
            prompt.append("\n");
        }
        
        // 6. 检测到的问题
        if (context.getDetectedIssues() != null) {
            OptimizationContext.DetectedIssues issues = context.getDetectedIssues();
            prompt.append("## 检测到的问题\n\n");
            prompt.append(String.format("**严重程度**: %s\n\n", issues.getSeverity()));
            
            if (issues.getIssueSummary() != null && !issues.getIssueSummary().isEmpty()) {
                prompt.append("**问题清单**:\n");
                issues.getIssueSummary().forEach(issue ->
                    prompt.append("- ").append(issue).append("\n"));
                prompt.append("\n");
            }
            
            List<String> detailedIssues = new java.util.ArrayList<>();
            if (issues.isExcessiveFullGC()) detailedIssues.add("Full GC次数过多");
            if (issues.isLongPauses()) detailedIssues.add(String.format("存在长暂停（最长%.2fms）", issues.getMaxPauseDuration()));
            if (issues.isHighGCFrequency()) detailedIssues.add("GC频率过高");
            if (issues.isMemoryFragmentation()) detailedIssues.add("内存碎片化");
            if (issues.isRapidPromotion()) detailedIssues.add("对象晋升过快");
            if (issues.isAbnormalMetaspaceGrowth()) detailedIssues.add("Metaspace异常增长");
            
            if (!detailedIssues.isEmpty()) {
                prompt.append("**详细问题**:\n");
                detailedIssues.forEach(issue -> prompt.append("- ").append(issue).append("\n"));
                prompt.append("\n");
            }
        }
        
        // 7. 趋势分析
        if (context.getTrendAnalysis() != null) {
            OptimizationContext.TrendAnalysis trend = context.getTrendAnalysis();
            prompt.append("## 趋势分析\n\n");
            prompt.append(String.format("- **堆使用趋势**: %s\n", trend.getHeapUsageTrend()));
            prompt.append(String.format("- **暂停时间趋势**: %s\n", trend.getPauseTimeTrend()));
            prompt.append(String.format("- **GC频率趋势**: %s\n", trend.getGcFrequencyTrend()));
            prompt.append(String.format("- **系统稳定性**: %s\n", trend.isStable() ? "稳定" : "不稳定"));
            
            if (trend.getPredictedIssues() != null && !trend.getPredictedIssues().isEmpty()) {
                prompt.append("\n**预测问题**:\n");
                trend.getPredictedIssues().forEach(issue ->
                    prompt.append("- ").append(issue).append("\n"));
            }
            prompt.append("\n");
        }
        
        prompt.append("\n---\n\n");
        prompt.append("请基于以上结构化数据，提供深度的、可操作的GC优化建议。");
        prompt.append("特别关注具体的JVM参数调整建议、风险评估和监控建议。\n");
        
        return prompt.toString();
    }
    
    /**
     * 使用自定义API (OpenRouter等) 发送请求
     */
    private AIDiagnosisResponse diagnoseWithCustomApi(String apiUrl, String apiKey, String model, 
                                                       AIDiagnosisRequest request, long startTime, Duration timeout) {
        try {
            log.info("使用自定义API模型: {}", model);
            log.info("请求端点: {}", apiUrl);
            
            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            
            // 构建消息列表
            List<Map<String, String>> messages = new ArrayList<>();
            Map<String, String> systemMsg = new HashMap<>();
            systemMsg.put("role", "system");
            systemMsg.put("content", buildSystemPrompt(request.isUseStructuredData()));
            messages.add(systemMsg);
            
            Map<String, String> userMsg = new HashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", buildUserPrompt(request));
            messages.add(userMsg);
            
            requestBody.put("messages", messages);
            requestBody.put("temperature", 0.7);
            requestBody.put("max_tokens", 2000);
            
            // 转换为JSON
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonBody = objectMapper.writeValueAsString(requestBody);
            
            log.debug("请求体大小: {} bytes", jsonBody.length());
            
            // 创建 OkHttpClient
            OkHttpClient client = new OkHttpClient.Builder()
                    .readTimeout(timeout)
                    .writeTimeout(timeout)
                    .connectTimeout(timeout)
                    .build();
            
            // 创建请求
            Request httpRequest = new Request.Builder()
                    .url(apiUrl)
                    .post(RequestBody.create(jsonBody, MediaType.parse("application/json")))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("HTTP-Referer", "https://gcpulse.app")
                    .header("X-Title", "GCPulse AI Diagnosis")
                    .header("Content-Type", "application/json")
                    .build();
            
            log.debug("发送请求到: {}", httpRequest.url());
            
            // 发送请求
            try (Response response = client.newCall(httpRequest).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "";
                
                log.info("响应状态码: {}", response.code());
                log.debug("响应体长度: {} bytes", responseBody.length());
                
                if (!response.isSuccessful()) {
                    log.error("API请求失败: {}", responseBody);
                    return AIDiagnosisResponse.builder()
                            .success(false)
                            .error("API请求失败 (状态码 " + response.code() + "): " + responseBody)
                            .processTime(System.currentTimeMillis() - startTime)
                            .build();
                }
                
                // 解析响应
                JsonNode jsonResponse = objectMapper.readTree(responseBody);
                String diagnosis = jsonResponse
                        .path("choices")
                        .get(0)
                        .path("message")
                        .path("content")
                        .asText();
                
                log.info("AI诊断成功，响应长度: {} chars", diagnosis.length());
                
                return AIDiagnosisResponse.builder()
                        .success(true)
                        .diagnosis(diagnosis)
                        .processTime(System.currentTimeMillis() - startTime)
                        .build();
            }
            
        } catch (Exception e) {
            log.error("自定义API诊断失败: {}", e.getMessage(), e);
            return AIDiagnosisResponse.builder()
                    .success(false)
                    .error("AI诊断失败: " + e.getMessage())
                    .processTime(System.currentTimeMillis() - startTime)
                    .build();
        }
    }
}

