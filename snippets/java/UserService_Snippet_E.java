@Service
public class UserService {
    private final UserClient userClient;
    private final Tracer tracer;

    @CircuitBreaker(name = "userServiceCB")
    @Bulkhead(name = "userServiceBH", type = Bulkhead.Type.SEMAPHORE)
    public String getUser(String id) {
        Span span = tracer.buildSpan("getUser").start();
        try {
            return userClient.getUser(id);
        } catch (Exception e) {
            span.setAttribute("error", true);
            throw e;
        } finally {
            span.finish();
        }
    }

    @Fallback(fallbackMethod = "getUserFallback")
    public String getUserWithFallback(String id) {
        return getUser(id);
    }

    public String getUserFallback(String id) {
        return "User not found";
    }
}
