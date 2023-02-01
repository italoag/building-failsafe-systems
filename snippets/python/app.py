import time
from fastapi import FastAPI
from fastapi.responses import JSONResponse
from fastapi.exceptions import HTTPException

app = FastAPI()

# Cache do estado do serviço externo
external_service_available = True

# Limite de solicitações por segundo
request_limit = 5

# Contador de solicitações
request_counter = 0

@app.exception_handler(HTTPException)
async def http_exception_handler(request, exc):
    return JSONResponse(
content={
"message": "Service Unavailable"
},
status_code=503
)

@app.get("/data")
async def data():
    global external_service_available
    global request_counter

    # Padrão Circuit Breaker
    if not external_service_available:
        raise HTTPException(status_code=503, detail="External service unavailable")

    # Padrão Rate Limiter
    if request_counter >= request_limit:
        raise HTTPException(status_code=429, detail="Too many requests")
    request_counter += 1

    # Padrão Retry
    retries = 0
    while retries < 3:
        try:
            response = requests.get("https://example.com/data")
            if response.status_code == 200:
                break
        except:
            retries += 1
    else:
        external_service_available = False
        raise HTTPException(status_code=503, detail="External service unavailable")

    # Padrão Bulkhead
    time.sleep(0.5)
    return response.json()

# Neste exemplo, estamos implementando os padrões de Circuit Breaker, Retry e Bulkhead.
# O padrão Circuit Breaker é implementado com uma variável global que mantém o estado do serviço externo.
# Se o serviço estiver indisponível, a resposta da rota será uma mensagem de erro 503 (Service Unavailable).
# O padrão Retry é implementado com um loop que tenta acessar o serviço externo até três vezes.
# Se as três tentativas falharem, o estado do serviço é definido como indisponível e uma exceção é lançada.
# O padrão Bulkhead é implementado com uma pausa artificial antes da resposta ser retornada para o cliente.

# também adicionamos a implementação do padrão "Rate Limiter".
# É mantido um contador de solicitações e um limite de solicitações por segundo.
# Se o número de solicitações exceder o limite, a resposta da rota será uma mensagem de erro 429 (Too Many Requests).