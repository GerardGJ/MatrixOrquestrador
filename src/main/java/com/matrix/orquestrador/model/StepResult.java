package com.matrix.orquestrador.model;

public class StepResult<T> {

    private final T value;
    private final ProcessExecution execution;

    public StepResult(T value, ProcessExecution execution) {
        this.value = value;
        this.execution = execution;
    }

    public T getValue() {
        return value;
    }

    public ProcessExecution getExecution() {
        return execution;
    }
}
