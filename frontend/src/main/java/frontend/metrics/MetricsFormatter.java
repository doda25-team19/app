package frontend.metrics;

import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Service responsible for formatting metrics data into Prometheus text exposition format.
 * This manually creates the metric output without using any metrics libraries.
 */
@Service
public class MetricsFormatter {

    /**
     * Format all metrics from the registry into Prometheus text exposition format.
     * @param registry The metrics registry containing all metric data
     * @return Formatted string in Prometheus format
     */
    public String formatMetrics(MetricsRegistry registry) {
        StringBuilder sb = new StringBuilder();

        // Format Counter: predictions_total with labels
        formatPredictionCounter(sb, registry);

        // Format Gauge: input_text_length
        formatInputTextLengthGauge(sb, registry);

        // Format Histogram: prediction_duration_seconds
        formatPredictionDurationHistogram(sb, registry);

        return sb.toString();
    }

    /**
     * Format the predictions_total counter metric.
     */
    private void formatPredictionCounter(StringBuilder sb, MetricsRegistry registry) {
        sb.append("# HELP doda_predictions_total Total number of SMS predictions by result\n");
        sb.append("# TYPE doda_predictions_total counter\n");

        Map<String, Long> counters = registry.getPredictionCounters();
        for (Map.Entry<String, Long> entry : counters.entrySet()) {
            sb.append(String.format("doda_predictions_total{result=\"%s\"} %d\n",
                    entry.getKey(), entry.getValue()));
        }
        sb.append("\n");
    }

    /**
     * Format the input_text_length gauge metric.
     */
    private void formatInputTextLengthGauge(StringBuilder sb, MetricsRegistry registry) {
        sb.append("# HELP doda_input_text_length Length in characters of the most recent SMS input\n");
        sb.append("# TYPE doda_input_text_length gauge\n");
        sb.append(String.format("doda_input_text_length %d\n",
                registry.getInputTextLength()));
        sb.append("\n");
    }

    /**
     * Format the prediction_duration_seconds histogram metric.
     */
    private void formatPredictionDurationHistogram(StringBuilder sb, MetricsRegistry registry) {
        sb.append("# HELP doda_prediction_duration_seconds Time taken for SMS predictions\n");
        sb.append("# TYPE doda_prediction_duration_seconds histogram\n");

        Map<Double, Long> buckets = registry.getDurationBuckets();
        double[] boundaries = registry.getBucketBoundaries();

        // Format buckets in order
        for (double boundary : boundaries) {
            Long count = buckets.get(boundary);
            sb.append(String.format("doda_prediction_duration_seconds_bucket{le=\"%.2f\"} %d\n",
                    boundary, count != null ? count : 0));
        }

        // Format +Inf bucket
        Long infCount = buckets.get(Double.POSITIVE_INFINITY);
        sb.append(String.format("doda_prediction_duration_seconds_bucket{le=\"+Inf\"} %d\n",
                infCount != null ? infCount : 0));

        // Format sum and count
        sb.append(String.format("doda_prediction_duration_seconds_sum %.6f\n",
                registry.getDurationSum()));
        sb.append(String.format("doda_prediction_duration_seconds_count %d\n",
                registry.getDurationCount()));
    }
}
