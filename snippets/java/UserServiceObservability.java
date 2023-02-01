import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Service
public class UserServiceObservability {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserServiceObservability.class);

    private final RestTemplate restTemplate;

    public UserServiceObservability(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Retry(name = "retry")
    @CircuitBreaker(name = "circuitBreaker")
    @RateLimiter(name = "rateLimiter")
    @Bulkhead(name = "bulkhead")
    @TimeLimiter(name = "timeLimiter")
    public String getUser() {
        return restTemplate.getForObject("https://jsonplaceholder.typicode.com/users/1", String.class);
    }

    public String getUserFallback() {
        LOGGER.error("User service is not available");
        return "User service is not available";
    }

    // Gatilhos para os padrões de resiliencia utilizando métricas do OpenTelemetry
    public void setupResilienceTriggers() {
        // Latencia
        LatencyTrigger latencyTrigger = LatencyTrigger.of(Duration.ofMillis(500));
        circuitBreaker.getEventPublisher()
                .onCallNotPermitted(event -> {
                    if (event.getCircuitBreaker().getMetrics().getNumberOfBufferedCalls() > 0) {
                        latencyTrigger.recordSuccess();
                    } else {
                        latencyTrigger.recordError();
                    }
                })
                .onSuccess(event -> latencyTrigger.recordSuccess())
                .onError(event -> latencyTrigger.recordError());
        circuitBreaker.getEventPublisher().onStateTransition(event -> {
            if (event.getStateTransition().getFrom() == CircuitBreaker.State.FORCED_OPEN && event.getStateTransition().getTo() == CircuitBreaker.State.HALF_OPEN) {
                latencyTrigger.reset();
            }
        });
        circuitBreaker.getEventPublisher().onStateTransition(event -> {
            if (event.getStateTransition().getFrom() == CircuitBreaker.State.HALF_OPEN && event.getStateTransition().getTo() == CircuitBreaker.State.CLOSED) {
                latencyTrigger.reset();
            }
        });

        SemaphoreTrigger semaphoreTrigger = SemaphoreTrigger.ofDefaults("userService");
        semaphoreTrigger.register(bulkhead.getEventPublisher());

        // latency trigger
        LatencyTrigger latencyTrigger = LatencyTrigger.ofDefaults("userService");
        latencyTrigger.register(timeLimiter.getEventPublisher());
    }

}

//Neste exemplo, estamos utilizando as classes SemaphoreTrigger e LatencyTrigger do Resilience4j
// para gerenciar as metricas geradas pelo OpenTelemetry. Em setupResilienceTriggers()
// estamos criando uma instância de cada classe e registrando no publisher de eventos dos
// respectivos padrões (bulkhead e time limiter). Isso permite que essas metricas sejam utilizadas
// para gatilhar ações de fallback, abertura e fechamento do circuit breaker e etc.
