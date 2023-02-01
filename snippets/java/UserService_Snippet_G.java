@Service
public class UserService {

    private final CircuitBreaker circuitBreaker;
    private final Bulkhead bulkhead;
    private final TimeLimiter timeLimiter;
    private final MetricRegistry metricRegistry;
    private final Tracer tracer;
    private final RabbitTemplate rabbitTemplate;

@Autowired
public UserService(CircuitBreaker circuitBreaker, Bulkhead bulkhead, TimeLimiter timeLimiter, MetricRegistry metricRegistry, Tracer tracer, RabbitTemplate rabbitTemplate) {
    this.circuitBreaker = circuitBreaker;
    this.bulkhead = bulkhead;
    this.timeLimiter = timeLimiter;
    this.metricRegistry = metricRegistry;
    this.tracer = tracer;
    this.rabbitTemplate = rabbitTemplate;
}

public void setupResilienceTriggers() {
    metricRegistry.registerAll(CircuitBreakerMetrics.ofCircuitBreaker(circuitBreaker));
    metricRegistry.registerAll(BulkheadMetrics.ofBulkhead(bulkhead));
    metricRegistry.registerAll(TimeLimiterMetrics.ofTimeLimiter(timeLimiter));
}

public String getUser(String id) {
    try {
        return timeLimiter.executeCheckedSupplier(() -> getUserBlocking(id), UserNotFoundException.class);
    } catch (UserNotFoundException e) {
        return getUserFallback(id);
    }
}

public String getUserBlocking(String id) {
    return bulkhead.executeSupplier(() -> {
        return circuitBreaker.executeSupplier(() -> {
            return getUserFromRemoteService(id);
        });
    });
}

public String getUserFromRemoteService(String id) {
    // Adicionar o OpenTelemetry para rastrear a chamada à API remota
    Span span = tracer.spanBuilder("getUserFromRemoteService").startSpan();
    try {
        // Simula a chamada ao serviço remoto
        if (Math.random() < 0.5) {
            throw new RuntimeException("Erro ao buscar usuário");
        }
        return "Usuário " + id;
    } finally {
        span.end();
    }
}

public String getUserFallback(String id) {
    // Adicionar o OpenTelemetry para rastrear a chamada à fila AMQ
    Span span = tracer.spanBuilder("getUserFallback").startSpan();
    try {
        // Inicia a transação de compensação para buscar o usuário na fila AMQ
        return rabbitTemplate.convertSendAndReceive("userQueue", id);
    } finally {
        span.end();
    }
}
}