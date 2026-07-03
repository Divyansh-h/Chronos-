package com.taskflow.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class TaskTest {

    @Test
    void retryCountAndMaxRetries_ShouldSetAndGetCorrectly() {
        Task task = new Task();
        task.setRetryCount(2);
        task.setMaxRetries(5);

        assertEquals(2, task.getRetryCount());
        assertEquals(5, task.getMaxRetries());
        assertTrue(task.getRetryCount() < task.getMaxRetries(), "Retry count should be less than max retries");
    }

    @Test
    void equals_ShouldReturnTrueForSameInstance() {
        Task task = new Task();
        task.setId(UUID.randomUUID());

        assertEquals(task, task);
    }

    @Test
    void equals_ShouldReturnTrueForSameId() {
        UUID sharedId = UUID.randomUUID();

        Task task1 = new Task();
        task1.setId(sharedId);

        Task task2 = new Task();
        task2.setId(sharedId);

        assertEquals(task1, task2);
        assertEquals(task2, task1);
    }

    @Test
    void equals_ShouldReturnFalseForDifferentId() {
        Task task1 = new Task();
        task1.setId(UUID.randomUUID());

        Task task2 = new Task();
        task2.setId(UUID.randomUUID());

        assertNotEquals(task1, task2);
    }

    @Test
    void equals_ShouldReturnFalseWhenIdIsNull() {
        Task task1 = new Task(); // id is null

        Task task2 = new Task();
        task2.setId(UUID.randomUUID());

        assertNotEquals(task1, task2);
        assertNotEquals(task2, task1);
    }

    @Test
    void equals_ShouldReturnFalseForNullOrDifferentClass() {
        Task task = new Task();
        task.setId(UUID.randomUUID());

        assertNotEquals(null, task);
        assertNotEquals("Not a task", task);
    }

    @Test
    void hashCode_ShouldBeConsistent() {
        Task task1 = new Task();
        Task task2 = new Task();

        assertEquals(task1.hashCode(), task2.hashCode(), "HashCode should be constant across instances to satisfy Hibernate entity requirements");
    }
}
