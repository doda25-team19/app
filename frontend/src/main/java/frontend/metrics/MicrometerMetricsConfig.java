package frontend.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class to register custom metrics with Micrometer.
 * This bridges our manual metrics registry with Spring Boot Actuator/Prometheus.
 */
@Configuration
public class MicrometerMetricsConfig {

    /**
     * Register custom Counter for successful predictions.
     */
    @Bean
    public Counter successPredictionCounter(MeterRegistry registry) {
        return Counter.builder("doda.predictions.success")
                .description("Total number of successful SMS predictions")
                .tag("result", "success")
                .register(registry);
    }

    /**
     * Register custom Counter for failed predictions.
     */
    @Bean
    public Counter errorPredictionCounter(MeterRegistry registry) {
        return Counter.builder("doda.predictions.error")
                .description("Total number of failed SMS predictions")
                .tag("result", "error")
                .register(registry);
    }

    /**
     * Register custom Gauge for input text length.
     * This tracks the length of the most recent SMS input.
     */
    @Bean
    public Gauge inputTextLengthGauge(MeterRegistry registry, MetricsRegistry metricsRegistry) {
        return Gauge.builder("doda.input.text.length", metricsRegistry, MetricsRegistry::getInputTextLength)
                .description("Length of the most recent SMS input text")
                .register(registry);
    }
}
