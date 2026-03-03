package com.franosch.manager.service;

import com.franosch.manager.model.ResourceRequirements;
import com.franosch.manager.model.WorkerInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
public class WorkerRegistry {
    private final Map<String, WorkerInfo> workers = Collections.synchronizedMap(new LinkedHashMap<>());
    private int roundRobinIndex = 0;

    public void registerWorker(String workerId, String baseUrl, com.franosch.manager.model.ResourceRequirements capacity) {
        WorkerInfo worker = new WorkerInfo(workerId, baseUrl, capacity);
        workers.put(workerId, worker);
        log.info("Worker registered: {} at {}", workerId, baseUrl);
    }

    public void deregisterWorker(String workerId) {
        workers.remove(workerId);
        log.info("Worker deregistered: {}", workerId);
    }

    public WorkerInfo getWorker(String workerId) {
        return workers.get(workerId);
    }

    public List<WorkerInfo> getAllWorkers() {
        return new ArrayList<>(workers.values());
    }

    public List<WorkerInfo> getAvailableWorkers() {
        List<WorkerInfo> availableWorkers = new ArrayList<>();
        for (WorkerInfo allWorker : this.getAllWorkers()) {
            if (allWorker.isAvailable()) availableWorkers.add(allWorker);
        }
        return availableWorkers;
    }

    public synchronized WorkerInfo getAvailableWorkerForCapacity(ResourceRequirements requirements) {
        List<WorkerInfo> allWorkers = this.getAllWorkers();
        int n = allWorkers.size();
        if (n == 0) return null;

        for (int i = 0; i < n; i++) {
            int index = (roundRobinIndex + i) % n;
            WorkerInfo worker = allWorkers.get(index);

            if (worker.isAvailable() && canHandle(worker, requirements)) {
                roundRobinIndex = (index + 1) % n;
                return worker;
            }
        }
        return null;
    }

    private boolean canHandle(WorkerInfo worker, ResourceRequirements requirements) {
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
