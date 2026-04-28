package com.example.workflow.bonus;

import com.example.workflow.persistence.entity.UserTaskEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingNotificationPublisher implements NotificationPublisher {
    private static final Logger log = LoggerFactory.getLogger(LoggingNotificationPublisher.class);

    @Override
    public void publishTaskEscalated(UserTaskEntity task) {
        log.info("Escalation notification for task {}", task.getId());
    }
}
