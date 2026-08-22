import pytest
from fastapi.testclient import TestClient
from app.main import app

client = TestClient(app)


class TestHealth:
    def test_health_endpoint(self):
        response = client.get("/health")
        assert response.status_code == 200
        data = response.json()
        assert data["status"] == "ok"
        assert data["service"] == "aulaia-fastapi"

    def test_root_endpoint(self):
        response = client.get("/")
        assert response.status_code == 200
        data = response.json()
        assert data["service"] == "AulaIA FastAPI"
        assert "docs" in data
        assert "health" in data

    def test_analysis_health(self):
        response = client.get("/api/v1/analisis/health")
        assert response.status_code == 200
        data = response.json()
        assert data["status"] == "ok"
        assert data["module"] == "analysis"