package com.example.workflow.engine;

import com.example.workflow.definition.WorkflowDefinitionLoader;
import com.example.workflow.dsl.DecisionCondition;
import com.example.workflow.dsl.StepDefinition;
import com.example.workflow.dsl.StepType;
import com.example.workflow.dsl.WorkflowDefinition;
import com.example.workflow.engine.SubWorkflowContextKeys;
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
        String entryStep = resolveEntryStepId(definition);
        workflowRepository.updateState(state.workflowId(), "RUNNING", entryStep, state.context());
        executeCurrent(state.workflowId());
        return getWorkflow(state.workflowId());
    }

    @Override
    public WorkflowState resumeWorkflow(String workflowId, Map<String, Object> input) {
        WorkflowState current = workflowRepository.find(workflowId).orElseThrow();
        if ("WAITING".equals(current.status()) && current.currentStep() != null) {
            StepDefinition waitingStep = findStep(current.definitionId(), current.currentStep());
            if (waitingStep.type() == StepType.SUB_WORKFLOW) {
                throw new IllegalStateException(
                    "Workflow " + workflowId + " is waiting on embedded sub-workflow step " + current.currentStep()
                        + "; resume the parent after the child instance completes (or resume the child if it is waiting).");
            }
        }
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
        if (state.currentStep() == null || !state.currentStep().equals(stepId)) {
            throw new IllegalStateException(
                "Cannot complete user task for step " + stepId + ": workflow " + workflowId
                    + " is at step " + state.currentStep() + " (status " + state.status() + "). "
                    + "Finish the current step first (e.g. complete the embedded sub-workflow before older tasks).");
        }
        StepDefinition step = findStep(state.definitionId(), stepId);
        if (step.type() != StepType.USER) {
            throw new IllegalStateException(
                "Cannot complete user task for step " + stepId + ": not a USER step (type " + step.type() + ")");
        }
        Map<String, Object> ctx = new HashMap<>(state.context());
        if (input != null) {
            ctx.putAll(input);
        }
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
            Map<String, Object> finalSnapshot = new HashMap<>(state.context());
            notifyParentAfterChildCompletedIfApplicable(workflowId, finalSnapshot);
            workflowRepository.updateState(workflowId, "COMPLETED", null, finalSnapshot);
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
     * Entry step = not referenced as {@code next} or decision target. Matches catalog layout where order may differ from graph order.
     * {@link WorkflowRepository#create} does not set {@code currentStep}; callers must set it before {@link #executeCurrent(String)}
     * or the instance is incorrectly completed immediately.
     */
    static String resolveEntryStepId(WorkflowDefinition definition) {
        List<StepDefinition> steps = definition.steps();
        if (steps.isEmpty()) {
            throw new IllegalStateException("Definition has no steps: " + definition.id());
        }
        Set<String> incoming = new HashSet<>();
        for (StepDefinition step : steps) {
            if (step.next() != null && !step.next().isBlank()) {
                incoming.add(step.next());
            }
            if (step.conditions() != null) {
                for (DecisionCondition c : step.conditions()) {
                    if (c.next() != null && !c.next().isBlank()) {
                        incoming.add(c.next());
                    }
                }
            }
        }
        for (StepDefinition step : steps) {
            if (!incoming.contains(step.id())) {
                return step.id();
            }
        }
        return steps.get(0).id();
    }

    /**
     * History rows use granular statuses ({@code SYSTEM_COMPLETED}, {@code USER_WAITING}, {@code SUB_WORKFLOW_WAITING},
     * {@code API_SUCCESS}, …),
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

    public StepExecutionResult runSubWorkflowStep(StepDefinition step, StepExecutionContext context) {
        String defId = step.subWorkflowDefinitionId();
        if (defId == null || defId.isBlank()) {
            throw new IllegalStateException("SUB_WORKFLOW step missing subWorkflowDefinitionId: " + step.id());
        }
        definitionLoader.load(defId);

        String trackedChildId = (String) context.variables().get(SubWorkflowContextKeys.ACTIVE_CHILD_WORKFLOW_ID);
        if (trackedChildId != null && !trackedChildId.isBlank()) {
            Optional<WorkflowState> tracked = workflowRepository.find(trackedChildId);
            if (tracked.isPresent()) {
                String cs = tracked.get().status();
                if ("WAITING".equals(cs) || "RUNNING".equals(cs)) {
                    return StepExecutionResult.waitState();
                }
            }
        }

        Map<String, Object> seed = buildSubWorkflowChildSeed(context.variables(), step);
        seed.put(SubWorkflowContextKeys.PARENT_WORKFLOW_ID, context.workflowId());
        seed.put(SubWorkflowContextKeys.PARENT_SUB_STEP_ID, step.id());

        WorkflowState child = workflowRepository.create(defId, seed);
        WorkflowDefinition childDefinition = definitions.computeIfAbsent(defId, definitionLoader::load);
        String childEntry = resolveEntryStepId(childDefinition);
        workflowRepository.updateState(child.workflowId(), "RUNNING", childEntry, child.context());
        executeCurrent(child.workflowId());

        WorkflowState childDone = workflowRepository.find(child.workflowId()).orElseThrow();
        if ("WAITING".equals(childDone.status())) {
            context.variables().put(SubWorkflowContextKeys.ACTIVE_CHILD_WORKFLOW_ID, child.workflowId());
            context.appendHistory(step, "SUB_WORKFLOW_WAITING");
            return StepExecutionResult.waitState();
        }
        if ("COMPLETED".equals(childDone.status())) {
            WorkflowState parentFresh = workflowRepository.find(context.workflowId()).orElseThrow();
            Map<String, Object> merged = mergeSubWorkflowOutputsIntoParent(parentFresh.context(), childDone.context(), step);
            context.variables().clear();
            context.variables().putAll(merged);
            context.appendHistory(step, "SUB_WORKFLOW_COMPLETED");
            return StepExecutionResult.next(step.next());
        }
        throw new IllegalStateException("Sub-workflow " + child.workflowId() + " ended with status " + childDone.status());
    }

    private void notifyParentAfterChildCompletedIfApplicable(String childWorkflowId, Map<String, Object> completedChildContext) {
        String parentId = (String) completedChildContext.get(SubWorkflowContextKeys.PARENT_WORKFLOW_ID);
        if (parentId == null || parentId.isBlank()) {
            return;
        }
        workflowRepository.find(parentId).ifPresent(parent -> {
            if (!"WAITING".equals(parent.status())) {
                return;
            }
            String tracked = (String) parent.context().get(SubWorkflowContextKeys.ACTIVE_CHILD_WORKFLOW_ID);
            if (!childWorkflowId.equals(tracked)) {
                return;
            }
            String subStepId = (String) completedChildContext.get(SubWorkflowContextKeys.PARENT_SUB_STEP_ID);
            if (subStepId == null) {
                return;
            }
            StepDefinition parentSubStep = findStep(parent.definitionId(), subStepId);
            Map<String, Object> merged = mergeSubWorkflowOutputsIntoParent(parent.context(), completedChildContext, parentSubStep);
            merged.remove(SubWorkflowContextKeys.ACTIVE_CHILD_WORKFLOW_ID);
            historyRepository.append(parentId, parentSubStep.id(), "SUB_WORKFLOW_COMPLETED", merged);
            workflowRepository.updateState(parentId, "RUNNING", parentSubStep.next(), merged);
            listeners.forEach(l -> l.afterStep(parentId, parentSubStep, merged));
            executeCurrent(parentId);
        });
    }

    private static Map<String, Object> buildSubWorkflowChildSeed(Map<String, Object> parentVars, StepDefinition step) {
        Map<String, Object> seed = new HashMap<>();
        boolean isolate = Boolean.TRUE.equals(step.subWorkflowIsolateContext());
        if (!isolate) {
            for (Map.Entry<String, Object> e : parentVars.entrySet()) {
                if (e.getKey() == null || e.getKey().startsWith("__")) {
                    continue;
                }
                if (SubWorkflowContextKeys.isInternalKey(e.getKey())) {
                    continue;
                }
                seed.put(e.getKey(), e.getValue());
            }
        }
        if (step.subWorkflowInput() != null) {
            seed.putAll(step.subWorkflowInput());
        }
        return seed;
    }

    private static Map<String, Object> mergeSubWorkflowOutputsIntoParent(
        Map<String, Object> parentCtx,
        Map<String, Object> completedChildCtx,
        StepDefinition parentSubWorkflowStep
    ) {
        Map<String, Object> merged = new HashMap<>(parentCtx);
        Map<String, Object> childOut = new HashMap<>();
        for (Map.Entry<String, Object> e : completedChildCtx.entrySet()) {
            String k = e.getKey();
            if (k == null || k.startsWith("__")) {
                continue;
            }
            if (SubWorkflowContextKeys.isInternalKey(k)) {
                continue;
            }
            childOut.put(k, e.getValue());
        }
        String nestKey = parentSubWorkflowStep.subWorkflowOutputKey();
        if (nestKey != null && !nestKey.isBlank()) {
            merged.put(nestKey, childOut);
        } else {
            merged.putAll(childOut);
        }
        return merged;
    }
}
