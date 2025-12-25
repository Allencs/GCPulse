package com.gcpulse.service;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * AI诊断报告导出服务（已移除PDF功能）
 * 支持导出为HTML、Markdown等格式
 */
@Slf4j
@Service
public class DiagnosisExportService {

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 导出AI诊断为HTML格式（使用渲染后的HTML内容）
     */
    public byte[] exportToHtml(String renderedHtml, String diagnosis, String collectorType, Integer eventCount) {
        log.info("开始导出HTML格式诊断报告（使用渲染后的HTML）");
        
        try {
            StringBuilder html = new StringBuilder();
            html.append("<!DOCTYPE html>\n");
            html.append("<html lang=\"zh-CN\">\n");
            html.append("<head>\n");
            html.append("    <meta charset=\"UTF-8\">\n");
            html.append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
            html.append("    <title>GCPulse AI诊断报告</title>\n");
            html.append("    <style>\n");
            html.append(getHtmlStyles());
            html.append("    </style>\n");
            html.append("</head>\n");
            html.append("<body>\n");
            html.append("    <div class=\"container\">\n");
            html.append("        <div class=\"header\">\n");
            html.append("            <h1>🔍 GCPulse AI诊断报告</h1>\n");
            html.append("            <div class=\"metadata\">\n");
            html.append("                <p><strong>生成时间：</strong>").append(LocalDateTime.now().format(DATETIME_FORMATTER)).append("</p>\n");
            html.append("                <p><strong>GC收集器：</strong>").append(collectorType != null ? collectorType : "Unknown").append("</p>\n");
            html.append("                <p><strong>GC事件数：</strong>").append(eventCount != null ? eventCount : 0).append("</p>\n");
            html.append("            </div>\n");
            html.append("        </div>\n");
            html.append("        <div class=\"content\">\n");
            
            // 使用前端渲染后的HTML内容（如果提供）
            if (renderedHtml != null && !renderedHtml.trim().isEmpty()) {
                html.append(renderedHtml);
            } else {
                // 后备方案：将Markdown转换为HTML
                Parser parser = Parser.builder().build();
                HtmlRenderer renderer = HtmlRenderer.builder().build();
                Node document = parser.parse(diagnosis);
                String contentHtml = renderer.render(document);
                html.append(contentHtml);
            }
            
            html.append("        </div>\n");
            html.append("        <div class=\"footer\">\n");
            html.append("            <p>本报告由 <strong>GCPulse</strong> 自动生成</p>\n");
            html.append("            <p>Powered by AI | © 2024 GCPulse</p>\n");
            html.append("        </div>\n");
            html.append("    </div>\n");
            html.append("</body>\n");
            html.append("</html>");
            
            log.info("HTML格式导出完成，大小: {} bytes", html.length());
            return html.toString().getBytes(StandardCharsets.UTF_8);
            
        } catch (Exception e) {
            log.error("导出HTML失败: {}", e.getMessage(), e);
            throw new RuntimeException("导出HTML失败: " + e.getMessage(), e);
        }
    }

    /**
     * 导出为Markdown格式
     */
    public byte[] exportToMarkdown(String diagnosis, String collectorType, Integer eventCount) {
        log.info("开始导出Markdown格式诊断报告");
        
        try {
            StringBuilder markdown = new StringBuilder();
            markdown.append("# 🔍 GCPulse AI诊断报告\n\n");
            markdown.append("---\n\n");
            markdown.append("**生成时间：** ").append(LocalDateTime.now().format(DATETIME_FORMATTER)).append("\n\n");
            markdown.append("**GC收集器类型：** ").append(collectorType != null ? collectorType : "Unknown").append("\n\n");
            markdown.append("**GC事件数量：** ").append(eventCount != null ? eventCount : 0).append("\n\n");
            markdown.append("---\n\n");
            markdown.append(diagnosis);
            markdown.append("\n\n---\n\n");
            markdown.append("*本报告由 **GCPulse** 自动生成 | Powered by AI*\n");
            
            log.info("Markdown格式导出完成，大小: {} bytes", markdown.length());
            return markdown.toString().getBytes(StandardCharsets.UTF_8);
            
        } catch (Exception e) {
            log.error("导出Markdown失败: {}", e.getMessage(), e);
            throw new RuntimeException("导出Markdown失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 导出GC分析结果为HTML（保持原有页面样式）
     */
    public byte[] exportAnalysisToHtml(String renderedHtml, String analysisDataJson) {
        log.info("开始导出GC分析结果为HTML格式");
        
        try {
            StringBuilder html = new StringBuilder();
            html.append("<!DOCTYPE html>\n");
            html.append("<html lang=\"zh-CN\">\n");
            html.append("<head>\n");
            html.append("    <meta charset=\"UTF-8\">\n");
            html.append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
            html.append("    <title>GCPulse 分析报告</title>\n");
            html.append("    <style>\n");
            html.append(getAnalysisHtmlStyles());
            html.append("    </style>\n");
            html.append("</head>\n");
            html.append("<body>\n");
            html.append("    <div class=\"container\">\n");
            html.append("        <div class=\"header\">\n");
            html.append("            <h1>📊 GCPulse 分析报告</h1>\n");
            html.append("            <div class=\"metadata\">\n");
            html.append("                <p><strong>生成时间：</strong>").append(LocalDateTime.now().format(DATETIME_FORMATTER)).append("</p>\n");
            html.append("            </div>\n");
            html.append("        </div>\n");
            html.append("        <div class=\"content\">\n");
            
            // 使用前端渲染后的HTML内容
            if (renderedHtml != null && !renderedHtml.trim().isEmpty()) {
                html.append(renderedHtml);
            } else {
                html.append("<p>分析结果内容</p>\n");
            }
            
            html.append("        </div>\n");
            html.append("        <div class=\"footer\">\n");
            html.append("            <p>本报告由 <strong>GCPulse</strong> 自动生成</p>\n");
            html.append("            <p>© 2024 GCPulse | Java GC日志分析平台</p>\n");
            html.append("        </div>\n");
            html.append("    </div>\n");
            html.append("</body>\n");
            html.append("</html>");
            
            log.info("分析结果HTML格式导出完成，大小: {} bytes", html.length());
            return html.toString().getBytes(StandardCharsets.UTF_8);
            
        } catch (Exception e) {
            log.error("导出分析结果HTML失败: {}", e.getMessage(), e);
            throw new RuntimeException("导出分析结果HTML失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取AI诊断HTML样式
     */
    private String getHtmlStyles() {
        return """
                * {
                    margin: 0;
                    padding: 0;
                    box-sizing: border-box;
                }
                
                body {
                    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
                    line-height: 1.6;
                    color: #333;
                    background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
                    padding: 20px;
                }
                
                .container {
                    max-width: 1000px;
                    margin: 0 auto;
                    background: white;
                    border-radius: 12px;
                    box-shadow: 0 10px 40px rgba(0, 0, 0, 0.1);
                    overflow: hidden;
                }
                
                .header {
                    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                    color: white;
                    padding: 40px;
                    text-align: center;
                }
                
                .header h1 {
                    font-size: 32px;
                    margin-bottom: 20px;
                    font-weight: 700;
                }
                
                .metadata {
                    display: flex;
                    justify-content: center;
                    gap: 30px;
                    flex-wrap: wrap;
                    opacity: 0.95;
                }
                
                .metadata p {
                    font-size: 14px;
                    margin: 5px 0;
                }
                
                .content {
                    padding: 40px;
                    background: #fafbfc;
                }
                
                .content h1 {
                    font-size: 28px;
                    color: #1a1a1a;
                    margin: 30px 0 20px 0;
                    padding-bottom: 12px;
                    border-bottom: 3px solid #667eea;
                }
                
                .content h2 {
                    font-size: 22px;
                    color: #2c3e50;
                    margin: 25px 0 15px 0;
                    padding-left: 12px;
                    border-left: 4px solid #667eea;
                }
                
                .content h3 {
                    font-size: 18px;
                    color: #34495e;
                    margin: 20px 0 12px 0;
                }
                
                .content h3::before {
                    content: '▸ ';
                    color: #667eea;
                    font-weight: 700;
                }
                
                .content p {
                    margin: 12px 0;
                    color: #4a5568;
                    font-size: 15px;
                    line-height: 1.8;
                }
                
                .content ul, .content ol {
                    margin: 16px 0;
                    padding-left: 32px;
                }
                
                .content li {
                    margin: 10px 0;
                    color: #4a5568;
                    line-height: 1.8;
                }
                
                .content code {
                    background: linear-gradient(135deg, #f7f8fa 0%, #eef0f4 100%);
                    padding: 3px 8px;
                    border-radius: 4px;
                    font-family: 'Monaco', 'Menlo', 'Courier New', monospace;
                    font-size: 13px;
                    color: #e83e8c;
                    border: 1px solid #e1e4e8;
                }
                
                .content pre {
                    background: #1e1e1e;
                    padding: 20px;
                    border-radius: 8px;
                    overflow-x: auto;
                    margin: 20px 0;
                    border: 1px solid #333;
                }
                
                .content pre code {
                    background: none;
                    padding: 0;
                    border: none;
                    color: #d4d4d4;
                    font-size: 13px;
                    line-height: 1.6;
                }
                
                .content blockquote {
                    border-left: 4px solid #667eea;
                    padding: 16px 20px;
                    margin: 20px 0;
                    background: linear-gradient(135deg, #f0f4ff 0%, #f5f0ff 100%);
                    border-radius: 0 8px 8px 0;
                    color: #5a6c7d;
                    font-style: italic;
                }
                
                .content table {
                    width: 100%;
                    border-collapse: collapse;
                    margin: 20px 0;
                    border-radius: 8px;
                    overflow: hidden;
                    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
                }
                
                .content th, .content td {
                    border: 1px solid #e1e4e8;
                    padding: 12px 16px;
                    text-align: left;
                }
                
                .content th {
                    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                    color: white;
                    font-weight: 600;
                    text-transform: uppercase;
                    letter-spacing: 0.5px;
                }
                
                .content tr:nth-child(even) {
                    background: #f8f9fa;
                }
                
                .content tr:hover {
                    background: #e3f2fd;
                }
                
                .content strong {
                    color: #2c3e50;
                    font-weight: 700;
                }
                
                .content em {
                    color: #5a6c7d;
                    font-style: italic;
                }
                
                .content hr {
                    border: none;
                    height: 2px;
                    background: linear-gradient(90deg, transparent 0%, #667eea 50%, transparent 100%);
                    margin: 32px 0;
                }
                
                .footer {
                    background: #2c3e50;
                    color: white;
                    text-align: center;
                    padding: 30px;
                    font-size: 14px;
                }
                
                .footer p {
                    margin: 5px 0;
                    opacity: 0.9;
                }
                
                @media print {
                    body {
                        background: white;
                        padding: 0;
                    }
                    
                    .container {
                        box-shadow: none;
                    }
                }
                """;
    }
    
    /**
     * 获取分析结果HTML样式
     */
    private String getAnalysisHtmlStyles() {
        return """
                * {
                    margin: 0;
                    padding: 0;
                    box-sizing: border-box;
                }
                
                body {
                    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
                    line-height: 1.6;
                    color: #333;
                    background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
                    padding: 20px;
                }
                
                .container {
                    max-width: 1400px;
                    margin: 0 auto;
                    background: white;
                    border-radius: 12px;
                    box-shadow: 0 10px 40px rgba(0, 0, 0, 0.1);
                    overflow: hidden;
                }
                
                .header {
                    background: linear-gradient(135deg, #409EFF 0%, #667eea 100%);
                    color: white;
                    padding: 40px;
                    text-align: center;
                }
                
                .header h1 {
                    font-size: 32px;
                    margin-bottom: 20px;
                    font-weight: 700;
                }
                
                .metadata {
                    display: flex;
                    justify-content: center;
                    gap: 30px;
                    flex-wrap: wrap;
                    opacity: 0.95;
                }
                
                .metadata p {
                    font-size: 14px;
                    margin: 5px 0;
                }
                
                .content {
                    padding: 40px;
                    background: #fafbfc;
                }
                
                .footer {
                    background: #2c3e50;
                    color: white;
                    text-align: center;
                    padding: 30px;
                    font-size: 14px;
                }
                
                .footer p {
                    margin: 5px 0;
                    opacity: 0.9;
                }
                
                @media print {
                    body {
                        background: white;
                        padding: 0;
                    }
                    
                    .container {
                        box-shadow: none;
                    }
                }
                """;
    }
}
