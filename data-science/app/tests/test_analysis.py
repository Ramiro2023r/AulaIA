import pytest
from fastapi.testclient import TestClient
from app.main import app

client = TestClient(app)


class TestAnalisisAsistencia:
    def test_analisis_vacio(self):
        """Test con lista vacía de estudiantes"""
        response = client.post(
            "/api/v1/analisis/asistencia",
            json={"estudiantes": []}
        )
        assert response.status_code == 200
        data = response.json()
        assert data["resumen_general"]["total_estudiantes"] == 0
        assert len(data["insights_estudiantes"]) == 0

    def test_analisis_estudiante_unico_presente(self):
        """Test con un estudiante con asistencia perfecta"""
        request = {
            "estudiantes": [{
                "estudiante_id": 1,
                "nombre": "Juan Pérez",
                "presentes": 10,
                "tardanzas": 0,
                "ausentes": 0,
                "justificados": 0,
                "total_sesiones": 10
            }]
        }
        response = client.post("/api/v1/analisis/asistencia", json=request)
        assert response.status_code == 200
        data = response.json()

        assert data["resumen_general"]["total_estudiantes"] == 1
        assert data["resumen_general"]["porcentaje_asistencia_global"] == 100.0

        insight = data["insights_estudiantes"][0]
        assert insight["estudiante_id"] == 1
        assert insight["porcentaje_asistencia"] == 100.0
        assert insight["tendencia"] == "ASCENDENTE"
        assert insight["nivel_atencion"] == "ALTO"

        # Debe detectar asistencia perfecta
        patrones = [p["tipo"] for p in data["patrones_detectados"]]
        assert "ASISTENCIA_PERFECTA" in patrones

    def test_analisis_estudiante_con_tardanzas(self):
        """Test estudiante con tardanzas recurrentes"""
        request = {
            "estudiantes": [{
                "estudiante_id": 2,
                "nombre": "María López",
                "presentes": 5,
                "tardanzas": 5,
                "ausentes": 0,
                "justificados": 0,
                "total_sesiones": 10
            }]
        }
        response = client.post("/api/v1/analisis/asistencia", json=request)
        assert response.status_code == 200
        data = response.json()

        insight = data["insights_estudiantes"][0]
        assert insight["porcentaje_asistencia"] == 100.0  # presentes + tardanzas = 100%
        assert "Alta proporción de tardanzas" in insight["observaciones"]

        # Debe detectar tardanzas recurrentes
        patrones = [p["tipo"] for p in data["patrones_detectados"]]
        assert "TARDANZAS_RECURRENTES" in patrones

    def test_analisis_estudiante_alto_ausentismo(self):
        """Test estudiante con alto ausentismo"""
        request = {
            "estudiantes": [{
                "estudiante_id": 3,
                "nombre": "Carlos Ruiz",
                "presentes": 3,
                "tardanzas": 1,
                "ausentes": 6,
                "justificados": 0,
                "total_sesiones": 10
            }]
        }
        response = client.post("/api/v1/analisis/asistencia", json=request)
        assert response.status_code == 200
        data = response.json()

        insight = data["insights_estudiantes"][0]
        assert insight["porcentaje_asistencia"] == 40.0  # (3+1)/10 * 100
        assert insight["nivel_atencion"] == "BAJO"
        assert insight["tendencia"] == "DESCENDENTE"
        assert "Ausentismo elevado" in insight["observaciones"]

        patrones = [p["tipo"] for p in data["patrones_detectados"]]
        assert "ALTO_AUSENTISMO" in patrones
        assert "DESCENSO_ASISTENCIA" in patrones

    def test_analisis_multiples_estudiantes(self):
        """Test con múltiples estudiantes"""
        request = {
            "estudiantes": [
                {
                    "estudiante_id": 1,
                    "nombre": "Ana García",
                    "presentes": 9,
                    "tardanzas": 1,
                    "ausentes": 0,
                    "justificados": 0,
                    "total_sesiones": 10
                },
                {
                    "estudiante_id": 2,
                    "nombre": "Luis Torres",
                    "presentes": 4,
                    "tardanzas": 1,
                    "ausentes": 5,
                    "justificados": 0,
                    "total_sesiones": 10
                },
                {
                    "estudiante_id": 3,
                    "nombre": "Sofía Díaz",
                    "presentes": 10,
                    "tardanzas": 0,
                    "ausentes": 0,
                    "justificados": 0,
                    "total_sesiones": 10
                }
            ]
        }
        response = client.post("/api/v1/analisis/asistencia", json=request)
        assert response.status_code == 200
        data = response.json()

        assert data["resumen_general"]["total_estudiantes"] == 3
        assert len(data["insights_estudiantes"]) == 3

        # Verificar resumen global
        assert data["resumen_general"]["total_presentes"] == 23
        assert data["resumen_general"]["total_tardanzas"] == 2
        assert data["resumen_general"]["total_ausentes"] == 5

        # Debe haber recomendaciones
        assert len(data["recomendaciones"]) > 0

    def test_analisis_con_justificados(self):
        """Test con justificaciones"""
        request = {
            "estudiantes": [{
                "estudiante_id": 4,
                "nombre": "Pedro Martín",
                "presentes": 7,
                "tardanzas": 1,
                "ausentes": 1,
                "justificados": 1,
                "total_sesiones": 10
            }]
        }
        response = client.post("/api/v1/analisis/asistencia", json=request)
        assert response.status_code == 200
        data = response.json()

        insight = data["insights_estudiantes"][0]
        # (7+1+1)/10 = 90%
        assert insight["porcentaje_asistencia"] == 90.0
        assert "Tiene 1 justificaciones" in insight["observaciones"]

    def test_analisis_sesiones_cero(self):
        """Test edge case: total_sesiones = 0"""
        request = {
            "estudiantes": [{
                "estudiante_id": 5,
                "nombre": "Nuevo Estudiante",
                "presentes": 0,
                "tardanzas": 0,
                "ausentes": 0,
                "justificados": 0,
                "total_sesiones": 0
            }]
        }
        response = client.post("/api/v1/analisis/asistencia", json=request)
        assert response.status_code == 200
        data = response.json()

        insight = data["insights_estudiantes"][0]
        assert insight["porcentaje_asistencia"] == 0.0

    def test_respuesta_estructura_completa(self):
        """Verificar que la respuesta tiene todos los campos esperados"""
        request = {
            "estudiantes": [{
                "estudiante_id": 1,
                "nombre": "Test",
                "presentes": 5,
                "tardanzas": 0,
                "ausentes": 0,
                "justificados": 0,
                "total_sesiones": 5
            }]
        }
        response = client.post("/api/v1/analisis/asistencia", json=request)
        assert response.status_code == 200
        data = response.json()

        # Campos requeridos en respuesta
        assert "resumen_general" in data
        assert "insights_estudiantes" in data
        assert "patrones_detectados" in data
        assert "recomendaciones" in data

        # Resumen general tiene campos esperados
        resumen = data["resumen_general"]
        assert "total_estudiantes" in resumen
        assert "total_sesiones" in resumen
        assert "porcentaje_asistencia_global" in resumen
        assert "tendencia_global" in resumen

        # Insights tienen campos esperados
        insight = data["insights_estudiantes"][0]
        assert "estudiante_id" in insight
        assert "nombre" in insight
        assert "porcentaje_asistencia" in insight
        assert "tendencia" in insight
        assert "nivel_atencion" in insight
        assert "observaciones" in insight