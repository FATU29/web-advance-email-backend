package com.hcmus.awad_email.config;

import com.hcmus.awad_email.service.KanbanService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Configuration for scheduled tasks.
 * Handles automatic processing of expired snoozes.
 */
@Configuration
@EnableScheduling
@Slf4j
public class SchedulerConfig {
    
    @Autowired
    private KanbanService kanbanService;
    
    /**
     * Process expired snoozes every minute.
     * Restores snoozed emails to their previous columns when snooze time expires.
     */
    @Scheduled(fixedRate = 60000) // Run every 60 seconds
    public void processExpiredSnoozes() {
        try {
            kanbanService.processExpiredSnoozes();
        } catch (Exception e) {
            log.error("Error processing expired snoozes: {}", e.getMessage());
        }
    }
}

