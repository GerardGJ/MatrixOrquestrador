package com.matrix.orquestrador.service;

import com.matrix.orquestrador.model.Matrix;
import com.matrix.orquestrador.model.OrchestrationReport;
import com.matrix.orquestrador.model.ProcessExecution;
import com.matrix.orquestrador.model.StepResult;
import com.matrix.orquestrador.model.TableData;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public class MatrixOrchestrator {

    public OrchestrationReport run() {
        List<ProcessExecution> executions = new ArrayList<>();

        StepResult<TableData> queriedTableStep = executeStep(
                "1. Query DB",
                this::queryDb,
                TableData::getRowCount
        );
        executions.add(queriedTableStep.getExecution());

        StepResult<Matrix> matrixQStep = executeStep(
                "2. Build matrix Q",
                () -> buildMatrixQ(queriedTableStep.getValue()),
                Matrix::getRowCount
        );
        executions.add(matrixQStep.getExecution());

        StepResult<Matrix> matrixPStep = executeStep(
                "3. Query DB for column metadata and build matrix P",
                () -> buildMatrixP(queriedTableStep.getValue()),
                Matrix::getRowCount
        );
        executions.add(matrixPStep.getExecution());

        StepResult<Matrix> multiplicationStep = executeStep(
                "4. Multiply Q x P",
                () -> multiplyMatrices(matrixQStep.getValue(), matrixPStep.getValue()),
                Matrix::getRowCount
        );
        executions.add(multiplicationStep.getExecution());

        return new OrchestrationReport(
                executions,
                matrixQStep.getValue(),
                matrixPStep.getValue(),
                multiplicationStep.getValue()
        );
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

    private <T> StepResult<T> executeStep(
            String processName,
            Supplier<T> action,
            Function<T, Integer> outputRowsCounter
    ) {
        LocalDateTime startTime = LocalDateTime.now();
        T value = action.get();
        LocalDateTime endTime = LocalDateTime.now();

        ProcessExecution execution = new ProcessExecution(
                processName,
                startTime,
                endTime,
                outputRowsCounter.apply(value)
        );

        return new StepResult<>(value, execution);
    }
}
