package com.matrix.orquestrador.model;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

public class ProcessExecution {

    private final String processId;
    private final String processName;
    private final List<String> dependencyIds;
    private final ProcessStatus status;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;
    private final long durationMillis;
    private final int outputRows;
    private final String message;

    public ProcessExecution(
            String processId,
            String processName,
            List<String> dependencyIds,
            ProcessStatus status,
            LocalDateTime startTime,
            LocalDateTime endTime,
            int outputRows,
            String message
    ) {
        this.processId = processId;
        this.processName = processName;
        this.dependencyIds = List.copyOf(dependencyIds);
        this.status = status;
        this.startTime = startTime;
        this.endTime = endTime;
        this.durationMillis = startTime == null || endTime == null
                ? 0
                : Duration.between(startTime, endTime).toMillis();
        this.outputRows = outputRows;
        this.message = message;
    }

    public String getProcessId() {
        return processId;
    }

    public String getProcessName() {
        return processName;
    }

    public List<String> getDependencyIds() {
        return dependencyIds;
    }

    public ProcessStatus getStatus() {
        return status;
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

    public int getOutputRows() {
        return outputRows;
    }

    public String getMessage() {
        return message;
    }
}
