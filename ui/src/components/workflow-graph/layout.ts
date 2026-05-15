import { WorkflowDefinition, WorkflowHistoryRecord } from "../../api/tasks";
import {
  getEntryStep,
  GraphEdge,
  GraphNode,
  nodeStatusFromHistory,
  toLabel,
  WorkflowGraphInstance,
  WorkflowGraphModel
} from "./types";

const X_GAP = 230;
const Y_GAP = 130;
const PADDING_X = 80;
const PADDING_Y = 90;
const NODE_HALF_W = 96;
const NODE_HALF_H = 44;
const OUTER_MARGIN = 72;

type Position = { x: number; y: number };

function buildEdges(definition: WorkflowDefinition, nodeIdPrefix = ""): GraphEdge[] {
  const pid = (id: string) => (nodeIdPrefix ? `${nodeIdPrefix}${id}` : id);
  const edges: GraphEdge[] = [];
  for (const step of definition.steps) {
    if (step.next) {
      edges.push({
        id: `${pid(step.id)}->${pid(step.next)}`,
        from: pid(step.id),
        to: pid(step.next),
        kind: "normal",
        isBranch: false
      });
    }
    (step.conditions || []).forEach((condition, idx) => {
      const label = condition.expression || `branch-${idx + 1}`;
      const lowered = label.toLowerCase();
      const kind = lowered.includes("true") ? "decisionTrue" : lowered.includes("false") ? "decisionFalse" : "normal";
      edges.push({
        id: `${pid(step.id)}->${pid(condition.next)}:${idx}`,
        from: pid(step.id),
        to: pid(condition.next),
        kind,
        label,
        isBranch: true
      });
    });
  }
  return edges;
}

function assignPositions(definition: WorkflowDefinition): Map<string, Position> {
  const stepsById = new Map(definition.steps.map((s) => [s.id, s]));
  const entry = getEntryStep(definition) || definition.steps[0];
  const positions = new Map<string, Position>();
  const queue: Array<{ id: string; x: number; y: number }> = [{ id: entry.id, x: 0, y: 0 }];
  const laneUseByX = new Map<number, Set<number>>();

  const reserveLane = (x: number, preferred: number): number => {
    const used = laneUseByX.get(x) || new Set<number>();
    if (!used.has(preferred)) {
      used.add(preferred);
      laneUseByX.set(x, used);
      return preferred;
    }
    for (let d = 1; d <= 8; d++) {
      for (const candidate of [preferred + d, preferred - d]) {
        if (!used.has(candidate)) {
          used.add(candidate);
          laneUseByX.set(x, used);
          return candidate;
        }
      }
    }
    used.add(preferred + 9);
    laneUseByX.set(x, used);
    return preferred + 9;
  };

  while (queue.length) {
    const current = queue.shift()!;
    const step = stepsById.get(current.id);
    if (!step) continue;

    if (!positions.has(step.id)) {
      positions.set(step.id, { x: current.x, y: reserveLane(current.x, current.y) });
    }

    const p = positions.get(step.id)!;
    const nextIds: Array<{ id: string; y: number }> = [];
    if (step.next) nextIds.push({ id: step.next, y: p.y });
    if (step.conditions?.length) {
      step.conditions.forEach((c, idx) => {
        const offset = idx % 2 === 0 ? idx / 2 + 1 : -((idx + 1) / 2);
        nextIds.push({ id: c.next, y: p.y + offset });
      });
    }

    for (const n of nextIds) {
      if (!stepsById.has(n.id)) continue;
      if (!positions.has(n.id)) {
        queue.push({ id: n.id, x: p.x + 1, y: n.y });
      }
    }
  }

  for (const step of definition.steps) {
    if (!positions.has(step.id)) {
      const fallbackX = positions.size;
      positions.set(step.id, { x: fallbackX, y: reserveLane(fallbackX, 0) });
    }
  }
  return positions;
}

export type BuildGraphOptions = {
  /** Prefix graph node/edge ids so nested definitions never collide with the parent DAG. */
  nodeIdPrefix?: string;
  /** When set, WAITING/RUNNING + currentStep override misleading history for the active step (e.g. SUB_WORKFLOW). */
  workflow?: WorkflowGraphInstance;
};

export function buildWorkflowGraphModel(
  definition: WorkflowDefinition,
  currentStepId: string | undefined,
  history: WorkflowHistoryRecord[],
  options?: BuildGraphOptions
): WorkflowGraphModel {
  const nodeIdPrefix = options?.nodeIdPrefix ?? "";
  const edges = buildEdges(definition, nodeIdPrefix);
  const positions = assignPositions(definition);

  const wf = options?.workflow ?? undefined;
  const rawNodes: GraphNode[] = definition.steps.map((step) => {
    const p = positions.get(step.id)!;
    const state = nodeStatusFromHistory(step.id, currentStepId, history, wf);
    return {
      id: nodeIdPrefix ? `${nodeIdPrefix}${step.id}` : step.id,
      label: toLabel(step),
      type: step.type,
      stage: step.stage,
      x: PADDING_X + p.x * X_GAP,
      y: PADDING_Y + p.y * Y_GAP,
      status: state.status,
      timestamp: state.timestamp,
      step
    };
  });

  const minNodeX = Math.min(...rawNodes.map((n) => n.x - NODE_HALF_W), PADDING_X - NODE_HALF_W);
  const maxNodeX = Math.max(...rawNodes.map((n) => n.x + NODE_HALF_W), PADDING_X + NODE_HALF_W);
  const minNodeY = Math.min(...rawNodes.map((n) => n.y - NODE_HALF_H), PADDING_Y - NODE_HALF_H);
  const maxNodeY = Math.max(...rawNodes.map((n) => n.y + NODE_HALF_H), PADDING_Y + NODE_HALF_H);

  const shiftX = minNodeX < OUTER_MARGIN ? OUTER_MARGIN - minNodeX : 0;
  const shiftY = minNodeY < OUTER_MARGIN ? OUTER_MARGIN - minNodeY : 0;

  const nodes: GraphNode[] = rawNodes.map((n) => ({
    ...n,
    x: n.x + shiftX,
    y: n.y + shiftY
  }));

  const width = Math.max(640, maxNodeX + shiftX + OUTER_MARGIN);
  const height = Math.max(300, maxNodeY + shiftY + OUTER_MARGIN);

  return { nodes, edges, width, height };
}
