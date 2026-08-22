from pydantic import BaseModel, Field
from typing import List, Optional
from enum import Enum


class EstadoAsistencia(str, Enum):
    PRESENTE = "PRESENTE"
    TARDANZA = "TARDANZA"
    AUSENTE = "AUSENTE"
    JUSTIFICADO = "JUSTIFICADO"


class TendenciaAsistencia(str, Enum):
    ASCENDENTE = "ASCENDENTE"
    DESCENDENTE = "DESCENDENTE"
    ESTABLE = "ESTABLE"


class NivelAtencion(str, Enum):
    ALTO = "ALTO"
    MEDIO = "MEDIO"
    BAJO = "BAJO"


class AsistenciaEstudianteDTO(BaseModel):
    """DTO para datos de asistencia de un estudiante individual"""
    estudiante_id: int = Field(..., description="ID del estudiante")
    nombre: str = Field(..., description="Nombre del estudiante")
    presentes: int = Field(ge=0, default=0)
    tardanzas: int = Field(ge=0, default=0)
    ausentes: int = Field(ge=0, default=0)
    justificados: int = Field(ge=0, default=0)
    total_sesiones: int = Field(ge=0, default=0)

    @property
    def porcentaje_asistencia(self) -> float:
        if self.total_sesiones == 0:
            return 0.0
        asistieron = self.presentes + self.tardanzas + self.justificados
        return round((asistieron / self.total_sesiones) * 100, 2)


class AnalisisRequest(BaseModel):
    """Request para análisis de asistencia"""
    pregunta: Optional[str] = Field(None, description="Pregunta del usuario")
    ai_provider: Optional[str] = Field(None, description="Proveedor de IA")
    ai_key: Optional[str] = Field(None, description="API Key del proveedor")
    estudiantes: List[AsistenciaEstudianteDTO] = Field(
        ..., description="Lista de estudiantes con sus datos de asistencia"
    )
    periodo: Optional[str] = Field(None, description="Periodo académico (ej: 2026-08)")


class InsightEstudiante(BaseModel):
    """Insight individual por estudiante"""
    estudiante_id: int
    nombre: str
    porcentaje_asistencia: float
    tendencia: TendenciaAsistencia
    nivel_atencion: NivelAtencion
    observaciones: List[str] = []


class PatronDetectado(BaseModel):
    """Patrón detectado en los datos"""
    tipo: str
    descripcion: str
    estudiantes_afectados: List[int] = []
    severidad: str = "info"  # info, warning, attention


class AnalisisResponse(BaseModel):
    """Response del análisis de asistencia"""
    respuesta_ia: Optional[str] = Field(None, description="Respuesta generada por el LLM")
    resumen_general: dict
    insights_estudiantes: List[InsightEstudiante] = []
    patrones_detectados: List[PatronDetectado] = []
    recomendaciones: List[str] = []