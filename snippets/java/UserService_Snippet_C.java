import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.timelimit.annotation.TimeLimiter;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    @TimeLimiter(name = "userTimeLimiter")
    @CircuitBreaker(name = "userCircuitBreaker", fallbackMethod = "getUserFallback")
    @Bulkhead(name = "userBulkhead")
    public String getUser(String userId) {
        logger.info("Getting user information for user id: {}", userId);
        // Code to get user information from external service
        return "User information for user id: " + userId;
    }

    public String getUserFallback(String userId, Throwable t) {
        logger.error("Error getting user information for user id: {}, reason: {}", userId, t.getMessage());
        // Code to compensate for failure
        return "Error getting user information";
    }

    public CompletableFuture<String> getUserWithCompensation(String userId) {
        return CompletableFuture.supplyAsync(() -> {
            // Code to put the request in a message queue (e.g. RabbitMQ)
            return "User request placed in message queue for user id: " + userId;
        });
    }
}
