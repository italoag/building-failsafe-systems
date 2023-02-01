@Configuration
public class Resilience4jConfig {

    @Bean
    public CircuitBreakerConfig circuitBreakerConfig() {
        return CircuitBreakerConfig.custom()
            .failureRateThreshold(50)
            .waitDurationInOpenState(Duration.ofSeconds(30))
            .build();
    }

    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry(CircuitBreakerConfig circuitBreakerConfig) {
        return CircuitBreakerRegistry.of(circuitBreakerConfig);
    }

    @Bean
    public BulkheadConfig bulkheadConfig() {
        return BulkheadConfig.custom()
            .maxConcurrentCalls(50)
            .build();
    }

    @Bean
    public BulkheadRegistry bulkheadRegistry(BulkheadConfig bulkheadConfig) {
        return BulkheadRegistry.of(bulkheadConfig);
    }

    @Bean
    public TimeoutConfig timeoutConfig() {
        return TimeoutConfig.custom()
            .timeoutDuration(Duration.ofMillis(1000))
            .build();
    }

    @Bean
    public TimeoutRegistry timeoutRegistry(TimeoutConfig timeoutConfig) {
        return TimeoutRegistry.of(timeoutConfig);
    }

    @Bean
    public RateLimiterConfig rateLimiterConfig() {
        return RateLimiterConfig.custom()
            .limitForPeriod(10).limitRefreshPeriod(Duration.ofSeconds(1))
            .build();
    }

    @Bean
    public RateLimiterRegistry rateLimiterRegistry(RateLimiterConfig rateLimiterConfig) {
        return RateLimiterRegistry.of(rateLimiterConfig);
    }

    @Bean
    public RetryConfig retryConfig() {
        return RetryConfig.custom()
        .maxAttempts(3)
        .waitDuration(Duration.ofMillis(500))
        .build();
    }

    @Bean
    public RetryRegistry retryRegistry(RetryConfig retryConfig) {
        return RetryRegistry.of(retryConfig);
    }
}
