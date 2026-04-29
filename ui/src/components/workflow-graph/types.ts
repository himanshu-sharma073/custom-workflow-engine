import { WorkflowDefinition, WorkflowHistoryRecord, WorkflowStep } from "../../api/tasks";

export type NodeStatus = "COMPLETED" | "CURRENT" | "FAILED" | "ROLLED_BACK" | "PENDING";

export type GraphNode = {
  id: string;
  label: string;
  type: string;
  stage?: string;
  x: number;
  y: number;
  status: NodeStatus;
  step: WorkflowStep;
  timestamp?: string;
};

export type GraphEdgeKind = "normal" | "decisionTrue" | "decisionFalse";

export type GraphEdge = {
  id: string;
  from: string;
  to: string;
  kind: GraphEdgeKind;
  label?: string;
  isBranch: boolean;
};

export type WorkflowGraphModel = {
  nodes: GraphNode[];
  edges: GraphEdge[];
  width: number;
  height: number;
};

export function nodeStatusFromHistory(
  stepId: string,
  currentStepId: string | undefined,
  history: WorkflowHistoryRecord[]
): { status: NodeStatus; timestamp?: string } {
  if (currentStepId && currentStepId === stepId) {
    const latest = [...history].reverse().find((h) => h.stepId === stepId);
    return { status: "CURRENT", timestamp: latest?.createdAt };
  }

  const records = history.filter((h) => h.stepId === stepId);
  const latest = records[records.length - 1];
  if (!latest) {
    return { status: "PENDING" };
  }
  if (latest.status === "ROLLED_BACK") {
    return { status: "ROLLED_BACK", timestamp: latest.createdAt };
  }
  if (latest.status === "FAILED") {
    return { status: "FAILED", timestamp: latest.createdAt };
  }
  return { status: "COMPLETED", timestamp: latest.createdAt };
}

export function summarizePayload(raw?: string): string {
  if (!raw) return "No payload";
  try {
    const parsed = JSON.parse(raw);
    const compact = JSON.stringify(parsed);
    return compact.length > 180 ? `${compact.slice(0, 180)}...` : compact;
  } catch {
    return raw.length > 180 ? `${raw.slice(0, 180)}...` : raw;
  }
}

export function toLabel(step: WorkflowStep): string {
  return step.name || step.id;
}

export function getEntryStep(definition: WorkflowDefinition): WorkflowStep | undefined {
  if (definition.steps.length === 0) return undefined;
  const incoming = new Set<string>();
  for (const step of definition.steps) {
    if (step.next) incoming.add(step.next);
    (step.conditions || []).forEach((c) => incoming.add(c.next));
  }
  return definition.steps.find((step) => !incoming.has(step.id)) || definition.steps[0];
}
