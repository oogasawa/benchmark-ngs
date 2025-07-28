package com.github.oogasawa.benchmark;

/**
 * Represents information about a process including its PID,
 * parent PID (PPID), and command line.
 */
public class ProcInfo {
    public final long pid;
    public final long ppid;
    public final String cmd;

    /**
     * Constructs a new ProcInfo object.
     *
     * @param pid  the process ID
     * @param ppid the parent process ID
     * @param cmd  the command used to invoke the process
     */
    public ProcInfo(long pid, long ppid, String cmd) {
        this.pid = pid;
        this.ppid = ppid;
        this.cmd = cmd;
    }

    @Override
    public String toString() {
        return String.format("[%d] (PPID=%d) %s", pid, ppid, cmd);
    }
}
