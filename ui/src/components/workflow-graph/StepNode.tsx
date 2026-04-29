import React from "react";
import { GraphNode, summarizePayload } from "./types";
import { WorkflowHistoryRecord } from "../../api/tasks";

const NODE_W = 170;
const NODE_H = 72;

function statusIcon(status: GraphNode["status"]): string {
  switch (status) {
    case "COMPLETED":
      return "✓";
    case "CURRENT":
      return "●";
    case "FAILED":
      return "✕";
    case "ROLLED_BACK":
      return "↩";
    default:
      return "○";
  }
}

export const StepNode = React.memo(function StepNode({
  node,
  history,
  selected,
  onSelect,
  onRollback
}: {
  node: GraphNode;
  history: WorkflowHistoryRecord[];
  selected: boolean;
  onSelect: (node: GraphNode) => void;
  onRollback?: (node: GraphNode) => void;
}) {
  const x = node.x - NODE_W / 2;
  const y = node.y - NODE_H / 2;
  const record = [...history].reverse().find((h) => h.stepId === node.id);
  const title = `${node.label} (${node.type})
Status: ${node.status}
When: ${node.timestamp || "-"}
Payload: ${summarizePayload(record?.payload)}`;

  const nodeClass = `wg-node wg-node-${node.status.toLowerCase()} ${node.type === "DECISION" ? "wg-node-decision" : ""} ${selected ? "wg-node-selected" : ""}`;

  return (
    <g onClick={() => onSelect(node)} style={{ cursor: "pointer" }}>
      <title>{title}</title>
      {node.type === "DECISION" ? (
        <polygon
          points={`${node.x},${y} ${node.x + NODE_W / 2},${node.y} ${node.x},${y + NODE_H} ${node.x - NODE_W / 2},${node.y}`}
          className={nodeClass}
        />
      ) : (
        <rect x={x} y={y} width={NODE_W} height={NODE_H} rx={10} className={nodeClass} />
      )}
      <text x={x + 10} y={y + 22} className="wg-node-title">
        {node.label}
      </text>
      <text x={x + 10} y={y + 40} className="wg-node-subtitle">
        {node.type}{node.stage ? ` · ${node.stage}` : ""}
      </text>
      <text x={x + NODE_W - 20} y={y + 20} className="wg-node-icon">
        {statusIcon(node.status)}
      </text>
      {onRollback ? (
        <foreignObject x={x + NODE_W - 62} y={y + NODE_H - 26} width={56} height={22}>
          <button
            className="wg-node-action"
            onClick={(evt) => {
              evt.stopPropagation();
              onRollback(node);
            }}
          >
            Rollback
          </button>
        </foreignObject>
      ) : null}
    </g>
  );
});
