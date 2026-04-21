package com.matrix.orquestrador.model;

import java.util.List;

public class OrchestrationReport {

    private final List<ProcessExecution> executions;
    private final Matrix matrixQ;
    private final Matrix matrixP;
    private final Matrix resultMatrix;

    public OrchestrationReport(
            List<ProcessExecution> executions,
            Matrix matrixQ,
            Matrix matrixP,
            Matrix resultMatrix
    ) {
        this.executions = executions;
        this.matrixQ = matrixQ;
        this.matrixP = matrixP;
        this.resultMatrix = resultMatrix;
    }

    public List<ProcessExecution> getExecutions() {
        return executions;
    }

    public Matrix getMatrixQ() {
        return matrixQ;
    }

    public Matrix getMatrixP() {
        return matrixP;
    }

    public Matrix getResultMatrix() {
        return resultMatrix;
    }
}
