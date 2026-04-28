package com.example.workflow.assignment;

import com.example.workflow.dsl.AssignmentSpec;
import com.example.workflow.dsl.CandidateSpec;

import java.util.List;
import java.util.Map;

public interface AssignmentResolver {
    boolean supports(String type);
    List<String> resolveCandidates(AssignmentSpec assignment, List<CandidateSpec> candidates, Map<String, Object> context);
    boolean canView(AssignmentSpec assignment, List<CandidateSpec> candidates, Map<String, Object> context);
}
