from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
import os

from app.api import analysis

app = FastAPI(
    title="AulaIA - Servicio de Análisis Inteligente",
    description="Servicio de IA para análisis de patrones de asistencia escolar",
    version="1.0.0"
)

# CORS para comunicación con Spring Boot
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # En producción restringir a Spring Boot
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Incluir routers
app.include_router(analysis.router, prefix="/api/v1", tags=["análisis"])


@app.get("/health", tags=["health"])
async def health_check():
    """Endpoint de salud para Docker y monitoreo"""
    return {"status": "ok", "service": "aulaia-fastapi"}


@app.get("/", tags=["root"])
async def root():
    return {
        "service": "AulaIA FastAPI",
        "version": "1.0.0",
        "docs": "/docs",
        "health": "/health"
    }


if __name__ == "__main__":
    import uvicorn
    port = int(os.getenv("PORT", "8000"))
    uvicorn.run("app.main:app", host="0.0.0.0", port=port, reload=True)