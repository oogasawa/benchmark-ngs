package com.github.oogasawa.benchmark;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;



/**
 * Executes a command and simultaneously monitors all process creation and termination
 * events for a given user during its execution.
 */
public class ProcessWatcher {

    private static final Logger logger = Logger.getLogger(ProcessWatcher.class.getName());

    private final String username;
    private final int intervalSeconds;
    private final List<String> eventLog = Collections.synchronizedList(new ArrayList<>());
    private volatile boolean keepRunning = true;
    private Map<Long, ProcInfo> finalSnapshot = new HashMap<>();

    public ProcessWatcher(String username, int intervalSeconds) {
        this.username = username;
        this.intervalSeconds = intervalSeconds;
    }

    /**
     * Runs the given command and monitors all user processes during its execution.
     *
     * @param command List of command and arguments to execute.
     * @throws IOException if the process fails to start.
     * @throws InterruptedException if the thread is interrupted.
     */
    public void watchAndRun(List<String> command) throws IOException, InterruptedException {
        logger.info(String.format("Starting monitored command: %s", String.join(" ", command)));

        Map<Long, ProcInfo> previousSnapshot = new HashMap<>();

        Thread monitorThread = new Thread(() -> {
            try {
                while (keepRunning) {
                    Map<Long, ProcInfo> currentSnapshot = getUserProcesses();

                    for (Long pid : currentSnapshot.keySet()) {
                        if (!previousSnapshot.containsKey(pid)) {
                            ProcInfo p = currentSnapshot.get(pid);
                            eventLog.add(timestamp() + " + " + p);
                        }
                    }

                    for (Long pid : previousSnapshot.keySet()) {
                        if (!currentSnapshot.containsKey(pid)) {
                            ProcInfo p = previousSnapshot.get(pid);
                            eventLog.add(timestamp() + " - " + p);
                        }
                    }

                    previousSnapshot.clear();
                    previousSnapshot.putAll(currentSnapshot);

                    Thread.sleep(intervalSeconds * 1000L);
                }
                finalSnapshot = getUserProcesses();
            } catch (IOException | InterruptedException e) {
                logger.warning(String.format("Process monitoring interrupted: %s", e.getMessage()));
                Thread.currentThread().interrupt();
            }
        });

        monitorThread.start();

        Process process = new ProcessBuilder(command)
                .redirectErrorStream(true)
                .inheritIO()
                .start();

        long pid = process.pid();
        logger.info(String.format("Root PID: %s", pid));

        int exitCode = process.waitFor();
        logger.info(String.format("Monitored command exited with code: %s", exitCode));

        Thread.sleep(intervalSeconds * 1000L);
        keepRunning = false;
        monitorThread.join();

        dumpLog();
        printProcessTree();
    }

    private Map<Long, ProcInfo> getUserProcesses() throws IOException {
        Map<Long, ProcInfo> map = new HashMap<>();
        Process proc = new ProcessBuilder("ps", "-eo", "pid,ppid,user,cmd", "--no-headers").start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.trim().split("\\s+", 4);
                if (parts.length < 4) continue;

                String user = parts[2];
                if (!user.equals(username)) continue;

                long pid = Long.parseLong(parts[0]);
                long ppid = Long.parseLong(parts[1]);
                String cmd = parts[3];
                map.put(pid, new ProcInfo(pid, ppid, cmd));
            }
        }
        return map;
    }

    private void dumpLog() {
        System.out.println(String.format("=== Process Events for user '%s' ===", username));
        for (String line : eventLog) {
            System.out.println(line);
        }
    }

    private void printProcessTree() {
        System.out.println("\n=== Final Process Tree ===");
        Map<Long, List<Long>> tree = ProcessTreePrinter.buildParentChildMap(finalSnapshot);
        for (Long pid : finalSnapshot.keySet()) {
            if (!finalSnapshot.containsKey(finalSnapshot.get(pid).ppid)) {
                ProcessTreePrinter.printTree(pid, tree, finalSnapshot, 0);
            }
        }
    }

    private String timestamp() {
        return "[" + LocalDateTime.now() + "]";
    }
} 

