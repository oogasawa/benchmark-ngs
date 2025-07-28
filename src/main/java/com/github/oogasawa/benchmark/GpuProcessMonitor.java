package com.github.oogasawa.benchmark;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

/**
 * Periodically records per-process GPU usage using `nvidia-smi`.
 * <p>
 * This class logs GPU memory usage and other metrics for each running process using NVIDIA GPUs.
 * Intended for monitoring GPU workloads over time.
 */
public class GpuProcessMonitor {

    private final int intervalSeconds;
    private final File outputFile;
    private volatile boolean running = false;
    private Thread monitorThread;

    /**
     * Constructs a GPU process monitor.
     *
     * @param intervalSeconds Sampling interval in seconds.
     * @param outputPath      Output file path to write GPU process statistics.
     */
    public GpuProcessMonitor(int intervalSeconds, String outputPath) {
        this.intervalSeconds = intervalSeconds;
        this.outputFile = new File(outputPath);
    }

    /**
     * Starts GPU process monitoring in a background thread.
     */
    public void start() {
        running = true;
        monitorThread = new Thread(this::monitorLoop);
        monitorThread.setDaemon(true);
        monitorThread.start();
    }

    /**
     * Stops the monitoring thread gracefully.
     */
    public void stop() {
        running = false;
        try {
            if (monitorThread != null) {
                monitorThread.join();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void monitorLoop() {
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
            writer.write("timestamp,pid,process_name,used_memory [MiB]");
            writer.newLine();

            while (running) {
                Process process = new ProcessBuilder("nvidia-smi",
                        "--query-compute-apps=pid,process_name,used_memory",
                        "--format=csv,noheader,nounits")
                        .redirectErrorStream(true)
                        .start();

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String timestamp = LocalDateTime.now().format(timeFormatter);
                    String line;
                    while ((line = reader.readLine()) != null) {
                        writer.write(timestamp + "," + line.trim());
                        writer.newLine();
                    }
                }

                writer.flush();
                TimeUnit.SECONDS.sleep(intervalSeconds);
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("GPU process monitoring failed: " + e.getMessage());
        }
    }
}
