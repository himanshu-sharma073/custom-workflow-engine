# Architecture

## Overview
The workflow platform is organized as a reusable engine module plus a host application and a separate UI application.

- `workflow-engine` provides DSL parsing, orchestration, handlers, persistence adapters, and configurable REST APIs.
- `host-app-example` owns security/auth context and business integration.
- `ui` is a standalone React app that consumes host APIs.

```mermaid
flowchart LR
  hostApp[HostSpringBootApp] --> autoConfig[WorkflowEngineAutoConfiguration]
  autoConfig --> workflowEngine[WorkflowEngineInterface]
  workflowEngine --> orchestrator[WorkflowExecutionService]
  orchestrator --> stepRegistry[StepHandlerRegistry]
  orchestrator --> persistenceSpi[PersistenceSPI]
  orchestrator --> definitionLoader[WorkflowDefinitionLoader]
  hostApp --> userContext[UserContextProvider]
  uiApp[SeparateReactUI] --> apiLayer[WorkflowRESTAPI]
  apiLayer --> workflowEngine
```

## Module Breakdown

### 1) `workflow-engine`
Core reusable module with:
- Auto-configuration (`workflow.engine.*`)
- Definition loading (`file`, `classpath`; `db` deferred)
- Workflow orchestration (`start`, `resume`, `rollback`, `get`)
- Step handler strategy
- Persistence SPI + JPA adapters
- Workflow/task/event APIs

### 2) `host-app-example`
Integration sample that demonstrates:
- Spring Security ownership
- `UserContextProvider` 11
- Example workflow start/rollback endpoints
- File-based definition loading config

### 3) `ui`
Standalone dashboard:
- Definitions list + details
- DAG visualization
- Running workflow instances
- Task management and approvals
- Runtime timeline

```mermaid
flowchart TB
  subgraph engineMod [workflow_engine]
    cfg[AutoConfig]
    dsl[DSLAndParser]
    defLoader[DefinitionLoader]
    exec[ExecutionOrchestrator]
    handlers[StepHandlers]
    repos[PersistenceAdapters]
    api[RESTControllers]
  end

  subgraph hostMod [host_app_example]
    hostApi[DemoControllers]
    sec[SecurityConfig]
    ucp[UserContextProviderBean]
  end

  subgraph uiMod [ui]
    dash[Dashboard]
    dag[DagView]
    tasks[TaskActions]
  end

  hostApi --> api
  sec --> ucp
  ucp --> exec
  dash --> api
  dag --> api
  tasks --> api
```

## Configuration Model
The engine behavior is driven by nested properties under `workflow.engine`.

- `enabled`
- `api.enabled`, `api.base-path`
- `ui.enabled`, `ui.path` (metadata; UI is separate)
- `definition.source`, `definition.path`, `definition.cache-enabled`
- `execution.max-retries`, `execution.retry-backoff-ms`
- `persistence.type`
- `security.enabled`

```mermaid
flowchart LR
  props[workflow.engine.properties] --> enabled[EngineEnabled]
  props --> apiCfg[ApiConfig]
  props --> defCfg[DefinitionConfig]
  props --> execCfg[ExecutionConfig]
  props --> persistCfg[PersistenceConfig]
  props --> secCfg[SecurityMetadata]
  apiCfg --> apiPath[BasePath]
  defCfg --> source[SourceSelector]
  source --> fs[FileSystemLoader]
  source --> cp[ClasspathLoader]
```

## Definition Loading and Caching
`WorkflowDefinitionLoader` is the source abstraction.

- File loader reads `${path}/${workflowId}.json`
- Classpath loader reads `${path}/${workflowId}.json` from resources
- Caching decorator controls hot-reload behavior:
  - cache enabled: memoized definitions
  - cache disabled: reload each request

```mermaid
flowchart LR
  orchestrator[WorkflowExecutionService] --> caching[CachingWorkflowDefinitionLoader]
  caching --> selector[SourceImplementation]
  selector --> fsLoader[FileSystemWorkflowLoader]
  selector --> cpLoader[ClasspathWorkflowLoader]
  selector --> dbStub[DatabaseWorkflowLoaderDeferred]
  fsLoader --> jsonFile[DefinitionJSONFile]
  cpLoader --> classpathJson[ClasspathDefinitionJSON]
```

## Orchestration Lifecycle
Engine orchestration follows a deterministic loop over current step pointer + context.

1. Start or resume workflow
2. Resolve current step from definition
3. Dispatch to handler by `StepType`
4. Persist history snapshot and updated context
5. Continue / wait / complete

```mermaid
flowchart TD
  start[StartOrResume] --> loadState[LoadWorkflowState]
  loadState --> resolveStep[ResolveCurrentStep]
  resolveStep --> dispatch[DispatchToStepHandler]
  dispatch --> result[StepExecutionResult]
  result -->|nextStepId| updateState[PersistStateAndHistory]
  result -->|waiting| waitState[PauseUntilTaskEventOrDelay]
  result -->|ended| complete[MarkWorkflowCompleted]
  updateState --> loop{MoreSteps}
  loop -->|yes| resolveStep
  loop -->|no| complete
```

## Step Handler Strategy
Each task type is implemented through `StepHandler`.

- `SYSTEM`: internal action invocation path
- `USER`: create/await user task
- `DECISION`: evaluate branching expressions
- `API`: outbound call with retry/backoff + response mapping
- `EVENT`: wait for correlation-based signal
- `DELAY`: scheduler-based pause
- `SCRIPT`: lightweight expression mutation

```mermaid
flowchart LR
  stepType[StepType] --> registry[HandlerRegistryMap]
  registry --> sysH[SystemStepHandler]
  registry --> userH[UserStepHandler]
  registry --> decH[DecisionStepHandler]
  registry --> apiH[ApiStepHandler]
  registry --> evtH[EventStepHandler]
  registry --> dlyH[DelayStepHandler]
  registry --> scriptH[ScriptStepHandler]
  apiH --> retry[RetryBackoffPolicy]
  apiH --> mapResp[JsonPathResponseMapping]
```

## Persistence Architecture
Persistence is abstracted behind SPI interfaces:
- `WorkflowRepository`
- `HistoryRepository`
- `TaskRepository`

Default adapters are JPA-backed.

```mermaid
flowchart TB
  orchestrator[WorkflowExecutionService] --> wfRepo[WorkflowRepository]
  orchestrator --> histRepo[HistoryRepository]
  taskSvc[UserTaskService] --> taskRepo[TaskRepository]

  wfRepo --> wfAdapter[JpaWorkflowRepositoryAdapter]
  histRepo --> histAdapter[JpaHistoryRepositoryAdapter]
  taskRepo --> taskAdapter[JpaTaskRepositoryAdapter]

  wfAdapter --> wfEntity[WorkflowInstanceEntity]
  histAdapter --> stepEntity[WorkflowStepEntity]
  taskAdapter --> userTaskEntity[UserTaskEntity]
  histAdapter --> auditEntity[WorkflowAuditEventEntity]
```

## API Surface
All engine APIs are under configurable base path (`workflow.engine.api.base-path`, default `/workflows`) and can be disabled with `workflow.engine.api.enabled=false`.

- Workflow: start/resume/rollback/get/list
- Task: list/claim/approve/reject/approvals
- Runtime events: publish/list
- Definitions: list/get

```mermaid
flowchart LR
  client[ClientUIOrPostman] --> wfCtrl[WorkflowEngineController]
  client --> taskCtrl[TaskController]
  client --> runtimeCtrl[WorkflowRuntimeController]
  client --> defCtrl[WorkflowDefinitionController]

  wfCtrl --> engine[WorkflowEngine]
  taskCtrl --> userTaskSvc[UserTaskService]
  runtimeCtrl --> execSvc[WorkflowExecutionService]
  defCtrl --> catalogSvc[WorkflowDefinitionCatalogService]
```

## User Task and Approval Model
User tasks are resolved by host-driven context and assignment strategies.

Visibility and actions are governed by:
- assignment (`USER`, `ROLE`, `GROUP`, `EXPRESSION`)
- candidates
- approval policy (`ANY`, `ALL`, `MIN_APPROVAL`)

```mermaid
flowchart TD
  user[CurrentUserContext] --> evaluator[AssignmentEvaluator]
  evaluator --> visible{TaskVisible}
  visible -->|no| skip[NotInInbox]
  visible -->|yes| inbox[ShowInTasks]
  inbox --> claim[Claim]
  inbox --> approve[Approve]
  inbox --> reject[Reject]
  approve --> policy[ApprovalPolicyService]
  policy --> done{ThresholdMet}
  done -->|yes| completeTask[TaskCompleted]
  done -->|no| pendingTask[RemainPending]
```

## Rollback and Compensation
Rollback is intentionally state-oriented, not side-effect reversal.

- marks recent history as `ROLLED_BACK`
- resets workflow pointer to prior step
- restores persisted context path
- recreates user interaction path through normal execution
- compensation hook is optional and host-defined

```mermaid
flowchart LR
  rollbackReq[RollbackRequest] --> history[HistoryRepository]
  history --> mark[MarkLastAsRolledBack]
  mark --> pointer[MovePointerToPreviousStep]
  pointer --> restore[RestoreContextSnapshot]
  restore --> reexec[ResumeExecutionLoop]

  apiFailure[ExternalAPIFailure] --> compHook[CompensationHandler]
  compHook --> hostComp[HostDefinedCompensation]
```

## Event and Delay Waiting Semantics
Current sample implementation keeps waiting registry in-memory.

- `EVENT` pauses until matching `eventName + correlationId`
- `DELAY` schedules resume via task scheduler

```mermaid
flowchart TD
  evtStep[EventStep] --> register[RegisterWaitingKey]
  register --> paused[WorkflowPaused]
  publish[PublishEventAPI] --> match{KeyMatched}
  match -->|yes| resume[ResumeWorkflowAtNextStep]
  match -->|no| ignore[NoWaitingWorkflow]

  delayStep[DelayStep] --> schedule[TaskSchedulerSchedule]
  schedule --> wake[WakeAtDelayExpiry]
  wake --> resume
```

## Security Boundaries
Security is host-owned.

- Engine does not implement authn/authz.
- Host app secures endpoints (sample uses HTTP Basic).
- Engine consumes user identity/roles via `UserContextProvider`.

## Extensibility Summary
Override points designed for host customization:
- `UserContextProvider`
- `WorkflowDefinitionLoader`
- `StepHandler`
- `AssignmentResolver`
- `WorkflowRepository` / `HistoryRepository` / `TaskRepository`
- `CompensationHandler`
- `StepExecutionListener`
- `NotificationPublisher`

## Operational Notes
- Use shared/external coordination for event waiting and delayed scheduling in multi-node production.
- Keep workflow definitions versioned and immutable where possible.
- Add host-side observability dashboards using Micrometer counters and audit streams.
