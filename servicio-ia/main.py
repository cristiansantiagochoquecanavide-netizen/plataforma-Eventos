import os
import json
import urllib.request
import urllib.error
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
    description="Servicio inteligente para interpretación de consultas por voz/texto y análisis de reportes con OpenAI / Google Gemini.",
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

class KpiReporte(BaseModel):
    etiqueta: str
    valor: str
    detalle: Optional[str] = ""

class GraficoItem(BaseModel):
    etiqueta: str
    valor: float
    extra: Optional[str] = ""

class ReporteEjecutivoRespuesta(BaseModel):
    titulo: str
    subtitulo: str
    tipo_reporte: str = "GENERAL"
    periodo: str = "Datos vigentes"
    resumen_ejecutivo: str
    kpis: List[KpiReporte] = Field(default_factory=list)
    columnas: List[str] = Field(default_factory=list)
    filas: List[List[str]] = Field(default_factory=list)
    conclusiones: List[str] = Field(default_factory=list)
    recomendaciones: List[str] = Field(default_factory=list)
    grafico_titulo: Optional[str] = ""
    grafico_datos: List[GraficoItem] = Field(default_factory=list)

def obtener_openai_config() -> tuple[Optional[str], str]:
    api_key = (os.getenv("REPORTES_OPENAI_API_KEY") or os.getenv("OPENAI_API_KEY") or "").strip()
    model = (os.getenv("REPORTES_OPENAI_MODEL") or "gpt-4o-mini").strip()
    if model in ("", "gpt-6-luna"):
        model = "gpt-4o-mini"
    return (api_key or None), model


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
    api_key = (os.getenv("GEMINI_API_KEY") or "").strip()
    if not api_key:
        return None
    try:
        from google import genai
        return genai.Client(api_key=api_key)
    except Exception as e:
        print(f"Error al inicializar Google GenAI Client: {e}")
        return None


def generar_json_con_ia(prompt: str, modelo_gemini: str = "gemini-2.5-flash") -> Optional[Dict[str, Any]]:
    """Genera una respuesta JSON estructurada usando OpenAI si está configurado, o Gemini como respaldo."""
    openai_key, openai_model = obtener_openai_config()
    if openai_key:
        payload = json.dumps({
            "model": openai_model,
            "messages": [
                {"role": "system", "content": "Responde únicamente con un objeto JSON válido, sin markdown ni texto adicional."},
                {"role": "user", "content": prompt},
            ],
            "response_format": {"type": "json_object"},
            "temperature": 0.2,
        }).encode("utf-8")
        req = urllib.request.Request(
            "https://api.openai.com/v1/chat/completions",
            data=payload,
            headers={
                "Authorization": f"Bearer {openai_key}",
                "Content-Type": "application/json",
            },
            method="POST",
        )
        try:
            with urllib.request.urlopen(req, timeout=30) as resp:
                body = json.loads(resp.read().decode("utf-8"))
                content = body["choices"][0]["message"]["content"]
                return json.loads(content)
        except Exception as e:
            print(f"Fallo al llamar a OpenAI ({openai_model}), intentando fallback: {e}")

    client = obtener_gemini_client()
    if client:
        try:
            from google.genai import types
            response = client.models.generate_content(
                model=modelo_gemini,
                contents=prompt,
                config=types.GenerateContentConfig(response_mime_type="application/json"),
            )
            return json.loads(response.text)
        except Exception as e:
            print(f"Fallo al llamar a Gemini ({modelo_gemini}): {e}")

    return None


@app.get("/api/salud", tags=["Operacion"])
def obtener_salud() -> dict[str, str]:
    openai_key, openai_model = obtener_openai_config()
    has_gemini = bool((os.getenv("GEMINI_API_KEY") or "").strip())
    return {
        "estado": "ok",
        "servicio": "servicio-ia",
        "openai_configurado": str(bool(openai_key)),
        "openai_modelo": openai_model if openai_key else "",
        "gemini_configurado": str(has_gemini),
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

    data = generar_json_con_ia(prompt, modelo_gemini="gemini-3.5-flash-lite")
    if data:
        try:
            return InterpretarConsultaRespuesta(**data)
        except Exception as e:
            print(f"Fallo al validar respuesta IA en interpretar: {e}")

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

    data = generar_json_con_ia(prompt, modelo_gemini="gemini-3.5-flash-lite")
    if data:
        try:
            return GenerarInsightsRespuesta(**data)
        except Exception as e:
            print(f"Fallo al validar respuesta IA en insights: {e}")

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


@app.post("/api/ia/reportes/generar", response_model=ReporteEjecutivoRespuesta, tags=["Reportes IA"])
async def generar_reporte_ejecutivo(request: Request):
    try:
        body = await request.json()
    except Exception:
        body = {}

    consulta = (body.get("consulta") or "").strip()
    datos_org = body.get("datos_organizacion") or {}
    if not consulta:
        consulta = "Reporte ejecutivo general de la organización"

    prompt = f"""
    Eres un director de inteligencia de negocios y analítica de una plataforma de gestión de eventos.
    El usuario ha solicitado por voz o texto el siguiente reporte:
    "{consulta}"

    Datos reales disponibles de la organización en JSON:
    {json.dumps(datos_org, ensure_ascii=False)}

    REGLAS OBLIGATORIAS:
    1. NUNCA respondas con saludos ni respuestas cortas conversacionales. SIEMPRE genera un INFORME EJECUTIVO FORMAL estructurado.
    2. Responde exactamente a lo que pide el usuario usando ÚNICAMENTE los datos reales del JSON provisto (finanzas, eventos, participantes, convocatorias, recursos, espacios, resumen). No inventes registros ni cifras.
    3. Si el usuario pregunta sobre cuánto gana, cuánto pierde, finanzas, costos, ingresos o rentabilidad, analiza rigurosamente el bloque "finanzas" (ingresosTotales, costosTotales, balanceNetoGlobal, roiGlobal, detalleEventos).
    4. Filtra, ordena o agrupa los registros según lo que el usuario haya pedido.
    5. Todas las celdas dentro de "filas" deben ser strings claros y formateados (sin null), y cada fila debe tener exactamente la misma cantidad de elementos que "columnas".
    6. Incluye al menos 3 KPIs numéricos concretos, 3 conclusiones basadas en los datos y 2 recomendaciones accionables.
    7. Si corresponde, llena "grafico_datos" con hasta 8 elementos numéricos representativos para graficar barras.

    Devuelve ÚNICAMENTE un objeto JSON válido con esta estructura exacta:
    {{
      "titulo": "Título formal del informe en español",
      "subtitulo": "Subtítulo descriptivo del alcance del informe",
      "tipo_reporte": "FINANZAS | EVENTOS | PARTICIPANTES | CONVOCATORIAS | RECURSOS | ESPACIOS | OCUPACION | GENERAL",
      "periodo": "Período o alcance analizado",
      "resumen_ejecutivo": "Diagnóstico formal de 2 a 4 oraciones resumiendo hallazgos cuantitativos y cualitativos.",
      "kpis": [
        {{"etiqueta": "Nombre KPI", "valor": "12", "detalle": "Breve contexto"}}
      ],
      "columnas": ["Columna 1", "Columna 2", "Columna 3"],
      "filas": [
        ["Valor 1", "Valor 2", "Valor 3"]
      ],
      "conclusiones": ["Hallazgo concreto 1", "Hallazgo concreto 2", "Hallazgo concreto 3"],
      "recomendaciones": ["Acción recomendada 1", "Acción recomendada 2"],
      "grafico_titulo": "Título del gráfico comparativo",
      "grafico_datos": [
        {{"etiqueta": "Item", "valor": 10, "extra": "10 unidades"}}
      ]
    }}
    """

    data = generar_json_con_ia(prompt, modelo_gemini="gemini-2.5-flash")
    if data:
        try:
            return ReporteEjecutivoRespuesta(**data)
        except Exception as e:
            print(f"Fallo al validar reporte ejecutivo IA, usando motor analítico local: {e}")

    return generar_reporte_heuristico(consulta, datos_org)


def _normalizar_filas(filas: List[List[Any]], num_cols: int) -> List[List[str]]:
    res = []
    for f in filas:
        row = [str(v) if v is not None else "-" for v in f[:num_cols]]
        while len(row) < num_cols:
            row.append("-")
        res.append(row)
    return res


def generar_reporte_heuristico(consulta: str, datos_org: Dict[str, Any]) -> ReporteEjecutivoRespuesta:
    q = consulta.lower()
    org_nombre = datos_org.get("organizacion", "Organización")
    eventos = datos_org.get("eventos") or []
    participantes = datos_org.get("participantes") or []
    convocatorias = datos_org.get("convocatorias") or []
    recursos = datos_org.get("recursos") or []
    espacios = datos_org.get("espacios") or []
    resumen = datos_org.get("resumen") or {}

    periodo = "Histórico completo"
    if "semana" in q or "7 días" in q or "7 dias" in q:
        periodo = "Últimos 7 días"
    elif "mes" in q or "30 días" in q or "30 dias" in q:
        periodo = "Últimos 30 días"
    elif "año" in q or "anual" in q:
        periodo = "Último año"

    # 0. Finanzas / Rentabilidad / Ingresos / Ganancias / Pérdidas / Costos
    if any(k in q for k in ["gana", "ganó", "ganancia", "pierde", "perdió", "pérdida", "perdida", "financier", "balance", "ingreso", "costo", "rentabil", "utilidad", "presupuesto", "superavit", "superávit", "deficit", "déficit"]):
        finanzas = datos_org.get("finanzas") or {}
        detalles_fin = finanzas.get("detalleEventos") or []
        ing_total = float(finanzas.get("ingresosTotales") or 0.0)
        cost_total = float(finanzas.get("costosTotales") or 0.0)
        bal_global = float(finanzas.get("balanceNetoGlobal") or (ing_total - cost_total))
        roi_global = float(finanzas.get("roiGlobal") or 0.0)

        filas = []
        graf_datos = []
        for d in detalles_fin:
            filas.append([
                str(d.get("nombre") or "-"),
                f"{int(d.get('inscripcionesConfirmadas') or 0):,}",
                f"{float(d.get('ingresos') or 0.0):,.2f} Bs",
                f"{float(d.get('costoEspacio') or 0.0):,.2f} Bs",
                f"{float(d.get('costoRecursos') or 0.0):,.2f} Bs",
                f"{float(d.get('costosTotales') or 0.0):,.2f} Bs",
                f"{float(d.get('balanceNeto') or 0.0):,.2f} Bs",
                f"{float(d.get('roi') or 0.0):.1f}%",
                str(d.get("situacion") or "EQUILIBRIO")
            ])
            graf_datos.append(GraficoItem(
                etiqueta=str(d.get("nombre") or "Evento"),
                valor=round(float(d.get("balanceNeto") or 0.0), 2),
                extra=f"{float(d.get('balanceNeto') or 0.0):,.2f} Bs neto"
            ))

        conclusiones = [
            f"El balance neto consolidado de la organización es de {bal_global:,.2f} Bs con ingresos de {ing_total:,.2f} Bs y costos operativos de {cost_total:,.2f} Bs.",
            f"La rentabilidad global (ROI) se sitúa en {roi_global:.1f}%.",
            f"Se registraron {finanzas.get('eventosConGanancia', 0)} eventos con superávit y {finanzas.get('eventosConPerdida', 0)} eventos con déficit financiero."
        ]
        recom = [
            "Para los eventos con déficit, ajustar el precio de los boletos o gestionar auspiciadores para amortizar el alquiler del espacio.",
            "Potenciar las categorías de entradas diferenciadas (VIP/Super VIP) que presentan mayor margen de utilidad."
        ]
        sit_global = "Superávit financiero positivo" if bal_global >= 0 else "Déficit financiero operativo"

        return ReporteEjecutivoRespuesta(
            titulo=f"Informe de Balance Financiero y Rentabilidad: {org_nombre}",
            subtitulo=f"Análisis económico de ingresos por entradas vs. costos operativos de espacios y recursos",
            tipo_reporte="FINANZAS",
            periodo=periodo,
            resumen_ejecutivo=f"La organización presenta un {sit_global} con un resultado neto de {bal_global:,.2f} Bs. Se recaudaron {ing_total:,.2f} Bs en venta de entradas frente a {cost_total:,.2f} Bs en gastos de espacios y equipamiento.",
            kpis=[
                KpiReporte(etiqueta="Ingresos Totales", valor=f"{ing_total:,.2f} Bs", detalle="Venta de tickets confirmados"),
                KpiReporte(etiqueta="Costos Operativos", valor=f"{cost_total:,.2f} Bs", detalle="Alquiler de salones y recursos"),
                KpiReporte(etiqueta="Resultado Neto", valor=f"{bal_global:,.2f} Bs", detalle="Utilidad o déficit global"),
                KpiReporte(etiqueta="Rentabilidad (ROI)", valor=f"{roi_global:.1f}%", detalle="Rendimiento del gasto"),
            ],
            columnas=["Evento", "Boletos", "Ingresos", "Costo Espacio", "Costo Recursos", "Costos Totales", "Balance Neto", "ROI", "Situación"],
            filas=filas,
            conclusiones=conclusiones,
            recomendaciones=recom,
            grafico_titulo="Balance Neto por Evento (Bs)",
            grafico_datos=graf_datos[:8]
        )

    # 1. Recursos / Materiales / Equipos
    if any(k in q for k in ["recurso", "material", "equipo", "inventario", "proyector", "parlante", "laptop", "prestamo", "préstamo"]):
        filtrados = list(recursos)
        if "disponible" in q:
            filtrados = [r for r in filtrados if str(r.get("estado", "")).upper() == "DISPONIBLE"]
        elif "mantenimiento" in q:
            filtrados = [r for r in filtrados if str(r.get("estado", "")).upper() == "MANTENIMIENTO"]
        if "mayor" in q or "mas" in q or "más" in q:
            filtrados.sort(key=lambda r: int(r.get("cantidadDisponible") or 0), reverse=True)
        elif "menor" in q or "poco" in q or "bajo" in q:
            filtrados.sort(key=lambda r: int(r.get("cantidadDisponible") or 0))

        total_u = sum(int(r.get("cantidadTotal") or 0) for r in filtrados)
        disp_u = sum(int(r.get("cantidadDisponible") or 0) for r in filtrados)
        uso_u = max(0, total_u - disp_u)
        pct_disp = round((disp_u / total_u) * 100, 1) if total_u > 0 else 0.0

        cols = ["Código", "Recurso", "Tipo", "Total", "Disponible", "En Uso", "Estado", "Ubicación"]
        filas = _normalizar_filas([
            [
                r.get("codigo", "-"),
                r.get("nombre", "-"),
                r.get("tipo", "-"),
                r.get("cantidadTotal", 0),
                r.get("cantidadDisponible", 0),
                max(0, int(r.get("cantidadTotal") or 0) - int(r.get("cantidadDisponible") or 0)),
                r.get("estado", "-"),
                r.get("ubicacion", "-") or "-",
            ]
            for r in filtrados
        ], len(cols))

        graf = [
            GraficoItem(etiqueta=str(r.get("nombre", "Recurso")), valor=float(r.get("cantidadDisponible") or 0), extra=f"{r.get('cantidadDisponible', 0)}/{r.get('cantidadTotal', 0)}")
            for r in filtrados[:8]
        ]

        return ReporteEjecutivoRespuesta(
            titulo="Informe de Inventario y Disponibilidad de Recursos",
            subtitulo=f"Solicitud: \"{consulta}\" · {org_nombre}",
            tipo_reporte="RECURSOS",
            periodo=periodo,
            resumen_ejecutivo=(
                f"Se analizaron {len(filtrados)} ítems de inventario pertenecientes a {org_nombre}, "
                f"totalizando {total_u} unidades registradas de las cuales {disp_u} ({pct_disp}%) se encuentran disponibles "
                f"y {uso_u} están asignadas o en uso operativo."
            ),
            kpis=[
                KpiReporte(etiqueta="Tipos de Recursos", valor=str(len(filtrados)), detalle="Ítems catalogados"),
                KpiReporte(etiqueta="Unidades Totales", valor=str(total_u), detalle="Stock patrimonial"),
                KpiReporte(etiqueta="Disponibles", valor=str(disp_u), detalle=f"{pct_disp}% del stock"),
                KpiReporte(etiqueta="En Uso / Asignados", valor=str(uso_u), detalle="Comprometidos en eventos"),
            ],
            columnas=cols,
            filas=filas,
            conclusiones=[
                f"La disponibilidad global del inventario alcanza el {pct_disp}% ({disp_u} de {total_u} unidades).",
                f"Se contabilizan {len(filtrados)} categorías/equipos bajo los criterios solicitados.",
                "Los recursos con menor stock disponible requieren priorización en la programación de próximos eventos." if filtrados else "No existen recursos registrados para el criterio solicitado."
            ],
            recomendaciones=[
                "Verificar periódicamente la devolución de materiales asignados al finalizar cada evento.",
                "Planificar reposición o mantenimiento preventivo en equipos con disponibilidad inferior al 30%."
            ],
            grafico_titulo="Unidades Disponibles por Recurso",
            grafico_datos=graf,
        )

    # 2. Espacios / Salas / Auditorios
    if any(k in q for k in ["espacio", "sala", "auditorio", "ambiente", "lugar", "salon", "salón"]):
        filtrados = list(espacios)
        if "menor" in q:
            filtrados.sort(key=lambda e: int(e.get("capacidad") or 0))
        else:
            filtrados.sort(key=lambda e: int(e.get("capacidad") or 0), reverse=True)

        cap_total = sum(int(e.get("capacidad") or 0) for e in filtrados)
        cap_prom = round(cap_total / len(filtrados), 1) if filtrados else 0.0
        mayor = filtrados[0] if filtrados else None

        cols = ["ID", "Espacio / Sala", "Ubicación", "Capacidad Máxima"]
        filas = _normalizar_filas([
            [e.get("id", "-"), e.get("nombre", "-"), e.get("ubicacion", "-"), f"{e.get('capacidad', 0)} personas"]
            for e in filtrados
        ], len(cols))

        graf = [
            GraficoItem(etiqueta=str(e.get("nombre", "Espacio")), valor=float(e.get("capacidad") or 0), extra=f"{e.get('capacidad', 0)} pers.")
            for e in filtrados[:8]
        ]

        return ReporteEjecutivoRespuesta(
            titulo="Informe de Espacios, Salas y Capacidad Instalada",
            subtitulo=f"Solicitud: \"{consulta}\" · {org_nombre}",
            tipo_reporte="ESPACIOS",
            periodo=periodo,
            resumen_ejecutivo=(
                f"La organización {org_nombre} dispone actualmente de {len(filtrados)} espacios físicos catalogados "
                f"con una capacidad instalada acumulada de {cap_total} personas (promedio de {cap_prom} personas por ambiente)."
            ),
            kpis=[
                KpiReporte(etiqueta="Espacios Registrados", valor=str(len(filtrados)), detalle="Salas y auditorios"),
                KpiReporte(etiqueta="Capacidad Total", valor=str(cap_total), detalle="Aforo acumulado"),
                KpiReporte(etiqueta="Capacidad Promedio", valor=str(cap_prom), detalle="Personas por sala"),
                KpiReporte(etiqueta="Mayor Aforo", valor=str(mayor.get("capacidad", 0)) if mayor else "0", detalle=str(mayor.get("nombre", "-"))[:22] if mayor else "-"),
            ],
            columnas=cols,
            filas=filas,
            conclusiones=[
                f"El recinto de mayor capacidad es {mayor.get('nombre')} con {mayor.get('capacidad')} plazas." if mayor else "No hay espacios registrados en la organización.",
                f"La infraestructura permite cubrir eventos simultáneos hasta un máximo teórico de {cap_total} asistentes.",
                "Distribuir eventos según el aforo estimado evita sobrecupo y optimiza el uso de salas."
            ],
            recomendaciones=[
                "Asignar los auditorios de mayor aforo a eventos con alta proyección de inscripciones.",
                "Validar cruces de horarios antes de publicar nuevos eventos en los espacios más demandados."
            ],
            grafico_titulo="Capacidad Máxima por Espacio",
            grafico_datos=graf,
        )

    # 3. Convocatorias / Staff / Personal
    if any(k in q for k in ["convocatoria", "staff", "personal", "voluntario", "postulaci", "equipo humano"]):
        filtradas = list(convocatorias)
        if "abierta" in q:
            filtradas = [c for c in filtradas if str(c.get("estado", "")).upper() == "ABIERTA"]
        elif "cerrada" in q:
            filtradas = [c for c in filtradas if str(c.get("estado", "")).upper() == "CERRADA"]
        elif "cancelada" in q:
            filtradas = [c for c in filtradas if str(c.get("estado", "")).upper() == "CANCELADA"]

        abiertas = sum(1 for c in filtradas if str(c.get("estado", "")).upper() == "ABIERTA")
        cupos = sum(int(c.get("cantidadRequerida") or 0) for c in filtradas)

        cols = ["ID", "Evento", "Tipo Personal", "Estado", "Cupos Requeridos", "Fecha Límite"]
        filas = _normalizar_filas([
            [
                c.get("id", "-"),
                c.get("evento", "-"),
                c.get("tipoPersonal", "-"),
                c.get("estado", "-"),
                c.get("cantidadRequerida", 0),
                c.get("fechaLimite", "-"),
            ]
            for c in filtradas
        ], len(cols))

        graf = [
            GraficoItem(etiqueta=str(c.get("evento", "Evento")), valor=float(c.get("cantidadRequerida") or 0), extra=f"{c.get('cantidadRequerida', 0)} cupos")
            for c in filtradas[:8]
        ]

        return ReporteEjecutivoRespuesta(
            titulo="Informe de Convocatorias y Requerimiento de Personal (Staff)",
            subtitulo=f"Solicitud: \"{consulta}\" · {org_nombre}",
            tipo_reporte="CONVOCATORIAS",
            periodo=periodo,
            resumen_ejecutivo=(
                f"El análisis registra {len(filtradas)} convocatorias de personal para {org_nombre}, "
                f"con {abiertas} convocatorias actualmente abiertas y una demanda total de {cupos} integrantes de staff/apoyo."
            ),
            kpis=[
                KpiReporte(etiqueta="Total Convocatorias", valor=str(len(filtradas)), detalle="Registros analizados"),
                KpiReporte(etiqueta="Convocatorias Abiertas", valor=str(abiertas), detalle="Aceptando postulaciones"),
                KpiReporte(etiqueta="Personal Requerido", valor=str(cupos), detalle="Cupos totales solicitados"),
            ],
            columnas=cols,
            filas=filas,
            conclusiones=[
                f"Existen {abiertas} convocatorias vigentes que requieren seguimiento de postulaciones.",
                f"La demanda operativa acumulada asciende a {cupos} puestos de apoyo para la ejecución de eventos.",
                "Mantener los turnos de staff cubiertos reduce riesgos logísticos el día del evento."
            ],
            recomendaciones=[
                "Revisar y aprobar postulaciones pendientes en las convocatorias próximas a su fecha límite.",
                "Difundir con anticipación las convocatorias abiertas con mayor requerimiento de personal."
            ],
            grafico_titulo="Personal Requerido por Convocatoria",
            grafico_datos=graf,
        )

    # 4. Participantes / Inscritos / Padrón
    if any(k in q for k in ["participante", "inscrito", "inscripci", "padron", "padrón", "asistente", "persona"]):
        filtrados = list(participantes)
        if "inactivo" in q:
            filtrados = [p for p in filtrados if str(p.get("estado", "")).upper() == "INACTIVO"]
        elif "activo" in q:
            filtrados = [p for p in filtrados if str(p.get("estado", "")).upper() == "ACTIVO"]
        if "confirmad" in q:
            filtrados = [p for p in filtrados if int(p.get("inscripcionesConfirmadas") or 0) > 0]
        elif "cancelad" in q:
            filtrados = [p for p in filtrados if int(p.get("inscripcionesCanceladas") or 0) > 0]

        filtrados.sort(key=lambda p: int(p.get("totalInscripciones") or 0), reverse=True)

        activos = sum(1 for p in filtrados if str(p.get("estado", "")).upper() == "ACTIVO")
        tot_ins = sum(int(p.get("totalInscripciones") or 0) for p in filtrados)
        conf_ins = sum(int(p.get("inscripcionesConfirmadas") or 0) for p in filtrados)
        canc_ins = sum(int(p.get("inscripcionesCanceladas") or 0) for p in filtrados)
        tasa_conf = round((conf_ins / tot_ins) * 100, 1) if tot_ins > 0 else 0.0

        cols = ["Participante", "Correo", "Estado", "Total Inscripciones", "Confirmadas", "Canceladas"]
        filas = _normalizar_filas([
            [
                f"{p.get('nombres', '')} {p.get('apellidos', '')}".strip(),
                p.get("correo", "-"),
                p.get("estado", "-"),
                p.get("totalInscripciones", 0),
                p.get("inscripcionesConfirmadas", 0),
                p.get("inscripcionesCanceladas", 0),
            ]
            for p in filtrados
        ], len(cols))

        graf = [
            GraficoItem(
                etiqueta=f"{p.get('nombres', '')} {p.get('apellidos', '')}".strip(),
                valor=float(p.get("totalInscripciones") or 0),
                extra=f"{p.get('inscripcionesConfirmadas', 0)} conf."
            )
            for p in filtrados[:8]
        ]

        return ReporteEjecutivoRespuesta(
            titulo="Informe de Participantes e Inscripciones",
            subtitulo=f"Solicitud: \"{consulta}\" · {org_nombre}",
            tipo_reporte="PARTICIPANTES",
            periodo=periodo,
            resumen_ejecutivo=(
                f"El padrón analizado comprende {len(filtrados)} participantes ({activos} activos) en {org_nombre}, "
                f"acumulando {tot_ins} inscripciones totales: {conf_ins} confirmadas ({tasa_conf}%) y {canc_ins} canceladas."
            ),
            kpis=[
                KpiReporte(etiqueta="Participantes", valor=str(len(filtrados)), detalle=f"{activos} activos"),
                KpiReporte(etiqueta="Inscripciones Totales", valor=str(tot_ins), detalle="Histórico acumulado"),
                KpiReporte(etiqueta="Confirmadas", valor=str(conf_ins), detalle=f"{tasa_conf}% de efectividad"),
                KpiReporte(etiqueta="Canceladas", valor=str(canc_ins), detalle="Bajas registradas"),
            ],
            columnas=cols,
            filas=filas,
            conclusiones=[
                f"La tasa de confirmación efectiva de inscripciones se sitúa en {tasa_conf}%.",
                f"Del total de {len(filtrados)} participantes consultados, {activos} mantienen estado ACTIVO.",
                "El seguimiento de recordatorios previos ayuda a minimizar el índice de cancelaciones."
            ],
            recomendaciones=[
                "Enviar recordatorios automáticos 24 horas antes del inicio de cada evento a los participantes confirmados.",
                "Reactivar a participantes con historial de cancelaciones mediante notificaciones segmentadas."
            ],
            grafico_titulo="Top Participantes por Inscripciones",
            grafico_datos=graf,
        )

    # 5. Eventos / Ocupación / Balance Ejecutivo General (default)
    filtrados = list(eventos)
    if "publicado" in q or "activo" in q:
        filtrados = [e for e in filtrados if str(e.get("estado", "")).upper() == "PUBLICADO"]
    elif "finalizado" in q or "terminado" in q:
        filtrados = [e for e in filtrados if str(e.get("estado", "")).upper() == "FINALIZADO"]
    elif "borrador" in q:
        filtrados = [e for e in filtrados if str(e.get("estado", "")).upper() == "BORRADOR"]
    elif "cancelado" in q:
        filtrados = [e for e in filtrados if str(e.get("estado", "")).upper() == "CANCELADO"]

    if "baja" in q or "menor" in q:
        filtrados.sort(key=lambda e: float(e.get("porcentajeLlenado") or 0))
    elif "alta" in q or "mayor" in q or "top" in q or "ocupaci" in q:
        filtrados.sort(key=lambda e: float(e.get("porcentajeLlenado") or 0), reverse=True)

    tot_ev = len(filtrados)
    pub_ev = sum(1 for e in filtrados if str(e.get("estado", "")).upper() == "PUBLICADO")
    tot_ins = sum(int(e.get("inscritos") or 0) for e in filtrados)
    tot_cap = sum(int(e.get("aforoMaximo") or 0) for e in filtrados)
    ocup_prom = round((tot_ins / tot_cap) * 100, 1) if tot_cap > 0 else 0.0

    cols = ["Evento", "Categoría", "Estado", "Fecha Inicio", "Aforo", "Inscritos", "Ocupación"]
    filas = _normalizar_filas([
        [
            e.get("nombre", "-"),
            e.get("categoria", "General"),
            e.get("estado", "-"),
            e.get("fechaInicio", "-"),
            e.get("aforoMaximo", 0),
            e.get("inscritos", 0),
            f"{e.get('porcentajeLlenado', 0)}%",
        ]
        for e in filtrados
    ], len(cols))

    graf = [
        GraficoItem(
            etiqueta=str(e.get("nombre", "Evento")),
            valor=float(e.get("inscritos") or 0),
            extra=f"{e.get('porcentajeLlenado', 0)}%"
        )
        for e in filtrados[:8]
    ]

    es_general = any(k in q for k in ["general", "ejecutivo", "balance", "todo", "resumen"])
    titulo = "Informe Ejecutivo General de la Organización" if es_general else "Informe de Rendimiento y Ocupación de Eventos"

    return ReporteEjecutivoRespuesta(
        titulo=titulo,
        subtitulo=f"Solicitud: \"{consulta}\" · {org_nombre}",
        tipo_reporte="GENERAL" if es_general else "EVENTOS",
        periodo=periodo,
        resumen_ejecutivo=(
            f"El informe consolida {tot_ev} eventos analizados en {org_nombre} ({pub_ev} publicados), "
            f"con {tot_ins} inscripciones confirmadas sobre una capacidad total de {tot_cap} plazas "
            f"(tasa global de ocupación del {ocup_prom}%). Además, la organización cuenta con {len(espacios)} espacios, "
            f"{len(recursos)} tipos de recursos y {len(convocatorias)} convocatorias de staff registradas."
        ),
        kpis=[
            KpiReporte(etiqueta="Eventos Analizados", valor=str(tot_ev), detalle=f"{pub_ev} publicados"),
            KpiReporte(etiqueta="Inscritos Confirmados", valor=str(tot_ins), detalle=f"Capacidad: {tot_cap}"),
            KpiReporte(etiqueta="Ocupación Global", valor=f"{ocup_prom}%", detalle="Promedio ponderado"),
            KpiReporte(etiqueta="Infraestructura", valor=f"{len(espacios)} salas / {len(recursos)} rec.", detalle=f"{len(convocatorias)} convocatorias"),
        ],
        columnas=cols,
        filas=filas,
        conclusiones=[
            f"La ocupación consolidada de los eventos analizados alcanza el {ocup_prom}% ({tot_ins} de {tot_cap} cupos).",
            f"Actualmente hay {pub_ev} eventos en estado PUBLICADO activos para captación de participantes.",
            f"La organización mantiene {resumen.get('totalParticipantes', len(participantes))} participantes registrados y {len(convocatorias)} convocatorias de personal."
        ],
        recomendaciones=[
            "Impulsar difusión dirigida en los eventos con porcentaje de llenado por debajo del 50%.",
            "Exportar este informe en PDF o Excel para revisión periódica con el equipo organizador."
        ],
        grafico_titulo="Inscritos por Evento",
        grafico_datos=graf,
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
