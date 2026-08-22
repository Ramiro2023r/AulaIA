from fastapi import APIRouter, HTTPException
from app.schemas.analysis import AnalisisRequest, AnalisisResponse
from app.services.analysis_service import AnalysisService

router = APIRouter()
analysis_service = AnalysisService()


@router.post(
    "/analisis/asistencia",
    response_model=AnalisisResponse,
    summary="Análisis estadístico de asistencia",
    description="""
    Recibe datos agregados de asistencia y devuelve:
    - Resumen general (totales, porcentaje global, tendencia)
    - Insights por estudiante (porcentaje, tendencia, nivel de atención, observaciones)
    - Patrones detectados (alto ausentismo, tardanzas recurrentes, descenso, asistencia perfecta)
    - Recomendaciones accionables para el docente
    
    Fase IA-1: Reglas + estadística determinista (Pandas/NumPy)
    No usa ML hasta dataset suficiente y validado.
    """
)
async def analizar_asistencia(request: AnalisisRequest):
    try:
        return analysis_service.analizar_asistencia(request)
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error en análisis: {str(e)}")


@router.get(
    "/analisis/health",
    summary="Health check del módulo de análisis",
    description="Verifica que el servicio de análisis esté operativo"
)
async def analysis_health():
    return {
        "status": "ok",
        "module": "analysis",
        "version": "IA-1 (reglas + estadística)"
    }