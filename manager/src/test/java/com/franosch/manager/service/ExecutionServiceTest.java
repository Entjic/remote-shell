package com.franosch.manager.service;

import com.franosch.manager.model.ExecutionRequest;
import com.franosch.manager.model.ExecutionStatus;
import com.franosch.manager.model.ResourceRequirements;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class ExecutionServiceTest {

    @Autowired
    private ExecutionService executionService;

    @Autowired
    private WorkerRegistry workerRegistry;

    private ResourceRequirements testCapacity;

    @MockitoBean
    private RestTemplate restTemplate;

    @BeforeEach
    void setUp() {
        testCapacity = new ResourceRequirements(4, 2048);
        // Clear any existing workers
        workerRegistry.getAllWorkers()
                .forEach(w -> workerRegistry.deregisterWorker(w.getId()));
    }

    @Test
    void testSubmitExecutionQueuesTaskToAvailableWorker() {
        workerRegistry.registerWorker("worker-1", "http://localhost:8081", testCapacity);

        ExecutionRequest request = new ExecutionRequest("echo hello", testCapacity);
        var execution = executionService.submitExecution(request);

        assertThat(execution).isNotNull();
        assertThat(execution.getId()).isNotNull();
        assertThat(execution.getStatus()).isEqualTo(ExecutionStatus.QUEUED);
        assertThat(execution.getCommand()).isEqualTo("echo hello");
        assertThat(execution.getWorkerId()).isEqualTo("worker-1");
    }

    @Test
    void testSubmitExecutionThrowsWhenNoAvailableWorker() {
        ExecutionRequest request = new ExecutionRequest("echo hello", testCapacity);

        assertThatThrownBy(() -> executionService.submitExecution(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("No available worker");
    }

    @Test
    void testGetExecution() {
        workerRegistry.registerWorker("worker-1", "http://localhost:8081", testCapacity);

        ExecutionRequest request = new ExecutionRequest("echo hello", testCapacity);
        var submitted = executionService.submitExecution(request);

        var retrieved = executionService.getExecution(submitted.getId());
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getId()).isEqualTo(submitted.getId());
    }

    @Test
    void testGetNonExistentExecution() {
        var execution = executionService.getExecution("non-existent-id");
        assertThat(execution).isNull();
    }

    @Test
    void testUpdateExecutionStatus() {
        workerRegistry.registerWorker("worker-1", "http://localhost:8081", testCapacity);

        ExecutionRequest request = new ExecutionRequest("echo hello", testCapacity);
        var submitted = executionService.submitExecution(request);

        executionService.updateExecutionStatus(submitted.getId(), ExecutionStatus.IN_PROGRESS, null, null);

        var execution = executionService.getExecution(submitted.getId());
        assertThat(execution.getStatus()).isEqualTo(ExecutionStatus.IN_PROGRESS);
    }

    @Test
    void testUpdateExecutionStatusWithResult() {
        workerRegistry.registerWorker("worker-1", "http://localhost:8081", testCapacity);

        ExecutionRequest request = new ExecutionRequest("echo hello", testCapacity);
        var submitted = executionService.submitExecution(request);

        executionService.updateExecutionStatus(
                submitted.getId(),
                ExecutionStatus.FINISHED,
                "hello world",
                null
        );

        var execution = executionService.getExecution(submitted.getId());
        assertThat(execution.getStatus()).isEqualTo(ExecutionStatus.FINISHED);
        assertThat(execution.getResult()).isEqualTo("hello world");
    }

    @Test
    void testUpdateExecutionStatusWithError() {
        workerRegistry.registerWorker("worker-1", "http://localhost:8081", testCapacity);

        ExecutionRequest request = new ExecutionRequest("echo hello", testCapacity);
        var submitted = executionService.submitExecution(request);

        executionService.updateExecutionStatus(
                submitted.getId(),
                ExecutionStatus.FAILED,
                null,
                "Command not found"
        );

        var execution = executionService.getExecution(submitted.getId());
        assertThat(execution.getStatus()).isEqualTo(ExecutionStatus.FAILED);
        assertThat(execution.getError()).isEqualTo("Command not found");
    }

    @Test
    void testRoundRobinDistribution() {
        workerRegistry.registerWorker("worker-1", "http://localhost:8081", testCapacity);
        workerRegistry.registerWorker("worker-2", "http://localhost:8082", testCapacity);
        workerRegistry.registerWorker("worker-3", "http://localhost:8083", testCapacity);

        ExecutionRequest request = new ExecutionRequest("echo hello", testCapacity);

        var exec1 = executionService.submitExecution(request);
        var exec2 = executionService.submitExecution(request);
        var exec3 = executionService.submitExecution(request);
        var exec4 = executionService.submitExecution(request);

        assertThat(exec1.getWorkerId()).isEqualTo("worker-1");
        assertThat(exec2.getWorkerId()).isEqualTo("worker-2");
        assertThat(exec3.getWorkerId()).isEqualTo("worker-3");
        assertThat(exec4.getWorkerId()).isEqualTo("worker-1");
    }

    @Test
    void testSubmitExecutionWithoutResources() {
        workerRegistry.registerWorker("worker-1", "http://localhost:8081", testCapacity);

        ExecutionRequest request = new ExecutionRequest("echo hello", null);
        var execution = executionService.submitExecution(request);

        assertThat(execution).isNotNull();
        assertThat(execution.getWorkerId()).isEqualTo("worker-1");
    }
}
