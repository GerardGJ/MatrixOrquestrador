package com.matrix.orquestrador.model;

import java.util.List;

public class ProcessNode {

    private final String id;
    private final String name;
    private final List<String> dependencyIds;
    private final ProcessAction action;

    public ProcessNode(String id, String name, List<String> dependencyIds, ProcessAction action) {
        this.id = id;
        this.name = name;
        this.dependencyIds = List.copyOf(dependencyIds);
        this.action = action;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<String> getDependencyIds() {
        return dependencyIds;
    }

    public ProcessAction getAction() {
        return action;
    }
}
