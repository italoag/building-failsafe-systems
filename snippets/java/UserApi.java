@FeignClient(name = "user-service", url = "http://localhost:8080/api",
    configuration = UserApiConfiguration.class)

public interface UserApi {
    @GetMapping("/users/{id}")
    @CircuitBreaker(name = "userCircuitBreaker", fallbackMethod = "getUserFallback")
    ResponseEntity<Object> getUser(@PathVariable long id);

    @GetMapping("/users/{id}/bulkhead")
    @Bulkhead(name = "userBulkhead", fallbackMethod = "getUserFallback")
    ResponseEntity<Object> getUserWithBulkhead(@PathVariable long id);

    @GetMapping("/users/{id}/timeout")
    @Timeout(name = "userTimeout", fallbackMethod = "getUserFallback")
    ResponseEntity<Object> getUserWithTimeout(@PathVariable long id);

    @GetMapping("/users/{id}/ratelimiter")
    @RateLimiter(name = "userRateLimiter", fallbackMethod = "getUserFallback")
    ResponseEntity<Object> getUserWithRateLimiter(@PathVariable long id);

    @GetMapping("/users/{id}/retry")
    @Retry(name = "userRetry", fallbackMethod = "getUserFallback")
    ResponseEntity<Object> getUserWithRetry(@PathVariable long id);

    @GetMapping("/users/{id}/cache")
    ResponseEntity<Object> getUserWithCache(@PathVariable long id);

    default ResponseEntity<Object> getUserFallback(long id, Throwable ex) {
        // code to handle fallback
    }
}
