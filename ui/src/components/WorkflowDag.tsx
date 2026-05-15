import React from "react";
import { WorkflowDefinition, WorkflowHistoryRecord } from "../api/tasks";
import { WorkflowGraph } from "./workflow-graph/WorkflowGraph";
import { GraphNode, WorkflowGraphInstance } from "./workflow-graph/types";

export function WorkflowDag({
  definition,
  currentStepId,
  history,
  workflow,
  onStepSelect,
  definitionLookup,
  nestedRuntime
}: {
  definition: WorkflowDefinition;
  currentStepId?: string;
  history?: WorkflowHistoryRecord[];
  workflow?: WorkflowGraphInstance;
  onStepSelect?: (node: GraphNode) => void;
  definitionLookup?: Map<string, WorkflowDefinition>;
  nestedRuntime?: {
    parentStepId: string;
    currentStepId?: string;
    history: WorkflowHistoryRecord[];
  } | null;
}) {
  return (
    <WorkflowGraph
      definition={definition}
      currentStepId={currentStepId}
      history={history || []}
      workflow={workflow}
      onStepSelect={onStepSelect}
      definitionLookup={definitionLookup}
      nestedRuntime={nestedRuntime ?? null}
    />
  );
}
