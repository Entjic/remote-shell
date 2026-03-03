package com.franosch.worker.controller;

import com.franosch.worker.model.ExecutionRequest;
import com.franosch.worker.model.ExecutionStatus;
import com.franosch.worker.service.ManagerRegistry;
import com.franosch.worker.service.CommandExecutor;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@RestController
@RequestMapping("executor/")
public class ExecutorController {

    private final ManagerRegistry managerRegistry;
    private final RestTemplate restTemplate;

    @Value("${worker.secret}")
    private String secret;

    @Autowired
    public ExecutorController(ManagerRegistry managerRegistry, RestTemplate restTemplate) {
        this.managerRegistry = managerRegistry;
        this.restTemplate = restTemplate;
    }

    @PostMapping("execute")
    public ResponseEntity<String> execute(@RequestBody ExecutionRequest request) {

        if(!request.getSecret().equals(secret)){
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Wrong secret");
        }

        try {
            // Extract execution ID from request header or generate one
            String executionId = request.getId();
            log.info("[Execution {}]: {}", executionId, request.getCommand());
            // Update manager that execution is starting
            updateManagerStatus(executionId, ExecutionStatus.IN_PROGRESS);

            // Execute the command
            CommandExecutor.ExecutionResult result = managerRegistry.executeCommand(request.getCommand());

            // Update manager with result
            if (result.isSuccess()) {
                updateManagerStatus(executionId, ExecutionStatus.FINISHED, result.getOutput(), null);
                return ResponseEntity.ok("Command executed successfully");
            } else {
                updateManagerStatus(executionId, ExecutionStatus.FAILED, null, result.getError());
                 throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Command failed: " + result.getError());
            }
        } catch (Exception e) {
            log.warn("[Execution {}] Internal error: {}", request.getId(), e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error executing command: " + e.getMessage());
        }
    }

    private void updateManagerStatus(String executionId, ExecutionStatus status) {
        updateManagerStatus(executionId, status, null, null);
    }

    private void updateManagerStatus(String executionId, ExecutionStatus status, String result, String error) {
        try {
            String url = managerRegistry.getManagerUrl() + "/worker/execution/" + executionId + "/status";
            StatusUpdateRequest request = new StatusUpdateRequest();
            request.setStatus(status);
            request.setResult(result);
            request.setError(error);

            restTemplate.put(url, request);
        } catch (Exception e) {
            log.error("Failed to update manager with status: {}", e.getMessage());
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class StatusUpdateRequest {
        private ExecutionStatus status;
        private String result;
        private String error;
    }
}
