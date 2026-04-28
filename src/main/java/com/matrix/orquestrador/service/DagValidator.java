package com.matrix.orquestrador.service;

import com.matrix.orquestrador.model.ProcessNode;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DagValidator {

    public void validate(List<ProcessNode> nodes) {
        Map<String, ProcessNode> nodesById = new HashMap<>();
        for (ProcessNode node : nodes) {
            if (node.getId() == null || node.getId().isBlank()) {
                throw new IllegalArgumentException("Process id must not be blank.");
            }
            if (nodesById.put(node.getId(), node) != null) {
                throw new IllegalArgumentException("Duplicate process id: " + node.getId());
            }
        }

        for (ProcessNode node : nodes) {
            for (String dependencyId : node.getDependencyIds()) {
                if (!nodesById.containsKey(dependencyId)) {
                    throw new IllegalArgumentException(
                            "Process " + node.getId() + " depends on missing process " + dependencyId
                    );
                }
            }
        }

        Set<String> visiting = new HashSet<>();
        Set<String> visited = new HashSet<>();
        for (ProcessNode node : nodes) {
            detectCycle(node, nodesById, visiting, visited);
        }
    }

    private void detectCycle(
            ProcessNode node,
            Map<String, ProcessNode> nodesById,
            Set<String> visiting,
            Set<String> visited
    ) {
        if (visited.contains(node.getId())) {
            return;
        }
        if (!visiting.add(node.getId())) {
            throw new IllegalArgumentException("Cycle detected around process " + node.getId());
        }

        for (String dependencyId : node.getDependencyIds()) {
            detectCycle(nodesById.get(dependencyId), nodesById, visiting, visited);
        }

        visiting.remove(node.getId());
        visited.add(node.getId());
    }
}
