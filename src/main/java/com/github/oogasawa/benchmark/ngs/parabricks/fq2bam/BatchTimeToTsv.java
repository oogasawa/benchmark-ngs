package com.github.oogasawa.benchmark.ngs.parabricks.fq2bam;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.regex.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parses stderr logs from Parabricks executions and extracts total runtime and status.
 * Matches input files against a regular expression pattern.
 */
public class BatchTimeToTsv {

    private static final Logger logger = LoggerFactory.getLogger(BatchTimeToTsv.class);

    private static final Pattern TIMESTAMP_PATTERN =
        Pattern.compile("\\[PB Info ([\\d]{4}-[A-Za-z]{3}-\\d{2} \\d{2}:\\d{2}:\\d{2})]");

    private static final Pattern STATUS_SUCCESS =
        Pattern.compile("Exiting with status: SUCCESS");

    private static final Pattern EXITCODE_SUCCESS =
        Pattern.compile("Main process exited with code: 0");

    private static final DateTimeFormatter TIMESTAMP_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MMM-dd HH:mm:ss", Locale.ENGLISH);

    /**
     * Recursively searches for files matching the given regular expression,
     * parses each one for execution duration and success, and prints the results as TSV.
     *
     * @param regexPattern Regular expression string to match target filenames.
     *                     Example: {@code ".+parabricks\\.out$"}
     * @throws IOException If directory traversal or file reading fails.
     */
    public static void parse(String regexPattern) throws IOException {

        Pattern filePattern = Pattern.compile(regexPattern);
        Path startDir = Paths.get(".").toAbsolutePath().normalize();

        Files.walkFileTree(startDir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Path relPath = startDir.relativize(file);
                if (filePattern.matcher(relPath.toString()).matches()) {
                    ExecutionResult result = parseExecutionResult(file);
                    System.out.printf("%s\t%d\t%s%n", result.fileName(), result.durationSeconds(), result.success() ? "OK" : "FAILED");
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * Extracts execution time and success status from a Parabricks stderr file.
     *
     * @param file Path to the stderr output file.
     * @return ExecutionResult with file name, runtime in seconds, and success flag.
     * @throws IOException If file read fails.
     */
    private static ExecutionResult parseExecutionResult(Path file) throws IOException {
        LocalDateTime first = null;
        LocalDateTime last = null;
        boolean explicitlySuccess = false;

        for (String line : Files.readAllLines(file)) {
            Matcher ts = TIMESTAMP_PATTERN.matcher(line);
            if (ts.find()) {
                LocalDateTime dt = LocalDateTime.parse(ts.group(1), TIMESTAMP_FORMATTER);
                if (first == null) {
                    first = dt;
                }
                last = dt;
            }
            if (STATUS_SUCCESS.matcher(line).find() || EXITCODE_SUCCESS.matcher(line).find()) {
                explicitlySuccess = true;
            }
        }

        long durationSeconds = (first != null && last != null) ? Duration.between(first, last).toSeconds() : 0;
        boolean success = (durationSeconds > 0);  // 実時間があれば成功とみなす

        return new ExecutionResult(file.getFileName().toString(), durationSeconds, success);
    }

    /**
     * A simple record to store result per file.
     */
    private record ExecutionResult(String fileName, long durationSeconds, boolean success) {}
}

