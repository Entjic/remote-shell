package com.franosch.manager.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkerInfo {
    private String id;
    private String baseUrl;
    private ResourceRequirements capacity;
    private boolean available;
    private long lastHeartbeat;

    public WorkerInfo(String id, String baseUrl, ResourceRequirements capacity) {
        this.id = id;
        this.baseUrl = baseUrl;
        this.capacity = capacity;
        this.available = true;
        this.lastHeartbeat = System.currentTimeMillis();
    }
}
