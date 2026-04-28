package com.matrix.orquestrador.model;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class OrchestrationReport {

    private final String executionId;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;
    private final long durationMillis;
    private final List<ProcessExecution> executions;
    private final Map<String, ProcessOutput> outputs;
    private final String generatedReport;

    public OrchestrationReport(
            String executionId,
            LocalDateTime startTime,
            LocalDateTime endTime,
            List<ProcessExecution> executions,
            Map<String, ProcessOutput> outputs,
            String generatedReport
    ) {
        this.executionId = executionId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.durationMillis = Duration.between(startTime, endTime).toMillis();
        this.executions = List.copyOf(executions);
        this.outputs = Map.copyOf(outputs);
        this.generatedReport = generatedReport;
    }

    public String getExecutionId() {
        return executionId;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public long getDurationMillis() {
        return durationMillis;
    }

    public List<ProcessExecution> getExecutions() {
        return executions;
    }

    public Map<String, ProcessOutput> getOutputs() {
        return outputs;
    }

    public String getGeneratedReport() {
        return generatedReport;
    }

    public Matrix getMatrixQ() {
        return getMatrixOutput("build-matrix-q");
    }

    public Matrix getMatrixP() {
        return getMatrixOutput("build-matrix-p");
    }

    public Matrix getResultMatrix() {
        return getMatrixOutput("multiply-q-p");
    }

    private Matrix getMatrixOutput(String processId) {
        ProcessOutput output = outputs.get(processId);
        if (output == null) {
            return null;
        }
        return output.getValueAs(Matrix.class);
    }
}
