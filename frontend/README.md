# SMS Checker Frontend

Spring Boot UI for submitting SMS texts to the classifier backend and exposing application metrics.

## Overview
- UI served at `/sms` (backed by `FrontendController`). Root `/` returns "Hello World!" for smoke checks.
- Forwards predictions to the backend at `MODEL_HOST` (default `http://localhost:8081`).
- **Application instrumentation**: `/metrics` endpoint (Prometheus text format) built without metrics libraries. Formatting is manual in `MetricsFormatter.java`; data stored in thread-safe structures in `MetricsRegistry.java`.

## Requirements
- Java 25+ (tested with 25.0.1)
- Maven
- Environment: `MODEL_HOST` for backend URL (protocol required), optional `APP_PORT` (default 8080)

## Run
```bash
cd frontend
MODEL_HOST="http://localhost:8081" mvn spring-boot:run
```
Then open `http://localhost:8080/sms`.

## Key Endpoints
- `GET /sms` → redirects to `/sms/`
- `GET /sms/` → renders SMS form; posts JSON to same path
- `POST /sms/` → body `{ "sms": "...", "guess": "ham|spam" }`, forwards to backend `/predict`
- `GET /metrics` → Prometheus text exposition format (manual generation)

## Implemented Metrics (all prefixed `doda_`)
- Counter `doda_predictions_total{result="success|error"}` tracks traffic and errors.
- Gauge `doda_input_text_length` captures length of last SMS input.
- Histogram `doda_prediction_duration_seconds_bucket{le="0.05".."5.00",+Inf}`, with `_sum` and `_count`, measures prediction latency.

## Verify Metrics
```bash
# 1) Run the app (see above)

# 2) Generate traffic
curl -X POST http://localhost:8080/sms/ -H "Content-Type: application/json" \
    -d '{"sms": "Test message"}'

# 3) Fetch metrics
curl http://localhost:8080/metrics

# 4) Expect output starting with "# HELP" and entries for doda_* metrics
```


