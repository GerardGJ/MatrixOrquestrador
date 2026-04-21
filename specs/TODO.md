# Orquestrador TODO

## Objective

Build a flexible Java orchestrator that is easy to change and extend. The orchestrator must execute processes using a DAG structure, support sequential and parallel execution, track execution metrics for every process, and generate a final PDF report with the execution logs in table format.

## Core Requirements

- [ ] Design the orchestrator around a DAG (Directed Acyclic Graph) of processes.
- [ ] Ensure each process runs only after all its required predecessor processes have finished successfully.
- [ ] Support parallel execution for independent processes in the DAG.
- [ ] Allow the DAG definition to be easy to modify when new processes are added or dependencies change.
- [ ] Define a clear process contract so each process has:
  - [ ] A unique identifier.
  - [ ] A name/description.
  - [ ] A list of dependencies.
  - [ ] An executable action.
  - [ ] A standard output/result structure.

## Execution Control

- [ ] Implement DAG validation to detect invalid graphs and cycles before execution starts.
- [ ] Build a scheduler/executor that:
  - [ ] Finds processes whose dependencies are already completed.
  - [ ] Runs ready processes.
  - [ ] Dispatches independent processes in parallel when possible.
  - [ ] Waits for dependent processes to finish before continuing downstream execution.
- [ ] Define failure behavior:
  - [ ] Stop the whole execution on a critical process failure, or
  - [ ] Mark dependent processes as blocked/skipped.
- [ ] Decide and document whether retries are supported for failed processes.

## Logging And Metrics

- [ ] Log execution metadata for every process in the DAG.
- [ ] For each process, capture:
  - [ ] Start time.
  - [ ] End time.
  - [ ] Total execution time.
  - [ ] Output size in number of rows.
  - [ ] Final status (`SUCCESS`, `FAILED`, `SKIPPED`, etc.).
- [ ] Store logs in a structured format that can be reused for console output, persistence, and PDF export.
- [ ] Add run-level metadata:
  - [ ] Execution ID.
  - [ ] Global start time.
  - [ ] Global end time.
  - [ ] Total orchestrator duration.

## PDF Reporting

- [ ] Generate a PDF once the full DAG execution is complete.
- [ ] Include an execution summary section in the PDF.
- [ ] Include a table with one row per process.
- [ ] The PDF table should contain at least:
  - [ ] Process ID.
  - [ ] Process name.
  - [ ] Status.
  - [ ] Start time.
  - [ ] End time.
  - [ ] Total execution time.
  - [ ] Output rows.
- [ ] Define the output location and naming convention for generated PDF reports.
- [ ] Choose a Java PDF library for implementation.

## Suggested Architecture Tasks

- [ ] Create a `ProcessNode` model to represent each process in the DAG.
- [ ] Create a `ProcessExecutionLog` model for process-level metrics.
- [ ] Create a `RunExecutionReport` model for the full orchestration result.
- [ ] Separate responsibilities into components:
  - [ ] DAG builder/parser.
  - [ ] DAG validator.
  - [ ] Execution scheduler.
  - [ ] Process runner.
  - [ ] Metrics/logger service.
  - [ ] PDF report generator.
- [ ] Keep process implementations decoupled from orchestration infrastructure.
- [ ] Make it easy to plug in dummy steps now and real database/JDBC steps later.

## Near-Term Implementation Plan

- [ ] Replace the current linear dummy orchestration with DAG-based execution.
- [ ] Add support for parallel process execution using Java concurrency utilities.
- [ ] Expand the current process tracking so it includes process status and run-level metadata.
- [ ] Add a final aggregation step that collects all process logs.
- [ ] Implement PDF export from the aggregated execution logs.
- [ ] Add sample DAG definitions for testing:
  - [ ] Pure sequential flow.
  - [ ] Mixed sequential + parallel flow.
  - [ ] Failure scenario.

## Acceptance Criteria

- [ ] A process never starts before all its dependencies are completed.
- [ ] Independent DAG branches can execute in parallel.
- [ ] Every executed process produces a structured execution log.
- [ ] The final execution produces a PDF report with all process logs in a table.
- [ ] The orchestrator is easy to extend with new process nodes and dependency changes.
