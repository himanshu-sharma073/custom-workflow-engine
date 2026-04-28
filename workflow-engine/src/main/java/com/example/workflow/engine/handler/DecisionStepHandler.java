package com.example.workflow.engine.handler;

import com.example.workflow.dsl.DecisionCondition;
import com.example.workflow.dsl.StepDefinition;
import com.example.workflow.dsl.StepType;
import org.springframework.context.expression.MapAccessor;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

@Component
public class DecisionStepHandler implements StepHandler {
    private final ExpressionParser parser = new SpelExpressionParser();

    @Override
    public StepType supports() { return StepType.DECISION; }

    @Override
    public StepExecutionResult execute(StepDefinition step, StepExecutionContext context) {
        if (step.conditions() != null) {
            for (DecisionCondition condition : step.conditions()) {
                StandardEvaluationContext ec = new StandardEvaluationContext(context.variables());
                ec.addPropertyAccessor(new MapAccessor());
                context.variables().forEach(ec::setVariable);
                Boolean matched;
                try {
                    matched = parser.parseExpression(condition.expression()).getValue(ec, Boolean.class);
                } catch (SpelEvaluationException ex) {
                    matched = Boolean.FALSE;
                }
                if (Boolean.TRUE.equals(matched)) {
                    context.appendHistory(step, "DECISION_MATCHED");
                    return StepExecutionResult.next(condition.next());
                }
            }
        }
        context.appendHistory(step, "DECISION_NO_MATCH");
        return StepExecutionResult.endState();
    }
}
