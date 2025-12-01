package frontend.ctrl;

import java.net.URI;
import java.net.URISyntaxException;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import frontend.data.Sms;
import frontend.metrics.MetricsRegistry;
import jakarta.servlet.http.HttpServletRequest;

import org.doda25.team19.libversion.VersionUtil;

@Controller
@RequestMapping(path = "/sms")
public class FrontendController {

    private String modelHost;

    private RestTemplateBuilder rest;

    private final MetricsRegistry metricsRegistry;

    public FrontendController(RestTemplateBuilder rest, Environment env, MetricsRegistry metricsRegistry) {
        this.rest = rest;
        this.modelHost = env.getProperty("MODEL_HOST", "http://localhost:8081");
        this.metricsRegistry = metricsRegistry;

        System.out.println("Application starting with Lib-Version: " + new VersionUtil().getVersion());

        assertModelHost();
    }

    private void assertModelHost() {
        if (modelHost == null || modelHost.strip().isEmpty()) {
            System.err.println("ERROR: ENV variable MODEL_HOST is null or empty");
            System.exit(1);
        }
        modelHost = modelHost.strip();
        if (modelHost.indexOf("://") == -1) {
            var m = "ERROR: ENV variable MODEL_HOST is missing protocol, like \"http://...\" (was: \"%s\")\n";
            System.err.printf(m, modelHost);
            System.exit(1);
        } else {
            System.out.printf("Working with MODEL_HOST=\"%s\"\n", modelHost);
        }
    }

    @GetMapping("")
    public String redirectToSlash(HttpServletRequest request) {
        // relative REST requests in JS will end up on / and not on /sms
        return "redirect:" + request.getRequestURI() + "/";
    }

    @GetMapping("/")
    public String index(Model m) {
        m.addAttribute("hostname", modelHost);
        return "sms/index";
    }

    @PostMapping({ "", "/" })
    @ResponseBody
    public Sms predict(@RequestBody Sms sms) {
        long startTime = System.nanoTime();
        System.out.printf("Requesting prediction for \"%s\" ...\n", sms.sms);

        try {
            // Track input text length (user behavior metric)
            try {
                metricsRegistry.setInputTextLength(sms.sms.length());
            } catch (Exception metricsError) {
                System.err.println("Failed to record input length metric: " + metricsError.getMessage());
            }

            sms.result = getPrediction(sms);
            System.out.printf("Prediction: %s\n", sms.result);

            // Track success
            try {
                metricsRegistry.incrementPredictionCounter("success");
            } catch (Exception metricsError) {
                System.err.println("Failed to record success metric: " + metricsError.getMessage());
            }

            return sms;
        } catch (Exception e) {
            // Track error
            try {
                metricsRegistry.incrementPredictionCounter("error");
            } catch (Exception metricsError) {
                System.err.println("Failed to record error metric: " + metricsError.getMessage());
            }
            throw e;
        } finally {
            // Always track duration (both success and error)
            try {
                long endTime = System.nanoTime();
                double durationSeconds = (endTime - startTime) / 1_000_000_000.0;
                metricsRegistry.recordPredictionDuration(durationSeconds);
            } catch (Exception metricsError) {
                System.err.println("Failed to record duration metric: " + metricsError.getMessage());
            }
        }
    }

    private String getPrediction(Sms sms) {
        try {
            var url = new URI(modelHost + "/predict");
            var c = rest.build().postForEntity(url, sms, Sms.class);
            return c.getBody().result.trim();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}