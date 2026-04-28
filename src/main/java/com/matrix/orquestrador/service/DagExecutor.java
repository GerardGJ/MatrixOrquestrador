package com.matrix.orquestrador.service;

import com.matrix.orquestrador.model.OrchestrationReport;
import com.matrix.orquestrador.model.ProcessContext;
import com.matrix.orquestrador.model.ProcessExecution;
import com.matrix.orquestrador.model.ProcessNode;
import com.matrix.orquestrador.model.ProcessOutput;
import com.matrix.orquestrador.model.ProcessStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DagExecutor {

    private static final String REPORT_PROCESS_ID = "generate-report";

    private final DagValidator validator;
    private final int threadCount;

    public DagExecutor() {
        this(new DagValidator(), Runtime.getRuntime().availableProcessors());
    }

    public DagExecutor(DagValidator validator, int threadCount) {
        this.validator = validator;
        this.threadCount = Math.max(1, threadCount);
    }

    public OrchestrationReport execute(List<ProcessNode> nodes) {
        validator.validate(nodes);

        String executionId = UUID.randomUUID().toString();
        LocalDateTime globalStartTime = LocalDateTime.now();
        ProcessContext context = new ProcessContext();
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);

        try {
            Map<String, ProcessNode> nodesById = mapById(nodes);
            Map<String, CompletableFuture<ProcessExecution>> futures = new HashMap<>();
            for (ProcessNode node : nodes) {
                buildFuture(node, nodesById, futures, context, executorService);
            }

            CompletableFuture<?>[] allFutures = futures.values().toArray(new CompletableFuture[0]);
            CompletableFuture.allOf(allFutures).join();

            List<ProcessExecution> executions = new ArrayList<>();
            for (ProcessNode node : nodes) {
                executions.add(futures.get(node.getId()).join());
            }
            executions.sort(Comparator.comparing(ProcessExecution::getStartTime, Comparator.nullsLast(Comparator.naturalOrder())));

            LocalDateTime globalEndTime = LocalDateTime.now();
            String generatedReport = context.getOutput(REPORT_PROCESS_ID)
                    .map(output -> output.getValueAs(String.class))
                    .orElse("");

            return new OrchestrationReport(
                    executionId,
                    globalStartTime,
                    globalEndTime,
                    executions,
                    context.getOutputsSnapshot(),
                    generatedReport
            );
        } finally {
            executorService.shutdown();
        }
    }

    private CompletableFuture<ProcessExecution> buildFuture(
            ProcessNode node,
            Map<String, ProcessNode> nodesById,
            Map<String, CompletableFuture<ProcessExecution>> futures,
            ProcessContext context,
            ExecutorService executorService
    ) {
        CompletableFuture<ProcessExecution> existingFuture = futures.get(node.getId());
        if (existingFuture != null) {
            return existingFuture;
        }

        List<CompletableFuture<ProcessExecution>> dependencyFutures = node.getDependencyIds().stream()
                .map(dependencyId -> buildFuture(nodesById.get(dependencyId), nodesById, futures, context, executorService))
                .toList();

        CompletableFuture<ProcessExecution> future = CompletableFuture
                .allOf(dependencyFutures.toArray(new CompletableFuture[0]))
                .thenCompose(ignored -> {
                    List<String> failedDependencies = dependencyFutures.stream()
                            .map(CompletableFuture::join)
                            .filter(execution -> execution.getStatus() != ProcessStatus.SUCCESS)
                            .map(ProcessExecution::getProcessId)
                            .toList();

                    if (!failedDependencies.isEmpty()) {
                        ProcessExecution skippedExecution = skipped(node, failedDependencies);
                        context.addExecution(skippedExecution);
                        return CompletableFuture.completedFuture(skippedExecution);
                    }

                    return CompletableFuture.supplyAsync(() -> run(node, context), executorService);
                });

        futures.put(node.getId(), future);
        return future;
    }

    private ProcessExecution run(ProcessNode node, ProcessContext context) {
        LocalDateTime startTime = LocalDateTime.now();
        try {
            ProcessOutput output = node.getAction().execute(context);
            LocalDateTime endTime = LocalDateTime.now();
            context.putOutput(node.getId(), output);
            ProcessExecution execution = new ProcessExecution(
                    node.getId(),
                    node.getName(),
                    node.getDependencyIds(),
                    ProcessStatus.SUCCESS,
                    startTime,
                    endTime,
                    output.getRowCount(),
                    output.getDescription()
            );
            context.addExecution(execution);
            return execution;
        } catch (RuntimeException exception) {
            LocalDateTime endTime = LocalDateTime.now();
            ProcessExecution execution = new ProcessExecution(
                    node.getId(),
                    node.getName(),
                    node.getDependencyIds(),
                    ProcessStatus.FAILED,
                    startTime,
                    endTime,
                    0,
                    exception.getMessage()
            );
            context.addExecution(execution);
            return execution;
        }
    }

    private ProcessExecution skipped(ProcessNode node, List<String> failedDependencies) {
        return new ProcessExecution(
                node.getId(),
                node.getName(),
                node.getDependencyIds(),
                ProcessStatus.SKIPPED,
                null,
                null,
                0,
                "Skipped because dependencies did not succeed: " + String.join(", ", failedDependencies)
        );
    }

    private Map<String, ProcessNode> mapById(List<ProcessNode> nodes) {
        Map<String, ProcessNode> nodesById = new HashMap<>();
        for (ProcessNode node : nodes) {
            nodesById.put(node.getId(), node);
        }
        return nodesById;
    }
}
