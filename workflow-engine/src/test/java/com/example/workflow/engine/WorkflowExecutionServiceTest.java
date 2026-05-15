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
                new StepDefinition("step1", StepType.SYSTEM, null, null, null, "end",
                    null, null, null, null, null, null, null, null, null, null, null, null,
                    null, null, null, null),
                new StepDefinition("end", StepType.END, null, null, null, null,
                    null, null, null, null, null, null, null, null, null, null, null, null,
                    null, null, null, null)
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

    @Test
    void runsNestedSubWorkflowAndMergesOutput() {
        WorkflowDefinition child = new WorkflowDefinition(
            "child-wf",
            1,
            List.of(systemStep("c1", "c-end"), end("c-end"))
        );
        WorkflowDefinition parent = new WorkflowDefinition(
            "parent-wf",
            1,
            List.of(
                systemStep("p1", "call-child"),
                subWorkflowStep("call-child", "child-wf", "p-end", "nested"),
                systemStep("p-end", "p-final"),
                end("p-final")
            )
        );
        WorkflowDefinitionLoader loader = id -> {
            if ("parent-wf".equals(id)) {
                return parent;
            }
            if ("child-wf".equals(id)) {
                return child;
            }
            throw new IllegalArgumentException(id);
        };

        InMemoryWorkflowRepo workflowRepo = new InMemoryWorkflowRepo();
        InMemoryHistoryRepo historyRepo = new InMemoryHistoryRepo();

        StepHandler system = new StepHandler() {
            @Override
            public StepType supports() {
                return StepType.SYSTEM;
            }

            @Override
            public StepExecutionResult execute(StepDefinition step, StepExecutionContext context) {
                context.variables().put("tick-" + step.id(), true);
                context.appendHistory(step, "SYSTEM_COMPLETED");
                return step.next() == null ? StepExecutionResult.endState() : StepExecutionResult.next(step.next());
            }
        };

        TaskScheduler scheduler = (task, startTime) -> {
            task.run();
            return null;
        };

        final WorkflowExecutionService[] svcRef = new WorkflowExecutionService[1];
        StepHandler sub = new StepHandler() {
            @Override
            public StepType supports() {
                return StepType.SUB_WORKFLOW;
            }

            @Override
            public StepExecutionResult execute(StepDefinition step, StepExecutionContext context) {
                return svcRef[0].runSubWorkflowStep(step, context);
            }
        };

        svcRef[0] = new WorkflowExecutionService(
            loader,
            workflowRepo,
            historyRepo,
            List.of(system, sub),
            List.of(),
            scheduler,
            (CompensationHandler) (stp, ctx) -> {}
        );

        WorkflowState started = svcRef[0].startWorkflow("parent-wf", Map.of("x", "root"));
        WorkflowState done = svcRef[0].getWorkflow(started.workflowId());

        Assertions.assertEquals("COMPLETED", done.status());
        Assertions.assertTrue(historyRepo.records.stream().anyMatch(r -> "SUB_WORKFLOW_COMPLETED".equals(r.status())));
        Map<String, Object> ctx = done.context();
        Assertions.assertTrue(ctx.containsKey("nested"));
        @SuppressWarnings("unchecked")
        Map<String, Object> nested = (Map<String, Object>) ctx.get("nested");
        Assertions.assertEquals(true, nested.get("tick-c1"));
        Assertions.assertEquals("root", nested.get("x"));
    }

    private static StepDefinition systemStep(String id, String next) {
        return new StepDefinition(id, StepType.SYSTEM, null, null, null, next,
            null, null, null, null, null, null, null, null, null, null, null, null,
            null, null, null, null);
    }

    private static StepDefinition subWorkflowStep(String id, String definitionId, String next, String outputKey) {
        return new StepDefinition(id, StepType.SUB_WORKFLOW, null, null, null, next,
            null, null, null, null, null, null, null, null, null, null, null, null,
            null, definitionId, null, null, outputKey);
    }

    private static StepDefinition end(String id) {
        return new StepDefinition(id, StepType.END, null, null, null, null,
            null, null, null, null, null, null, null, null, null, null, null, null,
            null, null, null, null);
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
