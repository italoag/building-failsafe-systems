private final RestTemplate restTemplate;
private final CircuitBreaker circuitBreaker;
private final Retry retry;

public UserServiceTriggers(RestTemplate restTemplate, CircuitBreaker circuitBreaker, Retry retry) {
    this.restTemplate = restTemplate;
    this.circuitBreaker = circuitBreaker;
    this.retry = retry;
}

@CircuitBreaker(name = "userService", fallbackMethod = "getUserFallback")
@Retry(name = "userService", fallbackMethod = "getUserFallback")
public String getUser(String userId) {
    try {
        return restTemplate.getForObject("https://jsonplaceholder.typicode.com/users/" + userId, String.class);
    } catch (HttpClientErrorException e) {
        if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
            throw new TooManyRequestsException();
        } else {
            throw e;
        }
    }
}

public String getUserFallback(String userId, Throwable throwable) {
    return "Error getting user: " + userId + ". Reason: " + throwable.getMessage();
}

public void setupResilienceTriggers() {
    final MeterRegistry meterRegistry = new SimpleMeterRegistry();
    final SleuthTracer tracer = new SleuthTracer(meterRegistry);
    final SleuthMeterEvents sleuthMeterEvents = new SleuthMeterEvents(tracer);

    final CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
            .failureRateThreshold(50)
            .waitDurationInOpenState(Duration.ofSeconds(30))
            .ringBufferSizeInHalfOpenState(2)
            .ringBufferSizeInClosedState(2)
            .build();
    final CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.of(circuitBreakerConfig);
    final CircuitBreakerMetrics circuitBreakerMetrics = new CircuitBreakerMetrics(circuitBreakerRegistry, meterRegistry);

    final RetryConfig retryConfig = RetryConfig.custom()
            .maxAttempts(3)
            .waitDuration(Duration.ofMillis(500))
            .exponentialBackoff(2, Duration.ofSeconds(1))
            .retryExceptions(TooManyRequestsException.class)
            .build();
    final RetryRegistry retryRegistry = RetryRegistry.of(retryConfig);
    final RetryMetrics retryMetrics = new RetryMetrics(retryRegistry, meterRegistry);

    final TracerRegistry tracerRegistry = new SimpleTracerRegistry();
    final OpenTelemetry openTelemetry = OpenTelemetry.builder()
            .setMeterRegistry(meterRegistry)
            .setTracerRegistry(tracerRegistry)
