@Service
public class UserService {

    private final TimeLimiter timeLimiter;
    private final Bulkhead bulkhead;
    private final CircuitBreaker circuitBreaker;
    private final Metrics metrics;
    private final Tracer tracer;
    private final RabbitTemplate rabbitTemplate;

    @Autowired
    public UserService(TimeLimiter timeLimiter, Bulkhead bulkhead, CircuitBreaker circuitBreaker, Metrics metrics, Tracer tracer, RabbitTemplate rabbitTemplate) {
        this.timeLimiter = timeLimiter;
        this.bulkhead = bulkhead;
        this.circuitBreaker = circuitBreaker;
        this.metrics = metrics;
        this.tracer = tracer;
        this.rabbitTemplate = rabbitTemplate;
    }

    public void setupResilienceTriggers() {
        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
        CircuitBreakerConfig circuitBreakerConfig = circuitBreakerRegistry.getDefaultConfig();
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("userServiceCircuitBreaker", circuitBreakerConfig);

        BulkheadRegistry bulkheadRegistry = BulkheadRegistry.ofDefaults();
        BulkheadConfig bulkheadConfig = bulkheadRegistry.getDefaultConfig();
        Bulkhead bulkhead = bulkheadRegistry.bulkhead("userServiceBulkhead", bulkheadConfig);

        TimeLimiterRegistry timeLimiterRegistry = TimeLimiterRegistry.ofDefaults();
        TimeLimiterConfig timeLimiterConfig = timeLimiterRegistry.getDefaultConfig();
        TimeLimiter timeLimiter = timeLimiterRegistry.timeLimiter("userServiceTimeLimiter", timeLimiterConfig);

        MetricRegistry metricRegistry = new SimpleMetricRegistry();
        metrics.registerCircuitBreakerMetrics(circuitBreaker, metricRegistry);
        metrics.registerBulkheadMetrics(bulkhead, metricRegistry);
        metrics.registerTimeLimiterMetrics(timeLimiter, metricRegistry);
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
        // Simula a chamada ao serviço remoto
        if (Math.random() < 0.5) {
            throw new RuntimeException("Erro ao buscar usuário");
        }
        return "Usuário " + id;
    }

    public String getUserFallback(String id) {
        // Inicia a transação de compensação para buscar o usuário na fila AMQ
        Compensable<String> compensable = Compensate.with(() -> {
            return rabbitTemplate.convertSendAndReceive("userQueue", id, m -> {
                m.getMessageProperties().setHeader("Fallback", "true");
                return m;
            });
        }).build();
        // Executa a transação de compensação
        return compensable.execute();
    }
}