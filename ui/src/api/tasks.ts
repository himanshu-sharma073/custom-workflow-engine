export type AssignmentSpec = {
  type: string;
  value: string;
};

export type CandidateSpec = {
  type: string;
  value: string;
};

export type ApprovalSpec = {
  type: string;
  minApprovals?: number;
};

export type DecisionCondition = {
  expression: string;
  next: string;
};

export type WorkflowStep = {
  id: string;
  type: string;
  assignment?: AssignmentSpec;
  candidates?: CandidateSpec[];
  approval?: ApprovalSpec;
  next?: string;
  action?: string;
  conditions?: DecisionCondition[];
};

export type WorkflowDefinition = {
  id: string;
  version?: number;
  steps: WorkflowStep[];
};

export type Task = {
  id: string;
  workflowId: string;
  stepId: string;
  status: string;
  assignmentType: string;
  assignmentValue: string;
  claimedBy?: string;
};

export type Approval = {
  userId: string;
  decision: string;
  timestamp: string;
};

export type WorkflowRuntimeEvent = {
  eventType: string;
  payload: string;
  createdAt: string;
};

export type WorkflowState = {
  workflowId: string;
  definitionId: string;
  status: string;
  currentStep: string;
  context: Record<string, unknown>;
  updatedAt: string;
};

export type WorkflowHistoryRecord = {
  workflowId: string;
  stepId: string;
  status: string;
  payload: string;
  createdAt: string;
};

const apiBaseUrl = (import.meta as any).env?.VITE_API_BASE_URL?.trim?.() || "";
const base = `${apiBaseUrl}/workflows`;

async function requestJson(path: string, init?: RequestInit): Promise<any> {
  const res = await fetch(path, init);
  const contentType = res.headers.get("content-type") || "";
  if (!res.ok) {
    const body = contentType.includes("application/json") ? JSON.stringify(await res.json()) : await res.text();
    throw new Error(`HTTP ${res.status} for ${path}: ${body}`);
  }
  if (!contentType.includes("application/json")) {
    const body = await res.text();
    throw new Error(`Expected JSON for ${path}, got ${contentType || "unknown"}: ${body.slice(0, 160)}`);
  }
  return res.json();
}

function asArray<T>(value: unknown): T[] {
  return Array.isArray(value) ? (value as T[]) : [];
}

export async function fetchDefinitions(): Promise<WorkflowDefinition[]> {
  const data = await requestJson(`${base}/definitions`);
  return asArray<WorkflowDefinition>(data);
}

export async function fetchDefinition(definitionId: string): Promise<WorkflowDefinition> {
  const res = await fetch(`${base}/definitions/${definitionId}`);
  return res.json();
}

export async function fetchTasks(): Promise<Task[]> {
  const data = await requestJson(`${base}/tasks`);
  return asArray<Task>(data);
}

export async function fetchApprovals(taskId: string): Promise<Approval[]> {
  const data = await requestJson(`${base}/tasks/${taskId}/approvals`);
  return asArray<Approval>(data);
}

export async function claimTask(taskId: string): Promise<void> {
  await requestJson(`${base}/tasks/${taskId}/claim`, { method: "POST" });
}

export async function approveTask(taskId: string, input: Record<string, unknown>, approvalType = "ANY", minApprovals = 1): Promise<void> {
  await requestJson(`${base}/tasks/${taskId}/approve`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ input, approvalType, minApprovals })
  });
}

export async function rejectTask(taskId: string, input: Record<string, unknown>): Promise<void> {
  await requestJson(`${base}/tasks/${taskId}/reject`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ input, approvalType: "ANY", minApprovals: 1 })
  });
}

export async function fetchWorkflowEvents(workflowId: string): Promise<WorkflowRuntimeEvent[]> {
  const data = await requestJson(`${base}/${workflowId}/events`);
  return asArray<WorkflowRuntimeEvent>(data);
}

export async function fetchWorkflows(): Promise<WorkflowState[]> {
  const data = await requestJson(`${base}`);
  return asArray<WorkflowState>(data);
}

export async function fetchWorkflow(workflowId: string): Promise<WorkflowState> {
  return requestJson(`${base}/${workflowId}`);
}

export async function rollbackWorkflow(workflowId: string): Promise<WorkflowState> {
  return requestJson(`${base}/${workflowId}/rollback`, { method: "POST" });
}

export async function fetchWorkflowHistory(workflowId: string): Promise<WorkflowHistoryRecord[]> {
  const data = await requestJson(`${base}/${workflowId}/history`);
  return asArray<WorkflowHistoryRecord>(data);
}
