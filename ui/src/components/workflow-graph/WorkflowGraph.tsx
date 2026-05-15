import React, { useEffect, useMemo, useState } from "react";
import { WorkflowDefinition, WorkflowHistoryRecord } from "../../api/tasks";
import { buildWorkflowGraphModel } from "./layout";
import { BranchConnector } from "./BranchConnector";
import { StepConnector } from "./StepConnector";
import { getEntryStep, GraphNode, WorkflowGraphInstance } from "./types";
import { StepNode } from "./StepNode";

const NODE_W = 170;
const NODE_H = 72;
const NEST_GAP = 32;
const OUTER = 72;

type NestedLayer = {
  parentStepId: string;
  ox: number;
  oy: number;
  model: ReturnType<typeof buildWorkflowGraphModel>;
};

function boxesOverlap(
  a: { left: number; top: number; right: number; bottom: number },
  b: { left: number; top: number; right: number; bottom: number },
  pad: number
): boolean {
  return !(a.right < b.left - pad || a.left > b.right + pad || a.bottom < b.top - pad || a.top > b.bottom + pad);
}

export const WorkflowGraph = React.memo(function WorkflowGraph({
  definition,
  history,
  currentStepId,
  workflow,
  onStepSelect,
  definitionLookup,
  nestedRuntime,
  allowSubWorkflowExpand = true
}: {
  definition: WorkflowDefinition;
  history: WorkflowHistoryRecord[];
  currentStepId?: string;
  /** Live instance snapshot so WAITING SUB_WORKFLOW is not derived from stale history alone. */
  workflow?: WorkflowGraphInstance;
  onStepSelect?: (node: GraphNode) => void;
  definitionLookup?: Map<string, WorkflowDefinition>;
  nestedRuntime?: {
    parentStepId: string;
    currentStepId?: string;
    history: WorkflowHistoryRecord[];
  } | null;
  allowSubWorkflowExpand?: boolean;
}) {
  const [selectedId, setSelectedId] = useState<string | undefined>(currentStepId);
  const [expandedSwSteps, setExpandedSwSteps] = useState<string[]>([]);

  useEffect(() => {
    setExpandedSwSteps([]);
  }, [definition.id]);

  const model = useMemo(
    () => buildWorkflowGraphModel(definition, currentStepId, history, { workflow }),
    [definition, currentStepId, history, workflow]
  );

  const byId = useMemo(() => new Map(model.nodes.map((n) => [n.id, n])), [model.nodes]);

  const nestedLayers = useMemo(() => {
    if (!definitionLookup || expandedSwSteps.length === 0) return [] as NestedLayer[];

    const sorted = [...expandedSwSteps].sort((a, b) => {
      const na = byId.get(a);
      const nb = byId.get(b);
      if (!na || !nb) return 0;
      if (na.y !== nb.y) return na.y - nb.y;
      return na.x - nb.x;
    });

    const layers: NestedLayer[] = [];
    const placed: Array<{ left: number; top: number; right: number; bottom: number }> = [];

    for (const parentStepId of sorted) {
      const parentNode = byId.get(parentStepId);
      const step = definition.steps.find((s) => s.id === parentStepId);
      if (!parentNode || !step?.subWorkflowDefinitionId) continue;
      const childDef = definitionLookup.get(step.subWorkflowDefinitionId);
      if (!childDef) continue;

      const prefix = `nested:${parentStepId}:`;
      const rt =
        nestedRuntime && nestedRuntime.parentStepId === parentStepId
          ? nestedRuntime
          : { parentStepId, currentStepId: undefined as string | undefined, history: [] as WorkflowHistoryRecord[] };

      const childModel = buildWorkflowGraphModel(childDef, rt.currentStepId, rt.history, { nodeIdPrefix: prefix });

      let ox = Math.max(OUTER, parentNode.x - childModel.width / 2);
      let oy = parentNode.y + NODE_H / 2 + NEST_GAP;

      const box = (x: number, y: number) => ({
        left: x,
        top: y,
        right: x + childModel.width,
        bottom: y + childModel.height
      });

      let b = box(ox, oy);
      let guard = 0;
      while (placed.some((o) => boxesOverlap(b, o, 12)) && guard < 80) {
        oy += 40;
        b = box(ox, oy);
        guard += 1;
      }
      placed.push(b);
      layers.push({ parentStepId, ox, oy, model: childModel });
    }
    return layers;
  }, [model, definition, definitionLookup, expandedSwSteps, nestedRuntime, byId]);

  const canvasSize = useMemo(() => {
    let w = model.width;
    let h = model.height;
    for (const layer of nestedLayers) {
      w = Math.max(w, layer.ox + layer.model.width + OUTER);
      h = Math.max(h, layer.oy + layer.model.height + OUTER);
    }
    return { width: w, height: h };
  }, [model, nestedLayers]);

  const toggleSwExpand = (stepId: string) => {
    setExpandedSwSteps((prev) => (prev.includes(stepId) ? prev.filter((x) => x !== stepId) : [...prev, stepId]));
  };

  const displayNodes = model.nodes.length > 50 ? model.nodes.slice(0, 50) : model.nodes;

  const renderEdgeGroup = (gModel: typeof model, gById: Map<string, GraphNode>, keyPrefix: string) =>
    gModel.edges.map((edge) => {
      const from = gById.get(edge.from);
      const to = gById.get(edge.to);
      if (!from || !to) return null;
      const fromX = from.x + NODE_W / 2;
      const fromY = from.y;
      const toX = to.x - NODE_W / 2;
      const toY = to.y;
      return edge.isBranch ? (
        <BranchConnector key={`${keyPrefix}${edge.id}`} fromX={fromX} fromY={fromY} toX={toX} toY={toY} edge={edge} />
      ) : (
        <StepConnector key={`${keyPrefix}${edge.id}`} fromX={fromX} fromY={fromY} toX={toX} toY={toY} edge={edge} />
      );
    });

  return (
    <div className="wg-shell">
      <svg width={canvasSize.width} height={canvasSize.height} className="wg-svg">
        <defs>
          <marker id="wgArrow" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="6" markerHeight="6" orient="auto-start-reverse">
            <path d="M 0 0 L 10 5 L 0 10 z" />
          </marker>
        </defs>

        {renderEdgeGroup(model, byId, "p-")}

        {nestedLayers.map((layer) => {
          const pn = byId.get(layer.parentStepId);
          if (!pn) return null;
          const pStep = definition.steps.find((s) => s.id === layer.parentStepId);
          const childDef = pStep?.subWorkflowDefinitionId ? definitionLookup?.get(pStep.subWorkflowDefinitionId) : undefined;
          const entry = childDef ? getEntryStep(childDef) : undefined;
          const entryNode = entry ? layer.model.nodes.find((n) => n.step.id === entry.id) : undefined;
          const tx = entryNode ? entryNode.x + layer.ox : layer.ox + layer.model.width / 2;
          const ty = entryNode ? entryNode.y - NODE_H / 2 + layer.oy : layer.oy;
          const fromY = pn.y + NODE_H / 2;
          return (
            <path
              key={`nestlink-${layer.parentStepId}`}
              d={`M ${pn.x} ${fromY} C ${pn.x} ${fromY + 48}, ${tx} ${ty - 48}, ${tx} ${ty}`}
              className="wg-nested-link"
              fill="none"
              markerEnd="url(#wgArrow)"
            />
          );
        })}

        {nestedLayers.map((layer) => {
          const childById = new Map(layer.model.nodes.map((n) => [n.id, n]));
          return (
            <g key={`nestg-${layer.parentStepId}`} transform={`translate(${layer.ox},${layer.oy})`} className="wg-nested-layer">
              <rect
                x={-8}
                y={-12}
                width={layer.model.width + 16}
                height={layer.model.height + 20}
                rx={12}
                className="wg-nested-layer-bg"
              />
              {renderEdgeGroup(layer.model, childById, `n-${layer.parentStepId}-`)}
              {(layer.model.nodes.length > 50 ? layer.model.nodes.slice(0, 50) : layer.model.nodes).map((node) => (
                <StepNode
                  key={node.id}
                  node={node}
                  history={
                    nestedRuntime && nestedRuntime.parentStepId === layer.parentStepId ? nestedRuntime.history : []
                  }
                  selected={selectedId === node.id}
                  onSelect={(n) => {
                    setSelectedId(n.id);
                    onStepSelect?.(n);
                  }}
                  subWorkflowExpandable={false}
                />
              ))}
            </g>
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
            subWorkflowExpandable={
              allowSubWorkflowExpand &&
              node.step.type.toUpperCase() === "SUB_WORKFLOW" &&
              !!node.step.subWorkflowDefinitionId &&
              !!definitionLookup?.get(node.step.subWorkflowDefinitionId)
            }
            subWorkflowExpanded={expandedSwSteps.includes(node.id)}
            onSubWorkflowToggle={() => toggleSwExpand(node.id)}
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
