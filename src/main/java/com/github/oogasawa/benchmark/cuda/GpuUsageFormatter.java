package com.github.oogasawa.benchmark.cuda;

import java.io.IOException;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;

import tech.tablesaw.aggregate.AggregateFunctions;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;
import tech.tablesaw.io.csv.CsvReadOptions;
import tech.tablesaw.io.csv.CsvWriteOptions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A utility class to convert a Parabricks-style `nvidia-smi` GPU usage log into a wide-format CSV
 * file where each GPU is represented as a column.
 *
 * <p>
 * The input file is expected to be a CSV log with columns like "timestamp", "index", and
 * "utilization.gpu". This formatter performs the following:
 * <ul>
 * <li>Normalizes column names (lowercase, dot-separated)</li>
 * <li>Converts and cleans up the timestamp format</li>
 * <li>Transforms GPU index to GPU label (e.g., GPU0, GPU1)</li>
 * <li>Pivots the table so each GPU becomes a separate column</li>
 * <li>Sorts by timestamp</li>
 * <li>Writes the result to a new CSV file</li>
 * </ul>
 * </p>
 */
public class GpuUsageFormatter {

    private static final Logger logger = LoggerFactory.getLogger(GpuUsageFormatter.class);


    /**
     * Generate a GPU usage table for all GPUs from the output of {@code nvidia-smi} and write to CSV.
     * <p>
     * This method reads a CSV file exported from
     * {@code nvidia-smi --query-gpu=index,utilization.gpu,timestamp --format=csv -l 1},
     * normalizes and pivots the data so that each GPU's utilization becomes a separate column,
     * with rows indexed by cleaned timestamps.
     * </p>
     *
     * <p>
     * <b>Example input (CSV):</b>
     * </p>
     * 
     * <pre>
     * timestamp, index, utilization.gpu [%]
     * 2025/07/05 15:01:02.123, 0, 56 %
     * 2025/07/05 15:01:02.123, 1, 42 %
     * 2025/07/05 15:01:03.123, 0, 60 %
     * 2025/07/05 15:01:03.123, 1, 48 %
     * </pre>
     *
     * <p>
     * <b>Example output (pivoted Table):</b>
     * </p>
     * 
     * <pre>
     * timestamp_clean        | GPU0   | GPU1
     * ------------------------|--------|------
     * 2025-07-05 15:01:02     | 56 %   | 42 %
     * 2025-07-05 15:01:03     | 60 %   | 48 %
     * </pre>
     *
     * @param nvidiaSmiLog the input {@code .nvidia-smi.out} CSV log file
     * @param outfile the output CSV file path (currently unused in this method)
     * @return a {@link Table} object in wide format with one column per GPU and one row per
     *         timestamp
     * @throws IOException if reading the CSV fails
     */
    public static Table gpuUsage(Path nvidiaSmiLog, Path outfile) {
        try {
            Table result = pivotGpuMetric(nvidiaSmiLog, "utilization.gpu");
            result.write().csv(outfile.toFile());
            return result;
        } catch (IOException e) {
            logger.error("Failed to read or write GPU usage log: " + nvidiaSmiLog, e);
            //e.printStackTrace();
            return null;
        }
    }



    /**
     * Generate a GPU memory usage table for all GPUs from the output of {@code nvidia-smi} and write to CSV.
     * <p>
     * This method reads a CSV file exported from
     * {@code nvidia-smi --query-gpu=index,memory.used,timestamp --format=csv -l 1},
     * normalizes and pivots the data so that each GPU's memory usage becomes a separate column,
     * with rows indexed by cleaned timestamps.
     * </p>
     *
     * <p>
     * <b>Example input (CSV):</b>
     * </p>
     * 
     * <pre>
     * timestamp, index, memory.used [MiB]
     * 2025/07/05 15:01:02.123, 0, 1234
     * 2025/07/05 15:01:02.123, 1, 1567
     * 2025/07/05 15:01:03.123, 0, 1300
     * 2025/07/05 15:01:03.123, 1, 1602
     * </pre>
     *
     * <p>
     * <b>Example output (pivoted Table):</b>
     * </p>
     * 
     * <pre>
     * timestamp_clean        | GPU0  | GPU1
     * ------------------------|--------|------
     * 2025-07-05 15:01:02     | 1234   | 1567
     * 2025-07-05 15:01:03     | 1300   | 1602
     * </pre>
     *
     * @param nvidiaSmiLog the input {@code .nvidia-smi.out} CSV log file
     * @param outfile the output CSV file path to write the pivoted result
     * @return a {@link Table} object in wide format with one column per GPU and one row per timestamp
     */
    public static Table gpuMemoryUsage(Path nvidiaSmiLog, Path outfile)  {
        try {
            Table result = pivotGpuMetric(nvidiaSmiLog, "memory.used");
            result.write().csv(outfile.toFile());
            return result;
        } catch (IOException e) {
            logger.error("Failed to read or write GPU memory usage log: " + nvidiaSmiLog, e);
            //e.printStackTrace();
            return null;
        }
    }




    
    /**
     * Shared logic to pivot a specific GPU metric column across timestamps and GPUs.
     *
     * <p>
     * This method performs the following transformations:
     * </p>
     *
     * <pre>
     * 1. Input CSV format (as produced by `nvidia-smi --format=csv`):
     *
     * $ head ~/tmp3/l40s-stats/l40s-gpu1-stat.nvidia-smi.out 
     * timestamp, index, utilization.gpu [%], utilization.memory [%], memory.used [MiB], memory.total [MiB]
     * 2025/07/04 22:57:22.144, 0, 0, 0, 1, 46068
     * 2025/07/04 22:57:22.146, 1, 0, 0, 1, 46068
     * 2025/07/04 22:57:22.148, 2, 0, 0, 1, 46068
     * 2025/07/04 22:57:22.151, 3, 0, 0, 1, 46068
     * 2025/07/04 22:57:22.153, 4, 0, 0, 1, 46068
     * 2025/07/04 22:57:22.155, 5, 0, 0, 1, 46068
     * 2025/07/04 22:57:22.157, 6, 0, 0, 1, 46068
     * 2025/07/04 22:57:22.160, 7, 0, 0, 1, 46068
     * 2025/07/04 22:57:27.162, 0, 50, 29, 25037, 46068
     *
     *
     * 2. Column name normalization:
     *
     *   Original                → Normalized
     *   ------------------------------------------
     *   "utilization.gpu [%]"  → "utilization.gpu"
     *   "memory.used [MiB]"    → "memory.used"
     *   "timestamp"            → "timestamp"
     *   (removal of brackets, percent signs, units, extra dots)
     *
     * 3. Timestamp reformatting:
     *
     *   "2025/07/04 22:57:22.144" → "2025-07-04 22:57:22"
     *
     * 4. Pivot:
     *   Rows grouped by timestamp_clean,
     *   columns split by GPU index (e.g., GPU0, GPU1, ...)
     *   and values taken from the specified metric column (e.g., utilization.gpu)
     *
     * 5. Output example (wide format):
     *
     *   timestamp_clean        | GPU0 | GPU1 | ...
     *   ------------------------|------|------|-----
     *   2025-07-04 22:57:22     | 0    | 0    | ...
     *   2025-07-04 22:57:27     | 50   | 42   | ...
     * </pre>
     *
     * @param nvidiaSmiLog the input nvidia-smi CSV file
     * @param metricColumn the column name to pivot (e.g., "utilization.gpu" or "memory.used")
     *                     — must match the normalized column name
     * @return pivoted wide-format table
     * @throws IOException if reading fails
     * @throws IllegalStateException if the target column is missing
     */
    public static Table pivotGpuMetric(Path nvidiaSmiLog, String metricColumn) throws IOException {

        // Read CSV file
        CsvReadOptions options = CsvReadOptions.builder(nvidiaSmiLog.toFile())
            .separator(',').header(true).build();

        Table df = Table.read().usingOptions(options);

        // Normalize column names
        df.columnNames().forEach(name -> {
                String normalized = normalizeColumnName(name);
                df.column(name).setName(normalized);
            });



        logger.info("Normalized columns from file {}: {}", nvidiaSmiLog, df.columnNames());

        // Check for target metric column
        if (!df.columnNames().contains(metricColumn)) {
            logger.error("Column '{}' not found in table: {}", metricColumn, df.name());
            logger.warn("Available columns are: {}", df.columnNames());
            throw new IllegalStateException("Column '" + metricColumn + "' not found in table: " + df.name());
        }

        // Clean and format timestamp
        DateTimeFormatter inFmt = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss.SSS");
        DateTimeFormatter outFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        StringColumn ts = df.stringColumn("timestamp")
            .map(s -> outFmt.format(inFmt.parse(s.trim())))
            .setName("timestamp_clean");

        df.addColumns(ts);

        // Select relevant columns
        Table reduced = df.selectColumns("timestamp_clean", "index", metricColumn);
        logger.debug("Reduced table preview:\n{}", reduced.first(5).print());

        // Create GPU label column
        StringColumn gpuCol = reduced.intColumn("index")
            .asStringColumn()
            .map(idx -> "GPU" + idx)
            .setName("gpu");
        reduced.addColumns(gpuCol);

        // Pivot by timestamp and GPU
        Table pivoted = reduced.pivot("timestamp_clean", "gpu", metricColumn, AggregateFunctions.first);

        // Sort by timestamp
        return pivoted.sortAscendingOn("timestamp_clean");
    }



    /**
     * Normalizes a raw column name by applying consistent formatting rules.
     *
     * <p><strong>Specification by Example:</strong></p>
     * <pre>{@code
     * normalizeColumnName(" utilization.gpu [%] ")   -> "utilization.gpu"
     * normalizeColumnName("memory.used [MiB]")        -> "memory.used"
     * normalizeColumnName(" power.draw [W]")          -> "power.draw"
     * normalizeColumnName("abc..def")                 -> "abc.def"
     * normalizeColumnName(" multiple...dots...here ") -> "multiple.dots.here"
     * normalizeColumnName(".trailing.dot.")           -> "trailing.dot"
     * }</pre>
     *
     * @param rawName The raw column name string from the CSV header
     * @return A normalized column name suitable for internal access
     */
    public static String normalizeColumnName(String rawName) {
        return rawName
            .toLowerCase()
            .replaceAll("\\[.+?\\]", "")
            .replaceAll("\\.+", ".")
            .trim() 
            .replaceAll("^\\.|\\.$", "");

    }



    
}


