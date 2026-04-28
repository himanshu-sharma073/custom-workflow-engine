package com.example.workflow.persistence.adapter;

import com.example.workflow.engine.UserTaskService;
import com.example.workflow.persistence.entity.UserTaskEntity;
import com.example.workflow.persistence.spi.TaskRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class JpaTaskRepositoryAdapter implements TaskRepository {
    private final UserTaskService userTaskService;

    public JpaTaskRepositoryAdapter(UserTaskService userTaskService) {
        this.userTaskService = userTaskService;
    }

    @Override
    public List<UserTaskEntity> findVisibleTasks() {
        return userTaskService.findMyTasks();
    }

    @Override
    public UserTaskEntity claim(UUID taskId) {
        return userTaskService.claim(taskId);
    }
}
