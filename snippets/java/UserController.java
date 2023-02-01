@RestController
@RequestMapping("/api")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/users/{id}/cache")
    public ResponseEntity<Object> getUserWithCache(@PathVariable long id) {
        return userService.getUserWithCache(id);
    }

    @CircuitBreaker(name = "userCircuitBreaker", fallbackMethod = "getUserFallback")
    @GetMapping("/users/{id}")
    public ResponseEntity<Object> getUser(@PathVariable long id) {
        return userService.getUser(id);
    }

    @Bulkhead(name = "userBulkhead", fallbackMethod = "getUserFallback")
    @GetMapping("/users/{id}/bulkhead")
    public ResponseEntity<Object> getUserWithBulkhead(@PathVariable long id) {
        return userService.getUser(id);
    }

    @Timeout(name = "userTimeout", fallbackMethod = "getUserFallback")
    @GetMapping("/users/{id}/timeout")
    public ResponseEntity<Object> getUserWithTimeout(@PathVariable long id) {
        return userService.getUser(id);
    }

    @RateLimiter(name = "userRateLimiter", fallbackMethod = "getUserFallback")
    @GetMapping("/users/{id}/ratelimiter")
    public ResponseEntity<Object> getUserWithRateLimiter(@PathVariable long id) {
        return userService.getUser(id);
    }

    @Retry(name = "userRetry", fallbackMethod = "getUserFallback")
    @GetMapping("/users/{id}/retry")
    public ResponseEntity<Object> getUserWithRetry(@PathVariable long id) {
        return userService.getUser(id);
    }
}