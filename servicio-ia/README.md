# Servicio IA

Servicio FastAPI reservado para funciones de inteligencia artificial de Sprint 4.

En Sprint 0 expone solamente `GET /api/salud`.

## Ejecucion local

```powershell
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
uvicorn main:app --reload --port 8000
```

La documentacion OpenAPI estara disponible en `http://localhost:8000/docs`.
