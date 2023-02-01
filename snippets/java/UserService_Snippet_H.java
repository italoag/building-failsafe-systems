@Service
public class UserService {

    private final RestTemplate restTemplate;
    private final Semaphore semaphore;
    private final TimeLimiter timeLimiter;
    private final CircuitBreaker circuitBreaker;

    public UserService(RestTemplate restTemplate, Semaphore semaphore, TimeLimiter timeLimiter, CircuitBreaker circuitBreaker) {
        this.restTemplate = restTemplate;
        this.semaphore = semaphore;
        this.timeLimiter = timeLimiter;
        this.circuitBreaker = circuitBreaker;
    }

    public String getUser(String userId) {
        return timeLimiter.executeSupplier(() -> semaphore.executeSupplier(() -> circuitBreaker.executeSupplier(() -> {
            try {
                return restTemplate.getForObject("https://jsonplaceholder.typicode.com/users/" + userId, String.class);
            } catch (HttpClientErrorException e) {
                if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                    throw new TooManyRequestsException();
                } else {
                    throw e;
                }
            }
        })));
    }

    public String getUserFallback(String userId, TooManyRequestsException e) {
        return "Too many requests";
    }

    public void setupResilienceTriggers() {
        final MeterRegistry meterRegistry = new SimpleMeterRegistry();
        final SleuthTracer tracer = new SleuthTracer(meterRegistry);
        final SleuthMeterEvents sleuthMeterEvents = new SleuthMeterEvents(tracer);

        final Clock clock = Clock.SYSTEM;
        final TimeLimiterConfig config = TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofMillis(1000))
                .cancelRunningFuture(true)
                .build();
        final TimeLimiterRegistry timeLimiterRegistry = TimeLimiterRegistry.of(config);
        final TimeLimiterMetrics timeLimiterMetrics = new TimeLimiterMetrics(timeLimiterRegistry, meterRegistry, clock);

        final SemaphoreConfig semaphoreConfig = SemaphoreConfig.custom()
                .maxConcurrentCalls(10)
                .build();
        Registry semaphoreRegistry = SemaphoreRegistry.of(semaphoreConfig);
        final SemaphoreMetrics semaphoreMetrics = new SemaphoreMetrics(semaphoreRegistry, meterRegistry, clock);
        final CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
            .failureRateThreshold(50)
            .ringBufferSizeInHalfOpenState(2)
            .ringBufferSizeInClosedState(2)
            .waitDurationInOpenState(Duration.ofMillis(1000))
            .build();
        final CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.of(circuitBreakerConfig);
        final CircuitBreakerMetrics circuitBreakerMetrics = new CircuitBreakerMetrics(circuitBreakerRegistry, meterRegistry, clock);

        sleuthMeterEvents.bindTo(timeLimiterMetrics);
        sleuthMeterEvents.bindTo(semaphoreMetrics);
        sleuthMeterEvents.bindTo(circuitBreakerMetrics);
    }
}