package com.example.workflow.engine.handler;

import com.example.workflow.dsl.StepDefinition;
import com.example.workflow.dsl.StepType;
import com.example.workflow.engine.WorkflowExecutionService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
public class SubWorkflowStepHandler implements StepHandler {

    private final WorkflowExecutionService workflowExecutionService;

    public SubWorkflowStepHandler(@Lazy WorkflowExecutionService workflowExecutionService) {
        this.workflowExecutionService = workflowExecutionService;
    }

    @Override
    public StepType supports() {
        return StepType.SUB_WORKFLOW;
    }

    @Override
    public StepExecutionResult execute(StepDefinition step, StepExecutionContext context) {
        return workflowExecutionService.runSubWorkflowStep(step, context);
    }
}
