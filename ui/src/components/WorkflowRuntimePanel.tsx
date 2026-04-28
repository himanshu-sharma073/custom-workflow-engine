import React, { useEffect, useState } from "react";
import { fetchWorkflowEvents, WorkflowRuntimeEvent } from "../api/tasks";

export function WorkflowRuntimePanel({ workflowId }: { workflowId: string }) {
  const [events, setEvents] = useState<WorkflowRuntimeEvent[]>([]);

  useEffect(() => {
    fetchWorkflowEvents(workflowId).then(setEvents).catch(() => setEvents([]));
  }, [workflowId]);

  const apiEvents = events.filter(e => e.eventType.startsWith("API_TASK"));
  const delayEvents = events.filter(e => e.eventType.startsWith("DELAY_"));
  const waitingEvents = events.filter(e => e.eventType.startsWith("EVENT_"));

  return (
    <div style={{ marginTop: 10 }}>
      <h4 style={{ marginBottom: 6 }}>Workflow Runtime Timeline</h4>
      <div style={{ display: "flex", gap: 8, flexWrap: "wrap", marginBottom: 8 }}>
        <span className="pill">API calls: {apiEvents.length}</span>
        <span className="pill">Delay timers: {delayEvents.length}</span>
        <span className="pill">Event waits: {waitingEvents.length}</span>
      </div>
      {events.length === 0 ? (
        <p className="subtle">No runtime events captured yet.</p>
      ) : (
        <ul className="list">
          {events.map((e, idx) => (
            <li key={idx} className="listItem" style={{ display: "block" }}>
              <div><strong>{e.eventType}</strong> <span className="subtle">@ {new Date(e.createdAt).toLocaleString()}</span></div>
              <pre style={{ whiteSpace: "pre-wrap", marginTop: 6, background: "#f8fafc", padding: 8, borderRadius: 8 }}>{e.payload}</pre>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
