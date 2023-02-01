@Configuration
public class UserApiConfiguration {
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final BulkheadRegistry bulkheadRegistry;
    private final TimeoutRegistry timeoutRegistry;
    private final RateLimiterRegistry rateLimiterRegistry;
    private final RetryRegistry retryRegistry;

    public UserApiConfiguration(
            CircuitBreakerRegistry circuitBreakerRegistry,
            BulkheadRegistry bulkheadRegistry,
            TimeoutRegistry timeoutRegistry,
            RateLimiterRegistry rateLimiterRegistry,
            RetryRegistry retryRegistry
    ) {
        this.circiruitBreakerRegistry = circuitBreakerRegistry;
        this.bulkheadRegistry = bulkheadRegistry;
        this.timeoutRegistry = timeoutRegistry;
        this.rateLimiterRegistry = rateLimiterRegistry;
        this.retryRegistry = retryRegistry;
    }

    @Bean
    public CircuitBreaker circuitBreaker(CircuitBreakerRegistry circuitBreakerRegistry) {
        return FeignCircuitBreaker.of(circuitBreakerRegistry);
    }

    @Bean
    public Bulkhead bulkhead(BulkheadRegistry bulkheadRegistry) {
        return FeignBulkhead.of(bulkheadRegistry);
    }

    @Bean
    public Timeout timeout(TimeoutRegistry timeoutRegistry) {
        return FeignTimeout.of(timeoutRegistry);
    }

    @Bean
    public RateLimiter rateLimiter(RateLimiterRegistry rateLimiterRegistry) {
        return FeignRateLimiter.of(rateLimiterRegistry);
    }

    @Bean
    public Retry retry(RetryRegistry retryRegistry) {
        return FeignRetry.of(retryRegistry);
    }
}