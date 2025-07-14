package com.github.oogasawa.benchmark;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GpuUsageFormatterTest {

    @Test
    void testNormalizeColumnName() {
        assertEquals("utilization.gpu", GpuUsageFormatter.normalizeColumnName("utilization.gpu [%]"));
        assertEquals("memory.used", GpuUsageFormatter.normalizeColumnName("memory.used [MiB]"));
        assertEquals("memory.total", GpuUsageFormatter.normalizeColumnName("memory.total [MiB]"));
        assertEquals("power.draw", GpuUsageFormatter.normalizeColumnName(" power.draw [W]"));
        assertEquals("index", GpuUsageFormatter.normalizeColumnName("index"));
        assertEquals("timestamp", GpuUsageFormatter.normalizeColumnName("timestamp"));
        assertEquals("abc.def", GpuUsageFormatter.normalizeColumnName("abc..def"));  // ドット連続圧縮
    }
}
