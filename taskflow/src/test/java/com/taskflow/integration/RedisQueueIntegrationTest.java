package com.taskflow.integration;

import com.taskflow.model.Task;
import com.taskflow.model.Workflow;
import com.taskflow.model.enums.TaskStatus;
import com.taskflow.model.enums.WorkflowStatus;
import com.taskflow.repository.TaskRepository;
import com.taskflow.repository.WorkflowRepository;
import com.taskflow.service.TaskQueueService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RedisQueueIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private TaskQueueService taskQueueService;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private WorkflowRepository workflowRepository;

    @Test
    void shouldEnqueueAndPollMultipleTasksSuccessfully() {
        // Arrange: Create a Workflow first to satisfy foreign key constraints
        Workflow workflow = new Workflow();
        workflow.setName("Integration Workflow");
        workflow.setStatus(WorkflowStatus.PENDING);
        workflow.setDagDefinition(new ObjectMapper().createObjectNode());
        workflow = workflowRepository.save(workflow);

        List<Task> savedTasks = new ArrayList<>();
        
        for (int i = 0; i < 5; i++) {
            Task task = new Task();
            task.setWorkflowId(workflow.getId());
            task.setName("Integration Task " + i);
            task.setStatus(TaskStatus.PENDING);
            task.setMaxRetries(3);
            task.setRetryCount(0);
            savedTasks.add(taskRepository.save(task));
        }

        // Act 1: Enqueue them via Redis
        for (Task task : savedTasks) {
            taskQueueService.enqueueTask(task.getId());
            
            // Verify DB status changed to QUEUED natively
            Task updatedTask = taskRepository.findById(task.getId()).orElseThrow();
            assertEquals(TaskStatus.QUEUED, updatedTask.getStatus());
        }

        // Verify Redis Queue Size
        assertEquals(5, taskQueueService.getQueueSize());

        // Act 2 & Assert: Poll them one by one
        String workerId = "integration-worker-1";
        
        for (int i = 0; i < 5; i++) {
            Optional<Task> polledTaskOpt = taskQueueService.pollTask(workerId);
            assertTrue(polledTaskOpt.isPresent(), "Should pop a task from the Redis container");
            
            Task polledTask = polledTaskOpt.get();
            assertEquals(TaskStatus.RUNNING, polledTask.getStatus());
            assertEquals(workerId, polledTask.getAssignedWorker());
            
            // Verify the RUNNING state is actually persisted in Postgres
            Task dbTask = taskRepository.findById(polledTask.getId()).orElseThrow();
            assertEquals(TaskStatus.RUNNING, dbTask.getStatus());
            assertEquals(workerId, dbTask.getAssignedWorker());
        }

        // Assert Redis queue is completely drained
        assertEquals(0, taskQueueService.getQueueSize());
        
        // Assert blocking pop safely times out on empty queue
        assertTrue(taskQueueService.pollTask(workerId).isEmpty());
    }

    @Test
    void shouldPreventRaceConditionsDuringConcurrentPolling() throws InterruptedException {
        // Arrange: Create a Workflow first
        Workflow workflow = new Workflow();
        workflow.setName("Integration Workflow 2");
        workflow.setStatus(WorkflowStatus.PENDING);
        workflow.setDagDefinition(new ObjectMapper().createObjectNode());
        workflow = workflowRepository.save(workflow);

        // Arrange: Create exactly 1 task
        Task task = new Task();
        task.setWorkflowId(workflow.getId());
        task.setName("Concurrent Target");
        task.setStatus(TaskStatus.PENDING);
        task.setMaxRetries(3);
        task.setRetryCount(0);
        task = taskRepository.save(task);

        taskQueueService.enqueueTask(task.getId());

        int threadCount = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        AtomicInteger successfulClaims = new AtomicInteger(0);
        AtomicInteger emptyResults = new AtomicInteger(0);

        // Act: Spawn 10 simultaneous threads to attack the queue
        for (int i = 0; i < threadCount; i++) {
            final String workerId = "concurrent-worker-" + i;
            executorService.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await(); // Synchronize all threads to start at the exact same millisecond
                    
                    Optional<Task> claimedTask = taskQueueService.pollTask(workerId);
                    if (claimedTask.isPresent()) {
                        successfulClaims.incrementAndGet();
                    } else {
                        emptyResults.incrementAndGet();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        readyLatch.await(); // Wait until all 10 threads are spun up and waiting
        startLatch.countDown(); // Release the hounds! (Fire all threads simultaneously)
        doneLatch.await(); // Wait for all threads to complete execution
        executorService.shutdown();

        // Assert: Redis BLPOP guarantees absolute atomicity
        assertEquals(1, successfulClaims.get(), "Exactly ONE worker should successfully claim the task");
        assertEquals(threadCount - 1, emptyResults.get(), "All other 9 workers should safely receive empty results");
        
        // Verify final DB state is untouched by race conditions
        Task dbTask = taskRepository.findById(task.getId()).orElseThrow();
        assertEquals(TaskStatus.RUNNING, dbTask.getStatus());
    }
}
