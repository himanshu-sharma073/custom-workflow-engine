package com.example.workflow.bonus;

import com.example.workflow.persistence.entity.UserTaskEntity;
import com.example.workflow.persistence.repository.UserTaskRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
public class EscalationService {
    private final UserTaskRepository userTaskRepository;
    private final NotificationPublisher notificationPublisher;

    public EscalationService(UserTaskRepository userTaskRepository, NotificationPublisher notificationPublisher) {
        this.userTaskRepository = userTaskRepository;
        this.notificationPublisher = notificationPublisher;
    }

    @Scheduled(fixedDelayString = "${workflow.engine.escalation.scan-ms:60000}")
    public void scanOverdueTasks() {
        for (UserTaskEntity task : userTaskRepository.findByStatus("PENDING")) {
            if (Duration.between(task.getCreatedAt(), Instant.now()).toMinutes() >= 60) {
                notificationPublisher.publishTaskEscalated(task);
            }
        }
    }
}
