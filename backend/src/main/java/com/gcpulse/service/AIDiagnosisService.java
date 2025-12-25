package com.gcpulse.service;

import com.gcpulse.config.AIDiagnosisConfig;
import com.gcpulse.model.AIDiagnosisRequest;
import com.gcpulse.model.AIDiagnosisResponse;
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
            
            if (request.getGcLogContent() == null || request.getGcLogContent().trim().isEmpty()) {
                return AIDiagnosisResponse.builder()
                        .success(false)
                        .error("GC日志内容不能为空")
                        .processTime(System.currentTimeMillis() - startTime)
                        .build();
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
                messages.add(new ChatMessage(ChatMessageRole.SYSTEM.value(), buildSystemPrompt()));
                
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
    private String buildSystemPrompt() {
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
    
    /**
     * 构建用户提示词
     */
    private String buildUserPrompt(AIDiagnosisRequest request) {
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
            systemMsg.put("content", buildSystemPrompt());
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

