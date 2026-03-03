package com.franosch.manager.service;

import com.franosch.manager.model.WorkerInfo;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class WorkerRegistry {
    private final Map<String, WorkerInfo> workers = new ConcurrentHashMap<>();
    private int roundRobinIndex = 0;

    public void registerWorker(String workerId, String baseUrl, com.franosch.manager.model.ResourceRequirements capacity) {
        WorkerInfo worker = new WorkerInfo(workerId, baseUrl, capacity);
        workers.put(workerId, worker);
        System.out.println("Worker registered: " + workerId + " at " + baseUrl);
    }

    public void deregisterWorker(String workerId) {
        workers.remove(workerId);
        System.out.println("Worker deregistered: " + workerId);
    }

    public WorkerInfo getWorker(String workerId) {
        return workers.get(workerId);
    }

    public List<WorkerInfo> getAllWorkers() {
        return new ArrayList<>(workers.values());
    }

    public List<WorkerInfo> getAvailableWorkers() {
        return workers.values().stream()
                .filter(WorkerInfo::isAvailable)
                .toList();
    }

    public WorkerInfo getAvailableWorkerForCapacity(com.franosch.manager.model.ResourceRequirements requirements) {
        List<WorkerInfo> available = getAvailableWorkers();
        if (available.isEmpty()) {
            return null;
        }

        // Round-robin selection among available workers that meet requirements
        for (int i = 0; i < available.size(); i++) {
            int index = (roundRobinIndex + i) % available.size();
            WorkerInfo worker = available.get(index);
            if (canHandle(worker, requirements)) {
                roundRobinIndex = (index + 1) % available.size();
                return worker;
            }
        }
        return null;
    }

    private boolean canHandle(WorkerInfo worker, com.franosch.manager.model.ResourceRequirements requirements) {
        if (requirements == null) {
            return true;
        }
        return worker.getCapacity().getCpuCount() >= requirements.getCpuCount() &&
                worker.getCapacity().getMemoryMb() >= requirements.getMemoryMb();
    }

    public void markWorkerBusy(String workerId) {
        WorkerInfo worker = workers.get(workerId);
        if (worker != null) {
            worker.setAvailable(false);
        }
    }

    public void markWorkerAvailable(String workerId) {
        WorkerInfo worker = workers.get(workerId);
        if (worker != null) {
            worker.setAvailable(true);
        }
    }
}
