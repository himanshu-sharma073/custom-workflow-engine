import React, { useEffect, useMemo, useState } from "react";
import { fetchWorkflowEvents, WorkflowHistoryRecord, WorkflowRuntimeEvent } from "../api/tasks";

function formatJsonPayload(raw: string): string {
  if (!raw || raw.trim() === "") return "(empty)";
  try {
    const v = JSON.parse(raw);
    return JSON.stringify(v, null, 2);
  } catch {
    return raw;
  }
}

type TimelineRow =
  | { kind: "execution"; key: string; createdAt: string; title: string; subtitle: string; payload: string }
  | { kind: "audit"; key: string; createdAt: string; title: string; subtitle: string; payload: string };

export function WorkflowRuntimePanel({
  workflowId,
  history,
  defaultExpanded = false
}: {
  workflowId: string;
  /** Step execution rows (preferred — includes workflow context snapshots). */
  history?: WorkflowHistoryRecord[];
  /** When false, timeline shows only a compact tab row until expanded (default minimized). */
  defaultExpanded?: boolean;
}) {
  const [auditEvents, setAuditEvents] = useState<WorkflowRuntimeEvent[]>([]);
  const [expanded, setExpanded] = useState(defaultExpanded);

  useEffect(() => {
    setExpanded(defaultExpanded);
  }, [workflowId, defaultExpanded]);

  useEffect(() => {
    fetchWorkflowEvents(workflowId).then(setAuditEvents).catch(() => setAuditEvents([]));
  }, [workflowId]);

  const timeline = useMemo(() => {
    const rows: TimelineRow[] = [];
    let seq = 0;
    const historyList = history ?? [];
    for (const h of historyList) {
      const key = `${h.workflowId}:${h.createdAt}:${h.stepId}:${h.status}:${seq++}`;
      rows.push({
        kind: "execution",
        key,
        createdAt: h.createdAt,
        title: `${h.stepId}`,
        subtitle: h.status,
        payload: h.payload
      });
    }
    let aseq = 0;
    for (const e of auditEvents) {
      rows.push({
        kind: "audit",
        key: `audit:${e.createdAt}:${e.eventType}:${aseq++}`,
        createdAt: e.createdAt,
        title: e.eventType,
        subtitle: "Task audit",
        payload: e.payload
      });
    }
    rows.sort((a, b) => new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime());
    return rows;
  }, [history, auditEvents, workflowId]);

  const apiCallCount = useMemo(
    () => (history ?? []).filter((h) => h.status.startsWith("API")).length,
    [history]
  );
  const delayCount = useMemo(
    () => (history ?? []).filter((h) => h.status.startsWith("DELAY")).length,
    [history]
  );
  const eventWaitCount = useMemo(
    () => (history ?? []).filter((h) => h.status.startsWith("EVENT")).length,
    [history]
  );
  const subWorkflowCount = useMemo(
    () => (history ?? []).filter((h) => h.status.includes("SUB_WORKFLOW")).length,
    [history]
  );

  const timelineCount = timeline.length;
  const summaryEmpty = timelineCount === 0;

  return (
    <div className="rt-timeline">
      <button
        type="button"
        className="rt-timeline__tab"
        aria-expanded={expanded}
        aria-controls={`rt-timeline-body-${workflowId}`}
        id={`rt-timeline-tab-${workflowId}`}
        onClick={() => setExpanded((e) => !e)}
      >
        <span className="rt-timeline__tab-label">
          <span className="rt-timeline__chevron" aria-hidden>
            {expanded ? "▼" : "▶"}
          </span>
          Workflow Runtime Timeline
        </span>
        <span className="rt-timeline__tab-meta">
          {summaryEmpty ? (
            <span className="subtle">No events yet</span>
          ) : (
            <>
              <span className="pill rt-timeline__pill">{timelineCount} event{timelineCount === 1 ? "" : "s"}</span>
              {!expanded ? (
                <span className="subtle rt-timeline__tab-hint">{apiCallCount} API · {auditEvents.length} audits</span>
              ) : null}
            </>
          )}
        </span>
      </button>

      {expanded ? (
        <div
          className="rt-timeline__body"
          id={`rt-timeline-body-${workflowId}`}
          role="region"
          aria-labelledby={`rt-timeline-tab-${workflowId}`}
        >
          <div style={{ display: "flex", gap: 8, flexWrap: "wrap", marginBottom: 8 }}>
            <span className="pill">Execution rows: {(history ?? []).length}</span>
            <span className="pill">API steps: {apiCallCount}</span>
            <span className="pill">Delay waits: {delayCount}</span>
            <span className="pill">Event waits: {eventWaitCount}</span>
            <span className="pill">Sub-workflow steps: {subWorkflowCount}</span>
            <span className="pill">Task audits: {auditEvents.length}</span>
          </div>
          {summaryEmpty ? (
            <p className="subtle">No timeline data yet — run the workflow or open an instance that has history.</p>
          ) : (
            <ul className="list">
              {timeline.map((row) => (
                <li key={row.key} className="listItem" style={{ display: "block" }}>
                  <div>
                    <strong>{row.title}</strong>
                    <span className="subtle">
                      {" "}
                      ({row.kind === "execution" ? "step" : row.subtitle}) @ {new Date(row.createdAt).toLocaleString()}
                    </span>
                  </div>
                  {row.kind === "execution" ? (
                    <div className="subtle" style={{ marginTop: 2 }}>
                      Status: <code>{row.subtitle}</code>
                    </div>
                  ) : null}
                  <pre
                    style={{
                      whiteSpace: "pre-wrap",
                      marginTop: 6,
                      background: "#f8fafc",
                      padding: 8,
                      borderRadius: 8,
                      fontSize: 12
                    }}
                  >
                    {formatJsonPayload(row.payload)}
                  </pre>
                </li>
              ))}
            </ul>
          )}
        </div>
      ) : null}
    </div>
  );
}
