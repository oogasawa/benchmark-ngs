package com.github.oogasawa.benchmark.ngs.parabricks;

import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.*;

/**
 * A utility class that parses throughput information from a Parabricks `fq2bam` stderr output file
 * and writes it to a CSV file for visualization or further analysis.
 *
 * <p>This tool detects lines such as:
 * <pre>
 * [PB Info 2025-Jul-05 00:51:37] Throughput: 3.21 GB/sec
 * </pre>
 * and extracts both the timestamp and throughput value.
 * After the mapping stage is complete (detected via a "Total Time:" line),
 * it records subsequent timestamps with throughput value {@code 0.0} to maintain
 * continuity for plotting alongside CPU/GPU metrics.
 *
 * <p>The output CSV contains two columns:
 * <ul>
 *   <li>{@code timestamp} — the timestamp in ISO-8601 format (e.g., {@code 2025-07-05T00:51:37})</li>
 *   <li>{@code throughput_gbps} — the throughput value in GB/sec</li>
 * </ul>
 *
 * <p><b>Example usage:</b>
 * <pre>{@code
 * java -cp benchmark-parabricks-1.0.0.jar \
 *      com.github.oogasawa.benchmark.parabricks.ParabricksThroughputCsvExporter \
 *      b200-gpu1-stat.parabricks.out \
 *      b200-gpu1-throughput.csv
 * }</pre>
 *
 * @author  
 */
public class Fq2BamThroughput {

    private static final Pattern LOG_TIME_PATTERN = Pattern.compile("^\\[PB Info ([\\d\\w:-]+)\\]");
    private static final Pattern THROUGHPUT_PATTERN = Pattern.compile("\\s+([0-9.]+) bases/GPU/minute");
    private static final Pattern TOTAL_TIME_PATTERN = Pattern.compile("Total Time:");

    private static final DateTimeFormatter LOG_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MMM-dd HH:mm:ss", Locale.ENGLISH);

    /**
     * Parses the given Parabricks stderr log file and exports throughput information
     * to a CSV file. Timestamps after the end of mapping are filled with zero throughput.
     *
     * @param logFile   the input Parabricks stderr file
     * @param outputCsv the output CSV file to write
     * @throws IOException if an I/O error occurs reading or writing the files
     */
    public static void parseAndExport(Path logFile, Path outputCsv) throws IOException {
        List<String> lines = Files.readAllLines(logFile);
        List<ThroughputEntry> entries = new ArrayList<>();

        LocalDateTime lastTime = null;
        boolean afterMapping = false;
        double latestThroughput = 0.0;

        for (String line : lines) {
            Matcher logTimeMatcher = LOG_TIME_PATTERN.matcher(line);
            if (!logTimeMatcher.find()) {
                continue;
            }

            LocalDateTime timestamp = LocalDateTime.parse(logTimeMatcher.group(1), LOG_TIME_FORMATTER);
            lastTime = timestamp;

            if (TOTAL_TIME_PATTERN.matcher(line).find()) {
                afterMapping = true;
                latestThroughput = 0.0;
            }

            if (!afterMapping) {
                Matcher throughputMatcher = THROUGHPUT_PATTERN.matcher(line);
                if (throughputMatcher.find()) {
                    latestThroughput = Double.parseDouble(throughputMatcher.group(1));
                }
            }

            entries.add(new ThroughputEntry(timestamp, afterMapping ? 0.0 : latestThroughput));
        }

        if (!entries.isEmpty()) {
            writeCsv(entries, outputCsv);
        }
    }

    /**
     * Writes the collected throughput entries to a CSV file.
     *
     * @param entries   the list of timestamped throughput entries
     * @param outputCsv the path to the output CSV file
     * @throws IOException if writing fails
     */
    private static void writeCsv(List<ThroughputEntry> entries, Path outputCsv) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(outputCsv)) {
            writer.write("timestamp,throughput_gbps\n");
            for (ThroughputEntry e : entries) {
                writer.write(e.timestamp.toString() + "," + String.format(Locale.US, "%.2f", e.throughput) + "\n");
            }
        }
    }

    /**
     * A record class representing a timestamped throughput value.
     *
     * @param timestamp  the timestamp of the log line
     * @param throughput the throughput value in GB/sec
     */
    private record ThroughputEntry(LocalDateTime timestamp, double throughput) {}

    /**
     * Entry point for command-line execution.
     *
     * @param args command-line arguments: {@code <input_log_file> <output_csv_file>}
     * @throws IOException if an error occurs during parsing or writing
     */
    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.err.println("Usage: java ParabricksThroughputCsvExporter <input_log> <output_csv>");
            System.exit(1);
        }

        Path log = Paths.get(args[0]);
        Path out = Paths.get(args[1]);
        parseAndExport(log, out);
    }
}

