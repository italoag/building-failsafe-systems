import time
import asyncio
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

# Semáforo para gerenciar o acesso ao serviço externo
semaphore = asyncio.Semaphore(5)

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

    # Padrão Bulkhead
    async with semaphore:
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

        # Simulando tempo de resposta do serviço externo
        await asyncio.sleep(0.5)
        return response.json()

# Neste exemplo, adicionamos a implementação do padrão "Bulkhead" usando semáforos.
# O semáforo é usado para controlar o número máximo de solicitações ao serviço externo ao mesmo tempo.
# Isso evita que o serviço se torne sobrecarregado e garante a disponibilidade do serviço para outras solicitações.


