import React, { useMemo, useState } from "react";
import { WorkflowDefinition, WorkflowHistoryRecord } from "../../api/tasks";
import { buildWorkflowGraphModel } from "./layout";
import { BranchConnector } from "./BranchConnector";
import { StepConnector } from "./StepConnector";
import { GraphNode } from "./types";
import { StepNode } from "./StepNode";

const NODE_W = 170;

export const WorkflowGraph = React.memo(function WorkflowGraph({
  definition,
  history,
  currentStepId,
  onStepSelect,
  onRollbackStep
}: {
  definition: WorkflowDefinition;
  history: WorkflowHistoryRecord[];
  currentStepId?: string;
  onStepSelect?: (node: GraphNode) => void;
  onRollbackStep?: (node: GraphNode) => void;
}) {
  const [selectedId, setSelectedId] = useState<string | undefined>(currentStepId);

  const model = useMemo(
    () => buildWorkflowGraphModel(definition, currentStepId, history),
    [definition, currentStepId, history]
  );

  const byId = useMemo(() => new Map(model.nodes.map((n) => [n.id, n])), [model.nodes]);
  const displayNodes = model.nodes.length > 50 ? model.nodes.slice(0, 50) : model.nodes;

  return (
    <div className="wg-shell">
      <svg width={model.width} height={model.height} className="wg-svg">
        <defs>
          <marker id="wgArrow" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="6" markerHeight="6" orient="auto-start-reverse">
            <path d="M 0 0 L 10 5 L 0 10 z" />
          </marker>
        </defs>
        {model.edges.map((edge) => {
          const from = byId.get(edge.from);
          const to = byId.get(edge.to);
          if (!from || !to) return null;
          const fromX = from.x + NODE_W / 2;
          const fromY = from.y;
          const toX = to.x - NODE_W / 2;
          const toY = to.y;
          return edge.isBranch ? (
            <BranchConnector key={edge.id} fromX={fromX} fromY={fromY} toX={toX} toY={toY} edge={edge} />
          ) : (
            <StepConnector key={edge.id} fromX={fromX} fromY={fromY} toX={toX} toY={toY} edge={edge} />
          );
        })}

        {displayNodes.map((node) => (
          <StepNode
            key={node.id}
            node={node}
            history={history}
            selected={selectedId === node.id}
            onSelect={(n) => {
              setSelectedId(n.id);
              onStepSelect?.(n);
            }}
            onRollback={onRollbackStep}
          />
        ))}
      </svg>
      {model.nodes.length > 50 ? (
        <div className="subtle" style={{ marginTop: 8 }}>
          Showing first 50 steps for performance. Refine workflow or add paging for full rendering.
        </div>
      ) : null}
    </div>
  );
});
