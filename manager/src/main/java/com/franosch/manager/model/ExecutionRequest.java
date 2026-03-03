package com.franosch.manager.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionRequest {
    private String command;
    private ResourceRequirements resources;
    private String id;
    private String secret;
}
