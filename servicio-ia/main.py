from fastapi import FastAPI

app = FastAPI(
    title="Servicio IA - Plataforma de Eventos",
    version="0.1.0",
    description="Servicio preparado para integraciones de Sprint futuros.",
)


@app.get("/api/salud", tags=["Operacion"])
def obtener_salud() -> dict[str, str]:
    return {"estado": "ok", "servicio": "servicio-ia"}
