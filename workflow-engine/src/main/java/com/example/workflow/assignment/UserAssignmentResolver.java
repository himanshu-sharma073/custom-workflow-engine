package com.example.workflow.assignment;

import com.example.workflow.dsl.AssignmentSpec;
import com.example.workflow.dsl.AssignmentType;
import com.example.workflow.dsl.CandidateSpec;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class UserAssignmentResolver implements AssignmentResolver {
    @Override
    public boolean supports(String type) {
        return AssignmentType.USER.name().equals(type);
    }

    @Override
    public List<String> resolveCandidates(AssignmentSpec assignment, List<CandidateSpec> candidates, Map<String, Object> context) {
        List<String> users = new ArrayList<>();
        if (assignment != null && assignment.type() == AssignmentType.USER) {
            users.add(assignment.value());
        }
        for (CandidateSpec candidate : candidates) {
            if (candidate.type() == AssignmentType.USER) {
                users.add(candidate.value());
            }
        }
        return users;
    }

    @Override
    public boolean canView(AssignmentSpec assignment, List<CandidateSpec> candidates, Map<String, Object> context) {
        String currentUser = (String) context.get("currentUser");
        return resolveCandidates(assignment, candidates, context).stream().anyMatch(u -> Objects.equals(u, currentUser));
    }
}
