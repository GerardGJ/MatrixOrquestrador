package com.matrix.orquestrador;

import com.matrix.orquestrador.model.OrchestrationReport;
import com.matrix.orquestrador.service.MatrixOrchestrator;

public class Application {

    public static void main(String[] args) {
        MatrixOrchestrator orchestrator = new MatrixOrchestrator();
        OrchestrationReport report = orchestrator.run();

        System.out.println("=== Orchestration Summary ===");
        System.out.printf(
                "executionId=%s | start=%s | end=%s | durationMs=%d%n",
                report.getExecutionId(),
                report.getStartTime(),
                report.getEndTime(),
                report.getDurationMillis()
        );
        report.getExecutions().forEach(execution -> System.out.printf(
                "%s | %s | dependencies=%s | status=%s | start=%s | end=%s | durationMs=%d | outputRows=%d | %s%n",
                execution.getProcessId(),
                execution.getProcessName(),
                execution.getDependencyIds(),
                execution.getStatus(),
                execution.getStartTime(),
                execution.getEndTime(),
                execution.getDurationMillis(),
                execution.getOutputRows(),
                execution.getMessage()
        ));

        System.out.println();
        System.out.println("Generated report:");
        System.out.println(report.getGeneratedReport());
        System.out.println();
        System.out.println("Matrix Q:");
        System.out.println(report.getMatrixQ());
        System.out.println("Matrix P:");
        System.out.println(report.getMatrixP());
        System.out.println("Result matrix:");
        System.out.println(report.getResultMatrix());
    }
}
