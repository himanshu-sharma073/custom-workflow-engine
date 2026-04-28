package com.example.workflow.bonus;

import com.example.workflow.persistence.entity.UserTaskEntity;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
public class SlaService {
    public boolean isBreached(UserTaskEntity task, Duration threshold) {
        return Duration.between(task.getCreatedAt(), Instant.now()).compareTo(threshold) > 0;
    }
}
