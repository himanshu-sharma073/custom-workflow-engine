package com.example.workflow.assignment;

import com.example.workflow.dsl.AssignmentSpec;
import com.example.workflow.dsl.AssignmentType;
import com.example.workflow.dsl.CandidateSpec;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class GroupAssignmentResolver implements AssignmentResolver {
    @Override
    public boolean supports(String type) {
        return AssignmentType.GROUP.name().equals(type);
    }

    @Override
    public List<String> resolveCandidates(AssignmentSpec assignment, List<CandidateSpec> candidates, Map<String, Object> context) {
        return List.of();
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean canView(AssignmentSpec assignment, List<CandidateSpec> candidates, Map<String, Object> context) {
        List<String> groups = (List<String>) context.getOrDefault("groups", List.of());
        if (assignment != null && assignment.type() == AssignmentType.GROUP && groups.contains(assignment.value())) {
            return true;
        }
        return candidates.stream().anyMatch(c -> c.type() == AssignmentType.GROUP && groups.contains(c.value()));
    }
}
