package com.example.workflow.dsl;

public enum StepType {
    SYSTEM,
    USER,
    DECISION,
    API,
    EVENT,
    DELAY,
    SCRIPT,
    /** Runs another workflow definition to completion (or until it waits); then continues the parent. */
    SUB_WORKFLOW,
    END
}
