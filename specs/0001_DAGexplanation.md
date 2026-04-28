# DAG Orchestration Implementation Explanation

## Purpose

This execution replaced the previous linear matrix orchestration with a reusable DAG-based orchestration infrastructure written only in Java.

The main goal was to make sure every process runs only after all of its dependencies have completed successfully, while still allowing independent branches to run in parallel. The final process is now report generation, which aggregates process outputs and timing information after the rest of the DAG has finished.

## Previous State

Before this change, `MatrixOrchestrator` executed each step directly in fixed order:

1. Query the database.
2. Build matrix Q.
3. Build matrix P.
4. Multiply Q x P.
5. Return an `OrchestrationReport`.

This worked for a simple sequence, but it did not model dependencies explicitly. It also could not naturally express cases where:

- one process has multiple dependencies;
- multiple processes depend on the same process;
- independent processes can run in parallel;
- a final report process depends on every terminal business process;
- failures should block downstream processes.

## New DAG Infrastructure

### `ProcessNode`

Added `src/main/java/com/matrix/orquestrador/model/ProcessNode.java`.

This class represents one process in the DAG. Each node contains:

- `id`: unique process identifier.
- `name`: human-readable process name.
- `dependencyIds`: list of process ids that must finish before this process can start.
- `action`: executable Java action for the process.

This is now the central process contract used by the orchestrator.

### `ProcessAction`

Added `src/main/java/com/matrix/orquestrador/model/ProcessAction.java`.

This functional interface defines the executable action for each process node:

```java
ProcessOutput execute(ProcessContext context);
```

The action receives a shared `ProcessContext`, allowing it to read outputs from completed dependency processes.

### `ProcessOutput`

Added `src/main/java/com/matrix/orquestrador/model/ProcessOutput.java`.

This class standardizes process outputs. It stores:

- the actual output value;
- the number of output rows;
- a short description/message.

It also includes `getValueAs(Class<T> type)` so downstream processes can safely retrieve typed dependency outputs.

### `ProcessContext`

Added `src/main/java/com/matrix/orquestrador/model/ProcessContext.java`.

This class stores shared execution state:

- process outputs by process id;
- process execution logs.

It uses concurrent Java collections because independent DAG branches can run in parallel.

Important methods:

- `putOutput`: saves a successful process output.
- `requireOutput`: retrieves a required dependency output or fails clearly.
- `addExecution`: stores process execution metadata.
- `getExecutionsSnapshot`: returns a safe copy of execution logs.
- `getOutputsSnapshot`: returns a safe copy of outputs.

### `ProcessStatus`

Added `src/main/java/com/matrix/orquestrador/model/ProcessStatus.java`.

Supported statuses:

- `SUCCESS`
- `FAILED`
- `SKIPPED`

Downstream processes are marked `SKIPPED` when one or more dependencies did not succeed.

## DAG Validation

### `DagValidator`

Added `src/main/java/com/matrix/orquestrador/service/DagValidator.java`.

Validation runs before any process starts. It checks:

- every process id is present and not blank;
- process ids are unique;
- every dependency points to an existing process;
- the graph has no cycles.

Cycle detection is implemented with a depth-first traversal using `visiting` and `visited` sets.

If the DAG is invalid, execution fails before starting any process.

## DAG Execution

### `DagExecutor`

Added `src/main/java/com/matrix/orquestrador/service/DagExecutor.java`.

This is the new scheduler/executor. It is responsible for:

- validating the DAG;
- creating run-level metadata;
- building one `CompletableFuture` per process;
- waiting for all dependencies before executing a process;
- running independent ready processes in parallel;
- recording success, failure, and skipped executions;
- returning the final `OrchestrationReport`.

Execution uses:

```java
ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
CompletableFuture
```

For each process, the executor first builds futures for all dependencies. The process action only runs after:

```java
CompletableFuture.allOf(dependencyFutures...)
```

Once all dependencies finish, the executor checks their statuses. If any dependency is not `SUCCESS`, the process is not run and is recorded as `SKIPPED`.

This behavior supports:

- one process depending on multiple previous processes;
- several processes depending on the same previous process;
- parallel execution of independent branches;
- automatic blocking of downstream work when dependencies fail.

## Failure Behavior

The selected failure behavior is:

- the whole run does not immediately crash on process failure;
- the failed process is marked `FAILED`;
- dependent processes are marked `SKIPPED`;
- independent branches can continue;
- the final report process only runs if all of its dependencies succeeded.

This keeps the execution report useful because it can show which process failed and which downstream processes were skipped.

Retries were not implemented in this execution.

## Process Execution Logging

### `ProcessExecution`

Updated `src/main/java/com/matrix/orquestrador/model/ProcessExecution.java`.

It now records:

- process id;
- process name;
- dependency ids;
- status;
- start time;
- end time;
- duration in milliseconds;
- output rows;
- message.

Skipped processes have `null` start/end times and duration `0`, because their action never ran.

## Run-Level Report Model

### `OrchestrationReport`

Updated `src/main/java/com/matrix/orquestrador/model/OrchestrationReport.java`.

It now records:

- execution id;
- global start time;
- global end time;
- total duration;
- all process execution logs;
- all process outputs;
- generated report text.

The existing matrix getters were preserved:

- `getMatrixQ`
- `getMatrixP`
- `getResultMatrix`

These now read from the output map by process id, keeping compatibility with the sample application while moving the implementation to the generic DAG output structure.

## Matrix Orchestrator Changes

### `MatrixOrchestrator`

Updated `src/main/java/com/matrix/orquestrador/service/MatrixOrchestrator.java`.

The class no longer executes steps directly. Instead, it defines a DAG and passes it to `DagExecutor`.

Current business DAG:

```text
query-db
  -> build-matrix-q
  -> build-matrix-p

build-matrix-q + build-matrix-p
  -> multiply-q-p

multiply-q-p
  -> generate-report
```

Important dependency behavior:

- `build-matrix-q` depends on `query-db`.
- `build-matrix-p` also depends on `query-db`.
- Because Q and P only depend on `query-db`, they can run in parallel after `query-db` succeeds.
- `multiply-q-p` depends on both `build-matrix-q` and `build-matrix-p`.
- `generate-report` is added as the final process and depends on all terminal business processes.

The final report dependency is calculated with `findTerminalProcessIds`, so if more business branches are added later, the report process can automatically depend on all terminal business nodes instead of being tied to only one hardcoded final process.

## Report Generation Process

Report generation is now a normal DAG process with id:

```text
generate-report
```

It reads execution logs from `ProcessContext` and creates a text report containing one row per finished process.

The generated report includes:

- process id;
- process name;
- status;
- start time;
- end time;
- duration;
- output rows.

This implementation prepares the structure needed for a future PDF export. A PDF library was not added in this execution because the user requested only Java programming and no external dependency choice was confirmed.

## Application Output Changes

### `Application`

Updated `src/main/java/com/matrix/orquestrador/Application.java`.

The console output now prints:

- run execution id;
- global start/end/duration;
- process id;
- process name;
- dependencies;
- status;
- process start/end/duration;
- output rows;
- process message;
- generated report;
- matrix Q;
- matrix P;
- result matrix.

## TODO Updates

Updated `specs/TODO.md` to mark the completed DAG-related work:

- DAG-based process design;
- dependency-controlled execution;
- parallel execution support;
- reusable process contract;
- DAG validation;
- execution scheduler;
- skipped downstream processes;
- structured logs and metrics;
- run-level metadata;
- final aggregation report process.

PDF generation remains a future task.

## How To Add A New Process

To add a process, edit `buildDag` in `MatrixOrchestrator` and add a new `ProcessNode`.

Example:

```java
new ProcessNode(
        "new-process-id",
        "New process name",
        List.of("dependency-one", "dependency-two"),
        context -> {
            ProcessOutput dependencyOutput = context.requireOutput("dependency-one");
            Object result = doWork(dependencyOutput);
            return ProcessOutput.of(result, 1, "New process completed");
        }
)
```

The executor will automatically:

- validate the dependency ids;
- wait for all listed dependencies;
- run the process when ready;
- run it in parallel if no dependency blocks it;
- record metrics;
- skip it if any dependency fails.

## Verification Performed

The implementation was verified with:

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
- `build-matrix-q` and `build-matrix-p` started after `query-db`;
- both matrix build processes were able to run independently;
- `multiply-q-p` started only after both matrix processes completed;
- `generate-report` started after the terminal business process completed;
- matrix outputs and generated report were printed.

## Important Notes For Future Work

- Add unit tests for `DagValidator`, especially duplicate id, missing dependency, and cycle cases.
- Add unit tests for `DagExecutor`, especially parallel branches, multiple dependencies, failure, and skipped downstream processes.
- Decide whether failed processes should stop the entire DAG immediately or keep the current `FAILED` plus downstream `SKIPPED` behavior.
- Decide whether retries are required.
- Choose a Java PDF library before implementing PDF export.
- Consider moving DAG definitions out of `MatrixOrchestrator` if they become large or need to be configured externally.
