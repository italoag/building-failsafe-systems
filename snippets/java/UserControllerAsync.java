@EnableAsync
@RestController
@RequestMapping("/api")
public class UserControllerAsync {

    private final UserService userService;

    public UserControllerAsync(UserService userService) {
        this.userService = userService;
    }

    @CircuitBreaker(name = "userCircuitBreaker", fallbackMethod = "getUserFallbackAsync")
    @GetMapping("/users/{id}")
    public ResponseEntity<Object> getUser(@PathVariable long id) {
        return userService.getUser(id);
    }

    @Bulkhead(name = "userBulkhead", fallbackMethod = "getUserFallbackAsync")
    @GetMapping("/users/{id}/bulkhead")
    public ResponseEntity<Object> getUserWithBulkhead(@PathVariable long id) {
        return userService.getUser(id);
    }

    @Timeout(name = "userTimeout", fallbackMethod = "getUserFallbackAsync")
    @GetMapping("/users/{id}/timeout")
    public ResponseEntity<Object> getUserWithTimeout(@PathVariable long id) {
        return userService.getUser(id);
    }

    @RateLimiter(name = "userRateLimiter", fallbackMethod = "getUserFallbackAsync")
    @GetMapping("/users/{id}/ratelimiter")
    public ResponseEntity<Object> getUserWithRateLimiter(@PathVariable long id) {
        return userService.getUser(id);
    }

    @Retry(name = "userRetry", fallbackMethod = "getUserFallbackAsync")
    @GetMapping("/users/{id}/retry")
    public ResponseEntity<Object> getUserWithRetry(@PathVariable long id) {
        return userService.getUser(id);
    }

    @GetMapping("/users/{id}/cache")
    public ResponseEntity<Object> getUserWithCache(@PathVariable long id) {
        return userService.getUserWithCache(id);
    }

    @Async
    public CompletableFuture<ResponseEntity<Object>> getUserFallbackAsync(long id, Throwable ex) {
        // code to handle fallback
    }
}
//Neste exemplo, estamos utilizando as anotações @EnableAsync e @Async do Spring para tornar o método fallback assincrono



