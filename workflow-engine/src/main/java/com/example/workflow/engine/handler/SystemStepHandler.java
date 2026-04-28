package com.example.workflow.engine.handler;

import com.example.workflow.dsl.StepDefinition;
import com.example.workflow.dsl.StepType;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Map;

@Component
public class SystemStepHandler implements StepHandler {
    private final ApplicationContext applicationContext;

    public SystemStepHandler(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public StepType supports() { return StepType.SYSTEM; }

    @Override
    public StepExecutionResult execute(StepDefinition step, StepExecutionContext context) {
        invokeAction(step, context);
        context.appendHistory(step, "SYSTEM_COMPLETED");
        return step.next() == null ? StepExecutionResult.endState() : StepExecutionResult.next(step.next());
    }

    private void invokeAction(StepDefinition step, StepExecutionContext context) {
        String action = step.action();
        if (action == null || action.isBlank()) {
            return;
        }
        int idx = action.indexOf('.');
        if (idx <= 0 || idx == action.length() - 1) {
            throw new IllegalArgumentException("Invalid system action format. Expected beanName.methodName, got: " + action);
        }

        String beanName = action.substring(0, idx);
        String methodName = action.substring(idx + 1);
        Object bean = applicationContext.getBean(beanName);

        try {
            Method mapMethod = findMethod(bean.getClass(), methodName, Map.class);
            if (mapMethod != null) {
                Object result = mapMethod.invoke(bean, context.variables());
                mergeResult(result, context);
                return;
            }

            Method noArgMethod = findMethod(bean.getClass(), methodName);
            if (noArgMethod != null) {
                Object result = noArgMethod.invoke(bean);
                mergeResult(result, context);
                return;
            }

            throw new IllegalArgumentException("No supported method found for action " + action + ". Expected method() or method(Map<String,Object>)");
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to invoke system action " + action, e);
        }
    }

    private Method findMethod(Class<?> type, String name, Class<?>... args) {
        try {
            Method method = type.getMethod(name, args);
            method.setAccessible(true);
            return method;
        } catch (NoSuchMethodException ex) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private void mergeResult(Object result, StepExecutionContext context) {
        if (result == null) {
            return;
        }
        if (result instanceof Map<?, ?> mapResult) {
            context.variables().putAll((Map<String, Object>) mapResult);
            return;
        }
        context.variables().put("systemActionResult", result);
    }
}
