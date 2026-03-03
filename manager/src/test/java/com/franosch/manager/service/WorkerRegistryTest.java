package com.franosch.manager.service;

import com.franosch.manager.model.ResourceRequirements;
import com.franosch.manager.model.WorkerInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class WorkerRegistryTest {

    private WorkerRegistry workerRegistry;
    private ResourceRequirements testCapacity;

    @BeforeEach
    void setUp() {
        workerRegistry = new WorkerRegistry();
        testCapacity = new ResourceRequirements(4, 2048);
    }

    @Test
    void testRegisterWorker() {
        workerRegistry.registerWorker("worker-1", "http://localhost:8081", testCapacity);

        WorkerInfo worker = workerRegistry.getWorker("worker-1");
        assertThat(worker).isNotNull();
        assertThat(worker.getId()).isEqualTo("worker-1");
        assertThat(worker.getBaseUrl()).isEqualTo("http://localhost:8081");
        assertThat(worker.isAvailable()).isTrue();
    }

    @Test
    void testDeregisterWorker() {
        workerRegistry.registerWorker("worker-1", "http://localhost:8081", testCapacity);
        workerRegistry.deregisterWorker("worker-1");

        WorkerInfo worker = workerRegistry.getWorker("worker-1");
        assertThat(worker).isNull();
    }

    @Test
    void testGetAllWorkers() {
        workerRegistry.registerWorker("worker-1", "http://localhost:8081", testCapacity);
        workerRegistry.registerWorker("worker-2", "http://localhost:8082", testCapacity);

        List<WorkerInfo> workers = workerRegistry.getAllWorkers();
        assertThat(workers).hasSize(2);
        assertThat(workers).extracting(WorkerInfo::getId).containsExactlyInAnyOrder("worker-1", "worker-2");
    }

    @Test
    void testGetAvailableWorkers() {
        workerRegistry.registerWorker("worker-1", "http://localhost:8081", testCapacity);
        workerRegistry.registerWorker("worker-2", "http://localhost:8082", testCapacity);
        workerRegistry.markWorkerBusy("worker-1");

        List<WorkerInfo> available = workerRegistry.getAvailableWorkers();
        assertThat(available).hasSize(1);
        assertThat(available.get(0).getId()).isEqualTo("worker-2");
    }

    @Test
    void testMarkWorkerBusy() {
        workerRegistry.registerWorker("worker-1", "http://localhost:8081", testCapacity);
        assertThat(workerRegistry.getWorker("worker-1").isAvailable()).isTrue();

        workerRegistry.markWorkerBusy("worker-1");
        assertThat(workerRegistry.getWorker("worker-1").isAvailable()).isFalse();
    }

    @Test
    void testMarkWorkerAvailable() {
        workerRegistry.registerWorker("worker-1", "http://localhost:8081", testCapacity);
        workerRegistry.markWorkerBusy("worker-1");

        workerRegistry.markWorkerAvailable("worker-1");
        assertThat(workerRegistry.getWorker("worker-1").isAvailable()).isTrue();
    }

    @Test
    void testRoundRobinSelection() {
        workerRegistry.registerWorker("worker-1", "http://localhost:8081", testCapacity);
        workerRegistry.registerWorker("worker-2", "http://localhost:8082", testCapacity);
        workerRegistry.registerWorker("worker-3", "http://localhost:8083", testCapacity);

        WorkerInfo w1 = workerRegistry.getAvailableWorkerForCapacity(testCapacity);
        WorkerInfo w2 = workerRegistry.getAvailableWorkerForCapacity(testCapacity);
        WorkerInfo w3 = workerRegistry.getAvailableWorkerForCapacity(testCapacity);
        WorkerInfo w4 = workerRegistry.getAvailableWorkerForCapacity(testCapacity);

        assertThat(w1.getId()).isEqualTo("worker-1");
        assertThat(w2.getId()).isEqualTo("worker-2");
        assertThat(w3.getId()).isEqualTo("worker-3");
        assertThat(w4.getId()).isEqualTo("worker-1");
    }

    @Test
    void testWorkerSelectionWithInsufficientCapacity() {
        ResourceRequirements smallCapacity = new ResourceRequirements(2, 1024);
        workerRegistry.registerWorker("worker-1", "http://localhost:8081", smallCapacity);

        ResourceRequirements largeRequirement = new ResourceRequirements(8, 4096);
        WorkerInfo worker = workerRegistry.getAvailableWorkerForCapacity(largeRequirement);

        assertThat(worker).isNull();
    }

    @Test
    void testWorkerSelectionWithSufficientCapacity() {
        ResourceRequirements capacity = new ResourceRequirements(8, 4096);
        workerRegistry.registerWorker("worker-1", "http://localhost:8081", capacity);

        ResourceRequirements requirement = new ResourceRequirements(4, 2048);
        WorkerInfo worker = workerRegistry.getAvailableWorkerForCapacity(requirement);

        assertThat(worker).isNotNull();
        assertThat(worker.getId()).isEqualTo("worker-1");
    }

    @Test
    void testNoWorkerAvailable() {
        WorkerInfo worker = workerRegistry.getAvailableWorkerForCapacity(testCapacity);
        assertThat(worker).isNull();
    }

    @Test
    void testAllWorkersBusy() {
        workerRegistry.registerWorker("worker-1", "http://localhost:8081", testCapacity);
        workerRegistry.registerWorker("worker-2", "http://localhost:8082", testCapacity);

        workerRegistry.markWorkerBusy("worker-1");
        workerRegistry.markWorkerBusy("worker-2");

        WorkerInfo worker = workerRegistry.getAvailableWorkerForCapacity(testCapacity);
        assertThat(worker).isNull();
    }
}
