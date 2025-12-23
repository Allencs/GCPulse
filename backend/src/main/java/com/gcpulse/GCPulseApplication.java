package com.gcpulse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * GC日志分析平台主应用类
 * 
 * @author GCPulse Team
 */
@SpringBootApplication
public class GCPulseApplication {

    public static void main(String[] args) {
        SpringApplication.run(GCPulseApplication.class, args);
        System.out.println("""
            
            ========================================
            GCPulse Platform Started Successfully!
            API Endpoint: http://localhost:8080
            ========================================
            """);
    }
}

