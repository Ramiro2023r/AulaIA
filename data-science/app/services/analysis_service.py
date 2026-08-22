import pandas as pd
import numpy as np
import groq
from typing import List, Dict, Any
from app.schemas.analysis import (
    AnalisisRequest, AnalisisResponse,
    InsightEstudiante, PatronDetectado,
    TendenciaAsistencia, NivelAtencion, EstadoAsistencia
)


class AnalysisService:
    """Servicio de análisis estadístico para asistencia escolar"""

    def analizar_asistencia(self, request: AnalisisRequest) -> AnalisisResponse:
        """
        Análisis estadístico determinista (Fase IA-1: reglas + estadística)
        No usa ML hasta que haya dataset suficiente y validado.
        """
        if not request.estudiantes:
            return self._empty_response()

        # Convertir a DataFrame para análisis con Pandas
        df = self._to_dataframe(request.estudiantes)

        # 1. Resumen general
        resumen = self._calcular_resumen_general(df)

        # 2. Insights por estudiante
        insights = self._calcular_insights_estudiantes(df)

        # 3. Detección de patrones (Fase IA-2)
        patrones = self._detectar_patrones(df)

        # 4. Recomendaciones
        recomendaciones = self._generar_recomendaciones(insights, patrones)
        
        # 5. Integración con IA
        respuesta_ia = None
        if request.ai_key and request.ai_provider == 'groq':
            respuesta_ia = self._consultar_groq(request, resumen, df)

        return AnalisisResponse(
            respuesta_ia=respuesta_ia,
            resumen_general=resumen,
            insights_estudiantes=insights,
            patrones_detectados=patrones,
            recomendaciones=recomendaciones
        )
        
    def _consultar_groq(self, request: AnalisisRequest, resumen: dict, df: pd.DataFrame) -> str:
        try:
            client = groq.Groq(api_key=request.ai_key)
            
            # Construir contexto
            contexto = f"Datos del colegio:\n- Total Estudiantes: {resumen['total_estudiantes']}\n"
            contexto += f"- Porcentaje Asistencia: {resumen['porcentaje_asistencia_global']}%\n"
            contexto += f"- Sesiones totales registradas de los estudiantes en conjunto: {resumen['total_sesiones']}\n\nDetalle de estudiantes:\n"
            
            for _, row in df.iterrows():
                contexto += f"Estudiante: {row['nombre']} | Asistencia: {row['porcentaje_asistencia']}% | Tardanzas: {row['tardanzas']} | Ausencias: {row['ausentes']}\n"
                
            pregunta = request.pregunta if request.pregunta else "Dame un resumen general de la asistencia"
            prompt = f"Eres un asistente de IA (AulaIA) para docentes. Responde la siguiente pregunta o instrucción basándote estrictamente en los siguientes datos de asistencia. Si la pregunta no se relaciona con los datos, indica amablemente que solo puedes responder sobre la asistencia de la clase.\n\nContexto:\n{contexto}\n\nPregunta/Instrucción: {pregunta}"
            
            chat_completion = client.chat.completions.create(
                messages=[
                    {"role": "system", "content": "Eres un asistente amable, claro y analítico para profesores."},
                    {"role": "user", "content": prompt}
                ],
                model="openai/gpt-oss-20b",  # Modelo inteligente actualizado
                temperature=0.3,
            )
            return chat_completion.choices[0].message.content
        except Exception as e:
            try:
                # Try to fetch available models to help debug
                models_info = client.models.list()
                available = [m.id for m in models_info.data]
                return f"Error con Groq (Modelo no encontrado). Modelos disponibles en tu cuenta: {', '.join(available)}"
            except Exception:
                return f"Hubo un error al comunicarse con Groq: {str(e)}"
    def _to_dataframe(self, estudiantes) -> pd.DataFrame:
        data = []
        for e in estudiantes:
            data.append({
                "estudiante_id": e.estudiante_id,
                "nombre": e.nombre,
                "presentes": e.presentes,
                "tardanzas": e.tardanzas,
                "ausentes": e.ausentes,
                "justificados": e.justificados,
                "total_sesiones": e.total_sesiones,
                "porcentaje_asistencia": e.porcentaje_asistencia
            })
        return pd.DataFrame(data)

    def _calcular_resumen_general(self, df: pd.DataFrame) -> Dict[str, Any]:
        total_estudiantes = len(df)
        total_sesiones = df["total_sesiones"].sum()
        total_presentes = df["presentes"].sum()
        total_tardanzas = df["tardanzas"].sum()
        total_ausentes = df["ausentes"].sum()
        total_justificados = df["justificados"].sum()

        asistieron = total_presentes + total_tardanzas + total_justificados
        porcentaje_global = round((asistieron / total_sesiones * 100), 2) if total_sesiones > 0 else 0

        # Tendencia global (simplificada)
        tendencia_global = self._calcular_tendencia_global(df)

        return {
            "total_estudiantes": total_estudiantes,
            "total_sesiones": int(total_sesiones),
            "total_presentes": int(total_presentes),
            "total_tardanzas": int(total_tardanzas),
            "total_ausentes": int(total_ausentes),
            "total_justificados": int(total_justificados),
            "porcentaje_asistencia_global": porcentaje_global,
            "tendencia_global": tendencia_global.value,
            "periodo": "actual"
        }

    def _calcular_tendencia_global(self, df: pd.DataFrame) -> TendenciaAsistencia:
        # Simplificado: basado en promedio de asistencia
        promedio = df["porcentaje_asistencia"].mean()
        if promedio >= 90:
            return TendenciaAsistencia.ASCENDENTE
        elif promedio >= 75:
            return TendenciaAsistencia.ESTABLE
        else:
            return TendenciaAsistencia.DESCENDENTE

    def _calcular_insights_estudiantes(self, df: pd.DataFrame) -> List[InsightEstudiante]:
        insights = []
        for _, row in df.iterrows():
            # Determinar tendencia individual (simplificada)
            pct = row["porcentaje_asistencia"]
            if pct >= 90:
                tendencia = TendenciaAsistencia.ASCENDENTE
                nivel = NivelAtencion.ALTO
            elif pct >= 75:
                tendencia = TendenciaAsistencia.ESTABLE
                nivel = NivelAtencion.MEDIO
            else:
                tendencia = TendenciaAsistencia.DESCENDENTE
                nivel = NivelAtencion.BAJO

            observaciones = []
            if row["tardanzas"] > row["presentes"] * 0.2:
                observaciones.append("Alta proporción de tardanzas")
            if row["ausentes"] > row["total_sesiones"] * 0.3:
                observaciones.append("Ausentismo elevado")
            if row["justificados"] > 0:
                observaciones.append(f"Tiene {int(row['justificados'])} justificaciones")

            insights.append(InsightEstudiante(
                estudiante_id=int(row["estudiante_id"]),
                nombre=row["nombre"],
                porcentaje_asistencia=row["porcentaje_asistencia"],
                tendencia=tendencia,
                nivel_atencion=nivel,
                observaciones=observaciones
            ))

        return insights

    def _detectar_patrones(self, df: pd.DataFrame) -> List[PatronDetectado]:
        """Detección de patrones simples usando Pandas (Fase IA-2)"""
        patrones = []

        # Patrón 1: 3+ ausencias en últimas 4 sesiones (aprox)
        # Como no tenemos historial temporal, usamos ratio
        alto_ausentismo = df[df["ausentes"] / df["total_sesiones"].replace(0, np.nan) > 0.3]
        for _, row in alto_ausentismo.iterrows():
            patrones.append(PatronDetectado(
                tipo="ALTO_AUSENTISMO",
                descripcion=f"{row['nombre']} registra alto ausentismo ({int(row['ausentes'])}/{int(row['total_sesiones'])})",
                estudiantes_afectados=[int(row["estudiante_id"])],
                severidad="warning"
            ))

        # Patrón 2: Tardanzas recurrentes (>20% de asistencias)
        tardanzas_recurrentes = df[
            (df["tardanzas"] > 0) &
            (df["tardanzas"] / (df["presentes"] + df["tardanzas"]).replace(0, np.nan) > 0.2)
        ]
        for _, row in tardanzas_recurrentes.iterrows():
            patrones.append(PatronDetectado(
                tipo="TARDANZAS_RECURRENTES",
                descripcion=f"{row['nombre']} tiene tardanzas recurrentes ({int(row['tardanzas'])} tardanzas)",
                estudiantes_afectados=[int(row["estudiante_id"])],
                severidad="attention"
            ))

        # Patrón 3: Descenso de asistencia (estudiantes < 70%)
        bajo_rendimiento = df[df["porcentaje_asistencia"] < 70]
        for _, row in bajo_rendimiento.iterrows():
            patrones.append(PatronDetectado(
                tipo="DESCENSO_ASISTENCIA",
                descripcion=f"{row['nombre']} tiene asistencia por debajo del 70% ({row['porcentaje_asistencia']}%)",
                estudiantes_afectados=[int(row["estudiante_id"])],
                severidad="warning"
            ))

        # Patrón 4: Asistencia perfecta
        perfecta = df[
            (df["porcentaje_asistencia"] == 100) &
            (df["total_sesiones"] > 0)
        ]
        if len(perfecta) > 0:
            patrones.append(PatronDetectado(
                tipo="ASISTENCIA_PERFECTA",
                descripcion=f"{len(perfecta)} estudiante(s) con asistencia perfecta",
                estudiantes_afectados=perfecta["estudiante_id"].astype(int).tolist(),
                severidad="info"
            ))

        return patrones

    def _generar_recomendaciones(
        self,
        insights: List[InsightEstudiante],
        patrones: List[PatronDetectado]
    ) -> List[str]:
        recomendaciones = []

        # Basado en patrones detectados
        tipos_patrones = {p.tipo for p in patrones}

        if "ALTO_AUSENTISMO" in tipos_patrones:
            recomendaciones.append(
                "Revisar casos de alto ausentismo con docente/tutor para identificar causas"
            )

        if "TARDANZAS_RECURRENTES" in tipos_patrones:
            recomendaciones.append(
                "Evaluar horarios de entrada o transporte para estudiantes con tardanzas recurrentes"
            )

        if "DESCENSO_ASISTENCIA" in tipos_patrones:
            recomendaciones.append(
                "Programar seguimiento individual para estudiantes con asistencia < 70%"
            )

        if "ASISTENCIA_PERFECTA" in tipos_patrones:
            recomendaciones.append(
                "Reconocer estudiantes con asistencia perfecta como motivación grupal"
            )

        # Recomendación general si hay muchos estudiantes en riesgo
        en_riesgo = sum(1 for i in insights if i.nivel_atencion == NivelAtencion.BAJO)
        if en_riesgo > len(insights) * 0.3:
            recomendaciones.append(
                f"Atención: {en_riesgo} de {len(insights)} estudiantes requieren seguimiento prioritario"
            )

        if not recomendaciones:
            recomendaciones.append("La asistencia se mantiene en niveles aceptables. Continuar monitoreo regular.")

        return recomendaciones

    def _empty_response(self) -> AnalisisResponse:
        return AnalisisResponse(
            resumen_general={
                "total_estudiantes": 0,
                "total_sesiones": 0,
                "porcentaje_asistencia_global": 0,
                "tendencia_global": "ESTABLE"
            },
            insights_estudiantes=[],
            patrones_detectados=[],
            recomendaciones=["No hay datos suficientes para generar análisis"]
        )