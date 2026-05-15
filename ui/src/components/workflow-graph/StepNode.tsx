import React from "react";
import { GraphNode, summarizePayload } from "./types";
import { WorkflowHistoryRecord } from "../../api/tasks";
import { NodeTypeGlyph, GLYPH_DEFAULT_SCALE } from "./NodeTypeGlyph";

const NODE_W = 170;
const NODE_H = 72;
/** Left column reserved for glyphs (narrow default, wide USER silhouette). */
const GLYPH_SLOT_DEFAULT = 24;
/** ~54px avatar column fits a ~2×24px USER icon centered vertically. */
const USER_AVATAR_COL_W = 54;

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
  subWorkflowExpandable,
  subWorkflowExpanded,
  onSubWorkflowToggle
}: {
  node: GraphNode;
  history: WorkflowHistoryRecord[];
  selected: boolean;
  onSelect: (node: GraphNode) => void;
  /** When set, show expand/collapse control for SUB_WORKFLOW (click does not select the node). */
  subWorkflowExpandable?: boolean;
  subWorkflowExpanded?: boolean;
  onSubWorkflowToggle?: (e: React.MouseEvent) => void;
}) {
  const x = node.x - NODE_W / 2;
  const y = node.y - NODE_H / 2;
  const record = [...history].reverse().find((h) => h.stepId === node.step.id);
  const title = `${node.label} (${node.type})
Status: ${node.status}${node.status === "CURRENT" ? " (workflow is here)" : ""}
When: ${node.timestamp || "-"}
Payload: ${summarizePayload(record?.payload)}`;

  const isCurrent = node.status === "CURRENT";
  const typeClass = `wg-node-type-${node.type.toLowerCase()}`;
  const nodeClass = `wg-node wg-node-${node.status.toLowerCase()} ${typeClass} ${node.type === "DECISION" ? "wg-node-decision" : ""} ${selected ? "wg-node-selected" : ""} ${isCurrent ? "wg-node-is-current" : ""}`;
  const systemPoints = [
    `${x + 14},${y}`,
    `${x + NODE_W - 14},${y}`,
    `${x + NODE_W},${y + NODE_H / 2}`,
    `${x + NODE_W - 14},${y + NODE_H}`,
    `${x + 14},${y + NODE_H}`,
    `${x},${y + NODE_H / 2}`
  ].join(" ");

  const isUserStep = node.type.toUpperCase() === "USER";

  const textX = isUserStep ? x + USER_AVATAR_COL_W + 6 : x + 8 + GLYPH_SLOT_DEFAULT;

  const glyphIxDefault = x + 8;

  let glyphIx: number;
  let glyphIy: number;

  if (node.type === "DECISION") {
    const s = GLYPH_DEFAULT_SCALE;
    glyphIx = node.x - 12 * s;
    glyphIy = y + 8;
  } else {
    glyphIx = glyphIxDefault;
    glyphIy = y + 8;
  }

  return (
    <g onClick={() => onSelect(node)} style={{ cursor: "pointer" }}>
      <title>{title}</title>
      {isCurrent ? (
        <g aria-hidden="true" pointerEvents="none" transform={`translate(${node.x},${y - 20})`}>
          <circle className="wg-current-badge-disk" cx={0} cy={0} r={12} />
          <path
            className="wg-current-badge-symbol"
            transform="translate(-12,-13) scale(0.85)"
            d="M13 10V3L4 14h9v7l9-11h-9z"
          />
        </g>
      ) : null}
      {node.type === "DECISION" ? (
        <polygon
          points={`${node.x},${y} ${node.x + NODE_W / 2},${node.y} ${node.x},${y + NODE_H} ${node.x - NODE_W / 2},${node.y}`}
          className={nodeClass}
        />
      ) : node.type === "SYSTEM" ? (
        <polygon points={systemPoints} className={nodeClass} />
      ) : (
        <rect x={x} y={y} width={NODE_W} height={NODE_H} rx={10} className={nodeClass} />
      )}
      {isUserStep ? (
        <foreignObject
          x={x}
          y={y}
          width={USER_AVATAR_COL_W}
          height={NODE_H}
          style={{ overflow: "visible", pointerEvents: "none" }}
        >
          <div
            xmlns="http://www.w3.org/1999/xhtml"
            style={{
              height: "100%",
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              pointerEvents: "none",
              boxSizing: "border-box",
              padding: "6px 0"
            }}
          >
            <i
              className={`fa-regular fa-user wg-step-user-fa-icon${isCurrent ? " wg-step-user-fa-icon--current" : ""}${selected ? " wg-step-user-fa-icon--selected" : ""}`}
              aria-hidden
            />
          </div>
        </foreignObject>
      ) : (
        <NodeTypeGlyph type={node.type} ix={glyphIx} iy={glyphIy} />
      )}
      <text x={textX} y={y + 22} className="wg-node-title">
        {node.label}
      </text>
      <text x={textX} y={y + 40} className="wg-node-subtitle">
        {node.type}
        {node.stage ? ` · ${node.stage}` : ""}
        {node.step.subWorkflowDefinitionId
          ? ` → ${
              node.step.subWorkflowDefinitionId.length > 20
                ? `${node.step.subWorkflowDefinitionId.slice(0, 18)}…`
                : node.step.subWorkflowDefinitionId
            }`
          : ""}
      </text>
      <text
        x={x + NODE_W - 20}
        y={y + 20}
        className={`wg-node-icon${isCurrent ? " wg-node-icon-current" : ""}`}
      >
        {statusIcon(node.status)}
      </text>
      {subWorkflowExpandable ? (
        <g
          transform={`translate(${x + NODE_W - 22}, ${y + NODE_H - 16})`}
          onClick={(e) => {
            e.stopPropagation();
            onSubWorkflowToggle?.(e);
          }}
          style={{ cursor: "pointer" }}
        >
          <title>{subWorkflowExpanded ? "Hide nested workflow" : "Show nested workflow"}</title>
          <rect x={-6} y={-12} width={24} height={20} fill="transparent" />
          <text x={0} y={4} className="wg-nested-expand-icon">
            {subWorkflowExpanded ? "▼" : "▶"}
          </text>
        </g>
      ) : null}
    </g>
  );
});
