package com.example.workflow.engine.handler;

import com.example.workflow.dsl.StepDefinition;
import com.example.workflow.dsl.StepType;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

@Component
public class ScriptStepHandler implements StepHandler {
    private final ExpressionParser parser = new SpelExpressionParser();

    @Override
    public StepType supports() { return StepType.SCRIPT; }

    @Override
    public StepExecutionResult execute(StepDefinition step, StepExecutionContext context) {
        if (step.script() != null && !step.script().isBlank()) {
            StandardEvaluationContext ec = new StandardEvaluationContext();
            context.variables().forEach(ec::setVariable);
            Object result = parser.parseExpression(step.script()).getValue(ec);
            context.variables().put("scriptResult", result);
        }
        context.appendHistory(step, "SCRIPT_COMPLETED");
        return step.next() == null ? StepExecutionResult.endState() : StepExecutionResult.next(step.next());
    }
}
