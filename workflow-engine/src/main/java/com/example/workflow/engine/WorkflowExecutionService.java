package com.example.workflow.engine;

import com.example.workflow.definition.WorkflowDefinitionLoader;
import com.example.workflow.dsl.StepDefinition;
import com.example.workflow.dsl.StepType;
import com.example.workflow.dsl.WorkflowDefinition;
import com.example.workflow.engine.compensation.CompensationHandler;
import com.example.workflow.engine.handler.StepExecutionContext;
import com.example.workflow.engine.handler.StepExecutionResult;
import com.example.workflow.engine.handler.StepHandler;
import com.example.workflow.engine.listener.StepExecutionListener;
import com.example.workflow.persistence.spi.HistoryRepository;
import com.example.workflow.persistence.spi.WorkflowHistoryRecord;
import com.example.workflow.persistence.spi.WorkflowRepository;
import com.example.workflow.persistence.spi.WorkflowState;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class WorkflowExecutionService implements WorkflowEngine {
    private final WorkflowDefinitionLoader definitionLoader;
    private final WorkflowRepository workflowRepository;
    private final HistoryRepository historyRepository;
    private final Map<StepType, StepHandler> handlers;
    private final List<StepExecutionListener> listeners;
    private final TaskScheduler taskScheduler;
    private final CompensationHandler compensationHandler;

    private final Map<String, WorkflowDefinition> definitions = new ConcurrentHashMap<>();
    private final Map<String, String> waitingEvents = new ConcurrentHashMap<>();

    public WorkflowExecutionService(WorkflowDefinitionLoader definitionLoader,
                                    WorkflowRepository workflowRepository,
                                    HistoryRepository historyRepository,
                                    List<StepHandler> handlers,
                                    ObjectProvider<List<StepExecutionListener>> listenersProvider,
                                    TaskScheduler taskScheduler,
                                    CompensationHandler compensationHandler) {
        this.definitionLoader = definitionLoader;
        this.workflowRepository = workflowRepository;
        this.historyRepository = historyRepository;
        this.handlers = new EnumMap<>(StepType.class);
        handlers.forEach(h -> this.handlers.put(h.supports(), h));
        this.listeners = Optional.ofNullable(listenersProvider.getIfAvailable()).orElseGet(List::of);
        this.taskScheduler = taskScheduler;
        this.compensationHandler = compensationHandler;
    }

    @Override
    public WorkflowState startWorkflow(String definitionId, Map<String, Object> input) {
        WorkflowDefinition definition = definitionLoader.load(definitionId);
        definitions.put(definitionId, definition);
        WorkflowState state = workflowRepository.create(definitionId, input == null ? new HashMap<>() : new HashMap<>(input));
        String firstStep = definition.steps().stream().findFirst().map(StepDefinition::id).orElseThrow();
        workflowRepository.updateState(state.workflowId(), "RUNNING", firstStep, state.context());
        executeCurrent(state.workflowId());
        return getWorkflow(state.workflowId());
    }

    @Override
    public WorkflowState resumeWorkflow(String workflowId, Map<String, Object> input) {
        WorkflowState current = workflowRepository.find(workflowId).orElseThrow();
        Map<String, Object> ctx = new HashMap<>(current.context());
        if (input != null) {
            ctx.putAll(input);
        }
        workflowRepository.updateState(workflowId, "RUNNING", current.currentStep(), ctx);
        executeCurrent(workflowId);
        return getWorkflow(workflowId);
    }

    @Override
    public WorkflowState rollbackWorkflow(String workflowId, String targetStepId) {
        WorkflowState current = workflowRepository.find(workflowId).orElseThrow();
        List<com.example.workflow.persistence.spi.WorkflowHistoryRecord> history = historyRepository.history(workflowId);
        if (history.isEmpty()) {
            return current;
        }

        int targetIdx = resolveRollbackTargetIndex(history, targetStepId);

        if (targetIdx < 0) {
            return current;
        }

        for (int i = history.size() - 1; i > targetIdx; i--) {
            historyRepository.markRolledBack(workflowId, history.get(i).stepId());
        }

        String rollbackToStep = history.get(targetIdx).stepId();
        workflowRepository.updateState(workflowId, "RUNNING", rollbackToStep, current.context());
        executeCurrent(workflowId);
        return getWorkflow(workflowId);
    }

    @Override
    public WorkflowState getWorkflow(String workflowId) {
        return workflowRepository.find(workflowId).orElseThrow();
    }

    public List<WorkflowState> listWorkflows() {
        return workflowRepository.findAll();
    }

    public List<WorkflowHistoryRecord> history(String workflowId) {
        return historyRepository.history(workflowId);
    }

    public void onUserTaskCompleted(String workflowId, String stepId, Map<String, Object> input) {
        WorkflowState state = workflowRepository.find(workflowId).orElseThrow();
        Map<String, Object> ctx = new HashMap<>(state.context());
        if (input != null) {
            ctx.putAll(input);
        }
        StepDefinition step = findStep(state.definitionId(), stepId);
        String next = step.next();
        workflowRepository.updateState(workflowId, "RUNNING", next, ctx);
        executeCurrent(workflowId);
    }

    public boolean publishEvent(String workflowId, String eventName, String correlationId, Map<String, Object> payload) {
        String key = workflowId + "|" + eventName + ":" + correlationId;
        String nextStep = waitingEvents.remove(key);
        if (nextStep == null) {
            return false;
        }
        WorkflowState state = workflowRepository.find(workflowId).orElseThrow();
        Map<String, Object> ctx = new HashMap<>(state.context());
        if (payload != null) {
            ctx.putAll(payload);
        }
        workflowRepository.updateState(workflowId, "RUNNING", nextStep, ctx);
        executeCurrent(workflowId);
        return true;
    }

    private void executeCurrent(String workflowId) {
        WorkflowState state = workflowRepository.find(workflowId).orElseThrow();
        if (state.currentStep() == null) {
            workflowRepository.updateState(workflowId, "COMPLETED", null, state.context());
            return;
        }

        StepDefinition step = findStep(state.definitionId(), state.currentStep());
        if (step.type() == StepType.END) {
            historyRepository.append(workflowId, step.id(), "ENDED", state.context());
            workflowRepository.updateState(workflowId, "COMPLETED", null, state.context());
            return;
        }

        StepHandler handler = handlers.get(step.type());
        if (handler == null) {
            throw new IllegalStateException("No step handler for type " + step.type());
        }

        listeners.forEach(l -> l.beforeStep(workflowId, step, state.context()));

        StepExecutionContext ctx = new StepExecutionContext(
            workflowId,
            new HashMap<>(state.context()),
            historyRepository,
            (eventKey, payload) -> waitingEvents.put(workflowId + "|" + eventKey, step.next()),
            (delayMs, nextStep) -> taskScheduler.schedule(() -> {
                WorkflowState latest = workflowRepository.find(workflowId).orElseThrow();
                workflowRepository.updateState(workflowId, "RUNNING", nextStep, latest.context());
                executeCurrent(workflowId);
            }, Instant.now().plusMillis(delayMs))
        );

        StepExecutionResult executionResult;
        try {
            executionResult = handler.execute(step, ctx);
        } catch (RuntimeException ex) {
            if (step.compensation() != null) {
                compensationHandler.compensate(step, ctx.variables());
            }
            throw ex;
        }

        final boolean isEnded = executionResult.ended();
        final boolean isWaiting = executionResult.waiting();
        final String nextStepId = executionResult.nextStepId();

        final String resultingStep =
            isEnded ? null : (isWaiting ? step.id() : nextStepId);
        final String resultingStatus =
            isEnded ? "COMPLETED" : (isWaiting ? "WAITING" : "RUNNING");

        workflowRepository.updateState(workflowId, resultingStatus, resultingStep, ctx.variables());
        listeners.forEach(l -> l.afterStep(workflowId, step, ctx.variables()));

        if (!isWaiting && !isEnded && nextStepId != null) {
            executeCurrent(workflowId);
        }
    }

    private StepDefinition findStep(String definitionId, String stepId) {
        WorkflowDefinition def = definitions.computeIfAbsent(definitionId, definitionLoader::load);
        return def.steps().stream().filter(s -> s.id().equals(stepId)).findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Step " + stepId + " not found in definition " + definitionId));
    }

    /**
     * History rows use granular statuses ({@code SYSTEM_COMPLETED}, {@code USER_WAITING}, {@code API_SUCCESS}, …),
     * not a generic {@code COMPLETED}. Finds the newest applicable row index for rollback anchoring.
     */
    private int resolveRollbackTargetIndex(List<WorkflowHistoryRecord> history, String targetStepId) {
        if (history.isEmpty()) {
            return -1;
        }
        if (targetStepId != null && !targetStepId.isBlank()) {
            for (int i = history.size() - 1; i >= 0; i--) {
                WorkflowHistoryRecord record = history.get(i);
                if (!targetStepId.equals(record.stepId())) {
                    continue;
                }
                String st = record.status();
                if ("ROLLED_BACK".equals(st) || isHistoryFailureTerminalStatus(st)) {
                    continue;
                }
                return i;
            }
            return -1;
        }
        for (int i = history.size() - 1; i >= 0; i--) {
            String st = history.get(i).status();
            if (!"ROLLED_BACK".equals(st) && !isHistoryFailureTerminalStatus(st)) {
                return i;
            }
        }
        return -1;
    }

    private boolean isHistoryFailureTerminalStatus(String status) {
        if (status == null) {
            return false;
        }
        return "FAILED".equals(status) || "API_FAILED".equals(status) || "DECISION_NO_MATCH".equals(status);
    }
}
