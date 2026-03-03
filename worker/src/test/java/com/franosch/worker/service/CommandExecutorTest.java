package com.franosch.worker.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class CommandExecutorTest {

    private CommandExecutor executor = new CommandExecutor();

    @Test
    void testSimpleEcho() {
        CommandExecutor.ExecutionResult result = executor.execute("echo hello world");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOutput()).contains("hello world");
        assertThat(result.getExitCode()).isEqualTo(0);
    }

    @Test
    void testCommandWithPipe() {
        CommandExecutor.ExecutionResult result = executor.execute("echo 'hello\\nworld' | wc -l");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getExitCode()).isEqualTo(0);
    }

    @Test
    void testCommandWithFailure() {
        CommandExecutor.ExecutionResult result = executor.execute("exit 42");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getExitCode()).isEqualTo(42);
    }

    @Test
    void testInvalidCommand() {
        CommandExecutor.ExecutionResult result = executor.execute("this_command_definitely_does_not_exist_12345");

        assertThat(result.isSuccess()).isFalse();
    }

    @Test
    void testMultipleCommands() {
        CommandExecutor.ExecutionResult result = executor.execute("echo 'first'; echo 'second'; echo 'third'");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOutput()).contains("first");
        assertThat(result.getOutput()).contains("second");
        assertThat(result.getOutput()).contains("third");
    }

    @Test
    void testCommandWithVariables() {
        CommandExecutor.ExecutionResult result = executor.execute("VAR='test' && echo $VAR");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOutput()).contains("test");
    }

    @Test
    void testStderrCapture() {
        CommandExecutor.ExecutionResult result = executor.execute("ls /nonexistent 2>&1 || true");

        assertThat(result.getExitCode()).isEqualTo(0);
    }

    @Test
    void testEmptyCommand() {
        CommandExecutor.ExecutionResult result = executor.execute("");

        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void testCommandWithNumericOutput() {
        CommandExecutor.ExecutionResult result = executor.execute("expr 2 + 2");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOutput().trim()).isEqualTo("4");
    }

    @Test
    void testCommandWithSpecialCharacters() {
        CommandExecutor.ExecutionResult result = executor.execute("echo '!@#$%^&*()'");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOutput()).contains("!@#$%^&*()");
    }
}
