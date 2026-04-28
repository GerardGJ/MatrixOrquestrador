package com.matrix.orquestrador.service;

import com.matrix.orquestrador.model.Matrix;
import com.matrix.orquestrador.model.OrchestrationReport;
import com.matrix.orquestrador.model.ProcessExecution;
import com.matrix.orquestrador.model.ProcessNode;
import com.matrix.orquestrador.model.ProcessOutput;
import com.matrix.orquestrador.model.TableData;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class MatrixOrchestrator {

    public OrchestrationReport run() {
        return new DagExecutor().execute(buildDag());
    }

    private List<ProcessNode> buildDag() {
        List<ProcessNode> businessProcesses = List.of(
                new ProcessNode(
                        "query-db",
                        "Query DB",
                        List.of(),
                        context -> {
                            TableData tableData = queryDb();
                            return ProcessOutput.of(tableData, tableData.getRowCount(), "Loaded " + tableData.getTableName());
                        }
                ),
                new ProcessNode(
                        "build-matrix-q",
                        "Build matrix Q",
                        List.of("query-db"),
                        context -> {
                            TableData queriedTable = context.requireOutput("query-db").getValueAs(TableData.class);
                            Matrix matrix = buildMatrixQ(queriedTable);
                            return ProcessOutput.of(matrix, matrix.getRowCount(), "Built matrix Q");
                        }
                ),
                new ProcessNode(
                        "build-matrix-p",
                        "Query DB for column metadata and build matrix P",
                        List.of("query-db"),
                        context -> {
                            TableData queriedTable = context.requireOutput("query-db").getValueAs(TableData.class);
                            Matrix matrix = buildMatrixP(queriedTable);
                            return ProcessOutput.of(matrix, matrix.getRowCount(), "Built matrix P");
                        }
                ),
                new ProcessNode(
                        "multiply-q-p",
                        "Multiply Q x P",
                        List.of("build-matrix-q", "build-matrix-p"),
                        context -> {
                            Matrix matrixQ = context.requireOutput("build-matrix-q").getValueAs(Matrix.class);
                            Matrix matrixP = context.requireOutput("build-matrix-p").getValueAs(Matrix.class);
                            Matrix matrix = multiplyMatrices(matrixQ, matrixP);
                            return ProcessOutput.of(matrix, matrix.getRowCount(), "Calculated result matrix");
                        }
                )
        );

        List<ProcessNode> dag = new ArrayList<>(businessProcesses);
        dag.add(new ProcessNode(
                "generate-report",
                "Generate execution report",
                findTerminalProcessIds(businessProcesses),
                context -> {
                    String report = generateExecutionReport(context.getExecutionsSnapshot());
                    int rowCount = context.getOutputsSnapshot().size();
                    return ProcessOutput.of(report, rowCount, "Generated report for " + rowCount + " process outputs");
                }
        ));
        return dag;
    }

    private List<String> findTerminalProcessIds(List<ProcessNode> nodes) {
        Set<String> dependencyIds = new HashSet<>();
        for (ProcessNode node : nodes) {
            dependencyIds.addAll(node.getDependencyIds());
        }

        return nodes.stream()
                .map(ProcessNode::getId)
                .filter(processId -> !dependencyIds.contains(processId))
                .toList();
    }

    private TableData queryDb() {
        // Dummy query output that simulates rows returned from a database.
        return new TableData(
                "orders_snapshot",
                List.of("customer_score", "order_count", "total_amount"),
                new double[][]{
                        {1.0, 5.0, 100.0},
                        {2.0, 3.0, 80.0},
                        {4.0, 8.0, 210.0}
                }
        );
    }

    private Matrix buildMatrixQ(TableData queriedTable) {
        // Dummy transformation from the queried table to matrix Q.
        return new Matrix(queriedTable.getRows());
    }

    private Matrix buildMatrixP(TableData queriedTable) {
        // Dummy enrichment matrix derived from the original table columns.
        int columnCount = queriedTable.getColumns().size();
        double[][] values = new double[columnCount][2];

        for (int index = 0; index < columnCount; index++) {
            values[index][0] = index + 1;
            values[index][1] = queriedTable.getColumns().get(index).length();
        }

        return new Matrix(values);
    }

    private Matrix multiplyMatrices(Matrix matrixQ, Matrix matrixP) {
        return matrixQ.multiply(matrixP);
    }

    private String generateExecutionReport(List<ProcessExecution> executions) {
        String executionRows = executions.stream()
                .sorted(Comparator.comparing(ProcessExecution::getStartTime, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(execution -> String.format(
                        "%s | %s | status=%s | start=%s | end=%s | durationMs=%d | outputRows=%d",
                        execution.getProcessId(),
                        execution.getProcessName(),
                        execution.getStatus(),
                        execution.getStartTime(),
                        execution.getEndTime(),
                        execution.getDurationMillis(),
                        execution.getOutputRows()
                ))
                .collect(Collectors.joining(System.lineSeparator()));

        return "Process execution report"
                + System.lineSeparator()
                + executionRows;
    }
}
