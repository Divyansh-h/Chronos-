package com.taskflow.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskflow.model.Task;
import com.taskflow.model.Workflow;
import com.taskflow.model.enums.TaskStatus;
import com.taskflow.repository.TaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
public class DagResolutionServiceTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private DagResolutionService dagResolutionService;

    // ========== validateDag tests ==========

    @Test
    void validateDag_ShouldAcceptValidLinearDag() throws Exception {
        JsonNode dag = mapper.readTree("""
            [
                { "name": "A" },
                { "name": "B", "dependsOn": ["A"] },
                { "name": "C", "dependsOn": ["B"] }
            ]
        """);
        assertDoesNotThrow(() -> dagResolutionService.validateDag(dag));
    }

    @Test
    void validateDag_ShouldAcceptValidDiamondDag() throws Exception {
        JsonNode dag = mapper.readTree("""
            [
                { "name": "A" },
                { "name": "B", "dependsOn": ["A"] },
                { "name": "C", "dependsOn": ["A"] },
                { "name": "D", "dependsOn": ["B", "C"] }
            ]
        """);
        assertDoesNotThrow(() -> dagResolutionService.validateDag(dag));
    }

    @Test
    void validateDag_ShouldAcceptTasksWithNoDependencies() throws Exception {
        JsonNode dag = mapper.readTree("""
            [
                { "name": "Task1" },
                { "name": "Task2" },
                { "name": "Task3" }
            ]
        """);
        assertDoesNotThrow(() -> dagResolutionService.validateDag(dag));
    }

    @Test
    void validateDag_ShouldRejectNullInput() {
        assertThrows(IllegalArgumentException.class, () -> dagResolutionService.validateDag(null));
    }

    @Test
    void validateDag_ShouldRejectNonArrayInput() throws Exception {
        JsonNode dag = mapper.readTree("{ \"name\": \"not an array\" }");
        assertThrows(IllegalArgumentException.class, () -> dagResolutionService.validateDag(dag));
    }

    @Test
    void validateDag_ShouldRejectDuplicateTaskNames() throws Exception {
        JsonNode dag = mapper.readTree("""
            [
                { "name": "A" },
                { "name": "A" }
            ]
        """);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> dagResolutionService.validateDag(dag));
        assertTrue(ex.getMessage().contains("Duplicate"));
    }

    @Test
    void validateDag_ShouldRejectMissingTaskName() throws Exception {
        JsonNode dag = mapper.readTree("""
            [
                { "type": "extract" }
            ]
        """);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> dagResolutionService.validateDag(dag));
        assertTrue(ex.getMessage().contains("non-empty 'name'"));
    }

    @Test
    void validateDag_ShouldRejectBlankTaskName() throws Exception {
        JsonNode dag = mapper.readTree("""
            [
                { "name": "  " }
            ]
        """);
        assertThrows(IllegalArgumentException.class, () -> dagResolutionService.validateDag(dag));
    }

    @Test
    void validateDag_ShouldRejectUndeclaredDependency() throws Exception {
        JsonNode dag = mapper.readTree("""
            [
                { "name": "A" },
                { "name": "B", "dependsOn": ["NonExistent"] }
            ]
        """);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> dagResolutionService.validateDag(dag));
        assertTrue(ex.getMessage().contains("not declared"));
    }

    @Test
    void validateDag_ShouldRejectSelfDependency() throws Exception {
        JsonNode dag = mapper.readTree("""
            [
                { "name": "A", "dependsOn": ["A"] }
            ]
        """);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> dagResolutionService.validateDag(dag));
        assertTrue(ex.getMessage().contains("cannot depend on itself"));
    }

    @Test
    void validateDag_ShouldRejectCyclicDependency() throws Exception {
        JsonNode dag = mapper.readTree("""
            [
                { "name": "A", "dependsOn": ["C"] },
                { "name": "B", "dependsOn": ["A"] },
                { "name": "C", "dependsOn": ["B"] }
            ]
        """);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> dagResolutionService.validateDag(dag));
        assertTrue(ex.getMessage().contains("Cycle detected"));
    }

    // ========== hasDependencies tests ==========

    @Test
    void hasDependencies_ShouldReturnTrueForTaskWithDeps() throws Exception {
        JsonNode dag = mapper.readTree("""
            [
                { "name": "A" },
                { "name": "B", "dependsOn": ["A"] }
            ]
        """);
        assertTrue(dagResolutionService.hasDependencies(dag, "B"));
    }

    @Test
    void hasDependencies_ShouldReturnFalseForRootTask() throws Exception {
        JsonNode dag = mapper.readTree("""
            [
                { "name": "A" },
                { "name": "B", "dependsOn": ["A"] }
            ]
        """);
        assertFalse(dagResolutionService.hasDependencies(dag, "A"));
    }

    @Test
    void hasDependencies_ShouldReturnFalseForUnknownTask() throws Exception {
        JsonNode dag = mapper.readTree("[{ \"name\": \"A\" }]");
        assertFalse(dagResolutionService.hasDependencies(dag, "NonExistent"));
    }

    // ========== unblockReadyTasks tests ==========

    @Test
    void unblockReadyTasks_ShouldUnblockTaskWhenDepsCompleted() throws Exception {
        JsonNode dag = mapper.readTree("""
            [
                { "name": "A" },
                { "name": "B", "dependsOn": ["A"] }
            ]
        """);

        Workflow workflow = new Workflow();
        workflow.setId(UUID.randomUUID());
        workflow.setDagDefinition(dag);

        Task taskA = createTask("A", TaskStatus.COMPLETED);
        Task taskB = createTask("B", TaskStatus.BLOCKED);

        when(taskRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

        List<Task> unblocked = dagResolutionService.unblockReadyTasks(workflow, Arrays.asList(taskA, taskB));

        assertEquals(1, unblocked.size());
        assertEquals("B", unblocked.get(0).getName());
        assertEquals(TaskStatus.PENDING, unblocked.get(0).getStatus());
        verify(taskRepository, times(1)).saveAll(anyList());
    }

    @Test
    void unblockReadyTasks_ShouldNotUnblockWhenDepsNotCompleted() throws Exception {
        JsonNode dag = mapper.readTree("""
            [
                { "name": "A" },
                { "name": "B", "dependsOn": ["A"] }
            ]
        """);

        Workflow workflow = new Workflow();
        workflow.setId(UUID.randomUUID());
        workflow.setDagDefinition(dag);

        Task taskA = createTask("A", TaskStatus.RUNNING);
        Task taskB = createTask("B", TaskStatus.BLOCKED);

        List<Task> unblocked = dagResolutionService.unblockReadyTasks(workflow, Arrays.asList(taskA, taskB));

        assertTrue(unblocked.isEmpty());
        verify(taskRepository, never()).saveAll(anyList());
    }

    @Test
    void unblockReadyTasks_ShouldHandleDiamondDependency() throws Exception {
        JsonNode dag = mapper.readTree("""
            [
                { "name": "A" },
                { "name": "B", "dependsOn": ["A"] },
                { "name": "C", "dependsOn": ["A"] },
                { "name": "D", "dependsOn": ["B", "C"] }
            ]
        """);

        Workflow workflow = new Workflow();
        workflow.setId(UUID.randomUUID());
        workflow.setDagDefinition(dag);

        Task taskA = createTask("A", TaskStatus.COMPLETED);
        Task taskB = createTask("B", TaskStatus.COMPLETED);
        Task taskC = createTask("C", TaskStatus.COMPLETED);
        Task taskD = createTask("D", TaskStatus.BLOCKED);

        when(taskRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

        List<Task> unblocked = dagResolutionService.unblockReadyTasks(workflow,
                Arrays.asList(taskA, taskB, taskC, taskD));

        assertEquals(1, unblocked.size());
        assertEquals("D", unblocked.get(0).getName());
        assertEquals(TaskStatus.PENDING, unblocked.get(0).getStatus());
    }

    @Test
    void unblockReadyTasks_ShouldNotUnblockDiamondWhenOnlyPartialDepsCompleted() throws Exception {
        JsonNode dag = mapper.readTree("""
            [
                { "name": "A" },
                { "name": "B", "dependsOn": ["A"] },
                { "name": "C", "dependsOn": ["A"] },
                { "name": "D", "dependsOn": ["B", "C"] }
            ]
        """);

        Workflow workflow = new Workflow();
        workflow.setId(UUID.randomUUID());
        workflow.setDagDefinition(dag);

        Task taskA = createTask("A", TaskStatus.COMPLETED);
        Task taskB = createTask("B", TaskStatus.COMPLETED);
        Task taskC = createTask("C", TaskStatus.RUNNING);  // Not yet completed
        Task taskD = createTask("D", TaskStatus.BLOCKED);

        List<Task> unblocked = dagResolutionService.unblockReadyTasks(workflow,
                Arrays.asList(taskA, taskB, taskC, taskD));

        assertTrue(unblocked.isEmpty());
    }

    @Test
    void unblockReadyTasks_ShouldReturnEmptyForNullDag() {
        Workflow workflow = new Workflow();
        workflow.setId(UUID.randomUUID());
        workflow.setDagDefinition(null);

        List<Task> unblocked = dagResolutionService.unblockReadyTasks(workflow, List.of());
        assertTrue(unblocked.isEmpty());
    }

    @Test
    void unblockReadyTasks_ShouldReturnEmptyForNonArrayDag() {
        Workflow workflow = new Workflow();
        workflow.setId(UUID.randomUUID());
        workflow.setDagDefinition(mapper.createObjectNode());

        List<Task> unblocked = dagResolutionService.unblockReadyTasks(workflow, List.of());
        assertTrue(unblocked.isEmpty());
    }

    @Test
    void unblockReadyTasks_ShouldSkipNonBlockedTasks() throws Exception {
        JsonNode dag = mapper.readTree("""
            [
                { "name": "A" },
                { "name": "B", "dependsOn": ["A"] }
            ]
        """);

        Workflow workflow = new Workflow();
        workflow.setId(UUID.randomUUID());
        workflow.setDagDefinition(dag);

        Task taskA = createTask("A", TaskStatus.COMPLETED);
        Task taskB = createTask("B", TaskStatus.RUNNING);  // Already running, not BLOCKED

        List<Task> unblocked = dagResolutionService.unblockReadyTasks(workflow, Arrays.asList(taskA, taskB));
        assertTrue(unblocked.isEmpty());
    }

    private Task createTask(String name, TaskStatus status) {
        Task task = new Task();
        task.setId(UUID.randomUUID());
        task.setName(name);
        task.setStatus(status);
        return task;
    }
}
