package com.franosch.manager.controller;

import com.franosch.manager.model.ResourceRequirements;
import com.franosch.manager.model.WorkerInfo;
import com.franosch.manager.service.WorkerRegistry;
import com.franosch.manager.service.ExecutionService;
import com.franosch.manager.model.ExecutionStatus;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("worker/")
public class WorkerController {

    private final WorkerRegistry workerRegistry;
    private final ExecutionService executionService;

    @Autowired
    public WorkerController(WorkerRegistry workerRegistry, ExecutionService executionService) {
        this.workerRegistry = workerRegistry;
        this.executionService = executionService;
    }

    @PostMapping("register")
    public ResponseEntity<String> registerWorker(@RequestBody WorkerRegistrationRequest request) {
        try {
            workerRegistry.registerWorker(
                    request.getWorkerId(),
                    request.getBaseUrl(),
                    request.getCapacity()
            );
            return ResponseEntity.ok("Worker registered successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Failed to register worker: " + e.getMessage());
        }
    }

    @PostMapping("deregister/{workerId}")
    public ResponseEntity<String> deregisterWorker(@PathVariable String workerId) {
        workerRegistry.deregisterWorker(workerId);
        return ResponseEntity.ok("Worker deregistered successfully");
    }

    @GetMapping("list")
    public ResponseEntity<List<WorkerInfo>> listWorkers() {
        return ResponseEntity.ok(workerRegistry.getAllWorkers());
    }

    @GetMapping("available")
    public ResponseEntity<List<WorkerInfo>> getAvailableWorkers() {
        return ResponseEntity.ok(workerRegistry.getAvailableWorkers());
    }

    @PutMapping("execution/{executionId}/status")
    public ResponseEntity<String> updateExecutionStatus(
            @PathVariable String executionId,
            @RequestBody StatusUpdateRequest request) {
        try {
            executionService.updateExecutionStatus(
                    executionId,
                    request.getStatus(),
                    request.getResult(),
                    request.getError()
            );
            return ResponseEntity.ok("Execution status updated");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Failed to update status: " + e.getMessage());
        }
    }

    @Data
    public static class WorkerRegistrationRequest {
        private String workerId;
        private String baseUrl;
        private ResourceRequirements capacity;
    }


    @Data
    public static class StatusUpdateRequest {
        private ExecutionStatus status;
        private String result;
        private String error;
    }
}
