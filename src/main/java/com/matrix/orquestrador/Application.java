package com.matrix.orquestrador;

import com.matrix.orquestrador.model.OrchestrationReport;
import com.matrix.orquestrador.service.MatrixOrchestrator;

public class Application {

    public static void main(String[] args) {
        MatrixOrchestrator orchestrator = new MatrixOrchestrator();
        OrchestrationReport report = orchestrator.run();

        System.out.println("=== Orchestration Summary ===");
        report.getExecutions().forEach(execution -> System.out.printf(
                "%s | start=%s | end=%s | durationMs=%d | outputRows=%d%n",
                execution.getProcessName(),
                execution.getStartTime(),
                execution.getEndTime(),
                execution.getDurationMillis(),
                execution.getOutputRows()
        ));

        System.out.println();
        System.out.println("Matrix Q:");
        System.out.println(report.getMatrixQ());
        System.out.println("Matrix P:");
        System.out.println(report.getMatrixP());
        System.out.println("Result matrix:");
        System.out.println(report.getResultMatrix());
    }
}
