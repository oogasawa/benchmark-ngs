package com.github.oogasawa.benchmark.cuda;

import org.knowm.xchart.CategoryChart;
import org.knowm.xchart.CategoryChartBuilder;
import org.knowm.xchart.CategorySeries.CategorySeriesRenderStyle;
import org.knowm.xchart.BitmapEncoder;
import tech.tablesaw.api.NumberColumn;
import tech.tablesaw.api.Table;
import tech.tablesaw.columns.Column;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class GpuUsageChart {

    public static void draw(Table table, Path outputPng, String title) throws IOException {
        CategoryChart chart = new CategoryChartBuilder()
            .width(800)
            .height(600)
            .title(title)
            .xAxisTitle("Timestamp")
            .yAxisTitle("Utilization (%)")
            .build();

        // Set chart style
        chart.getStyler().setDefaultSeriesRenderStyle(CategorySeriesRenderStyle.Bar);
        chart.getStyler().setStacked(true);
        chart.getStyler().setLegendVisible(true);
        chart.getStyler().setXAxisLabelRotation(90);  // 時間が重なる場合の視認性向上
        chart.getStyler().setXAxisTicksVisible(false);     // 目盛り（数値）を非表示
        chart.getStyler().setXAxisTitleVisible(false);     // x軸のタイトルを非表示
        
        // X軸データ（全GPU共通）
        List<String> xData = table.stringColumn("timestamp_clean").asList();

        // 各GPU列をシリーズとして追加
        for (String columnName : table.columnNames()) {
            if (!columnName.equals("timestamp_clean")) {
                Column<?> column = table.column(columnName);
                if (column instanceof NumberColumn<?, ?>) {
                    NumberColumn<?, ?> numberColumn = (NumberColumn<?, ?>) column;

                    // asObjectList() の代替：List<Number> を手動で構築
                    List<Number> yData = new ArrayList<>();
                    for (int i = 0; i < numberColumn.size(); i++) {
                        yData.add((Number) numberColumn.get(i));
                    }

                    chart.addSeries(columnName, xData, yData);
                }
            }
        }

        BitmapEncoder.saveBitmap(chart, outputPng.toString(), BitmapEncoder.BitmapFormat.PNG);
    }
}
