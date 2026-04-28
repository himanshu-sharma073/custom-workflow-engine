import React, { useEffect, useState } from "react";
import { Approval, fetchApprovals } from "../api/tasks";

export function ApprovalHistory({ taskId }: { taskId: string }) {
  const [history, setHistory] = useState<Approval[]>([]);

  useEffect(() => {
    fetchApprovals(taskId).then(setHistory).catch(() => setHistory([]));
  }, [taskId]);

  return (
    <div style={{ marginTop: 10 }}>
      <h4 style={{ marginBottom: 8 }}>Approval History</h4>
      {history.length === 0 ? (
        <p className="subtle">No approvals recorded yet.</p>
      ) : (
        <ul className="list">
          {history.map((h, idx) => (
            <li key={idx} className="listItem">
              <div>
                <div><strong>{h.userId}</strong> · {h.decision}</div>
                <div className="subtle">{new Date(h.timestamp).toLocaleString()}</div>
              </div>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
