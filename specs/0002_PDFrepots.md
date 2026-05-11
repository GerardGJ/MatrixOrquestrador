# PDF Reporting Implementation Explanation

## Purpose

This execution implemented the final PDF report generated at the end of the orchestration run.

The report is created after the DAG finishes and includes:

- the date of execution;
- the global execution start time;
- the global execution end time;
- total execution duration;
- step-level start time, end time, duration, status, and message;
- matrix length at each step;
- overall execution time.

The generated PDF is written to the `target` folder using the execution id in the file name.

## Previous State

The DAG implementation already captured basic execution metadata in `ProcessExecution` and returned an `OrchestrationReport`.

Before this change:

- the application printed execution details to the console;
- `ProcessExecution` stored output rows, but not matrix columns or total matrix length;
- report generation existed as a text process inside the DAG;
- no PDF file was generated at the end of execution;
- PDF export was still listed as future work in the previous spec.

## Process Execution Metric Changes

### `ProcessExecution`

Updated `src/main/java/com/matrix/orquestrador/model/ProcessExecution.java`.

The process execution model now stores matrix size information:

- `outputRows`
- `outputColumns`
- `outputLength`

`outputLength` represents the total number of values in a matrix-like output:

```text
rows x columns = total values
```

Example:

```text
3 x 2 = 6 values
```

The class also now exposes:

- `getOutputColumns`
- `getOutputLength`
- `getMatrixLengthDescription`

`getMatrixLengthDescription` returns a human-readable value for the report. If a step does not produce a matrix-like value, it returns:

```text
N/A
```

The older constructor was kept and delegates to the new constructor so existing code can still create a `ProcessExecution` with only output rows.

## Table Dimension Support

### `TableData`

Updated `src/main/java/com/matrix/orquestrador/model/TableData.java`.

Added:

```java
getColumnCount()
```

This allows the first step, `query-db`, to be reported with a matrix-like length. The dummy database result is tabular, so its length is calculated as:

```text
row count x column count
```

For the current dummy data:

```text
3 x 3 = 9 values
```

## Matrix Length Measurement

### `DagExecutor`

Updated `src/main/java/com/matrix/orquestrador/service/DagExecutor.java`.

The executor now measures output dimensions immediately after each process action succeeds.

Added internal metric detection for:

- `Matrix`
- `TableData`

For `Matrix`, the executor uses:

- `matrix.getRowCount()`
- `matrix.getColumnCount()`
- `rows * columns`

For `TableData`, the executor uses:

- `tableData.getRowCount()`
- `tableData.getColumnCount()`
- `rows * columns`

For non-matrix outputs, such as the final generated text report, matrix length is left as not applicable.

This keeps the measurement centralized in the executor instead of forcing every process action to manually calculate dimensions.

## Text Report Update

### `MatrixOrchestrator`

Updated `src/main/java/com/matrix/orquestrador/service/MatrixOrchestrator.java`.

The generated text report now includes:

```text
matrixLength=<rows x columns = values>
```

Instead of only reporting output rows, it now describes the size of each matrix-like step.

The final report process still runs as a DAG node with id:

```text
generate-report
```

This means the in-memory report is still produced as part of the orchestration graph, while the PDF file is produced after the whole `OrchestrationReport` object is available.

## PDF Writer

### `ExecutionReportPdfWriter`

Added `src/main/java/com/matrix/orquestrador/service/ExecutionReportPdfWriter.java`.

This class is responsible for turning an `OrchestrationReport` into a PDF file.

It builds the report content with these sections:

1. Title.
2. Date of execution.
3. Execution id.
4. Global execution start time.
5. Global execution end time.
6. Total duration.
7. Step details.
8. Overall execution time.

Each step includes:

- process name;
- process id;
- status;
- start time;
- end time;
- duration;
- matrix length;
- message.

The writer uses Java standard library APIs only. No external PDF dependency was added.

The PDF is generated directly with a simple PDF structure:

- catalog object;
- pages object;
- Helvetica font object;
- page objects;
- content stream objects;
- xref table;
- trailer.

This is enough for a plain text execution report and avoids adding Maven dependencies for this small reporting requirement.

## PDF Output Location

### `Application`

Updated `src/main/java/com/matrix/orquestrador/Application.java`.

After the orchestrator finishes, the application now writes the PDF using:

```java
new ExecutionReportPdfWriter().write(
        report,
        Path.of("target", "execution-report-" + report.getExecutionId() + ".pdf")
);
```

The naming convention is:

```text
target/execution-report-<execution-id>.pdf
```

Example generated during verification:

```text
target/execution-report-3c552875-4379-4652-82a1-8f2bedad374b.pdf
```

The application also prints the absolute path of the generated PDF at the end of the console output.

## Current Report Values

With the current dummy orchestration, the report records these matrix lengths:

- `query-db`: `3 x 3 = 9 values`
- `build-matrix-q`: `3 x 3 = 9 values`
- `build-matrix-p`: `3 x 2 = 6 values`
- `multiply-q-p`: `3 x 2 = 6 values`
- `generate-report`: `N/A`

The `generate-report` step is `N/A` because it produces text, not a matrix or table.

## Verification Performed

The implementation was compiled and verified with:

```powershell
mvn test
```

Result:

- build succeeded;
- Java sources compiled successfully;
- no test sources were present.

The application was also run with:

```powershell
java -cp target\classes com.matrix.orquestrador.Application
```

Result:

- orchestration completed successfully;
- matrix lengths appeared in console output;
- generated text report included matrix lengths;
- a PDF report was created in the `target` folder;
- the generated PDF file had non-zero size.

## Files Changed

The reporting implementation touched these source files:

- `src/main/java/com/matrix/orquestrador/Application.java`
- `src/main/java/com/matrix/orquestrador/model/ProcessExecution.java`
- `src/main/java/com/matrix/orquestrador/model/TableData.java`
- `src/main/java/com/matrix/orquestrador/service/DagExecutor.java`
- `src/main/java/com/matrix/orquestrador/service/MatrixOrchestrator.java`
- `src/main/java/com/matrix/orquestrador/service/ExecutionReportPdfWriter.java`

## Notes For Future Work

- Replace the simple text PDF layout with a table layout if visual formatting becomes important.
- Add unit tests for `ExecutionReportPdfWriter`.
- Add assertions for matrix length reporting in executor-level tests.
- Consider adding a cleanup or retention policy for old generated PDF reports in `target`.
