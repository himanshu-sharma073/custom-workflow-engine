package com.example.workflow.dsl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

@Component
public class WorkflowDefinitionParser {
    private final ObjectMapper objectMapper;

    public WorkflowDefinitionParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public WorkflowDefinition parse(InputStream inputStream) throws IOException {
        ObjectMapper tolerantMapper = objectMapper.copy()
            .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS, true)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        WorkflowDefinition definition = tolerantMapper.readValue(inputStream, WorkflowDefinition.class);
        validate(definition);
        return definition;
    }

    public void validate(WorkflowDefinition definition) {
        Set<String> ids = new HashSet<>();
        for (StepDefinition step : definition.steps()) {
            if (!ids.add(step.id())) {
                throw new IllegalArgumentException("Duplicate step id: " + step.id());
            }
        }
        for (StepDefinition step : definition.steps()) {
            if (step.next() != null && ids.stream().noneMatch(id -> id.equals(step.next()))) {
                throw new IllegalArgumentException("Invalid next step: " + step.next());
            }
            if (step.conditions() != null) {
                for (DecisionCondition condition : step.conditions()) {
                    if (condition.next() != null && ids.stream().noneMatch(id -> id.equals(condition.next()))) {
                        throw new IllegalArgumentException("Invalid decision next step: " + condition.next());
                    }
                }
            }
            if (step.type() == StepType.USER && step.approval() != null && step.approval().type() == ApprovalType.MIN_APPROVAL
                && (step.approval().minApprovals() == null || step.approval().minApprovals() <= 0)) {
                throw new IllegalArgumentException("MIN_APPROVAL requires minApprovals > 0");
            }
            if (step.type() == StepType.SUB_WORKFLOW) {
                if (step.subWorkflowDefinitionId() == null || step.subWorkflowDefinitionId().isBlank()) {
                    throw new IllegalArgumentException("SUB_WORKFLOW step " + step.id() + " requires subWorkflowDefinitionId");
                }
                if (step.next() == null || step.next().isBlank()) {
                    throw new IllegalArgumentException("SUB_WORKFLOW step " + step.id() + " requires next");
                }
            }
        }
    }
}
