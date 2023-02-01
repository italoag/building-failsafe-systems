import time
import asyncio
import requests
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

        # Função de Fallback
        if response.json().get("error"):
            return {"message": "Using fallback data"}

        # Simulando tempo de resposta do serviço externo
        await asyncio.sleep(0.5)

        # Transação de compensação
        try:
            # Realizar alguma ação com o dado retornado do serviço externo
            pass
        except Exception as e:
            # Realizar a compensação da transação em caso de falha
            pass

        return response.json()

    # Neste exemplo, adicionamos a implementação da função de "Fallback" e da "Transação de compensação".
    # A função de "Fallback" é usada para retornar dados alternativos em caso de erro na resposta do serviço externo.
    # A "Transação de compensação" é usada para garantir a consistência dos dados, realizando uma ação de
    # compensação em caso de falha na ação principal.




