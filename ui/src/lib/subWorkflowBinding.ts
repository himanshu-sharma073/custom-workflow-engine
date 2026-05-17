import {
  ACTIVE_SUB_WORKFLOW_CONTEXT_KEY,
  fetchWorkflow,
  fetchWorkflowHistory,
  WorkflowDefinition,
  WorkflowHistoryRecord,
  WorkflowState
} from "../api/tasks";

export type SubWorkflowSnapshot = {
  childWorkflowId: string;
  childStatus: string;
  currentStep?: string;
  history: WorkflowHistoryRecord[];
};

function parseChildIdFromPayload(payload: string | undefined): string | undefined {
  if (!payload) return undefined;
  try {
    const ctx = JSON.parse(payload) as Record<string, unknown>;
    const id = ctx[ACTIVE_SUB_WORKFLOW_CONTEXT_KEY];
    return typeof id === "string" && id.trim() ? id : undefined;
  } catch {
    return undefined;
  }
}

/** Resolve embedded child instance id for a parent SUB_WORKFLOW step (active context or history). */
export function childWorkflowIdForSubStep(
  parentStepId: string,
  parentContext: Record<string, unknown>,
  parentHistory: WorkflowHistoryRecord[],
  parentCurrentStep?: string | null
): string | undefined {
  if (parentCurrentStep === parentStepId) {
    const active = parentContext[ACTIVE_SUB_WORKFLOW_CONTEXT_KEY];
    if (typeof active === "string" && active.trim()) {
      return active;
    }
  }
  const rows = parentHistory.filter(
    (h) =>
      h.stepId === parentStepId &&
      (h.status === "SUB_WORKFLOW_WAITING" || h.status === "SUB_WORKFLOW_COMPLETED")
  );
  for (let i = rows.length - 1; i >= 0; i--) {
    const id = parseChildIdFromPayload(rows[i].payload);
    if (id) return id;
  }
  return undefined;
}

export function subWorkflowStepIds(definition: WorkflowDefinition): string[] {
  return definition.steps.filter((s) => s.type.toLowerCase() === "sub_workflow").map((s) => s.id);
}

/** Load child instance + history for each SUB_WORKFLOW step that has (or had) an embedded run. */
export async function fetchSubWorkflowSnapshots(
  workflow: WorkflowState,
  definition: WorkflowDefinition,
  history: WorkflowHistoryRecord[]
): Promise<Record<string, SubWorkflowSnapshot>> {
  const out: Record<string, SubWorkflowSnapshot> = {};
  await Promise.all(
    subWorkflowStepIds(definition).map(async (parentStepId) => {
      const childId = childWorkflowIdForSubStep(parentStepId, workflow.context, history, workflow.currentStep);
      if (!childId) return;
      try {
        const [wf, hist] = await Promise.all([fetchWorkflow(childId), fetchWorkflowHistory(childId)]);
        out[parentStepId] = {
          childWorkflowId: childId,
          childStatus: wf.status,
          currentStep: wf.currentStep ?? undefined,
          history: hist
        };
      } catch {
        /* child may have been purged */
      }
    })
  );
  return out;
}

export function shouldPollSubWorkflowSnapshots(
  workflow: WorkflowState | null,
  snapshots: Record<string, SubWorkflowSnapshot>
): boolean {
  if (!workflow) return false;
  if (workflow.status === "WAITING" || workflow.status === "RUNNING") {
    if (typeof workflow.context[ACTIVE_SUB_WORKFLOW_CONTEXT_KEY] === "string") return true;
  }
  return Object.values(snapshots).some((s) => s.childStatus === "WAITING" || s.childStatus === "RUNNING");
}
