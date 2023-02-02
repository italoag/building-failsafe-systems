@Service
public class CircuitBreakerBackoffSnippet {

    private final RestTemplate restTemplate;

    public UserService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @CircuitBreaker(name = "userService", fallbackMethod = "getUserFallback")
    @Retry(name = "userService")
    public String getUser(String userId) {
        return restTemplate.getForObject("https://jsonplaceholder.typicode.com/users/" + userId, String.class);
    }

    public String getUserFallback(String userId, TooManyRequestsException e) {
        return "Too many requests";
    }
}
