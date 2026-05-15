package com.example.workflow.engine;

/**
 * Internal context keys for parent/child workflow linking. All use the {@code __sw} prefix to avoid
 * collisions with business variables.
 */
public final class SubWorkflowContextKeys {

    private SubWorkflowContextKeys() {}

    public static final String PARENT_WORKFLOW_ID = "__swParentWorkflowId";
    public static final String PARENT_SUB_STEP_ID = "__swParentSubStepId";
    /** While the child is in flight, the parent stores the active child instance id. */
    public static final String ACTIVE_CHILD_WORKFLOW_ID = "__swActiveChildWorkflowId";

    public static boolean isInternalKey(String key) {
        return key != null && key.startsWith("__sw");
    }
}
