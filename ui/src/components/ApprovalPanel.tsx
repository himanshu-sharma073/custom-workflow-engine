import React, { useState } from "react";
import { approveTask, rejectTask, Task } from "../api/tasks";

export function ApprovalPanel({ task, onDone }: { task: Task; onDone: () => void }) {
  const [jsonInput, setJsonInput] = useState('{\n  "approved": true\n}');
  const [busy, setBusy] = useState(false);

  const parsed = () => {
    try { return JSON.parse(jsonInput); } catch { return {}; }
  };

  const run = async (action: "approve" | "reject") => {
    setBusy(true);
    try {
      if (action === "approve") await approveTask(task.id, parsed());
      else await rejectTask(task.id, parsed());
      await onDone();
    } finally {
      setBusy(false);
    }
  };

  return (
    <div style={{ marginTop: 10 }}>
      <h4 style={{ marginBottom: 8 }}>Approval Actions</h4>
      <textarea value={jsonInput} onChange={(e) => setJsonInput(e.target.value)} rows={6} />
      <div style={{ display: "flex", gap: 8, marginTop: 8 }}>
        <button className="primary" disabled={busy} onClick={() => run("approve")}>{busy ? "Working..." : "Approve"}</button>
        <button disabled={busy} onClick={() => run("reject")}>{busy ? "Working..." : "Reject"}</button>
      </div>
    </div>
  );
}
