package com.gcpulse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * GC日志分析平台主应用类
 * 
 * @author GCPulse Team
 */
@Slf4j
@SpringBootApplication
public class GCPulseApplication {

    public static void main(String[] args) {
        SpringApplication.run(GCPulseApplication.class, args);
        log.info("\n========================================\n" +
                "GCPulse Platform Started Successfully!\n" +
                "API Endpoint: http://localhost:8080\n" +
                "========================================");
    }
}

