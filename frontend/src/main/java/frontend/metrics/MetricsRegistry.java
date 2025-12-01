package frontend.metrics;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Central registry for storing application metrics data using thread-safe data structures.
 * This service stores counters, gauges, and histograms that are manually exposed to Prometheus.
 */
@Service
public class MetricsRegistry {

    // Counter: predictions_total{result="success"} and {result="error"}
    private final ConcurrentHashMap<String, AtomicLong> predictionCounters;

    // Gauge: input_text_length
    private final AtomicInteger inputTextLength;

    // Histogram: prediction_duration_seconds
    private static final double[] BUCKETS = {0.05, 0.1, 0.25, 0.5, 1.0, 2.5, 5.0};
    private final ConcurrentHashMap<Double, AtomicLong> durationBuckets;
    private final AtomicLong durationSum; // Store as nanoseconds, convert to seconds when needed
    private final AtomicLong durationCount;

    public MetricsRegistry() {
        // Initialize counter map with default labels
        this.predictionCounters = new ConcurrentHashMap<>();
        this.predictionCounters.put("success", new AtomicLong(0));
        this.predictionCounters.put("error", new AtomicLong(0));

        // Initialize gauge
        this.inputTextLength = new AtomicInteger(0);

        // Initialize histogram buckets
        this.durationBuckets = new ConcurrentHashMap<>();
        for (double bucket : BUCKETS) {
            this.durationBuckets.put(bucket, new AtomicLong(0));
        }
        // Add +Inf bucket
        this.durationBuckets.put(Double.POSITIVE_INFINITY, new AtomicLong(0));

        this.durationSum = new AtomicLong(0);
        this.durationCount = new AtomicLong(0);
    }

    // ==================== Counter Operations ====================

    /**
     * Increment the prediction counter for the given result label.
     * @param result The result label (e.g., "success" or "error")
     */
    public void incrementPredictionCounter(String result) {
        predictionCounters.computeIfAbsent(result, k -> new AtomicLong(0))
                         .incrementAndGet();
    }

    /**
     * Get an immutable snapshot of all prediction counters.
     * @return Map of label to counter value
     */
    public Map<String, Long> getPredictionCounters() {
        Map<String, Long> snapshot = new HashMap<>();
        predictionCounters.forEach((k, v) -> snapshot.put(k, v.get()));
        return snapshot;
    }

    // ==================== Gauge Operations ====================

    /**
     * Set the input text length gauge value.
     * @param length The character length of the SMS input text
     */
    public void setInputTextLength(int length) {
        this.inputTextLength.set(length);
    }

    /**
     * Get the current input text length gauge value.
     * @return The current text length
     */
    public int getInputTextLength() {
        return this.inputTextLength.get();
    }

    // ==================== Histogram Operations ====================

    /**
     * Record a prediction duration observation in the histogram.
     * This updates the appropriate buckets, sum, and count.
     * @param seconds The duration in seconds
     */
    public void recordPredictionDuration(double seconds) {
        // Increment count
        durationCount.incrementAndGet();

        // Add to sum (convert to nanoseconds for precision, store as long)
        long nanos = (long) (seconds * 1_000_000_000);
        durationSum.addAndGet(nanos);

        // Increment appropriate buckets (cumulative)
        for (double bucket : BUCKETS) {
            if (seconds <= bucket) {
                durationBuckets.get(bucket).incrementAndGet();
            }
        }
        // Always increment +Inf bucket
        durationBuckets.get(Double.POSITIVE_INFINITY).incrementAndGet();
    }

    /**
     * Get an immutable snapshot of all histogram duration buckets.
     * @return Map of bucket boundary to cumulative count
     */
    public Map<Double, Long> getDurationBuckets() {
        Map<Double, Long> snapshot = new HashMap<>();
        durationBuckets.forEach((k, v) -> snapshot.put(k, v.get()));
        return snapshot;
    }

    /**
     * Get the sum of all recorded durations in seconds.
     * @return Total duration sum in seconds
     */
    public double getDurationSum() {
        return durationSum.get() / 1_000_000_000.0;
    }

    /**
     * Get the total count of recorded duration observations.
     * @return Total observation count
     */
    public long getDurationCount() {
        return durationCount.get();
    }

    /**
     * Get the histogram bucket boundaries.
     * @return Array of bucket boundaries (excluding +Inf)
     */
    public double[] getBucketBoundaries() {
        return BUCKETS;
    }
}
