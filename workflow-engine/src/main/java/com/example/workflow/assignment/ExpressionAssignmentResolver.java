package com.example.workflow.assignment;

import com.example.workflow.dsl.AssignmentSpec;
import com.example.workflow.dsl.AssignmentType;
import com.example.workflow.dsl.CandidateSpec;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class ExpressionAssignmentResolver implements AssignmentResolver {
    private final ExpressionParser parser = new SpelExpressionParser();

    @Override
    public boolean supports(String type) {
        return AssignmentType.EXPRESSION.name().equals(type);
    }

    @Override
    public List<String> resolveCandidates(AssignmentSpec assignment, List<CandidateSpec> candidates, Map<String, Object> context) {
        return List.of();
    }

    @Override
    public boolean canView(AssignmentSpec assignment, List<CandidateSpec> candidates, Map<String, Object> context) {
        if (assignment != null && assignment.type() == AssignmentType.EXPRESSION) {
            return eval(assignment.value(), context);
        }
        return candidates.stream().anyMatch(c -> c.type() == AssignmentType.EXPRESSION && eval(c.value(), context));
    }

    private boolean eval(String expression, Map<String, Object> context) {
        StandardEvaluationContext ec = new StandardEvaluationContext();
        context.forEach(ec::setVariable);
        Boolean result = parser.parseExpression(expression).getValue(ec, Boolean.class);
        return Boolean.TRUE.equals(result);
    }
}
