@Service
public class UserService {
    private final RestTemplate restTemplate;
    private final Semaphore semaphore;
    private final TimeLimiter timeLimiter;
    private final CircuitBreaker circuitBreaker;
    private final Tracer tracer;
    private final MeterRegistry meterRegistry;

    public UserService(RestTemplate restTemplate, Semaphore semaphore, TimeLimiter timeLimiter, CircuitBreaker circuitBreaker, Tracer tracer, MeterRegistry meterRegistry) {
        this.restTemplate = restTemplate;
        this.semaphore = semaphore;
        this.timeLimiter = timeLimiter;
        this.circuitBreaker = circuitBreaker;
        this.tracer = tracer;
        this.meterRegistry = meterRegistry;
    }

@CircuitBreaker(name = "userService", fallbackMethod = "getUserFallback")
@SemaphoreBulkhead(name = "userService", fallbackMethod = "getUserFallback")
@Timed(name = "userService")
public String getUser(String userId) {
        final Span span = tracer.nextSpan().name("getUser").start();
        try (final Scope scope = tracer.withSpan(span)) {
            return timeLimiter.executeSupplier(() -> semaphore.executeSupplier(() -> circuitBreaker.executeSupplier(() -> {
                try {
                    final String response = restTemplate.getForObject("https://jsonplaceholder.typicode.com/users/" + userId, String.class);
                    span.setAttribute("response", response);
                    return response;
                } catch (HttpClientErrorException e) {
                    if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                        throw new TooManyRequestsException();
                    } else {
                        throw e;
                    }
                }
            })));
        } finally {
            span.end();
        }
    }

public String getUserFallback(String userId, TooManyRequestsException e) {
    final Span span = tracer.nextSpan().name("getUserFallback").start();
    try (final Scope scope = tracer.withSpan(span)) {
        span.setAttribute("error", e.getMessage());
        return "Too many requests";
    } finally {
        span.end();
    }
}

@PostConstruct
public void setupResilienceTriggers() {
        final Clock clock = Clock.SYSTEM;
        final MeterRegistry meterRegistry = new SimpleMeterRegistry();
        final SleuthTracer tracer = new SleuthTracer(meterRegistry);
        final SleuthMeterEvents sleuthMeterEvents = new SleuthMeterEvents(tracer);

        final TimeLimiterConfig timeLimiterConfig = TimeLimiterConfig.custom()
            .timeoutDuration(Duration.ofMillis(1000))
            .cancelRunningFuture(true)
            .build();
        final TimeLimiterRegistry timeLimiterRegistry = TimeLimiterRegistry.of(timeLimiterConfig);
        final TimeLimiterMetrics timeLimiterMetrics = new TimeLimiterMetrics(timeLimiterRegistry, meterRegistry, clock);

        final SemaphoreConfig semaphoreConfig = SemaphoreConfig.custom()
                .maxConcurrentCalls(10)
                .build();
        final SemaphoreRegistry semaphoreRegistry = SemaphoreRegistry.of(semaphoreConfig);
        final SemaphoreMetrics semaphoreMetrics = new SemaphoreMetrics(semaphoreRegistry, meterRegistry, clock);

        final CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.ofDefaults();
        final CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.of(circuitBreakerConfig);
        final CircuitBreakerMetrics circuitBreakerMetrics = new CircuitBreakerMetrics(circuitBreakerRegistry, meterRegistry, clock);

        // Criação do gatilho de latência utilizando OpenTelemetry
        LatencyTrigger latencyTrigger = LatencyTrigger.of(Duration.ofMillis(500));
        latencyTrigger.attach(timeLimiter, meterRegistry, "timeLimiter", tags);

        // Criação do gatilho de circuit breaker utilizando OpenTelemetry
        CircuitBreakerTrigger circuitBreakerTrigger = CircuitBreakerTrigger.of();
        circuitBreakerTrigger.attach(circuitBreaker, meterRegistry, "circuitBreaker", tags);

        // Criação do gatilho de bulkhead utilizando Spring Cloud Sleuth e Resilience4j Metrics
        SleuthBulkheadTrigger sleuthBulkheadTrigger = SleuthBulkheadTrigger.of(tracer);
        sleuthBulkheadTrigger.attach(bulkhead, "bulkhead", tags);
    }
}

//Escreva um exemplo de código java utilizando as métricas de observabilidade fornecidas pelos Frameworks
// OpenTelemetry versão 1.22, Spring Cloud Sleuth 3.1.3 e Metricas do Resilience4j versão 2.0.2 para
// alimentar gatilhos dos componentes de resiliencia do Resilience4j, detalhe o uso de cada abordagem.
// Neste exemplo, estamos utilizando as métricas de observabilidade fornecidas pelos Frameworks OpenTelemetry,
// Spring Cloud Sleuth e Metricas do Resilience4j para alimentar gatilhos dos componentes de resiliencia
// do Resilience4j.
// Utilizamos o LatencyTrigger do OpenTelemetry para criar um gatilho de latência que será utilizado
// pelo componente TimeLimiter. O método attach() é utilizado para associar o gatilho ao componente
// específico e ao meterRegistry, que é responsável por coletar e armazenar as métricas. Além disso,
// também passamos as tags que serão utilizadas para identificar as métricas.

//Da mesma forma, utilizamos o CircuitBreakerTrigger do OpenTelemetry para criar um gatilho
