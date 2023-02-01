@Service
@CircuitBreaker(name = "userServiceCB")
@Bulkhead(name = "userServiceBH")
public class UserService {
    private final UserClient userClient;
    private final CircuitBreaker circuitBreaker;
    private final Bulkhead bulkhead;
    private final OpentelemetryTracer tracer;

    public UserService(UserClient userClient, CircuitBreakerRegistry circuitBreakerRegistry, BulkheadRegistry bulkheadRegistry, TracerProvider tracerProvider) {
        this.userClient = userClient;
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("userServiceCB");
        this.bulkhead = bulkheadRegistry.bulkhead("userServiceBH");
        this.tracer = tracerProvider.get("user-service");
    }

    @CircuitBreaker(name = "userServiceCB")
    @Bulkhead(name = "userServiceBH")
    public String getUser(String id) {
        return CircuitBreaker.decorateSupplier(circuitBreaker,
                Bulkhead.decorateSupplier(bulkhead,
                        () -> userClient.getUser(id))).get();
    }

    public String getUserFallback(String id) {
        return "User not found";
    }

    public void setupResilienceTriggers() {
        // Latency trigger for circuit breaker
        LatencyTrigger latencyTrigger = LatencyTrigger.of(Duration.ofMillis(500));
        circuitBreaker.getEventPublisher()
                .onCallNotPermitted(event -> {
                    if (event.getCircuitBreaker().getMetrics().getNumberOfBufferedCalls() > 0) {
                        latencyTrigger.recordSuccess();
                    } else {
                        latencyTrigger.recordError();
                    }
                })
                .onSuccess(event -> latencyTrigger.recordSuccess())
                .onError(event -> latencyTrigger.recordError());
        circuitBreaker.getEventPublisher().onStateTransition(event -> {
            if (event.getStateTransition().getFrom() == CircuitBreaker.State.FORCED_OPEN && event.getStateTransition().getTo() == CircuitBreaker.State.HALF_OPEN) {
                latencyTrigger.reset();
            }
        });
        circuitBreaker.getEventPublisher().onStateTransition(event -> {
            if (event.getStateTransition().getFrom() == CircuitBreaker.State.HALF_OPEN && event.getStateTransition().getTo() == CircuitBreaker.State.CLOSED) {
                latencyTrigger.reset();
            }
        });

        // Semaphore trigger for bulkhead
        SemaphoreTrigger semaphoreTrigger = SemaphoreTrigger.of(10);
        bulkhead.getEventPublisher().onCallNotPermitted(event -> semaphoreTrigger.recordError()).onSuccess(event -> semaphoreTrigger.recordSuccess());
        // Register triggers with circuit breaker and bulkhead
        circuitBreaker.configure(config -> config.addTrigger(latencyTrigger));
        bulkhead.configure(config -> config.addTrigger(semaphoreTrigger));
    }
}