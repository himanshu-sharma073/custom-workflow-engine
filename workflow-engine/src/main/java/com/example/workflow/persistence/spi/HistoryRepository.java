package com.example.workflow.persistence.spi;

import java.util.List;
import java.util.Map;

public interface HistoryRepository {
    void append(String workflowId, String stepId, String status, Map<String, Object> contextSnapshot);
    void markRolledBack(String workflowId, String stepId);
    List<WorkflowHistoryRecord> history(String workflowId);
}
