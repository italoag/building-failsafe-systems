import time
import requests

def backoff_retry(func, exceptions, max_tries, interval):
    for i in range(max_tries):
        try:
            return func()
        except exceptions as e:
            if i == max_tries - 1:
                raise
            time.sleep(interval)
            interval *= 2

class CircuitBreaker:
    def __init__(self, max_failures, reset_timeout):
        self.max_failures = max_failures
        self.reset_timeout = reset_timeout
        self.failures = 0
        self.open_since = None

    def is_open(self):
        if self.failures >= self.max_failures:
            if not self.open_since:
                self.open_since = time.time()
            elapsed = time.time() - self.open_since
            if elapsed > self.reset_timeout:
                self.failures = 0
                self.open_since = None
                return False
            return True
        return False

    def record_failure(self):
        self.failures += 1

    def record_success(self):
        self.failures = 0
        self.open_since = None

circuit_breaker = CircuitBreaker(3, 60)

def make_request():
    if circuit_breaker.is_open():
        raise Exception("Circuit breaker is open")
    try:
        response = requests.get("https://www.example.com")
        if response.status_code != 200:
            raise Exception("Request failed")
        circuit_breaker.record_success()
        return response
    except Exception as e:
        circuit_breaker.record_failure()
        raise

def get_data():
    return backoff_retry(make_request, Exception, 5, 1)

# Neste exemplo, o método backoff_retry é utilizado para fazer uma requisição HTTP com o padrão Retry.
# Se a requisição falhar, o método será chamado novamente após um intervalo que é dobrado a cada tentativa subsequente.
# O padrão Circuit Breaker é implementado através da classe CircuitBreaker, que mantém um contador de falhas e determina
# se o circuito está aberto ou fechado. Se o número de falhas ultrapassar o número máximo permitido, o circuito será aberto
# por um período de tempo especificado e qualquer nova requisição será imediatamente interrompida.
# Quando o tempo de reinício passar, o circuito será fechado e as requisições serão permitidas novamente.
# O método make_request utiliza o objeto circuit_breaker para determinar se o circuito está aberto ou fechado antes de
# fazer a requisição