import React from "react";
import { WorkflowDefinition, WorkflowHistoryRecord } from "../api/tasks";
import { WorkflowGraph } from "./workflow-graph/WorkflowGraph";
import { GraphNode } from "./workflow-graph/types";

export function WorkflowDag({
  definition,
  currentStepId,
  history,
  onStepSelect
}: {
  definition: WorkflowDefinition;
  currentStepId?: string;
  history?: WorkflowHistoryRecord[];
  onStepSelect?: (node: GraphNode) => void;
}) {
  return (
    <WorkflowGraph
      definition={definition}
      currentStepId={currentStepId}
      history={history || []}
      onStepSelect={onStepSelect}
    />
  );
}
