package com.franosch.worker.service;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class CommandExecutor {

    public ExecutionResult execute(String command) {
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"sh", "-c", command});
            
            // Capture stdout
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            // Capture stderr
            StringBuilder error = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    error.append(line).append("\n");
                }
            }

            // Wait for process completion
            int exitCode = process.waitFor();

            return new ExecutionResult(
                    exitCode == 0,
                    output.toString().trim(),
                    error.toString().trim(),
                    exitCode
            );
        } catch (Exception e) {
            return new ExecutionResult(false, "", e.getMessage(), -1);
        }
    }

    @Data
    @AllArgsConstructor
    public static class ExecutionResult {
        private boolean success;
        private String output;
        private String error;
        private int exitCode;
    }
}
