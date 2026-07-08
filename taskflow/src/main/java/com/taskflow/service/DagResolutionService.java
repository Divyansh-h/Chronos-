package com.taskflow.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.taskflow.model.Task;
import com.taskflow.model.Workflow;
import com.taskflow.model.enums.TaskStatus;
import com.taskflow.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Evaluates DAG dependencies and determines which tasks are ready to be enqueued.
 * Parses the workflow's dagDefinition JSON to build the dependency graph,
 * then cross-references task statuses to find unblocked tasks.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class DagResolutionService {

    private final TaskRepository taskRepository;

    /**
     * Validates the DAG definition for structural correctness:
     * - All dependsOn references must point to existing task names
     * - The graph must be acyclic (topological sort)
     *
     * @throws IllegalArgumentException if the DAG is invalid
     */
    public void validateDag(JsonNode dagDefinition) {
        if (dagDefinition == null || !dagDefinition.isArray()) {
            throw new IllegalArgumentException("dagDefinition must be a JSON array");
        }

        // Collect all declared task names
        Set<String> declaredNames = new LinkedHashSet<>();
        for (JsonNode node : dagDefinition) {
            String name = node.has("name") ? node.get("name").asText() : null;
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("Every task in dagDefinition must have a non-empty 'name'");
            }
            if (!declaredNames.add(name)) {
                throw new IllegalArgumentException("Duplicate task name in dagDefinition: " + name);
            }
        }

        // Build adjacency list and validate references
        Map<String, List<String>> adjacency = new LinkedHashMap<>();
        for (JsonNode node : dagDefinition) {
            String name = node.get("name").asText();
            List<String> deps = parseDependsOn(node);
            for (String dep : deps) {
                if (!declaredNames.contains(dep)) {
                    throw new IllegalArgumentException(
                            "Task '" + name + "' depends on '" + dep + "' which is not declared in dagDefinition");
                }
                if (dep.equals(name)) {
                    throw new IllegalArgumentException("Task '" + name + "' cannot depend on itself");
                }
            }
            adjacency.put(name, deps);
        }

        // Cycle detection via topological sort (Kahn's algorithm)
        detectCycles(declaredNames, adjacency);
    }

    /**
     * Finds all BLOCKED tasks whose upstream dependencies are now COMPLETED
     * and transitions them to PENDING so they become eligible for enqueueing.
     *
     * @return the list of tasks that were unblocked (transitioned BLOCKED → PENDING)
     */
    public List<Task> unblockReadyTasks(Workflow workflow, List<Task> tasks) {
        JsonNode dagDefinition = workflow.getDagDefinition();
        if (dagDefinition == null || !dagDefinition.isArray()) {
            return Collections.emptyList();
        }

        // Build a map of task name → Task
        Map<String, Task> taskByName = tasks.stream()
                .collect(Collectors.toMap(Task::getName, t -> t, (a, b) -> a));

        List<Task> unblocked = new ArrayList<>();

        for (Task task : tasks) {
            if (task.getStatus() != TaskStatus.BLOCKED) {
                continue;
            }

            // We still need statusByName for isTaskReady, or we can just adapt it
            Map<String, TaskStatus> statusByName = taskByName.entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getStatus()));

            if (isTaskReady(task, dagDefinition, statusByName)) {
                log.info("Unblocking task '{}' (id={}) — all dependencies completed",
                        task.getName(), task.getId());
                task.setStatus(TaskStatus.PENDING);
                
                // Merge dependency outputs into this task's inputs
                JsonNode taskNode = findNodeByName(dagDefinition, task.getName());
                if (taskNode != null) {
                    List<String> dependencies = parseDependsOn(taskNode);
                    if (!dependencies.isEmpty()) {
                        com.fasterxml.jackson.databind.node.ObjectNode mergedInputs = com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode();
                        for (String dep : dependencies) {
                            Task depTask = taskByName.get(dep);
                            if (depTask != null && depTask.getOutputData() != null) {
                                mergedInputs.set(dep, depTask.getOutputData());
                            }
                        }
                        if (!mergedInputs.isEmpty()) {
                            task.setInputData(mergedInputs);
                        }
                    }
                }

                unblocked.add(task);
            }
        }

        if (!unblocked.isEmpty()) {
            taskRepository.saveAll(unblocked);
        }

        return unblocked;
    }

    /**
     * Checks whether a specific task's upstream dependencies are all COMPLETED.
     */
    private boolean isTaskReady(Task task, JsonNode dagDefinition, Map<String, TaskStatus> statusByName) {
        // Find this task's node in the DAG definition
        JsonNode taskNode = findNodeByName(dagDefinition, task.getName());
        if (taskNode == null) {
            // Task not found in DAG — treat as ready (no constraints)
            return true;
        }

        List<String> dependencies = parseDependsOn(taskNode);
        if (dependencies.isEmpty()) {
            return true;
        }

        return dependencies.stream()
                .allMatch(depName -> statusByName.getOrDefault(depName, TaskStatus.BLOCKED) == TaskStatus.COMPLETED);
    }

    /**
     * Returns true if the given task name has any dependsOn entries in the DAG definition.
     */
    public boolean hasDependencies(JsonNode dagDefinition, String taskName) {
        JsonNode node = findNodeByName(dagDefinition, taskName);
        if (node == null) {
            return false;
        }
        return !parseDependsOn(node).isEmpty();
    }

    /**
     * Parses the "dependsOn" array from a DAG node.
     */
    private List<String> parseDependsOn(JsonNode node) {
        if (!node.has("dependsOn") || !node.get("dependsOn").isArray()) {
            return Collections.emptyList();
        }
        List<String> deps = new ArrayList<>();
        for (JsonNode dep : node.get("dependsOn")) {
            deps.add(dep.asText());
        }
        return deps;
    }

    /**
     * Finds a node in the dagDefinition array by its "name" field.
     */
    private JsonNode findNodeByName(JsonNode dagDefinition, String name) {
        for (JsonNode node : dagDefinition) {
            if (node.has("name") && node.get("name").asText().equals(name)) {
                return node;
            }
        }
        return null;
    }

    /**
     * Detects cycles using Kahn's algorithm (BFS topological sort).
     *
     * @throws IllegalArgumentException if a cycle is detected
     */
    private void detectCycles(Set<String> allNames, Map<String, List<String>> dependsOn) {
        // Build in-degree map and reverse adjacency (who depends on me)
        Map<String, Integer> inDegree = new LinkedHashMap<>();
        Map<String, List<String>> reverseAdj = new LinkedHashMap<>();

        for (String name : allNames) {
            inDegree.put(name, 0);
            reverseAdj.put(name, new ArrayList<>());
        }

        for (Map.Entry<String, List<String>> entry : dependsOn.entrySet()) {
            String task = entry.getKey();
            for (String dep : entry.getValue()) {
                // dep → task (dep must complete before task)
                reverseAdj.get(dep).add(task);
                inDegree.merge(task, 1, (a, b) -> a + b);
            }
        }

        // Start with nodes that have no incoming edges (root tasks)
        Queue<String> queue = new LinkedList<>();
        for (Map.Entry<String, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.add(entry.getKey());
            }
        }

        int processed = 0;
        while (!queue.isEmpty()) {
            String current = queue.poll();
            processed++;

            for (String downstream : reverseAdj.get(current)) {
                int newDegree = inDegree.get(downstream) - 1;
                inDegree.put(downstream, newDegree);
                if (newDegree == 0) {
                    queue.add(downstream);
                }
            }
        }

        if (processed != allNames.size()) {
            throw new IllegalArgumentException(
                    "Cycle detected in DAG definition — the dependency graph must be acyclic");
        }
    }
}
