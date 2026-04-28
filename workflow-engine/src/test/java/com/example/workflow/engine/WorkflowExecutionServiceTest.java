package com.example.workflow.engine;

import com.example.workflow.definition.WorkflowDefinitionLoader;
import com.example.workflow.dsl.StepDefinition;
import com.example.workflow.dsl.StepType;
import com.example.workflow.dsl.WorkflowDefinition;
import com.example.workflow.engine.compensation.CompensationHandler;
import com.example.workflow.engine.handler.StepExecutionContext;
import com.example.workflow.engine.handler.StepExecutionResult;
import com.example.workflow.engine.handler.StepHandler;
import com.example.workflow.persistence.spi.HistoryRepository;
import com.example.workflow.persistence.spi.WorkflowHistoryRecord;
import com.example.workflow.persistence.spi.WorkflowRepository;
import com.example.workflow.persistence.spi.WorkflowState;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.TaskScheduler;

import java.time.Instant;
import java.util.*;

class WorkflowExecutionServiceTest {

    @Test
    void startsWorkflowAndMovesToCompletion() {
        WorkflowDefinitionLoader loader = id -> new WorkflowDefinition(
            id,
            1,
            List.of(
                new StepDefinition("step1", StepType.SYSTEM, null, null, null, "end", null, null, null, null, null, null, null, null, null, null, null, null),
                new StepDefinition("end", StepType.END, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null)
            )
        );

        InMemoryWorkflowRepo workflowRepo = new InMemoryWorkflowRepo();
        InMemoryHistoryRepo historyRepo = new InMemoryHistoryRepo();

        StepHandler system = new StepHandler() {
            @Override public StepType supports() { return StepType.SYSTEM; }
            @Override public StepExecutionResult execute(StepDefinition step, StepExecutionContext context) {
                context.appendHistory(step, "SYSTEM_COMPLETED");
                return StepExecutionResult.next(step.next());
            }
        };

        TaskScheduler scheduler = (task, startTime) -> {
            task.run();
            return null;
        };

        WorkflowExecutionService service = new WorkflowExecutionService(
            loader,
            workflowRepo,
            historyRepo,
            List.of(system),
            List.of(),
            scheduler,
            (CompensationHandler) (step, context) -> {}
        );

        WorkflowState started = service.startWorkflow("wf", Map.of("x", 1));
        WorkflowState current = service.getWorkflow(started.workflowId());

        Assertions.assertEquals("COMPLETED", current.status());
        Assertions.assertTrue(historyRepo.records.stream().anyMatch(r -> "SYSTEM_COMPLETED".equals(r.status())));
    }

    private static class InMemoryWorkflowRepo implements WorkflowRepository {
        private final Map<String, WorkflowState> store = new HashMap<>();

        @Override
        public WorkflowState create(String definitionId, Map<String, Object> context) {
            WorkflowState state = new WorkflowState(UUID.randomUUID().toString(), definitionId, "RUNNING", null, new HashMap<>(context), Instant.now());
            store.put(state.workflowId(), state);
            return state;
        }

        @Override
        public Optional<WorkflowState> find(String workflowId) { return Optional.ofNullable(store.get(workflowId)); }

        @Override
        public List<WorkflowState> findAll() { return new ArrayList<>(store.values()); }

        @Override
        public void updateState(String workflowId, String status, String currentStep, Map<String, Object> context) {
            WorkflowState old = store.get(workflowId);
            store.put(workflowId, new WorkflowState(workflowId, old.definitionId(), status, currentStep, new HashMap<>(context), Instant.now()));
        }
    }

    private static class InMemoryHistoryRepo implements HistoryRepository {
        private final List<WorkflowHistoryRecord> records = new ArrayList<>();

        @Override
        public void append(String workflowId, String stepId, String status, Map<String, Object> contextSnapshot) {
            records.add(new WorkflowHistoryRecord(workflowId, stepId, status, contextSnapshot.toString(), Instant.now()));
        }

        @Override
        public void markRolledBack(String workflowId, String stepId) {
            records.add(new WorkflowHistoryRecord(workflowId, stepId, "ROLLED_BACK", "{}", Instant.now()));
        }

        @Override
        public List<WorkflowHistoryRecord> history(String workflowId) {
            return records.stream().filter(r -> r.workflowId().equals(workflowId)).toList();
        }
    }
}
