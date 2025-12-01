package frontend.ctrl;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import frontend.metrics.MetricsRegistry;
import frontend.metrics.MetricsFormatter;

/**
 * REST controller exposing the /metrics endpoint for Prometheus scraping.
 * This manually generates metrics in Prometheus text exposition format without using any metrics libraries.
 */
@RestController
@RequestMapping("/metrics")
public class MetricsController {

    private final MetricsFormatter formatter;
    private final MetricsRegistry registry;

    public MetricsController(MetricsFormatter formatter, MetricsRegistry registry) {
        this.formatter = formatter;
        this.registry = registry;
    }

    /**
     * Expose metrics in Prometheus text exposition format.
     * @return Formatted metrics string
     */
    @GetMapping(produces = "text/plain; version=0.0.4; charset=utf-8")
    public String metrics() {
        try {
            return formatter.formatMetrics(registry);
        } catch (Exception e) {
            System.err.println("Error formatting metrics: " + e.getMessage());
            return "# Error formatting metrics\n";
        }
    }
}
