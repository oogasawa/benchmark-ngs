package com.github.oogasawa.benchmark;

import java.util.*;

/**
 * Utility class for building and printing the process tree
 * for a specific user or root process ID.
 */
public class ProcessTreePrinter {

    /**
     * Builds a map from PPID to list of child PIDs.
     *
     * @param processes map from PID to ProcInfo
     * @return map from PPID to list of child PIDs
     */
    public static Map<Long, List<Long>> buildParentChildMap(Map<Long, ProcInfo> processes) {
        Map<Long, List<Long>> tree = new HashMap<>();
        for (ProcInfo proc : processes.values()) {
            tree.computeIfAbsent(proc.ppid, k -> new ArrayList<>()).add(proc.pid);
        }
        return tree;
    }

    
    /**
     * Prints the process tree rooted at the given PID.
     *
     * @param rootPid   the root process ID to start from
     * @param ppidMap   map from PPID to child PIDs
     * @param allProcs  map from PID to process info
     * @param level     current tree depth (used for indentation)
     */
    public static void printTree(long rootPid, Map<Long, List<Long>> ppidMap, Map<Long, ProcInfo> allProcs, int level) {
        ProcInfo info = allProcs.get(rootPid);
        if (info == null) return;

        String indent = "  ".repeat(level);
        System.out.println(indent + info);

        List<Long> children = ppidMap.getOrDefault(rootPid, List.of());
        for (Long child : children) {
            printTree(child, ppidMap, allProcs, level + 1);
        }
    }
} 


