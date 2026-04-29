import React from "react";
import { GraphEdge } from "./types";

export function BranchConnector({
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
  const elbowX = fromX + 32;
  const path = `M ${fromX} ${fromY} L ${elbowX} ${fromY} L ${elbowX} ${toY} L ${toX} ${toY}`;
  const className = edge.kind === "decisionTrue" ? "wg-edge wg-edge-true" : "wg-edge";

  return (
    <g>
      <path d={path} className={className} markerEnd="url(#wgArrow)" />
      {edge.label ? (
        <text x={elbowX + 8} y={Math.min(fromY, toY) - 8} className="wg-edge-label">
          {edge.label}
        </text>
      ) : null}
    </g>
  );
}
