package com.example.workflow.engine;

import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class RollbackService {
    private final UserTaskService userTaskService;

    public RollbackService(UserTaskService userTaskService) {
        this.userTaskService = userTaskService;
    }

    public void rollbackUserTask(UUID taskId, boolean resetApprovals) {
        userTaskService.reopenTask(taskId, resetApprovals);
    }
}
