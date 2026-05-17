import React from "react";
import { WorkflowDefinition, WorkflowHistoryRecord } from "../api/tasks";
import { WorkflowGraph } from "./workflow-graph/WorkflowGraph";
import { GraphNode, WorkflowGraphInstance } from "./workflow-graph/types";

export type NestedSubWorkflowRuntime = {
  currentStepId?: string;
  history: WorkflowHistoryRecord[];
  childStatus?: string;
  childDefinitionId: string;
};

export function WorkflowDag({
  definition,
  currentStepId,
  history,
  workflow,
  onStepSelect,
  definitionLookup,
  nestedRuntimeByParentStep,
  autoExpandSubWorkflowStepIds
}: {
  definition: WorkflowDefinition;
  currentStepId?: string;
  history?: WorkflowHistoryRecord[];
  workflow?: WorkflowGraphInstance;
  onStepSelect?: (node: GraphNode) => void;
  definitionLookup?: Map<string, WorkflowDefinition>;
  nestedRuntimeByParentStep?: Record<string, NestedSubWorkflowRuntime>;
  autoExpandSubWorkflowStepIds?: string[];
}) {
  return (
    <WorkflowGraph
      definition={definition}
      currentStepId={currentStepId}
      history={history || []}
      workflow={workflow}
      onStepSelect={onStepSelect}
      definitionLookup={definitionLookup}
      nestedRuntimeByParentStep={nestedRuntimeByParentStep}
      autoExpandSubWorkflowStepIds={autoExpandSubWorkflowStepIds}
    />
  );
}
