import os
import json
from typing import Optional, List, Dict, Any
from fastapi import FastAPI, HTTPException, Header, Depends, Request
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field
from dotenv import load_dotenv

# Cargar variables de entorno si existen en un archivo .env
load_dotenv()

app = FastAPI(
    title="Servicio IA - Plataforma de Eventos",
    version="1.0.0",
    description="Servicio inteligente para interpretación de consultas por voz/texto y análisis de reportes con Google Gemini.",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Modelos Pydantic para Requests y Responses
class InterpretarConsultaSolicitud(BaseModel):
    consulta: str = Field(default="", description="Texto de la consulta ingresada por el usuario (o transcrita por voz)")
    contexto_adicional: Optional[Dict[str, Any]] = Field(default=None, description="Filtros preexistentes o contexto actual")

class ParametrosFiltro(BaseModel):
    rango_dias: Optional[int] = Field(default=None, description="Dias hacia atras (ej. 7, 30, 90, 365) o null para todos")
    estado_evento: Optional[str] = Field(default=None, description="Filtro opcional: PUBLICADO, FINALIZADO, BORRADOR, EN_CURSO, CANCELADO")
    categoria: Optional[str] = Field(default=None, description="Filtro o nombre de categoria detectada")
    tipo_metrica: str = Field(default="GENERAL", description="Metrica principal: GENERAL, EVENTOS, ASISTENCIA, INSCRIPCIONES, OCUPACION")
    tipo_grafico_recomendado: str = Field(default="BAR_CHART", description="BAR_CHART, LINE_CHART, PIE_CHART, o KPI_CARDS")

class InterpretarConsultaRespuesta(BaseModel):
    intencion: str
    parametros: ParametrosFiltro
    mensaje_asistente: str

class GenerarInsightsSolicitud(BaseModel):
    consulta_original: str
    datos_reporte: Dict[str, Any]

class GenerarInsightsRespuesta(BaseModel):
    resumen_ejecutivo: str
    puntos_clave: List[str]
    recomendaciones: List[str]

class ParticipantePerfilIA(BaseModel):
    id_participante: int
    nombre: str
    intereses: Optional[List[str]] = Field(default_factory=list)
    historial_eventos: Optional[List[str]] = Field(default_factory=list)

class EventoCandidatoIA(BaseModel):
    id_evento: int
    nombre: str
    descripcion: Optional[str] = ""
    categoria: Optional[str] = ""
    fecha_inicio: Optional[str] = ""
    aforo_disponible: Optional[int] = None

class RecomendarEventosSolicitud(BaseModel):
    participante: ParticipantePerfilIA
    eventos: List[EventoCandidatoIA]

class RecomendacionItem(BaseModel):
    id_evento: int
    score: float
    motivo: str

class RecomendarEventosRespuesta(BaseModel):
    recomendaciones: List[RecomendacionItem]

def obtener_gemini_client():
    api_key = os.getenv("GEMINI_API_KEY")
    if not api_key:
        return None
    try:
        from google import genai
        return genai.Client(api_key=api_key)
    except Exception as e:
        print(f"Error al inicializar Google GenAI Client: {e}")
        return None

@app.get("/api/salud", tags=["Operacion"])
def obtener_salud() -> dict[str, str]:
    has_gemini = bool(os.getenv("GEMINI_API_KEY"))
    return {
        "estado": "ok",
        "servicio": "servicio-ia",
        "gemini_configurado": str(has_gemini)
    }

@app.post("/api/ia/reportes/interpretar", response_model=InterpretarConsultaRespuesta, tags=["Reportes IA"])
async def interpretar_consulta(request: Request):
    try:
        body = await request.json()
    except Exception:
        body = {}
    
    consulta = body.get("consulta", "") or ""
    contexto_adicional = body.get("contexto_adicional") or {}
    solicitud = InterpretarConsultaSolicitud(consulta=consulta, contexto_adicional=contexto_adicional)

    client = obtener_gemini_client()
    
    # Si no hay API key configurada o falla la conexión, respondemos con análisis heurístico de respaldo
    if not client:
        return interpretar_con_heuristica(solicitud.consulta)

    prompt = f"""
    Eres un asistente analítico experto en gestión de eventos para una plataforma de software.
    El administrador de una organización ha dictado o escrito la siguiente consulta sobre sus métricas y reportes:
    "{solicitud.consulta}"

    Analiza la intención del usuario y clasifica la consulta para generar los parámetros de filtrado del reporte.
    Debes responder ÚNICAMENTE un objeto JSON válido con la siguiente estructura exacta:
    {{
      "intencion": "Breve descripción de lo que busca el usuario",
      "parametros": {{
        "rango_dias": 30, // número entero (ej. 7, 30, 90, 365) o null si no especifica período
        "estado_evento": "PUBLICADO", // uno de: "PUBLICADO", "FINALIZADO", "BORRADOR", "EN_CURSO", "CANCELADO" o null
        "categoria": null, // nombre o palabra clave de categoría si se menciona, o null
        "tipo_metrica": "EVENTOS", // uno de: "GENERAL", "EVENTOS", "ASISTENCIA", "INSCRIPCIONES", "OCUPACION"
        "tipo_grafico_recomendado": "BAR_CHART" // uno de: "BAR_CHART", "LINE_CHART", "PIE_CHART", "KPI_CARDS"
      }},
      "mensaje_asistente": "Respuesta breve y cordial confirmando lo que se va a mostrar (máximo 2 oraciones en español)"
    }}
    """

    try:
        from google.genai import types
        response = client.models.generate_content(
            model='gemini-3.5-flash-lite',
            contents=prompt,
            config=types.GenerateContentConfig(
                response_mime_type="application/json"
            )
        )
        data = json.loads(response.text)
        return InterpretarConsultaRespuesta(**data)
    except Exception as e:
        print(f"Fallo al llamar a Gemini, usando respaldo: {e}")
        return interpretar_con_heuristica(solicitud.consulta)

@app.post("/api/ia/reportes/insights", response_model=GenerarInsightsRespuesta, tags=["Reportes IA"])
async def generar_insights(request: Request):
    try:
        body = await request.json()
    except Exception:
        body = {}
    
    consulta_original = body.get("consulta_original", "") or body.get("consulta", "Análisis general")
    datos_reporte = body.get("datos_reporte") or body
    solicitud = GenerarInsightsSolicitud(consulta_original=consulta_original, datos_reporte=datos_reporte)

    client = obtener_gemini_client()
    
    if not client:
        # Fallback si no hay API key de Gemini
        return GenerarInsightsRespuesta(
            resumen_ejecutivo="Resumen basado en las métricas actuales de la organización.",
            puntos_clave=[
                f"Total de eventos analizados: {solicitud.datos_reporte.get('totalEventos', 0)}",
                f"Total de inscripciones registradas: {solicitud.datos_reporte.get('totalInscripciones', 0)}",
                f"Porcentaje de asistencia promedio: {solicitud.datos_reporte.get('tasaAsistencia', '0%')}"
            ],
            recomendaciones=[
                "Promover eventos con menor porcentaje de aforo completado.",
                "Mantener la comunicación con los participantes inscritos previo al evento."
            ]
        )

    prompt = f"""
    Eres un consultor ejecutivo de analítica de eventos para administradores de organizaciones.
    El usuario solicitó: "{solicitud.consulta_original}".
    
    Los datos consolidados del sistema son los siguientes en formato JSON:
    {json.dumps(solicitud.datos_reporte, ensure_ascii=False)}

    Genera un informe analítico conciso y de alto valor para la directiva.
    Responde ÚNICAMENTE un objeto JSON válido con esta estructura:
    {{
      "resumen_ejecutivo": "Párrafo de 2 a 3 líneas resumiendo el estado general y los hallazgos más destacados en español.",
      "puntos_clave": [
        "Insight 1 destacando números o tendencias relevantes",
        "Insight 2 sobre asistencia o estados de eventos",
        "Insight 3 relevante"
      ],
      "recomendaciones": [
        "Recomendación práctica 1 para maximizar asistencia o participación",
        "Recomendación práctica 2 para la organización"
      ]
    }}
    """

    try:
        from google.genai import types
        response = client.models.generate_content(
            model='gemini-3.5-flash-lite',
            contents=prompt,
            config=types.GenerateContentConfig(
                response_mime_type="application/json"
            )
        )
        data = json.loads(response.text)
        return GenerarInsightsRespuesta(**data)
    except Exception as e:
        print(f"Fallo al generar insights con Gemini: {e}")
        return GenerarInsightsRespuesta(
            resumen_ejecutivo="Resumen automático de las métricas obtenidas para su organización.",
            puntos_clave=["Métricas procesadas correctamente desde la base de datos."],
            recomendaciones=["Revisar el aforo y la programación de las próximas fechas."]
        )

def interpretar_con_heuristica(texto: str) -> InterpretarConsultaRespuesta:
    lower = texto.lower()
    rango = None
    if "semana" in lower or "últimos 7 días" in lower or "ultimos 7 dias" in lower:
        rango = 7
    elif "mes" in lower or "últimos 30 días" in lower or "ultimos 30 dias" in lower:
        rango = 30
    elif "trimestre" in lower or "90 días" in lower:
        rango = 90
    elif "año" in lower or "este año" in lower:
        rango = 365

    estado = None
    if "finalizado" in lower or "terminado" in lower:
        estado = "FINALIZADO"
    elif "publicado" in lower or "activo" in lower:
        estado = "PUBLICADO"
    elif "borrador" in lower:
        estado = "BORRADOR"

    metrica = "GENERAL"
    grafico = "BAR_CHART"
    if "asistencia" in lower or "asistente" in lower:
        metrica = "ASISTENCIA"
        grafico = "LINE_CHART"
    elif "inscripci" in lower or "inscrito" in lower or "registrado" in lower:
        metrica = "INSCRIPCIONES"
        grafico = "BAR_CHART"
    elif "ocupaci" in lower or "espacio" in lower or "aforo" in lower:
        metrica = "OCUPACION"
        grafico = "PIE_CHART"
    elif "evento" in lower:
        metrica = "EVENTOS"
        grafico = "BAR_CHART"

    return InterpretarConsultaRespuesta(
        intencion="Consulta de métricas filtrada por parámetros de texto",
        parametros=ParametrosFiltro(
            rango_dias=rango,
            estado_evento=estado,
            tipo_metrica=metrica,
            tipo_grafico_recomendado=grafico
        ),
        mensaje_asistente=f"He configurado tu reporte con métrica de {metrica.lower()} para los datos correspondientes."
    )

@app.post("/api/ia/eventos/recomendar", response_model=RecomendarEventosRespuesta, tags=["Recomendaciones IA"])
async def recomendar_eventos(solicitud: RecomendarEventosSolicitud):
    client = obtener_gemini_client()
    
    if not client or not solicitud.eventos:
        return recomendar_con_heuristica(solicitud)

    prompt = f"""
    Eres un asistente de recomendación inteligente de eventos para una plataforma universitaria y profesional.
    
    Perfil del Participante:
    - Nombre: {solicitud.participante.nombre}
    - Intereses declarados: {", ".join(solicitud.participante.intereses) if solicitud.participante.intereses else "General"}
    - Eventos previos asistidos: {", ".join(solicitud.participante.historial_eventos) if solicitud.participante.historial_eventos else "Ninguno aún"}

    Eventos disponibles a evaluar:
    {json.dumps([e.model_dump() for e in solicitud.eventos], ensure_ascii=False)}

    Evalúa la afinidad de cada evento con el participante y ordénalos del más recomendado al menos recomendado.
    Asigna una puntuación (score) de 0 a 100 y una breve frase en español explicando el motivo de la sugerencia (máximo 1 oración concisa).
    
    Responde ÚNICAMENTE un objeto JSON válido con esta estructura:
    {{
      "recomendaciones": [
        {{
          "id_evento": 1,
          "score": 95.0,
          "motivo": "Recomendado por su relación con tu interés en tecnología y datos."
        }}
      ]
    }}
    """

    try:
        from google.genai import types
        response = client.models.generate_content(
            model='gemini-3.5-flash-lite',
            contents=prompt,
            config=types.GenerateContentConfig(
                response_mime_type="application/json"
            )
        )
        data = json.loads(response.text)
        return RecomendarEventosRespuesta(**data)
    except Exception as e:
        print(f"Fallo al invocar Gemini para recomendaciones: {e}")
        return recomendar_con_heuristica(solicitud)

def recomendar_con_heuristica(solicitud: RecomendarEventosSolicitud) -> RecomendarEventosRespuesta:
    items = []
    base_score = 95.0
    for i, ev in enumerate(solicitud.eventos):
        motivo = "Evento destacado de la plataforma con alta relevancia institucional."
        if ev.categoria:
            motivo = f"Recomendado especialmente por tu interés en temáticas de {ev.categoria}."
        if solicitud.participante.historial_eventos:
            motivo += " Complementa tus participaciones anteriores."
            
        score = max(50.0, base_score - (i * 8.0))
        items.append(RecomendacionItem(
            id_evento=ev.id_evento,
            score=score,
            motivo=motivo
        ))
    return RecomendarEventosRespuesta(recomendaciones=items)

