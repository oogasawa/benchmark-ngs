package com.github.oogasawa.benchmark;

import java.io.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

/**
 * A simple system resource monitoring class that executes a target command
 * while recording various system statistics in parallel.
 * <p>
 * Tools monitored include:
 * <ul>
 *   <li>{@code mpstat} for CPU usage</li>
 *   <li>{@code iostat} for disk I/O</li>
 *   <li>{@code ifstat} for network I/O</li>
 *   <li>{@code nvidia-smi} for GPU usage (if available)</li>
 *   <li>{@code free} for memory usage</li>
 * </ul>
 * Each monitoring output is written to a separate file named with a given basename.
 */
public class SimpleMonitor {

    private Thread freeThread;
    private volatile boolean freeThreadRunning = false;

    /**
     * Executes the target command with concurrent monitoring using system tools.
     * The monitoring processes are terminated after the target command finishes.
     *
     * @param commandAndArgs The target command and its arguments to execute.
     * @param interval       Sampling interval in seconds.
     * @param basename       Basename used for all output files.
     * @param gpuFlg         If {@code true}, GPU monitoring via {@code nvidia-smi} is enabled.
     */
    public void executeWithMonitoring(List<String> commandAndArgs, int interval, String basename, boolean gpuFlg) {
        try {
            Process mpstat = startMonitoring("mpstat",
                    List.of("mpstat", "-P", "ALL", String.valueOf(interval)),
                    interval, basename + ".mpstat.out");

            Process iostat = startMonitoring("iostat",
                    List.of("iostat", "-xz", String.valueOf(interval)),
                    interval, basename + ".iostat.out");

            Process ifstat = startMonitoring("ifstat",
                    List.of("ifstat", String.valueOf(interval)),
                    interval, basename + ".ifstat.out");

            Process nvidiaSmi = null;
            if (gpuFlg && isCommandAvailable("nvidia-smi")) {
                nvidiaSmi = startMonitoring("nvidia-smi",
                        List.of("nvidia-smi",
                                "--query-gpu=timestamp,index,utilization.gpu,utilization.memory," +
                                        "memory.used,memory.total,temperature.gpu,fan.speed,power.draw,power.limit",
                                "--format=csv,nounits",
                                "--loop=" + interval),
                        interval, basename + ".nvidia-smi.out");
            } else if (gpuFlg) {
                System.err.println("nvidia-smi not found. Skipping GPU monitoring.");
            }

            startFreeMonitoring(interval, basename + ".free.out");

            // Launch the monitored command with pidstat
            List<String> pidstatCommand = new ArrayList<>();
            pidstatCommand.add("pidstat");
            pidstatCommand.add("-urdh");
            pidstatCommand.add("-t");
            pidstatCommand.add("-e");
            pidstatCommand.add("bash");
            pidstatCommand.add("-c");
            pidstatCommand.add("exec " + String.join(" ", commandAndArgs));
            pidstatCommand.add(String.valueOf(interval));

            ProcessBuilder pb = new ProcessBuilder(pidstatCommand);
            pb.redirectOutput(new File(basename + ".pidstat.out"));      // pidstat output
            pb.redirectError(new File(basename + ".program.stdout"));    // program stdout via stderr

            Process pidstatProcess = pb.start();
            int exitCode = pidstatProcess.waitFor();
            Thread.sleep(interval * 1000L);

            stopProcess(mpstat, "mpstat");
            stopProcess(iostat, "iostat");
            stopProcess(ifstat, "ifstat");
            stopProcess(nvidiaSmi, "nvidia-smi");
            stopFreeMonitoring();

            System.out.println("Monitored process exited with code: " + exitCode);

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Automatically detects GPU availability and runs the command with appropriate monitoring.
     *
     * @param commandAndArgs The command and its arguments to execute.
     * @param interval       Sampling interval in seconds.
     * @param basename       Basename used for all output files.
     */
    public void runAutoDetectingGPU(List<String> commandAndArgs, int interval, String basename) {
        boolean hasGpu = detectNvidiaGPU();
        System.out.println("GPU detected: " + hasGpu);
        executeWithMonitoring(commandAndArgs, interval, basename, hasGpu);
    }

    /**
     * Starts a monitoring process with a given command and writes output to a specified file.
     * The first line of the output includes a timestamp and interval for reference.
     *
     * @param name       Name of the tool (used for logging).
     * @param command    Command and its arguments as a list.
     * @param interval   Sampling interval in seconds.
     * @param outputFile The file to which monitoring output is written.
     * @return The started {@code Process} object, or {@code null} if the tool is not available.
     * @throws IOException If the process fails to start.
     */
    private Process startMonitoring(String name, List<String> command, int interval, String outputFile) throws IOException {
        if (!isCommandAvailable(command.get(0))) {
            System.err.printf("%s not found. Skipping %s monitoring.%n", name, name);
            return null;
        }

        // First, write the timestamp and interval line
        File outFile = new File(outputFile);
        try (PrintWriter writer = new PrintWriter(new FileWriter(outFile, false))) {
            writer.printf("[%s] Monitoring started at %s, interval: %d seconds%n",
                          name, LocalDateTime.now().toString(), interval);
        }

        // Now, append mode
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectOutput(ProcessBuilder.Redirect.appendTo(outFile));
        return pb.start();
    }


    /**
     * Attempts to terminate a monitoring process gracefully.
     * If the process does not stop within 5 seconds, a forced termination is issued.
     *
     * @param process The process to stop.
     * @param name    Name of the tool (for logging).
     */
    private void stopProcess(Process process, String name) {
        if (process == null) return;

        process.destroy();
        try {
            if (!process.waitFor(5, TimeUnit.SECONDS)) {
                System.err.println(name + " did not terminate in time, forcing shutdown...");
                process.destroyForcibly();
                process.waitFor();
            }
        } catch (InterruptedException e) {
            System.err.println("Interrupted while waiting for " + name + " to terminate.");
            process.destroyForcibly();
        }
    }

    /**
     * Starts a background thread to record memory usage using {@code free -m}
     * at fixed intervals. Output is written to the specified file.
     *
     * @param interval   Sampling interval in seconds.
     * @param outputFile Output file path for {@code free} data.
     */
    private void startFreeMonitoring(int interval, String outputFile) {
        freeThreadRunning = true;
        freeThread = new Thread(() -> {
            try (PrintWriter writer = new PrintWriter(new FileWriter(outputFile, false))) {
                writer.printf("[free] Monitoring started at %s, interval: %d seconds%n",
                        LocalDateTime.now().toString(), interval);

                while (freeThreadRunning) {
                    Process process = new ProcessBuilder("free", "-m")
                            .redirectErrorStream(true).start();

                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(process.getInputStream()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            writer.println(line);
                        }
                        writer.println();
                        writer.flush();
                    }

                    process.waitFor();
                    Thread.sleep(interval * 1000L);
                }
            } catch (IOException | InterruptedException e) {
                System.err.println("free monitoring interrupted.");
            }
        });
        freeThread.start();
    }

    /**
     * Stops the background thread that runs the {@code free} monitoring loop.
     */
    private void stopFreeMonitoring() {
        freeThreadRunning = false;
        if (freeThread != null) {
            try {
                freeThread.join(5000);
            } catch (InterruptedException e) {
                System.err.println("free monitor thread join interrupted.");
            }
        }
    }

    /**
     * Checks whether a given command is available in the current environment.
     *
     * @param command The name of the command to check (e.g. {@code "mpstat"}).
     * @return {@code true} if the command exists in PATH, {@code false} otherwise.
     */
    private boolean isCommandAvailable(String command) {
        try {
            Process p = new ProcessBuilder("which", command).redirectErrorStream(true).start();
            return p.waitFor() == 0;
        } catch (IOException | InterruptedException e) {
            return false;
        }
    }

    /**
     * Detects whether an NVIDIA GPU is available by running {@code nvidia-smi -L}.
     *
     * @return {@code true} if at least one NVIDIA GPU is detected, {@code false} otherwise.
     */
    private boolean detectNvidiaGPU() {
        if (!isCommandAvailable("nvidia-smi")) return false;

        try {
            Process process = new ProcessBuilder("nvidia-smi", "-L")
                    .redirectErrorStream(true)
                    .start();

            int exitCode = process.waitFor();
            if (exitCode != 0) return false;

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                return reader.lines().anyMatch(line -> line.contains("GPU"));
            }
        } catch (IOException | InterruptedException e) {
            return false;
        }
    }
}
