public class UsuarioService {
    private final TimeLimiter timeLimiter;
    private final Bulkhead bulkhead;
    private final CircuitBreaker circuitBreaker;
    private final RabbitTemplate rabbitTemplate;

@Autowired
public UserService(TimeLimiter timeLimiter, Bulkhead bulkhead, CircuitBreaker circuitBreaker, RabbitTemplate rabbitTemplate) {
        this.timeLimiter = timeLimiter;
        this.bulkhead = bulkhead;
        this.circuitBreaker = circuitBreaker;
        this.rabbitTemplate = rabbitTemplate;
    }
@Compensable(fallbackMethod = "getUserFallback")
@TimeLimiter(name = "userServiceTimeLimiter")
@Bulkhead(name = "userServiceBulkhead")
@CircuitBreaker(name = "userServiceCircuitBreaker")
public String getUser(String id) {
        try {
            return getUserFromRemoteService(id);
        } catch (RuntimeException e) {
            throw new UserNotFoundException("Erro ao buscar usuário", e);
        }
    }

    public String getUserFromRemoteService(String id) {
        // Simula a chamada ao serviço remoto
    if (Math.random() < 0.5) {
        throw new RuntimeException("Erro ao buscar usuário");
    }
    return "Usuário " + id;
    }

    public String getUserFallback(String id, UserNotFoundException e) {
        // Inicia a transação de compensação para buscar o usuário na fila AMQ
    String user = (String) rabbitTemplate.convertSendAndReceive("userQueue", id);
    if (user == null) {
        throw e;
    }
    return user;
    }
}