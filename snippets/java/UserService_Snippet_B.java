@Service
public class UserService {
    private final CircuitBreaker circuitBreaker;
    private final Bulkhead bulkhead;
    private final TimeLimiter timeLimiter;
    private final RabbitTemplate rabbitTemplate;

    @Autowired
    public UserService(CircuitBreaker circuitBreaker, Bulkhead bulkhead, TimeLimiter timeLimiter, RabbitTemplate rabbitTemplate) {
        this.circuitBreaker = circuitBreaker;
        this.bulkhead = bulkhead;
        this.timeLimiter = timeLimiter;
        this.rabbitTemplate = rabbitTemplate;
    }

    @CircuitBreaker(name = "userServiceCircuitBreaker", fallbackMethod = "getUserFallback")
    public String getUser(String id) {
        return timeLimiter.executeCheckedSupplier(() -> getUserBlocking(id), UserNotFoundException.class);
    }

    @Bulkhead(name = "userServiceBulkhead")
    public String getUserBlocking(String id) {
        return circuitBreaker.executeSupplier(() -> getUserFromRemoteService(id));
    }

    @TimeLimiter(name = "userServiceRemove")
    public String getUserFromRemoteService(String id) {
        // Simula a chamada ao serviço remoto
        if (Math.random() < 0.5) {
            throw new RuntimeException("Erro ao buscar usuário");
        }
        return "Usuário " + id;
    }

    public String getUserFallback(String id, Throwable throwable) {
        // Inicia a transação de compensação para buscar o usuário na fila AMQ
        return rabbitTemplate.convertSendAndReceive("userQueue", id, m -> {
            m.getMessageProperties().setHeader("Fallback", "true");
            return m;
        });
    }
}
