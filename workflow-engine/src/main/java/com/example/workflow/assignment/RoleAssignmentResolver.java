package com.example.workflow.assignment;

import com.example.workflow.dsl.AssignmentSpec;
import com.example.workflow.dsl.AssignmentType;
import com.example.workflow.dsl.CandidateSpec;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class RoleAssignmentResolver implements AssignmentResolver {
    @Override
    public boolean supports(String type) {
        return AssignmentType.ROLE.name().equals(type);
    }

    @Override
    public List<String> resolveCandidates(AssignmentSpec assignment, List<CandidateSpec> candidates, Map<String, Object> context) {
        return List.of();
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean canView(AssignmentSpec assignment, List<CandidateSpec> candidates, Map<String, Object> context) {
        List<String> roles = (List<String>) context.getOrDefault("roles", List.of());
        if (assignment != null && assignment.type() == AssignmentType.ROLE && roles.contains(assignment.value())) {
            return true;
        }
        return candidates.stream().anyMatch(c -> c.type() == AssignmentType.ROLE && roles.contains(c.value()));
    }
}
