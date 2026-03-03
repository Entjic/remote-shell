package com.franosch.manager.controller;

import com.franosch.manager.model.ExecutionRequest;
import com.franosch.manager.model.Execution;
import com.franosch.manager.model.ExecutionResponse;
import com.franosch.manager.service.ExecutionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("task/")
public class TaskController {

    private final ExecutionService executionService;

    @Autowired
    public TaskController(ExecutionService executionService) {
        this.executionService = executionService;
    }

    @PostMapping("submit")
    public ResponseEntity<ExecutionResponse> submitTask(@RequestBody ExecutionRequest request) {
        try {
            Execution execution = executionService.submitExecution(request);
            return ResponseEntity.ok(new ExecutionResponse(execution.getId(), execution.getStatus()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(null);
        }
    }

    @GetMapping("{executionId}")
    public ResponseEntity<Execution> getExecutionStatus(@PathVariable String executionId) {
        Execution execution = executionService.getExecution(executionId);
        if (execution == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(execution);
    }

    @GetMapping("all")
    public ResponseEntity<List<Execution>> getAllExecutions() {
        return ResponseEntity.ok(executionService.getAllExecutions());
    }
}
