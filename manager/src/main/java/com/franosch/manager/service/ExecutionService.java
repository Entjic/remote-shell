package com.franosch.manager.service;

import com.franosch.manager.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ExecutionService {
    private final Map<String, Execution> executions = new ConcurrentHashMap<>();
    private final WorkerRegistry workerRegistry;
    private final RestTemplate restTemplate;

    @Value("${worker.secret}")
    private String secret;

    @Autowired
    public ExecutionService(WorkerRegistry workerRegistry, RestTemplate restTemplate) {
        this.workerRegistry = workerRegistry;
        this.restTemplate = restTemplate;
    }

    public Execution submitExecution(ExecutionRequest request) {
        // Find an available worker
        WorkerInfo worker = workerRegistry.getAvailableWorkerForCapacity(request.getResources());
        if (worker == null) {
            throw new RuntimeException("No available worker with sufficient resources");
        }

        // Create execution
        String executionId = UUID.randomUUID().toString();
        Execution execution = new Execution(executionId, request.getCommand(), request.getResources());
        execution.setWorkerId(worker.getId());
        execution.setStatus(ExecutionStatus.QUEUED);

        executions.put(executionId, execution);

        // Send command to worker asynchronously
        new Thread(() -> sendCommandToWorker(execution, worker)).start();

        return execution;
    }

    private void sendCommandToWorker(Execution execution, WorkerInfo worker) {
        try {
            String url = worker.getBaseUrl() + "/executor/execute";
            ExecutionRequest request = new ExecutionRequest(
                    execution.getCommand(),
                    execution.getResources(),
                    execution.getId(),
                    this.secret
            );
            restTemplate.postForObject(url, request, String.class);

        } catch (Exception e) {
            execution.setStatus(ExecutionStatus.FAILED);
            execution.setError("Failed to send command to worker: " + e.getMessage());
        }
    }

    public Execution getExecution(String executionId) {
        return executions.get(executionId);
    }

    public void updateExecutionStatus(String executionId, ExecutionStatus status, String result, String error) {
        Execution execution = executions.get(executionId);
        if (execution != null) {
            execution.setStatus(status);
            if (result != null) {
                execution.setResult(result);
            }
            if (error != null) {
                execution.setError(error);
            }
        }
    }

    public List<Execution> getAllExecutions() {
        return new ArrayList<>(executions.values());
    }
}
