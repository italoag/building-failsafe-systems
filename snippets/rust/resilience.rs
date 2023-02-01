use actix_web::{web, App, HttpResponse, HttpServer};
use std::sync::{Arc, Mutex};
use std::time::Duration;
use tokio::time::delay_for;

struct State {
    request_counter: i32,
    semaphore: Arc<Semaphore>,
    circuit_breaker: Arc<Mutex<CircuitBreaker>>,
}

#[derive(Clone, Copy, Debug)]
struct CircuitBreaker {
    failed_requests: i32,
    success_requests: i32,
    state: CircuitBreakerState,
}

enum CircuitBreakerState {
    Open,
    Closed,
    HalfOpen,
}

impl CircuitBreaker {
    fn new() -> CircuitBreaker {
        CircuitBreaker {
            failed_requests: 0,
            success_requests: 0,
            state: CircuitBreakerState::Closed,
        }
    }

    fn update_state(&mut self, result: Result<(), ()>) {
        match result {
            Ok(()) => {
                self.success_requests += 1;
                if self.state == CircuitBreakerState::HalfOpen {
                    self.state = CircuitBreakerState::Closed;
                }
            }
            Err(()) => {
                self.failed_requests += 1;
                if self.failed_requests >= 5 && self.state == CircuitBreakerState::Closed {
                    self.state = CircuitBreakerState::Open;
                } else if self.state == CircuitBreakerState::Open {
                    self.state = CircuitBreakerState::HalfOpen;
                }
            }
        }
    }

    fn can_request(&self) -> bool {
        match self.state {
            CircuitBreakerState::Open => false,
            _ => true,
        }
    }
}

struct Semaphore {
    limit: i32,
    counter: i32,
}

impl Semaphore {
    fn new(limit: i32) -> Semaphore {
        Semaphore { limit, counter: 0 }
    }

    fn acquire(&mut self) -> bool {
        if self.counter >= self.limit {
            return false;
        }
        self.counter += 1;
        true
    }

    fn release(&mut self) {
        self.counter -= 1;
    }
}

async fn get_data_from_external_service() -> Result<String, ()> {
    // Simulating external service
    delay_for(Duration::from_secs(1)).await;
    Ok("External Service Data".to_owned())
}

async fn handle_request(
        state: web::Data<Arc<Mutex<State>>>,
        ) -> Result<HttpResponse, actix_web::Error> {
    let mut state = state.lock().unwrap();

    // Check rate limiter
    if state.request_counter >= 1000 {
        return Ok(HttpResponse::TooManyRequests().body("Too many requests"));
    }
    state.request_counter += 1;

    // Check semaphore
    if !state.semaphore.acquire() {
        return Ok(HttpResponse::ServiceUnavailable().body("Service unavailable"));
    }
    defer! {{ state.semaphore.release(); }};

    // Check circuit breaker
    let mut circuit_breaker = state.circuit_breaker.lock().unwrap();
    if !circuit_breaker.can_request() {
        return Ok(HttpResponse::ServiceUnavailable().body("Circuit breaker is open"));
    }

    let result = match get_data_from_external_service().await {
        Ok(data) => Ok((HttpResponse::Ok().body(data), ())),
        Err(()) => {
            let response = HttpResponse::InternalServerError().body("Error getting data");
            Err((response, ()))
        }
    };

    circuit_breaker.update_state(result.as_ref().map(|_| ()).map_err(|_| ()));
    result.map(|(response, _)| response)
}

#[actix_web::main]
async fn main() -> std::io::Result<()> {
    let state = Arc::new(Mutex::new(State {
        request_counter: 0,
        semaphore: Arc::new(Semaphore::new(100)),
        circuit_breaker: Arc::new(Mutex::new(CircuitBreaker::new())),
    }));

    HttpServer::new(move || {
        App::new()
            .data(state.clone())
            .route("/data", web::get().to(handle_request))
    })
    .bind("127.0.0.1:8080")?
    .run()
    .await
}