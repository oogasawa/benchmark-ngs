package com.github.oogasawa.benchmark;

import java.util.List;


/**
 * Combines system-wide and GPU per-process monitoring while executing a command.
 *
 * <p>This class launches both {@link BenchmarkMonitor} and {@link GpuProcessMonitor}:
 * <ul>
 *   <li><b>BenchmarkMonitor</b>: monitors node-level metrics such as CPU, memory, I/O, and network usage
 *       using tools like {@code mpstat}, {@code pidstat}, {@code iostat}, and {@code ifstat}.</li>
 *   <li><b>GpuProcessMonitor</b>: monitors per-process GPU resource usage (e.g., memory usage) using {@code nvidia-smi}.</li>
 * </ul>
 *
 * <p>The GPU presence is automatically detected using {@code nvidia-smi -L}, and GPU monitoring is
 * enabled only if a compatible NVIDIA GPU is detected.
 *
 * <p>This class is useful for benchmarking deep learning or GPU-accelerated tasks,
 * where both system-wide and fine-grained per-process GPU statistics are needed.
 *
 * <p>Example usage:
 * <pre>{@code
 *     var runner = new ProcessBenchmarkRunner();
 *     runner.run(List.of("python3", "train.py"), 5, "test1");
 * }</pre>
 *
 * <p>Output files include:
 * <ul>
 *   <li>{@code test1.mpstat.out}</li>
 *   <li>{@code test1.iostat.out}</li>
 *   <li>{@code test1.ifstat.out}</li>
 *   <li>{@code test1.pidstat.out}</li>
 *   <li>{@code test1.nvidia-smi.out} (if GPU available)</li>
 *   <li>{@code test1.gpu-process.out} (per-process GPU usage, if GPU available)</li>
 * </ul>
 */
public class BenchmarkRunner {

    private final SimpleMonitor benchmarkMonitor = new SimpleMonitor();

    /**
     * Runs the command with system-wide monitoring and per-process GPU monitoring if applicable.
     *
     * @param commandAndArgs Command to execute
     * @param interval       Sampling interval in seconds
     * @param basename       Basename for output files
     */
    public void run(List<String> commandAndArgs, int interval, String basename) {
        boolean hasGpu = detectNvidiaGPU();
        GpuProcessMonitor gpuProcMon = null;

        if (hasGpu) {
            gpuProcMon = new GpuProcessMonitor(interval, basename + ".gpu-process.out");
            gpuProcMon.start();
        }

        benchmarkMonitor.executeWithMonitoring(commandAndArgs, interval, basename, hasGpu);

        if (gpuProcMon != null) {
            gpuProcMon.stop();
        }
    }

    private boolean detectNvidiaGPU() {
        try {
            Process process = new ProcessBuilder("nvidia-smi", "-L")
                    .redirectErrorStream(true)
                    .start();
            int exitCode = process.waitFor();
            if (exitCode != 0) return false;

            try (var reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream()))) {
                return reader.lines().anyMatch(line -> line.contains("GPU"));
            }

        } catch (Exception e) {
            return false;
        }
    }
}
