package com.example.workflow.bonus;

import com.example.workflow.persistence.entity.UserTaskEntity;

public interface NotificationPublisher {
    void publishTaskEscalated(UserTaskEntity task);
}
