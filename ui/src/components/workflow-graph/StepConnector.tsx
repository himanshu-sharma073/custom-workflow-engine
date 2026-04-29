import React from "react";
import { GraphEdge } from "./types";

export function StepConnector({
  fromX,
  fromY,
  toX,
  toY,
  edge
}: {
  fromX: number;
  fromY: number;
  toX: number;
  toY: number;
  edge: GraphEdge;
}) {
  const midX = (fromX + toX) / 2;
  const path = `M ${fromX} ${fromY} C ${midX} ${fromY}, ${midX} ${toY}, ${toX} ${toY}`;
  const className = edge.kind === "decisionFalse" ? "wg-edge wg-edge-false" : "wg-edge";

  return (
    <g>
      <path d={path} className={className} markerEnd="url(#wgArrow)" />
      {edge.label ? (
        <text x={midX} y={(fromY + toY) / 2 - 6} className="wg-edge-label">
          {edge.label}
        </text>
      ) : null}
    </g>
  );
}
