package com.franosch.manager.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Execution {
    private String id;
    private String command;
    private ExecutionStatus status;
    private ResourceRequirements resources;
    private String workerId;
    private String result;
    private String error;

    public Execution(String id, String command, ResourceRequirements resources) {
        this.id = id;
        this.command = command;
        this.resources = resources;
        this.status = ExecutionStatus.QUEUED;
    }
}
