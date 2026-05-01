import React, { useEffect, useMemo, useState } from "react";
import {
  claimTask,
  fetchCurrentUser,
  fetchDefinition,
  fetchDefinitions,
  fetchTasks,
  fetchWorkflow,
  fetchWorkflowHistory,
  fetchWorkflows,
  rollbackWorkflow,
  Task,
  WorkflowDefinition,
  WorkflowHistoryRecord,
  WorkflowState
} from "../api/tasks";
import { ApprovalPanel } from "../components/ApprovalPanel";
import { ApprovalHistory } from "../components/ApprovalHistory";
import { WorkflowRuntimePanel } from "../components/WorkflowRuntimePanel";
import { WorkflowDag } from "../components/WorkflowDag";
import { GraphNode, nodeStatusFromHistory } from "../components/workflow-graph/types";

export function TaskDashboardPage() {
  const [tasks, setTasks] = useState<Task[]>([]);
  const [workflows, setWorkflows] = useState<WorkflowState[]>([]);
  const [definitions, setDefinitions] = useState<WorkflowDefinition[]>([]);
  const [workflowHistory, setWorkflowHistory] = useState<WorkflowHistoryRecord[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [selectedTask, setSelectedTask] = useState<Task | null>(null);
  const [selectedWorkflow, setSelectedWorkflow] = useState<WorkflowState | null>(null);
  const [selectedDefinition, setSelectedDefinition] = useState<WorkflowDefinition | null>(null);
  const [query, setQuery] = useState("");
  const [selectedGraphNode, setSelectedGraphNode] = useState<GraphNode | null>(null);
  const [rollbackTargetStepId, setRollbackTargetStepId] = useState("");
  const [currentUserName, setCurrentUserName] = useState<string | null>(null);

  const load = async () => {
    try {
      setError(null);
      const [taskData, workflowData, definitionData] = await Promise.all([fetchTasks(), fetchWorkflows(), fetchDefinitions()]);
      try {
        const session = await fetchCurrentUser();
        setCurrentUserName(session.userName);
      } catch {
        setCurrentUserName(null);
      }
      setTasks(taskData);
      if (selectedTask) {
        const refreshedSelectedTask = taskData.find((t) => t.id === selectedTask.id) || null;
        setSelectedTask(refreshedSelectedTask);
      } else if (taskData.length > 0) {
        setSelectedTask(taskData[0]);
      }

      setWorkflows(workflowData);
      setDefinitions(definitionData);

      if (selectedWorkflow) {
        const freshWorkflow = await fetchWorkflow(selectedWorkflow.workflowId);
        setSelectedWorkflow(freshWorkflow);

        const history = await fetchWorkflowHistory(freshWorkflow.workflowId);
        setWorkflowHistory(history);

        if (!selectedDefinition || selectedDefinition.id !== freshWorkflow.definitionId) {
          setSelectedDefinition(await fetchDefinition(freshWorkflow.definitionId));
        }
      }

      if (selectedDefinition && !selectedWorkflow) {
        setSelectedDefinition(await fetchDefinition(selectedDefinition.id));
      }
    } catch (e: any) {
      setError(e?.message || "Failed to load workflow data from server APIs.");
    }
  };

  useEffect(() => { load(); }, []);

  const myTasks = useMemo(() => tasks.filter(t => !!t.claimedBy), [tasks]);
  const candidateTasks = useMemo(() => tasks.filter(t => !t.claimedBy), [tasks]);

  const filteredDefinitions = useMemo(
    () => definitions.filter(d => d.id.toLowerCase().includes(query.toLowerCase())),
    [definitions, query]
  );

  const stepTypeStats = useMemo(() => {
    if (!selectedDefinition) return [] as Array<[string, number]>;
    const map = new Map<string, number>();
    selectedDefinition.steps.forEach(s => map.set(s.type, (map.get(s.type) ?? 0) + 1));
    return [...map.entries()];
  }, [selectedDefinition]);

  /** Steps that progressed past execution (matches DAG “completed”; raw history uses USER_WAITING / SYSTEM_COMPLETED / API_SUCCESS etc.) */
  const completedRollbackSteps = useMemo(() => {
    if (!selectedWorkflow || !selectedDefinition) return [] as string[];

    const cur = selectedWorkflow.currentStep;
    const items: string[] = [];
    for (const s of selectedDefinition.steps) {
      const { status } = nodeStatusFromHistory(s.id, cur, workflowHistory);
      if (status === "COMPLETED") {
        items.push(s.id);
      }
    }
    return items;
  }, [selectedWorkflow, selectedDefinition, workflowHistory]);

  useEffect(() => {
    if (completedRollbackSteps.length === 0) {
      setRollbackTargetStepId("");
      return;
    }
    setRollbackTargetStepId(completedRollbackSteps[completedRollbackSteps.length - 1]);
  }, [completedRollbackSteps]);

  const openWorkflow = async (workflowId: string) => {
    const workflow = await fetchWorkflow(workflowId);
    setSelectedWorkflow(workflow);
    setSelectedDefinition(await fetchDefinition(workflow.definitionId));
    setWorkflowHistory(await fetchWorkflowHistory(workflow.workflowId));
  };

  return (
    <div className="dashboard">
      <div className="pageHeader">
        <div>
          <h1 style={{ margin: 0 }}>Workflow Dashboard</h1>
          <p className="subtle" style={{ marginTop: 4 }}>Interactive view of definitions, runtime instances, DAG, and human tasks.</p>
        </div>
        <div style={{ display: "flex", alignItems: "center", gap: 16 }}>
          {currentUserName ? (
            <span className="dashboard-user-pill" title="Current user (from workflow engine session)">
              <i className="fa-regular fa-user dashboard-user-pill__icon" aria-hidden />
              <span className="dashboard-user-pill__name">{currentUserName}</span>
            </span>
          ) : null}
          <div style={{ display: "flex", gap: 8 }}>
            {selectedDefinition && <button onClick={() => setSelectedDefinition(null)}>Close DAG View</button>}
            <button className="primary" onClick={load}>Refresh</button>
          </div>
        </div>
      </div>

      {error && <div className="error">API error: {error}</div>}

      <div className="kpiRow">
        <div className="kpiCard"><div className="kpiLabel">Definitions</div><div className="kpiValue">{definitions.length}</div></div>
        <div className="kpiCard"><div className="kpiLabel">Running Workflows</div><div className="kpiValue">{workflows.length}</div></div>
        <div className="kpiCard"><div className="kpiLabel">My Tasks</div><div className="kpiValue">{myTasks.length}</div></div>
        <div className="kpiCard"><div className="kpiLabel">Candidate Tasks</div><div className="kpiValue">{candidateTasks.length}</div></div>
      </div>

      <div className="grid">
        <div className="card">
          <h2 className="sectionTitle">Workflow Definitions</h2>
          <input
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Filter definitions..."
            style={{ width: "100%", padding: 8, borderRadius: 8, border: "1px solid #cbd5e1", marginBottom: 8 }}
          />
          {filteredDefinitions.length === 0 ? <p className="subtle">No matching definitions.</p> : (
            <ul className="list">
              {filteredDefinitions.map(d => (
                <li key={d.id} className={`listItem ${selectedDefinition?.id === d.id ? "active" : ""}`}>
                  <div>
                    <div>{d.id}</div>
                    <div className="subtle">v{d.version ?? 1} | steps: {d.steps.length}</div>
                  </div>
                  <button
                    onClick={async () => {
                      setSelectedWorkflow(null);
                      setWorkflowHistory([]);
                      setSelectedGraphNode(null);
                      setSelectedDefinition(await fetchDefinition(d.id));
                    }}
                  >
                    Open
                  </button>
                </li>
              ))}
            </ul>
          )}
        </div>

        <div className="card">
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 8 }}>
            <h2 className="sectionTitle" style={{ margin: 0 }}>Definition Detail</h2>
            {selectedDefinition && <button onClick={() => setSelectedDefinition(null)}>Close</button>}
          </div>
          {!selectedDefinition ? <p className="subtle">Select a workflow definition to view DAG and metadata.</p> : (
            <>
              <div className="twoCol">
                <div>
                  <div><strong>ID:</strong> {selectedDefinition.id}</div>
                  <div><strong>Version:</strong> {selectedDefinition.version ?? 1}</div>
                  <div><strong>Total Steps:</strong> {selectedDefinition.steps.length}</div>
                </div>
                <div>
                  <strong>Step Types</strong>
                  <div style={{ marginTop: 6, display: "flex", flexWrap: "wrap", gap: 6 }}>
                    {stepTypeStats.map(([t, c]) => <span className="pill" key={t}>{t}: {c}</span>)}
                  </div>
                </div>
              </div>
              <WorkflowDag
                definition={selectedDefinition}
                currentStepId={selectedWorkflow?.currentStep}
                history={workflowHistory}
                onStepSelect={(node) => setSelectedGraphNode(node)}
              />
              {selectedGraphNode ? (
                <div style={{ marginTop: 10, borderTop: "1px solid #dbeafe", paddingTop: 8 }}>
                  <div><strong>Selected Step:</strong> {selectedGraphNode.label}</div>
                  <div className="subtle">
                    {selectedGraphNode.id} | {selectedGraphNode.type} | {selectedGraphNode.status}
                    {selectedGraphNode.stage ? ` | ${selectedGraphNode.stage}` : ""}
                  </div>
                </div>
              ) : null}
            </>
          )}
        </div>
      </div>

      <div className="grid" style={{ marginTop: 12 }}>
        <div className="card">
          <h2 className="sectionTitle">Running Workflows</h2>
          {workflows.length === 0 ? <p className="subtle">No workflow instances running.</p> : (
            <ul className="list">
              {workflows.map(w => (
                <li key={w.workflowId} className={`listItem ${selectedWorkflow?.workflowId === w.workflowId ? "active" : ""}`}>
                  <div>
                    <div className="mono">{w.workflowId}</div>
                    <div className="subtle">{w.definitionId} | {w.status}</div>
                  </div>
                  <button onClick={async () => openWorkflow(w.workflowId)}>Inspect</button>
                </li>
              ))}
            </ul>
          )}
        </div>

        <div className="card">
          <h2 className="sectionTitle">Workflow Instance Detail</h2>
          {!selectedWorkflow ? <p className="subtle">Select a running workflow to inspect timeline and rollback.</p> : (
            <>
              <div className="twoCol">
                <div>
                  <div><strong>ID:</strong> <span className="mono">{selectedWorkflow.workflowId}</span></div>
                  <div><strong>Definition:</strong> {selectedWorkflow.definitionId}</div>
                </div>
                <div>
                  <div><strong>Status:</strong> {selectedWorkflow.status}</div>
                  <div><strong>Current Step:</strong> {selectedWorkflow.currentStep || "-"}</div>
                </div>
              </div>
              <div style={{ marginTop: 8 }}>
                <div style={{ display: "flex", gap: 8, alignItems: "center", flexWrap: "wrap" }}>
                  <label htmlFor="rollbackTargetStep"><strong>Rollback to completed step:</strong></label>
                  <select
                    id="rollbackTargetStep"
                    value={rollbackTargetStepId}
                    onChange={(e) => setRollbackTargetStepId(e.target.value)}
                    disabled={completedRollbackSteps.length === 0}
                  >
                    {completedRollbackSteps.length === 0 ? (
                      <option value="">No completed steps</option>
                    ) : (
                      completedRollbackSteps.map((stepId) => (
                        <option key={stepId} value={stepId}>
                          {stepId}
                        </option>
                      ))
                    )}
                  </select>
                  <button
                    disabled={!rollbackTargetStepId}
                    onClick={async () => {
                      if (!rollbackTargetStepId) return;
                      const confirmed = window.confirm(`Rollback workflow to "${rollbackTargetStepId}"?`);
                      if (!confirmed) return;
                      await rollbackWorkflow(selectedWorkflow.workflowId, rollbackTargetStepId);
                      await load();
                    }}
                  >
                    Rollback Workflow
                  </button>
                </div>
              </div>
              <WorkflowRuntimePanel workflowId={selectedWorkflow.workflowId} history={workflowHistory} />
            </>
          )}
        </div>
      </div>

      <div className="grid" style={{ marginTop: 12 }}>
        <div className="card">
          <h2 className="sectionTitle">My Tasks</h2>
          {myTasks.length === 0 ? <p className="subtle">No tasks currently assigned to you.</p> : (
            <ul className="list">
              {myTasks.map(t => (
                <li key={t.id} className={`listItem ${selectedTask?.id === t.id ? "active" : ""}`}>
                  <div>
                    <div>{t.stepId}</div>
                    <div className="subtle mono">{t.id}</div>
                  </div>
                  <button onClick={() => setSelectedTask(t)}>Open</button>
                </li>
              ))}
            </ul>
          )}

          <h3 style={{ marginTop: 14, marginBottom: 8 }}>Candidate Tasks</h3>
          {candidateTasks.length === 0 ? <p className="subtle">No candidate tasks available.</p> : (
            <ul className="list">
              {candidateTasks.map(t => (
                <li key={t.id} className={`listItem ${selectedTask?.id === t.id ? "active" : ""}`}>
                  <div>
                    <div>{t.stepId}</div>
                    <div className="subtle">{t.assignmentType}:{t.assignmentValue}</div>
                  </div>
                  <div style={{ display: "flex", gap: 6 }}>
                    <button
                      onClick={async () => {
                        const claimed = await claimTask(t.id);
                        await load();
                        setSelectedTask(claimed);
                      }}
                    >
                      Claim & Open
                    </button>
                    <button onClick={() => setSelectedTask(t)}>Open</button>
                  </div>
                </li>
              ))}
            </ul>
          )}
        </div>

        <div className="card">
          <h2 className="sectionTitle">Task Detail</h2>
          {!selectedTask ? <p className="subtle">Select a task to approve or reject.</p> : (
            <>
              <div><strong>Task ID:</strong> <span className="mono">{selectedTask.id}</span></div>
              <div><strong>Step:</strong> {selectedTask.stepId}</div>
              <div><strong>Status:</strong> {selectedTask.status}</div>
              {!selectedTask.claimedBy ? (
                <div style={{ marginTop: 10 }}>
                  <p className="subtle">Claim this task to take approval action.</p>
                  <button
                    onClick={async () => {
                      const claimed = await claimTask(selectedTask.id);
                      await load();
                      setSelectedTask(claimed);
                    }}
                  >
                    Claim This Task
                  </button>
                </div>
              ) : null}
              {selectedTask.claimedBy ? <ApprovalPanel task={selectedTask} onDone={load} /> : null}
              <ApprovalHistory taskId={selectedTask.id} />
            </>
          )}
        </div>
      </div>
    </div>
  );
}
