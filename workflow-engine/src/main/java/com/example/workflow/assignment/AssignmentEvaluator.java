package com.example.workflow.assignment;

import com.example.workflow.dsl.AssignmentSpec;
import com.example.workflow.dsl.CandidateSpec;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class AssignmentEvaluator {
    private final List<AssignmentResolver> resolvers;

    public AssignmentEvaluator(List<AssignmentResolver> resolvers) {
        this.resolvers = resolvers;
    }

    public boolean canView(AssignmentSpec assignment, List<CandidateSpec> candidates, Map<String, Object> context) {
        for (AssignmentResolver resolver : resolvers) {
            if ((assignment != null && resolver.supports(assignment.type().name())) ||
                candidates.stream().anyMatch(c -> resolver.supports(c.type().name()))) {
                if (resolver.canView(assignment, candidates, context)) {
                    return true;
                }
            }
        }
        return false;
    }
}
