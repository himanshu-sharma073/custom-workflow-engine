package com.example.workflow.engine.handler;

import com.example.workflow.dsl.CandidateSpec;
import com.example.workflow.dsl.StepDefinition;
import com.example.workflow.dsl.StepType;
import com.example.workflow.engine.UserTaskService;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UserStepHandler implements StepHandler {
    private final UserTaskService userTaskService;

    public UserStepHandler(UserTaskService userTaskService) {
        this.userTaskService = userTaskService;
    }

    @Override
    public StepType supports() { return StepType.USER; }

    @Override
    public StepExecutionResult execute(StepDefinition step, StepExecutionContext context) {
        userTaskService.createUserTask(context.workflowId(), step.id(), step.assignment(), step.candidates() == null ? List.of() : step.candidates());
        context.appendHistory(step, "USER_WAITING");
        return StepExecutionResult.waitState();
    }
}
