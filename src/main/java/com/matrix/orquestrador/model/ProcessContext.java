package com.matrix.orquestrador.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class ProcessContext {

    private final Map<String, ProcessOutput> outputs = new ConcurrentHashMap<>();
    private final List<ProcessExecution> executions = new CopyOnWriteArrayList<>();

    public void putOutput(String processId, ProcessOutput output) {
        outputs.put(processId, output);
    }

    public Optional<ProcessOutput> getOutput(String processId) {
        return Optional.ofNullable(outputs.get(processId));
    }

    public ProcessOutput requireOutput(String processId) {
        return getOutput(processId)
                .orElseThrow(() -> new IllegalStateException("Missing output for process " + processId));
    }

    public Map<String, ProcessOutput> getOutputsSnapshot() {
        return Collections.unmodifiableMap(new ConcurrentHashMap<>(outputs));
    }

    public void addExecution(ProcessExecution execution) {
        executions.add(execution);
    }

    public List<ProcessExecution> getExecutionsSnapshot() {
        return Collections.unmodifiableList(new ArrayList<>(executions));
    }
}
