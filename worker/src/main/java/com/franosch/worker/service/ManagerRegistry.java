package com.franosch.worker.service;

import com.franosch.worker.model.ResourceRequirements;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@Getter
public class ManagerRegistry {
    private final RestTemplate restTemplate;
    private final String workerId;
    private final String baseUrl;
    private final String managerUrl;
    private final int cpuCount;
    private final long memoryMb;
    private final CommandExecutor executor;

    @Autowired
    public ManagerRegistry(
            RestTemplate restTemplate,
            @Value("${worker.id}") String workerId,
            @Value("${worker.base-url}") String baseUrl,
            @Value("${manager.url}") String managerUrl,
            @Value("${worker.cpu-count}") int cpuCount,
            @Value("${worker.memory-mb}") long memoryMb,
            @Value("${worker.auto-register:true}") boolean autoRegister) {
        this.restTemplate = restTemplate;
        this.workerId = workerId;
        this.baseUrl = baseUrl;
        this.managerUrl = managerUrl;
        this.cpuCount = cpuCount;
        this.memoryMb = memoryMb;
        this.executor = new CommandExecutor();

        // Register with manager on startup if enabled
        if (autoRegister) {
            registerWithManager();
        }
    }

    public void registerWithManager() {
        try {
            String url = managerUrl + "/worker/register";
            WorkerRegistrationRequest request = new WorkerRegistrationRequest();
            request.setWorkerId(workerId);
            request.setBaseUrl(baseUrl);
            request.setCapacity(new ResourceRequirements(cpuCount, memoryMb));

            restTemplate.postForObject(url, request, String.class);
            log.info("Successfully registered with manager: {}", managerUrl);
        } catch (Exception e) {
            log.error("Failed to register with manager: {}", e.getMessage());
        }
    }

    public CommandExecutor.ExecutionResult executeCommand(String command) {
        return executor.execute(command);
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class WorkerRegistrationRequest {
        private String workerId;
        private String baseUrl;
        private ResourceRequirements capacity;
    }
}
