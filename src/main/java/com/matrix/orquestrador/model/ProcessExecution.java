package com.matrix.orquestrador.model;

import java.time.Duration;
import java.time.LocalDateTime;

public class ProcessExecution {

    private final String processName;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;
    private final long durationMillis;
    private final int outputRows;

    public ProcessExecution(
            String processName,
            LocalDateTime startTime,
            LocalDateTime endTime,
            int outputRows
    ) {
        this.processName = processName;
        this.startTime = startTime;
        this.endTime = endTime;
        this.durationMillis = Duration.between(startTime, endTime).toMillis();
        this.outputRows = outputRows;
    }

    public String getProcessName() {
        return processName;
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
}
