import React from "react";
import { WorkflowDefinition, WorkflowHistoryRecord } from "../api/tasks";

type Point = { x: number; y: number };

function edgeList(def: WorkflowDefinition): Array<{ from: string; to: string; label?: string }> {
  const edges: Array<{ from: string; to: string; label?: string }> = [];
  for (const step of def.steps) {
    if (step.next) {
      edges.push({ from: step.id, to: step.next });
    }
    if (step.conditions) {
      for (const c of step.conditions) {
        edges.push({ from: step.id, to: c.next, label: c.expression });
      }
    }
  }
  return edges;
}

export function WorkflowDag({
  definition,
  currentStepId,
  history
}: {
  definition: WorkflowDefinition;
  currentStepId?: string;
  history?: WorkflowHistoryRecord[];
}) {
  const nodes = definition.steps;
  const edges = edgeList(definition);

  const width = 950;
  const rowHeight = 90;
  const nodeWidth = 200;
  const nodeHeight = 56;
  const paddingX = 60;
  const paddingY = 40;
  const height = Math.max(180, paddingY * 2 + nodes.length * rowHeight);

  const points = new Map<string, Point>();
  nodes.forEach((n, idx) => {
    points.set(n.id, { x: paddingX + (idx % 2) * 340 + 40, y: paddingY + idx * rowHeight });
  });

  const completedSteps = new Set(
    (history || [])
      .filter(h => h.status !== "ROLLED_BACK")
      .map(h => h.stepId)
  );

  const baseColor = (type: string) => {
    switch (type) {
      case "USER": return "#3b82f6";
      case "API": return "#a855f7";
      case "DECISION": return "#f59e0b";
      case "EVENT": return "#10b981";
      case "DELAY": return "#f97316";
      case "SYSTEM": return "#6366f1";
      default: return "#64748b";
    }
  };

  const statusStyle = (stepId: string, type: string) => {
    if (currentStepId && stepId === currentStepId) {
      return { fill: "#0ea5e9", stroke: "#0369a1", strokeWidth: 3 };
    }
    if (completedSteps.has(stepId)) {
      return { fill: "#16a34a", stroke: "#166534", strokeWidth: 2 };
    }
    return { fill: baseColor(type), stroke: "transparent", strokeWidth: 1 };
  };

  return (
    <div style={{ overflowX: "auto", border: "1px solid #e5e7eb", borderRadius: 8, padding: 8, marginTop: 12 }}>
      <svg width={width} height={height}>
        <defs>
          <marker id="arrow" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="6" markerHeight="6" orient="auto-start-reverse">
            <path d="M 0 0 L 10 5 L 0 10 z" fill="#6b7280" />
          </marker>
        </defs>

        {edges.map((e, idx) => {
          const from = points.get(e.from);
          const to = points.get(e.to);
          if (!from || !to) return null;
          const x1 = from.x + nodeWidth;
          const y1 = from.y + nodeHeight / 2;
          const x2 = to.x;
          const y2 = to.y + nodeHeight / 2;
          const cx = (x1 + x2) / 2;
          return (
            <g key={`${e.from}-${e.to}-${idx}`}>
              <path d={`M ${x1} ${y1} C ${cx} ${y1}, ${cx} ${y2}, ${x2} ${y2}`} fill="none" stroke="#6b7280" strokeWidth={1.5} markerEnd="url(#arrow)" />
              {e.label && <text x={cx} y={(y1 + y2) / 2 - 4} fontSize={11} fill="#374151">{e.label}</text>}
            </g>
          );
        })}

        {nodes.map((n) => {
          const p = points.get(n.id)!;
          const style = statusStyle(n.id, n.type);
          const done = completedSteps.has(n.id);
          const current = currentStepId === n.id;
          return (
            <g key={n.id}>
              <rect x={p.x} y={p.y} width={nodeWidth} height={nodeHeight} rx={8} fill={style.fill} stroke={style.stroke} strokeWidth={style.strokeWidth} opacity={0.95} />
              <text x={p.x + 10} y={p.y + 22} fill="white" fontSize={12} style={{ fontWeight: 700 }}>{n.id}</text>
              <text x={p.x + 10} y={p.y + 40} fill="white" fontSize={11}>{n.type}</text>
              {done && <text x={p.x + nodeWidth - 18} y={p.y + 18} fill="white" fontSize={16} style={{ fontWeight: 700 }}>?</text>}
              {current && <text x={p.x + nodeWidth - 64} y={p.y + 18} fill="white" fontSize={10} style={{ fontWeight: 700 }}>CURRENT</text>}
            </g>
          );
        })}
      </svg>
    </div>
  );
}
