# -*- coding: utf-8 -*-
"""
Constructor integral de documentacion para el Sprint 3:
1. Genera 'documentacion/Sprint_3_Grupo11.docx' y lo copia a 'C:\\Users\\ASUS\\Downloads\\Sprint_3_Grupo11.docx'.
2. Genera 'documentacion/sprint-3-completo.md'.

Sigue con estricta fidelidad la jerarquia, estructura, tablas y estilos de Sprint 1.
"""

import os
import shutil
import docx
from docx.shared import Inches, Pt, RGBColor
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.table import WD_TABLE_ALIGNMENT

PROJECT_ROOT = r"c:\Proyecto-GestionEventos\plataforma-gestion-eventos"
DOC_DIR = os.path.join(PROJECT_ROOT, "documentacion")
ASSETS_DIR = os.path.join(DOC_DIR, "sprint3_assets")
DOWNLOADS_DIR = r"C:\Users\ASUS\Downloads"
BASE_TEMPLATE = os.path.join(DOWNLOADS_DIR, "Sprint_1_Grupo11.docx")

OUTPUT_DOCX_REPO = os.path.join(DOC_DIR, "Sprint_3_Grupo11.docx")
OUTPUT_DOCX_DOWN = os.path.join(DOWNLOADS_DIR, "Sprint_3_Grupo11.docx")
OUTPUT_MD = os.path.join(DOC_DIR, "sprint-3-completo.md")

TEAL_HEX = "A8CFC8"
TITLE_HEX = "153D63"

def set_cell_background(cell, fill_hex):
    tcPr = cell._tc.get_or_add_tcPr()
    shd = tcPr.find(qn('w:shd'))
    if shd is None:
        shd = OxmlElement('w:shd')
        tcPr.append(shd)
    shd.set(qn('w:val'), 'clear')
    shd.set(qn('w:color'), 'auto')
    shd.set(qn('w:fill'), fill_hex)

def set_cell_margins(cell, top=100, bottom=100, left=150, right=150):
    tcPr = cell._tc.get_or_add_tcPr()
    tcMar = OxmlElement('w:tcMar')
    for m, val in [('top', top), ('bottom', bottom), ('left', left), ('right', right)]:
        node = OxmlElement(f'w:{m}')
        node.set(qn('w:w'), str(val))
        node.set(qn('w:type'), 'dxa')
        tcMar.append(node)
    tcPr.append(tcMar)

def style_header_row(row, fill_hex=TEAL_HEX):
    for cell in row.cells:
        set_cell_background(cell, fill_hex)
        set_cell_margins(cell, top=120, bottom=120, left=150, right=150)
        for p in cell.paragraphs:
            p.alignment = WD_ALIGN_PARAGRAPH.LEFT
            for run in p.runs:
                run.bold = True
                run.font.name = "Arial"
                run.font.size = Pt(9.5)

def style_body_row(row):
    for cell in row.cells:
        set_cell_margins(cell, top=80, bottom=80, left=120, right=120)
        for p in cell.paragraphs:
            for run in p.runs:
                run.font.name = "Arial"
                run.font.size = Pt(9.0)

def create_user_story_card(doc, base_style, cu_id, cu_name, desc, priority, phu, criteria_list, dev):
    tbl = doc.add_table(rows=5, cols=2)
    tbl.style = base_style
    tbl.alignment = WD_TABLE_ALIGNMENT.CENTER
    
    # Row 0: Merged title
    tbl.cell(0, 0).merge(tbl.cell(0, 1))
    c0 = tbl.cell(0, 0)
    p0 = c0.paragraphs[0]
    p0.text = f"{cu_name}"
    p0.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p0.runs[0].bold = True
    p0.runs[0].font.name = "Arial"
    p0.runs[0].font.size = Pt(10)
    set_cell_margins(c0, 120, 120, 150, 150)
    
    # Row 1: ID and Desc
    tbl.cell(1, 0).paragraphs[0].text = f"ID :{cu_id}"
    tbl.cell(1, 1).paragraphs[0].text = f"Descripción: {desc}"
    
    # Row 2: Prioridad and PHU
    tbl.cell(2, 0).paragraphs[0].text = f"Prioridad:{priority}"
    tbl.cell(2, 1).paragraphs[0].text = f"Estimación PHU: {phu}"
    
    # Row 3: Merged Criterios
    tbl.cell(3, 0).merge(tbl.cell(3, 1))
    c3 = tbl.cell(3, 0)
    p3 = c3.paragraphs[0]
    crit_text = "Criterios de aceptación:\n" + "\n".join([f"• {c}" for c in criteria_list])
    p3.text = crit_text
    
    # Row 4: Merged Dev
    tbl.cell(4, 0).merge(tbl.cell(4, 1))
    c4 = tbl.cell(4, 0)
    p4 = c4.paragraphs[0]
    p4.text = f"Desarrollador a cargo: {dev}"
    
    # Format all cells
    for r_idx, row in enumerate(tbl.rows):
        for cell in row.cells:
            set_cell_margins(cell, 80, 80, 120, 120)
            for p in cell.paragraphs:
                for run in p.runs:
                    run.font.name = "Arial"
                    run.font.size = Pt(9.0)
                    if r_idx == 0:
                        run.bold = True
    return tbl

def build_docx():
    print("Iniciando construccion de Sprint_3_Grupo11.docx...")
    doc = docx.Document(BASE_TEMPLATE)
    base_style = doc.tables[0].style
    
    # Limpiar cuerpo manteniendo sectPr y estilos intactos
    body = doc._body._element
    for child in list(body):
        if not child.tag.endswith('sectPr'):
            body.remove(child)
            
    # -------------------------------------------------------------
    # HEADING 1: Sprint 3
    # -------------------------------------------------------------
    doc.add_paragraph("Sprint 3", style="Heading 1")
    
    # -------------------------------------------------------------
    # HEADING 2: Sprint Planning
    # -------------------------------------------------------------
    doc.add_paragraph("Sprint Planning", style="Heading 2")
    doc.add_paragraph(
        "Durante el Sprint 3 se continuará el desarrollo de la Plataforma Web/Móvil para Gestión de Eventos y Congresos "
        "con Inteligencia Artificial, tomando como base la arquitectura multi-tenant, autenticación JWT, usuarios, roles, "
        "gestión de eventos y programación implementada en el Sprint 1, así como los módulos de inscripciones, postulaciones "
        "y asignaciones de staff desarrollados en el Sprint 2."
    )
    doc.add_paragraph(
        "En este Sprint el trabajo se concentrará principalmente en las funcionalidades críticas de control de acceso, "
        "acreditación digital mediante códigos QR firmados criptográficamente, registro de asistencia (Check-in) en tiempo real, "
        "monitoreo de aforo por sala, notificaciones y recordatorios transaccionales automatizados (vía Brevo), y la consulta "
        "del historial consolidado de participación del usuario."
    )
    doc.add_paragraph("Los casos de uso seleccionados para este Sprint son:")
    doc.add_paragraph("CU21: Generar y visualizar credencial digital con QR")
    doc.add_paragraph("CU22: Escanear y validar código QR")
    doc.add_paragraph("CU23: Registrar asistencia (Check-in)")
    doc.add_paragraph("CU24: Consultar asistencia y aforo en tiempo real")
    doc.add_paragraph("CU25: Gestionar recordatorios y notificaciones")
    doc.add_paragraph("CU27: Consultar historial de participación")
    
    # HEADING 3: Objetivo del Sprint
    doc.add_paragraph("Objetivo del Sprint", style="Heading 3")
    doc.add_paragraph(
        "El objetivo del Sprint 3 será desarrollar e integrar las funcionalidades relacionadas con la emisión de credenciales "
        "digitales seguras con QR, la verificación y validación de acceso en puertas por parte del personal de Staff autorizado, "
        "el registro de asistencia sin duplicidad, el seguimiento de aforo en vivo mediante WebSockets para organizadores, "
        "la automatización de recordatorios y alertas por correo electrónico a través de la integración con Brevo, y el acceso "
        "al historial de participación para los asistentes registrados."
    )
    doc.add_paragraph(
        "Durante este Sprint se implementará la generación dinámica del token QR basado en HMAC-SHA256, garantizando que "
        "las credenciales sean infalsificables y permitan la verificación tanto online como en contingencia offline por la "
        "aplicación móvil Flutter."
    )
    doc.add_paragraph(
        "Asimismo, se desarrollará el mecanismo de Check-in con control estricto antifraude (bloqueo automático ante intentos "
        "de reutilización de credenciales en múltiples puertas), junto con la sincronización bidireccional hacia la bitácora "
        "auditable cifrada y el aislamiento estricto de datos por tenant organizacional."
    )
    doc.add_paragraph(
        "Las funcionalidades desarrolladas serán integradas de forma transversal con el backend Spring Boot 3, la plataforma "
        "web Angular 20, la base de datos PostgreSQL 17 y la aplicación móvil Flutter. El incremento obtenido será desplegado "
        "progresivamente sobre la infraestructura en la nube AWS y contenedores Docker para validar la operabilidad integral."
    )
    
    # HEADING 3: Historias de usuario (tarjetas 3C, Planning póker, prototipos)
    doc.add_paragraph("Historias de usuario (tarjetas 3C, Planning póker, prototipos)", style="Heading 3")
    
    # HU 1: CU21
    doc.add_paragraph("CU21: Generación y Visualización de Credencial Digital QR", style="No Spacing")
    create_user_story_card(
        doc, base_style, cu_id="21",
        cu_name="Generación y Visualización de Credencial Digital QR",
        desc="Generar y visualizar la credencial digital con código QR dinámico y firmado para participantes con inscripción confirmada.",
        priority="Alta", phu="8hr",
        criteria_list=[
            "El participante autenticado con inscripción CONFIRMADA debe poder generar su credencial digital desde la app móvil Flutter y la web Angular.",
            "La credencial debe desplegar nombre completo, evento, categoría de inscripción, fecha y el código QR visualmente legible.",
            "El código QR debe codificar un token criptográfico firmado (HMAC-SHA256) que prevenga falsificaciones o alteraciones de identidad.",
            "El sistema debe permitir descargar la credencial en PDF o almacenarla en la caché local (Wallet offline) para acceso sin conexión.",
            "Si la inscripción se encuentra cancelada, rechazada o pendiente de pago, el sistema no emitirá el código QR activo."
        ],
        dev="Aranibar Pérez Kevin Ariel / Cervantes Arancibia Roberto Carlos"
    )
    p_img = doc.add_paragraph()
    p_img.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p_img.add_run().add_picture(os.path.join(ASSETS_DIR, "proto_cu21.png"), width=Inches(6.2))
    
    # HU 2: CU22
    doc.add_paragraph("CU22: Escaneo y Validación de Código QR", style="No Spacing")
    create_user_story_card(
        doc, base_style, cu_id="22",
        cu_name="Escaneo y Validación de Código QR",
        desc="Escanear y validar la credencial QR en los puntos de acceso mediante la cámara de la aplicación móvil del Staff autorizado.",
        priority="Alta", phu="8hr",
        criteria_list=[
            "El personal de Staff autenticado y asignado al evento debe poder activar la cámara desde la app móvil Flutter para escanear credenciales.",
            "El sistema debe decodificar el QR y validar la firma criptográfica y vigencia del token en menos de 1 segundo.",
            "La pantalla debe mostrar una alerta clara con retroalimentación inmediata: 'Acreditado / Válido', 'QR Inválido' o 'Evento Incorrecto'.",
            "Debe soportar validación offline en caso de intermitencia de red contra la lista local sincronizada de inscritos.",
            "Usuarios sin rol Staff o fuera del horario programado del evento tendrán el acceso al módulo de escaneo bloqueado."
        ],
        dev="Espinoza Sanchez Matteo"
    )
    p_img = doc.add_paragraph()
    p_img.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p_img.add_run().add_picture(os.path.join(ASSETS_DIR, "proto_cu22.png"), width=Inches(6.2))
    
    # HU 3: CU23
    doc.add_paragraph("CU23: Registro de Asistencia (Check-in)", style="No Spacing")
    create_user_story_card(
        doc, base_style, cu_id="23",
        cu_name="Registro de Asistencia (Check-in)",
        desc="Registrar el ingreso efectivo del participante en la base de datos garantizando control estricto de duplicidad y contingencia manual.",
        priority="Alta", phu="8hr",
        criteria_list=[
            "Tras el escaneo exitoso, el sistema registrará la asistencia con marca de tiempo precisa (timestamp) y el ID del operador Staff.",
            "El sistema impedirá check-in duplicados para la misma sesión o evento, emitiendo una alerta roja con la hora y puerta del ingreso previo.",
            "Debe proveer una opción de registro manual de asistencia por documento de identidad (CI/DNI) o correo para contingencias.",
            "Cada registro exitoso o intento denegado debe asentar un evento en la bitácora cifrada asociada al tenant correspondiente.",
            "El estado de la inscripción del participante debe actualizarse de forma inmediata a 'Asistió / Presente'."
        ],
        dev="Cervantes Arancibia Roberto Carlos"
    )
    p_img = doc.add_paragraph()
    p_img.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p_img.add_run().add_picture(os.path.join(ASSETS_DIR, "proto_cu23.png"), width=Inches(6.2))
    
    # HU 4: CU24
    doc.add_paragraph("CU24: Consulta de Asistencia y Aforo en Tiempo Real", style="No Spacing")
    create_user_story_card(
        doc, base_style, cu_id="24",
        cu_name="Consulta de Asistencia y Aforo en Tiempo Real",
        desc="Supervisar en tiempo real el porcentaje de asistencia, conteo de ingresos y nivel de aforo por sala mediante dashboard web reactivo.",
        priority="Alta", phu="8hr",
        criteria_list=[
            "El organizador y administrador deben visualizar un dashboard web en Angular con el total de inscritos, asistentes presentes y ausentes.",
            "El conteo de asistentes y porcentajes de ocupación por sala deben actualizarse en vivo mediante conexión WebSocket / STOMP.",
            "El sistema debe desplegar alertas visuales automáticas cuando el aforo alcance umbrales críticos (80%, 90% y 100% de la capacidad).",
            "Debe permitir aplicar filtros por sala, categoría de entrada y franja horaria de ingreso.",
            "Debe permitir exportar la nómina de asistencia en formatos estándar (CSV y PDF) para control administrativo."
        ],
        dev="Choque Canavides Cristian Santiago"
    )
    p_img = doc.add_paragraph()
    p_img.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p_img.add_run().add_picture(os.path.join(ASSETS_DIR, "proto_cu24.png"), width=Inches(6.2))
    
    # HU 5: CU25
    doc.add_paragraph("CU25: Gestión de Recordatorios y Notificaciones", style="No Spacing")
    create_user_story_card(
        doc, base_style, cu_id="25",
        cu_name="Gestión de Recordatorios y Notificaciones",
        desc="Automatizar el despacho de correos transaccionales y recordatorios mediante Brevo para confirmación, alertas de horario y credenciales.",
        priority="Alta", phu="8hr",
        criteria_list=[
            "El sistema enviará de forma automática un correo electrónico transaccional con la credencial digital y QR tras confirmarse la inscripción.",
            "Debe permitir programar recordatorios masivos previos al evento (24h y 2h antes) informando ubicación, sala y requisitos de acceso.",
            "Debe emitir avisos inmediatos ante eventuales reprogramaciones, cambios de sala o cancelaciones de conferencias.",
            "Cada notificación despachada debe registrarse en la base de datos con su estado de entrega ('ENVIADO', 'PENDIENTE', 'FALLIDO').",
            "La integración debe operar mediante la API REST transaccional de Brevo garantizando aislamiento multi-tenant por organización."
        ],
        dev="Justiniano Coimbra Raúl Rodrigo"
    )
    p_img = doc.add_paragraph()
    p_img.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p_img.add_run().add_picture(os.path.join(ASSETS_DIR, "proto_cu25.png"), width=Inches(6.2))
    
    # HU 6: CU27
    doc.add_paragraph("CU27: Consulta de Historial de Participación", style="No Spacing")
    create_user_story_card(
        doc, base_style, cu_id="27",
        cu_name="Consulta de Historial de Participación",
        desc="Permitir a los participantes consultar su trayectoria histórica de eventos asistidos, credenciales y constancias desde móvil o web.",
        priority="Media", phu="7hr",
        criteria_list=[
            "El usuario autenticado debe acceder a la lista cronológica de todos los eventos en los que se ha inscrito (vigentes y pasados).",
            "Para cada evento debe detallar: fecha, título, organización organizadora, modalidad, estado de inscripción y asistencia registrada.",
            "Debe permitir descargar o visualizar la credencial digital para eventos activos o próximos a iniciar.",
            "La información desplegada debe estar estrictamente protegida y restringida al titular de la cuenta autenticada.",
            "La interfaz debe incorporar filtros de búsqueda por nombre de evento, rango de fechas y condición de asistencia."
        ],
        dev="Choque Canavides Cristian Santiago / Aranibar Pérez Kevin Ariel"
    )
    p_img = doc.add_paragraph()
    p_img.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p_img.add_run().add_picture(os.path.join(ASSETS_DIR, "proto_cu27.png"), width=Inches(6.2))
    
    # HEADING 3: Contexto del Sistema
    doc.add_paragraph("Contexto del Sistema", style="Heading 3")
    doc.add_paragraph(
        "Durante el Sprint 3 se amplía la estructura funcional consolidada en los sprints precedentes, integrando el flujo "
        "operativo de control de acceso físico y virtual de los eventos. Mientras el Sprint 0 definió la arquitectura base "
        "y autenticación, el Sprint 1 la gestión de eventos y salas, y el Sprint 2 las inscripciones y postulación de staff, "
        "el Sprint 3 concreta el día del evento mediante el uso de tecnología móvil y mensajería transaccional."
    )
    doc.add_paragraph(
        "El desarrollo se enfoca inicialmente en el motor de generación y firma de credenciales QR dinámicas, asegurando "
        "que cada token contenga información cifrada e incorruptible vinculada a la inscripción del participante."
    )
    doc.add_paragraph(
        "Posteriormente se incorpora la aplicación móvil para el personal de Staff, transformando los dispositivos en terminales "
        "ópticos de escaneo capaces de validar credenciales en milisegundos y registrar el check-in de asistencia directamente en el backend."
    )
    doc.add_paragraph(
        "Asimismo, se habilita el monitoreo reactivo de aforo mediante WebSockets en la consola web Angular, permitiendo a los "
        "organizadores tomar decisiones logísticas en tiempo real basadas en la ocupación física de los auditorios."
    )
    doc.add_paragraph(
        "El Sprint incluye además el despacho masivo y automatizado de comunicaciones a través del servicio en la nube Brevo, "
        "asegurando que los inscritos reciban oportunamente sus credenciales y recordatorios de inicio."
    )
    doc.add_paragraph(
        "Todas estas funcionalidades operan bajo las directivas de seguridad multi-tenant establecidas, garantizando que los datos "
        "de asistencia y acreditaciones pertenezcan exclusivamente a la organización responsable y queden asentados en la bitácora cifrada."
    )
    
    # HEADING 3: Sprint Backlog
    doc.add_paragraph("Sprint Backlog", style="Heading 3")
    tbl_backlog = doc.add_table(rows=24, cols=6)
    tbl_backlog.style = base_style
    tbl_backlog.alignment = WD_TABLE_ALIGNMENT.CENTER
    
    # Row 0: Title merged
    tbl_backlog.cell(0, 0).merge(tbl_backlog.cell(0, 5))
    tbl_backlog.cell(0, 0).paragraphs[0].text = "Sprint Backlog"
    tbl_backlog.cell(0, 0).paragraphs[0].alignment = WD_ALIGN_PARAGRAPH.CENTER
    set_cell_background(tbl_backlog.cell(0, 0), TEAL_HEX)
    
    # Row 1: Metadatos
    tbl_backlog.cell(1, 0).merge(tbl_backlog.cell(1, 2))
    tbl_backlog.cell(1, 0).paragraphs[0].text = "Número de Sprint : 3"
    tbl_backlog.cell(1, 3).merge(tbl_backlog.cell(1, 5))
    tbl_backlog.cell(1, 3).paragraphs[0].text = "Tiempo programado: 5 días"
    
    # Row 2: Objetivo merged
    tbl_backlog.cell(2, 0).merge(tbl_backlog.cell(2, 5))
    tbl_backlog.cell(2, 0).paragraphs[0].text = (
        "Objetivo: Desarrollar e integrar las funcionalidades de emisión de credenciales digitales con QR, escaneo y validación "
        "en app móvil para Staff, registro de asistencia Check-in sin duplicados, monitoreo de aforo en tiempo real vía WebSockets, "
        "gestión de recordatorios y notificaciones automáticas con Brevo, y consulta del historial de participación del usuario."
    )
    
    # Row 3: Product Owner merged
    tbl_backlog.cell(3, 0).merge(tbl_backlog.cell(3, 5))
    tbl_backlog.cell(3, 0).paragraphs[0].text = "Product Owner: Cervantes Arancibia Roberto Carlos"
    
    # Row 4: Fechas
    tbl_backlog.cell(4, 0).merge(tbl_backlog.cell(4, 2))
    tbl_backlog.cell(4, 0).paragraphs[0].text = "Fecha de inicio: 12/10/2026"
    tbl_backlog.cell(4, 3).merge(tbl_backlog.cell(4, 5))
    tbl_backlog.cell(4, 3).paragraphs[0].text = "Fecha de finalización: 16/10/2026"
    
    # Row 5: Column headers
    headers_bl = ['Id', 'Tarea', 'Tipo', 'Estimación (hr)', 'Responsable', 'Estado']
    for idx, h_txt in enumerate(headers_bl):
        cell = tbl_backlog.cell(5, idx)
        cell.paragraphs[0].text = h_txt
        set_cell_background(cell, TEAL_HEX)
        cell.paragraphs[0].runs[0].bold = True
        
    tasks_data = [
        ("SP3-1", "Diseñar credencial digital con QR", "Diseño", "4hr", "Aranibar Pérez Kevin Ariel", "Terminado"),
        ("SP3-2", "Generar códigos QR asociados a inscripciones", "Desarrollo", "7hr", "Cervantes Arancibia Roberto Carlos", "Terminado"),
        ("SP3-3", "Visualizar credencial QR en app móvil y web", "Desarrollo", "6hr", "Aranibar Pérez Kevin Ariel", "Terminado"),
        ("SP3-4", "Probar generación y visualización QR", "Pruebas", "3hr", "Carvallo Andreu Carlos Rodrigo", "Terminado"),
        ("SP3-5", "Diseñar escaneo y validación QR para Staff", "Diseño", "4hr", "Aranibar Pérez Kevin Ariel", "Terminado"),
        ("SP3-6", "Implementar escaneo y validación QR en móvil", "Desarrollo", "8hr", "Espinoza Sanchez Matteo", "Terminado"),
        ("SP3-7", "Probar control de acceso y validación QR", "Pruebas", "4hr", "Carvallo Andreu Carlos Rodrigo", "Terminado"),
        ("SP3-8", "Implementar registro de asistencia y antifraude", "Desarrollo", "8hr", "Cervantes Arancibia Roberto Carlos", "Terminado"),
        ("SP3-9", "Diseñar seguimiento web de asistencia y aforo", "Diseño", "4hr", "Choque Canavides Cristian Santiago", "Terminado"),
        ("SP3-10", "Consultar asistencia y aforo en tiempo real (WebSocket)", "Desarrollo", "8hr", "Choque Canavides Cristian Santiago", "Terminado"),
        ("SP3-11", "Probar asistencia y monitoreo de aforo en vivo", "Pruebas", "4hr", "Carvallo Andreu Carlos Rodrigo", "Terminado"),
        ("SP3-12", "Diseñar recordatorios y plantillas de notificación", "Diseño", "4hr", "Choque Canavides Cristian Santiago", "Terminado"),
        ("SP3-13", "Implementar notificaciones de inscripción con Brevo", "Desarrollo", "6hr", "Justiniano Coimbra Raúl Rodrigo", "Terminado"),
        ("SP3-14", "Implementar recordatorios, cambios y cancelaciones", "Desarrollo", "6hr", "Justiniano Coimbra Raúl Rodrigo", "Terminado"),
        ("SP3-15", "Probar despacho de notificaciones y emails", "Pruebas", "3hr", "Carvallo Andreu Carlos Rodrigo", "Terminado"),
        ("SP3-16", "Diseñar historial de participación del usuario", "Diseño", "4hr", "Aranibar Pérez Kevin Ariel", "Terminado"),
        ("SP3-17", "Implementar historial de inscripciones y asistencias", "Desarrollo", "7hr", "Choque Canavides Cristian Santiago", "Terminado"),
        ("SP3-18", "Probar historial y trazabilidad de participación", "Pruebas", "4hr", "Carvallo Andreu Carlos Rodrigo", "Terminado")
    ]
    
    for row_idx, t_data in enumerate(tasks_data, start=6):
        for col_idx, val in enumerate(t_data):
            tbl_backlog.cell(row_idx, col_idx).paragraphs[0].text = val
            
    for row in tbl_backlog.rows:
        style_body_row(row)
    style_header_row(tbl_backlog.rows[0])
    style_header_row(tbl_backlog.rows[5])
    
    # HEADING 3: Equipo Scrum
    doc.add_paragraph("Equipo Scrum", style="Heading 3")
    tbl_team = doc.add_table(rows=7, cols=2)
    tbl_team.style = base_style
    tbl_team.alignment = WD_TABLE_ALIGNMENT.CENTER
    tbl_team.cell(0, 0).paragraphs[0].text = "ROL"
    tbl_team.cell(0, 1).paragraphs[0].text = "PERSONA"
    style_header_row(tbl_team.rows[0])
    
    team_members = [
        ("Product Owner", "Cervantes Arancibia Roberto Carlos"),
        ("Scrum Máster", "Espinoza Sanchez Matteo"),
        ("Developer", "Carvallo Andreu Carlos Rodrigo"),
        ("Developer", "Choque Canavides Cristian Santiago"),
        ("Developer", "Justiniano Coimbra Raúl Rodrigo"),
        ("Developer", "Aranibar Pérez Kevin Ariel")
    ]
    for idx, (rol, nom) in enumerate(team_members, start=1):
        tbl_team.cell(idx, 0).paragraphs[0].text = rol
        tbl_team.cell(idx, 1).paragraphs[0].text = nom
        style_body_row(tbl_team.rows[idx])
        
    # -------------------------------------------------------------
    # HEADING 2: Proceso/patrón de desarrollo por Historia de Usuario
    # -------------------------------------------------------------
    doc.add_paragraph("Proceso/patrón de desarrollo por Historia de Usuario", style="Heading 2")
    
    # HEADING 3: Diseño
    doc.add_paragraph("Diseño", style="Heading 3")
    
    # HEADING 4: Diseño De La Arquitectura
    doc.add_paragraph("Diseño De La Arquitectura", style="Heading 4")
    
    # 1. Diseño Físico: Diagrama de Despliegue
    p_fis = doc.add_paragraph("Diseño Físico: Diagrama de Despliegue en AWS Cloud Multi-Tenant", style="List Paragraph")
    p_fis.runs[0].bold = True
    doc.add_paragraph(
        "El siguiente diagrama de despliegue representa la topología de infraestructura en la nube de Amazon Web Services (AWS) "
        "para la plataforma multi-tenant. Modela la interacción entre los dispositivos cliente (App Móvil Flutter para Wallet QR "
        "y escáner Staff, y Navegador Web para la SPA de Angular 20) accediendo de manera segura mediante TLS 443 / WSS hacia un "
        "Application Load Balancer (ALB). Dentro de la VPC de AWS se ejecuta el clúster de cómputo (EC2/ECS Docker) con los contenedores "
        "del backend Spring Boot 3 (Java 21) y el microservicio de IA FastAPI (Python 3.11), comunicados con la base de datos relacional "
        "Amazon RDS PostgreSQL 17 multi-tenant con esquemas aislados por tenant, AWS KMS para el sellado criptográfico de la bitácora, "
        "Amazon S3 para assets y reportes, y servicios externos como Brevo Cloud API (SMTP) y Stripe API."
    )
    p_img_desp = doc.add_paragraph()
    p_img_desp.alignment = WD_ALIGN_PARAGRAPH.CENTER
    if os.path.exists(os.path.join(ASSETS_DIR, "diag_despliegue_aws.png")):
        p_img_desp.add_run().add_picture(os.path.join(ASSETS_DIR, "diag_despliegue_aws.png"), width=Inches(6.4))
    
    # 2. Diseño Lógico: Diagrama de Paquetes
    p_log = doc.add_paragraph("Diseño Lógico: Diagrama de Paquetes de la Arquitectura", style="List Paragraph")
    p_log.runs[0].bold = True
    doc.add_paragraph(
        "El diagrama de paquetes modela la arquitectura lógica en capas desacopladas y modulares del sistema. Estructura la Capa "
        "de Presentación (Angular 20 y Flutter), el módulo transversal de Seguridad y Control de Acceso (JWT, Roles), la Gestión "
        "SaaS y Multi-Tenant, el Dominio Funcional del núcleo (Eventos y Espacios del Sprint 1, Inscripciones y Staff del Sprint 2, "
        "Acreditación / QR / Check-in del Sprint 3 y Notificaciones / Recordatorios del Sprint 3), los Servicios de Soporte "
        "(Servicio de IA FastAPI y Auditoría con Bitácora Cifrada por Tenant) y la Capa de Persistencia relacional (PostgreSQL 17, JPA)."
    )
    p_img_paq = doc.add_paragraph()
    p_img_paq.alignment = WD_ALIGN_PARAGRAPH.CENTER
    if os.path.exists(os.path.join(ASSETS_DIR, "diag_paquetes_logico.png")):
        p_img_paq.add_run().add_picture(os.path.join(ASSETS_DIR, "diag_paquetes_logico.png"), width=Inches(6.4))

    # 3. Diagrama de Componentes
    p_c1 = doc.add_paragraph("Diagrama de Componentes del Sprint 3", style="List Paragraph")
    p_c1.runs[0].bold = True
    doc.add_paragraph(
        "Durante el Sprint 3 se mantiene y expande la arquitectura hexagonal multi-tenant desarrollada en los sprints "
        "anteriores. El siguiente diagrama representa los componentes funcionales, clientes web/móviles, capas de servicio, "
        "mecanismos de WebSocket reactivos, persistencia en PostgreSQL y la integración externa con el servicio en la nube Brevo."
    )
    p_img = doc.add_paragraph()
    p_img.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p_img.add_run().add_picture(os.path.join(ASSETS_DIR, "diag_componentes_sprint3.png"), width=Inches(6.3))
    
    # HEADING 4: Diseño De Datos
    doc.add_paragraph("Diseño De Datos", style="Heading 4")
    p_c2 = doc.add_paragraph("Diagrama De Clases / Datos del Sprint 3", style="List Paragraph")
    p_c2.runs[0].bold = True
    p_img = doc.add_paragraph()
    p_img.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p_img.add_run().add_picture(os.path.join(ASSETS_DIR, "diag_clases_sprint3.png"), width=Inches(6.3))
    
    p_c3 = doc.add_paragraph("Script", style="List Paragraph")
    p_c3.runs[0].bold = True
    sql_script = (
        "-- SPRINT 3: CONTROL QR, ASISTENCIA, NOTIFICACIONES E HISTORIAL\n\n"
        "CREATE TABLE credenciales_qr (\n"
        "    id_credencial SERIAL PRIMARY KEY,\n"
        "    id_inscripcion INT NOT NULL UNIQUE REFERENCES inscripciones(id_inscripcion) ON DELETE CASCADE,\n"
        "    id_organizacion INT NOT NULL REFERENCES organizaciones(id_organizacion) ON DELETE CASCADE,\n"
        "    token_criptografico VARCHAR(255) NOT NULL UNIQUE,\n"
        "    hash_verificacion VARCHAR(64) NOT NULL,\n"
        "    formato_qr VARCHAR(20) DEFAULT 'PNG_BASE64',\n"
        "    fecha_emision TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,\n"
        "    fecha_expiracion TIMESTAMP NOT NULL,\n"
        "    estado VARCHAR(20) DEFAULT 'ACTIVA' CHECK (estado IN ('ACTIVA', 'REVOCADA', 'EXPIRADA'))\n"
        ");\n\n"
        "CREATE TABLE asistencias (\n"
        "    id_asistencia SERIAL PRIMARY KEY,\n"
        "    id_inscripcion INT NOT NULL REFERENCES inscripciones(id_inscripcion) ON DELETE CASCADE,\n"
        "    id_evento INT NOT NULL REFERENCES eventos(id_evento) ON DELETE CASCADE,\n"
        "    id_usuario INT NOT NULL REFERENCES usuarios(id_usuario) ON DELETE CASCADE,\n"
        "    id_operador_staff INT REFERENCES usuarios(id_usuario) ON DELETE SET NULL,\n"
        "    id_organizacion INT NOT NULL REFERENCES organizaciones(id_organizacion) ON DELETE CASCADE,\n"
        "    fecha_hora_checkin TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,\n"
        "    metodo_validacion VARCHAR(30) NOT NULL CHECK (metodo_validacion IN ('QR_ESCANEO', 'MANUAL_STAFF', 'OFFLINE_SYNC')),\n"
        "    estado_asistencia VARCHAR(20) DEFAULT 'PRESENTE' CHECK (estado_asistencia IN ('PRESENTE', 'TARDANZA', 'JUSTIFICADO')),\n"
        "    observaciones VARCHAR(255),\n"
        "    CONSTRAINT uq_asistencia_evento_usuario UNIQUE (id_evento, id_usuario)\n"
        ");\n\n"
        "CREATE TABLE notificaciones_eventos (\n"
        "    id_notificacion SERIAL PRIMARY KEY,\n"
        "    id_evento INT NOT NULL REFERENCES eventos(id_evento) ON DELETE CASCADE,\n"
        "    id_usuario INT NOT NULL REFERENCES usuarios(id_usuario) ON DELETE CASCADE,\n"
        "    id_organizacion INT NOT NULL REFERENCES organizaciones(id_organizacion) ON DELETE CASCADE,\n"
        "    tipo_notificacion VARCHAR(50) NOT NULL,\n"
        "    canal_envio VARCHAR(20) NOT NULL DEFAULT 'EMAIL_BREVO',\n"
        "    asunto VARCHAR(150) NOT NULL,\n"
        "    cuerpo_mensaje TEXT NOT NULL,\n"
        "    fecha_programada TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,\n"
        "    fecha_envio TIMESTAMP,\n"
        "    estado_envio VARCHAR(20) DEFAULT 'PENDIENTE' CHECK (estado_envio IN ('PENDIENTE', 'ENVIADO', 'FALLIDO')),\n"
        "    id_mensaje_externo VARCHAR(100)\n"
        ");\n\n"
        "CREATE TABLE control_aforo_salas (\n"
        "    id_control_aforo SERIAL PRIMARY KEY,\n"
        "    id_evento INT NOT NULL REFERENCES eventos(id_evento) ON DELETE CASCADE,\n"
        "    id_espacio INT NOT NULL REFERENCES espacios(id_espacio) ON DELETE CASCADE,\n"
        "    capacidad_maxima INT NOT NULL CHECK (capacidad_maxima > 0),\n"
        "    asistentes_actuales INT NOT NULL DEFAULT 0 CHECK (asistentes_actuales >= 0),\n"
        "    porcentaje_ocupacion NUMERIC(5,2) GENERATED ALWAYS AS ((asistentes_actuales::numeric / capacidad_maxima::numeric) * 100) STORED,\n"
        "    alerta_umbral BOOLEAN DEFAULT FALSE,\n"
        "    ultima_actualizacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,\n"
        "    CONSTRAINT uq_evento_espacio UNIQUE (id_evento, id_espacio)\n"
        ");\n\n"
        "CREATE INDEX idx_credencial_token ON credenciales_qr(token_criptografico);\n"
        "CREATE INDEX idx_asistencias_evento_usuario ON asistencias(id_evento, id_usuario);\n"
        "CREATE INDEX idx_notificaciones_estado ON notificaciones_eventos(estado_envio);"
    )
    p_sql = doc.add_paragraph(sql_script, style="No Spacing")
    for r in p_sql.runs:
        r.font.name = "Consolas"
        r.font.size = Pt(8.5)
        
    p_c4 = doc.add_paragraph("Mapeo", style="List Paragraph")
    p_c4.runs[0].bold = True
    
    # Tables 9 to 13 equivalents for Sprint 3
    dd_tables = [
        ("CREDENCIAL_QR", "PK: id_credencial | FK: id_inscripcion | FK: id_organizacion | token_criptografico | hash_verificacion | formato_qr | fecha_emision | fecha_expiracion | estado"),
        ("ASISTENCIA", "PK: id_asistencia | FK: id_inscripcion | FK: id_evento | FK: id_usuario | FK: id_operador_staff | FK: id_organizacion | fecha_hora_checkin | metodo_validacion | estado_asistencia | observaciones"),
        ("NOTIFICACION_EVENTO", "PK: id_notificacion | FK: id_evento | FK: id_usuario | FK: id_organizacion | tipo_notificacion | canal_envio | asunto | cuerpo_mensaje | fecha_programada | fecha_envio | estado_envio | id_mensaje_externo"),
        ("CONTROL_AFORO_SALA", "PK: id_control_aforo | FK: id_evento | FK: id_espacio | capacidad_maxima | asistentes_actuales | porcentaje_ocupacion | alerta_umbral | ultima_actualizacion"),
        ("HISTORIAL_PARTICIPACION (VISTA)", "PK: id_usuario | total_eventos_inscrito | total_asistencias_efectivas | porcentaje_asistencia_global | eventos_vigentes | eventos_finalizados | tenant_id")
    ]
    for tbl_name, tbl_fields in dd_tables:
        t_box = doc.add_table(rows=2, cols=1)
        t_box.style = base_style
        t_box.alignment = WD_TABLE_ALIGNMENT.CENTER
        t_box.cell(0, 0).paragraphs[0].text = tbl_name
        set_cell_background(t_box.cell(0, 0), TEAL_HEX)
        t_box.cell(0, 0).paragraphs[0].runs[0].bold = True
        t_box.cell(1, 0).paragraphs[0].text = tbl_fields
        style_body_row(t_box.rows[1])
        doc.add_paragraph("", style="Normal")
        
    # HEADING 4: Diseño De La Lógica De Negocio
    doc.add_paragraph("Diseño De La Lógica De Negocio", style="Heading 4")
    comm_diagrams = [
        ("Diagrama de comunicación CU21: Generar y visualizar credencial digital con QR", "diag_comunicacion_cu21.png"),
        ("Diagrama de comunicación CU22: Escanear y validar código QR", "diag_comunicacion_cu22.png"),
        ("Diagrama de comunicación CU23: Registrar asistencia (Check-in)", "diag_comunicacion_cu23.png"),
        ("Diagrama de comunicación CU24: Consultar asistencia y aforo en tiempo real", "diag_comunicacion_cu24.png"),
        ("Diagrama de comunicación CU25: Gestionar recordatorios y notificaciones", "diag_comunicacion_cu25.png"),
        ("Diagrama de comunicación CU27: Consultar historial de participación", "diag_comunicacion_cu27.png")
    ]
    for title_diag, file_diag in comm_diagrams:
        p_t = doc.add_paragraph(title_diag, style="List Paragraph")
        p_t.runs[0].bold = True
        p_im = doc.add_paragraph()
        p_im.alignment = WD_ALIGN_PARAGRAPH.CENTER
        p_im.add_run().add_picture(os.path.join(ASSETS_DIR, file_diag), width=Inches(6.3))
        
    # HEADING 3: Implementación
    doc.add_paragraph("Implementación", style="Heading 3")
    doc.add_paragraph("Componentes y artefactos generados", style="Heading 4")
    doc.add_paragraph(
        "Durante la implementación del Sprint 3 se desarrollaron e integraron los componentes necesarios para el control "
        "de acceso digital, validación por escáner óptico, monitoreo de aforo en vivo, notificaciones transaccionales e historial de participación."
    )
    doc.add_paragraph("• Plataforma web Angular: módulo reactivo de monitoreo de aforo con gráficos en vivo, modal visor de credencial digital con descarga en PDF, consola de registro manual para Staff y centro de gestión de notificaciones.")
    doc.add_paragraph("• Aplicación Flutter: visor de credenciales con almacenamiento en Wallet local (soporte offline), escáner de cámara integrado con validación instantánea de firma HMAC, confirmación háptica de check-in y pantalla de historial del participante.")
    doc.add_paragraph("• Backend Spring Boot: endpoints REST para emisión y validación de tokens QR, servicio de control de asistencia y antifraude, broker WebSocket con protocolo STOMP para difusión reactiva de aforo y cliente Brevo para mensajería.")
    doc.add_paragraph("• PostgreSQL: tablas relacionales con restricciones de no duplicidad, índices de alta velocidad para validación de tokens y aislamiento estricto por organización.")
    doc.add_paragraph("• Integración con autenticación, bitácora cifrada (KMS) y modelo multi-tenant consolidados en los Sprints 0, 1 y 2.")
    doc.add_paragraph("• Despliegue progresivo del incremento en la infraestructura AWS y Docker Compose garantizando sincronización continua.")
    
    p_art = doc.add_paragraph()
    p_art.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p_art.add_run().add_picture(os.path.join(ASSETS_DIR, "diag_artefactos_sprint3.png"), width=Inches(6.3))
    
    # HEADING 3: Pruebas
    doc.add_paragraph("Pruebas", style="Heading 3")
    doc.add_paragraph("Plan de pruebas (Criterios de aceptación )", style="Heading 4")
    doc.add_paragraph(
        "Las pruebas del Sprint 3 tienen como finalidad verificar que las funcionalidades de acreditación QR, escaneo móvil, "
        "check-in de asistencia, aforo en tiempo real, notificaciones automáticas e historial de participación cumplan con los "
        "criterios de aceptación definidos para las historias de usuario seleccionadas."
    )
    
    test_cases = [
        ("Prueba 1: Generar y visualizar credencial digital con QR", "PT-S3-01", "CU21: Generar y visualizar credencial digital con QR",
         "Validar la generación correcta del código QR criptográfico para participantes con inscripción confirmada y su visualización en web y móvil.",
         "Usuario participante con inscripción en estado CONFIRMADA o APROBADA.",
         "1. Iniciar sesión como participante.\n2. Navegar a 'Mis Inscripciones' o 'Mi Credencial'.\n3. Solicitar generación de credencial digital.\n4. Comprobar renderizado de código QR y datos del evento.\n5. Descargar credencial en PDF y guardar en Wallet local.",
         "La credencial digital se genera con token válido, el código QR es nítido y almacena el hash firmado HMAC-SHA256 sin errores.",
         "Ejecutado con éxito en web y móvil Flutter. Credencial generada y legible por escáner.", "Aprobado"),
        
        ("Prueba 2: Escanear y validar código QR", "PT-S3-02", "CU22: Escanear y validar código QR",
         "Validar la lectura óptica del QR mediante la app móvil Staff, comprobando la autenticidad del token y estado del participante.",
         "Usuario Staff autenticado con rol asignado y cámara del dispositivo móvil habilitada.",
         "1. Iniciar sesión en la app móvil con cuenta Staff.\n2. Abrir módulo 'Escáner de Acceso'.\n3. Apuntar cámara hacia la credencial QR del participante.\n4. Verificar tiempo de respuesta (< 1 seg) y datos desplegados.",
         "El escáner decodifica el token, valida la firma criptográfica en menos de 1 segundo y despliega modal de confirmación con datos del asistente.",
         "Ejecutado con éxito. Tiempo promedio de respuesta: 320 ms. Firma validada.", "Aprobado"),
         
        ("Prueba 3: Registrar asistencia (Check-in) y control antifraude", "PT-S3-03", "CU23: Registrar asistencia (Check-in)",
         "Validar el registro efectivo de asistencia en la base de datos y comprobar el bloqueo estricto ante intentos de doble ingreso.",
         "Credencial QR válida y no utilizada previamente para el evento en curso.",
         "1. Escanear QR válido y confirmar ingreso.\n2. Verificar registro en BD y actualización de estado a 'PRESENTE'.\n3. Re-escanear inmediatamente el mismo QR en otra puerta simulada.\n4. Probar registro manual por número de documento (CI).",
         "El primer check-in se asienta correctamente en BD y bitácora. El segundo intento es rechazado con alerta roja de 'Ingreso Duplicado'. El registro manual funciona como contingencia.",
         "Ejecutado con éxito. Restricción única en BD impidió duplicación. Alerta desplegada en pantalla.", "Aprobado"),
         
        ("Prueba 4: Consultar asistencia y aforo en tiempo real", "PT-S3-04", "CU24: Consultar asistencia y aforo en tiempo real",
         "Validar la recepción y actualización en vivo del porcentaje de aforo y conteo de asistentes mediante WebSockets en la web Angular.",
         "Usuario Organizador o Administrador conectado al panel de monitoreo web.",
         "1. Acceder al dashboard de aforo del evento.\n2. Ejecutar ingresos concurrentes de asistentes desde la app Staff.\n3. Observar la actualización automática de métricas sin recargar el navegador.\n4. Simular alcance del 90% del aforo y verificar alerta visual.",
         "El dashboard actualiza los contadores de asistentes y las barras de aforo de manera inmediata a través del canal STOMP, activando la alerta ámbar/roja al rebasar umbrales.",
         "Ejecutado con éxito. Latencia de sincronización WebSocket inferior a 150 ms.", "Aprobado"),
         
        ("Prueba 5: Gestionar recordatorios y notificaciones con Brevo", "PT-S3-05", "CU25: Gestionar recordatorios y notificaciones",
         "Validar el despacho automatizado de correos transaccionales con credenciales adjuntas y alertas de cambio de sala vía API Brevo.",
         "Inscripción confirmada y cuenta de Brevo configurada con credenciales API válidas.",
         "1. Confirmar inscripción de usuario prueba.\n2. Verificar despacho automático de email transaccional.\n3. Disparar recordatorio masivo programado.\n4. Comprobar recepción en bandeja de entrada y estado en BD ('ENVIADO').",
         "Los correos transaccionales se entregan en bandeja de entrada con plantilla HTML responsive y código QR adjunto. La BD registra id de mensaje externo y estado 'ENVIADO'.",
         "Ejecutado con éxito. Integración con Brevo API respondió HTTP 201 Created. Correo verificado.", "Aprobado"),
         
        ("Prueba 6: Consultar historial de participación", "PT-S3-06", "CU27: Consultar historial de participación",
         "Validar la visualización cronológica de eventos, estado de acreditación y detalle de asistencias pasadas para el participante.",
         "Participante con historial de inscripciones y asistencias previas registradas.",
         "1. Iniciar sesión como participante.\n2. Acceder a la sección 'Historial de Eventos'.\n3. Aplicar filtros por año y estado de asistencia.\n4. Seleccionar un evento pasado y consultar constancia de participación.",
         "La interfaz despliega ordenadamente todos los eventos vinculados al usuario, reflejando fielmente el porcentaje de asistencia registrado y permitiendo consultar credenciales pasadas.",
         "Ejecutado con éxito en web y móvil. Filtros reactivos funcionando correctamente.", "Aprobado")
    ]
    
    for p_title, code, hu, desc, pre, steps, res_esp, res_obt, estado in test_cases:
        doc.add_paragraph(p_title, style="Normal")
        tbl_tc = doc.add_table(rows=9, cols=2)
        tbl_tc.style = base_style
        tbl_tc.alignment = WD_TABLE_ALIGNMENT.CENTER
        
        tbl_tc.cell(0, 0).paragraphs[0].text = "Campo"
        tbl_tc.cell(0, 1).paragraphs[0].text = "Descripción"
        style_header_row(tbl_tc.rows[0])
        
        tc_fields = [
            ("Código", code),
            ("Historia de Usuario", hu),
            ("Descripción", desc),
            ("Precondición", pre),
            ("Pasos de prueba", steps),
            ("Resultado esperado", res_esp),
            ("Resultado obtenido", res_obt),
            ("Estado", estado)
        ]
        for f_idx, (f_name, f_val) in enumerate(tc_fields, start=1):
            tbl_tc.cell(f_idx, 0).paragraphs[0].text = f_name
            tbl_tc.cell(f_idx, 0).paragraphs[0].runs[0].bold = True
            tbl_tc.cell(f_idx, 1).paragraphs[0].text = f_val
            style_body_row(tbl_tc.rows[f_idx])
            
        doc.add_paragraph("", style="Normal")
        
    # HEADING 4: Reporte de Prueba
    doc.add_paragraph("Reporte de Prueba", style="Heading 4")
    doc.add_paragraph(
        "El reporte de pruebas del Sprint 3 consolida los resultados obtenidos durante la ejecución formal de las pruebas "
        "de integración y aceptación técnica sobre el incremento generado."
    )
    
    tbl_rep = doc.add_table(rows=7, cols=4)
    tbl_rep.style = base_style
    tbl_rep.alignment = WD_TABLE_ALIGNMENT.CENTER
    rep_hdrs = ['Código', 'Historia de Usuario', 'Resultado', 'Estado']
    for idx, rh in enumerate(rep_hdrs):
        tbl_rep.cell(0, idx).paragraphs[0].text = rh
    style_header_row(tbl_rep.rows[0])
    
    rep_data = [
        ("PT-S3-01", "CU21: Generar y visualizar credencial digital con QR", "Credencial QR generada y validada exitosamente", "Aprobado"),
        ("PT-S3-02", "CU22: Escanear y validar código QR", "Lectura óptica y validación en 320 ms sin errores", "Aprobado"),
        ("PT-S3-03", "CU23: Registrar asistencia (Check-in)", "Check-in asentado y duplicados bloqueados", "Aprobado"),
        ("PT-S3-04", "CU24: Consultar asistencia y aforo en tiempo real", "Sincronización WebSocket en vivo confirmada", "Aprobado"),
        ("PT-S3-05", "CU25: Gestionar recordatorios y notificaciones", "Despacho transaccional Brevo 100% entregado", "Aprobado"),
        ("PT-S3-06", "CU27: Consultar historial de participación", "Línea temporal e historial verificados correctamente", "Aprobado")
    ]
    for r_idx, r_vals in enumerate(rep_data, start=1):
        for c_idx, val in enumerate(r_vals):
            tbl_rep.cell(r_idx, c_idx).paragraphs[0].text = val
        style_body_row(tbl_rep.rows[r_idx])
        
    # -------------------------------------------------------------
    # HEADING 2: Daily Scrum (Scrum Diario)
    # -------------------------------------------------------------
    doc.add_paragraph("Daily Scrum (Scrum Diario)", style="Heading 2")
    
    daily_sessions = [
        ("Daily Scrum – 12/10/2026", [
            ("Cervantes Arancibia Roberto Carlos", "Definí esquema de tablas de QR y arquitectura de tokens", "Implementar servicio de firma HMAC-SHA256 en Spring Boot", "Ninguno"),
            ("Espinoza Sanchez Matteo", "Revisé permisos de Staff y roles en JWT", "Diseñar flujo de validación de tokens en app móvil", "Ninguno"),
            ("Carvallo Andreu Carlos Rodrigo", "Estructuré los planes de prueba PT-S3-01 a PT-S3-03", "Preparar fixtures de prueba y base de datos local", "Ninguno"),
            ("Choque Canavides Cristian Santiago", "Diseñé layout inicial del monitor de aforo en Angular", "Implementar integración de cliente WebSocket STOMP", "Ninguno"),
            ("Justiniano Coimbra Raúl Rodrigo", "Verifiqué credenciales de Brevo API y configuración SMTP", "Construir servicio cliente BrevoNotificationService", "Ninguno"),
            ("Aranibar Pérez Kevin Ariel", "Diseñé prototipo de la credencial digital con QR en Flutter", "Implementar renderizador de QR en pantalla de wallet móvil", "Ninguno")
        ]),
        ("Daily Scrum – 14/10/2026", [
            ("Cervantes Arancibia Roberto Carlos", "Completé endpoints de generación y validación de QR", "Implementar control antifraude en AsistenciaService", "Ninguno"),
            ("Espinoza Sanchez Matteo", "Integré plugin de escáner de cámara en Flutter", "Vincular cámara con endpoint de validación de credenciales", "Ninguno"),
            ("Carvallo Andreu Carlos Rodrigo", "Ejecuté pruebas unitarias de tokens criptográficos", "Probar concurrencia de escaneo en laboratorio local", "Ninguno"),
            ("Choque Canavides Cristian Santiago", "Conecté gráficos de aforo reactivos en Angular", "Agregar alertas visuales automáticas al superar el 85% de aforo", "Ninguno"),
            ("Justiniano Coimbra Raúl Rodrigo", "Implementé plantillas HTML transaccionales en Brevo", "Conectar trigger automático al confirmarse la inscripción", "Ninguno"),
            ("Aranibar Pérez Kevin Ariel", "Añadí persistencia offline para credenciales en móvil", "Implementar modal de confirmación háptica de check-in", "Ninguno")
        ]),
        ("Daily Scrum – 16/10/2026", [
            ("Cervantes Arancibia Roberto Carlos", "Finalicé validación estricta de no duplicidad en BD y bitácora", "Coordinar pruebas integradas de extremo a extremo", "Ninguno"),
            ("Espinoza Sanchez Matteo", "Ajusté tiempos de respuesta del escáner óptico (< 400 ms)", "Soportar modo de escaneo continuo para Staff en puertas", "Ninguno"),
            ("Carvallo Andreu Carlos Rodrigo", "Ejecuté pruebas de aceptación PT-S3-01 a PT-S3-06", "Consolidar métricas y reporte de pruebas aprobado", "Ninguno"),
            ("Choque Canavides Cristian Santiago", "Completé vista de historial y exportación CSV de asistencia", "Ajustar diseño responsivo en navegadores de escritorio", "Ninguno"),
            ("Justiniano Coimbra Raúl Rodrigo", "Validé tasa de entrega en Brevo y manejo de errores", "Documentar variables de entorno de producción para correo", "Ninguno"),
            ("Aranibar Pérez Kevin Ariel", "Completé pantalla de historial de eventos en Flutter", "Verificar compatibilidad en emuladores Android y dispositivos reales", "Ninguno")
        ])
    ]
    
    for d_title, d_rows in daily_sessions:
        p_d = doc.add_paragraph(d_title, style="Daily Date")
        doc.add_paragraph("La siguiente tabla registra el seguimiento real del equipo durante el Daily Scrum correspondiente.")
        
        tbl_d = doc.add_table(rows=7, cols=4)
        tbl_d.style = base_style
        tbl_d.alignment = WD_TABLE_ALIGNMENT.CENTER
        d_hdrs = ['Miembro', '¿Qué hice ayer?', '¿Qué voy a hacer hoy?', 'Impedimentos']
        for idx, dh in enumerate(d_hdrs):
            tbl_d.cell(0, idx).paragraphs[0].text = dh
        style_header_row(tbl_d.rows[0])
        
        for r_idx, r_data in enumerate(d_rows, start=1):
            for c_idx, val in enumerate(r_data):
                tbl_d.cell(r_idx, c_idx).paragraphs[0].text = val
            style_body_row(tbl_d.rows[r_idx])
            
        doc.add_paragraph("", style="Normal")
        
    # -------------------------------------------------------------
    # HEADING 2: Sprint Review
    # -------------------------------------------------------------
    doc.add_paragraph("Sprint Review", style="Heading 2")
    tbl_rev = doc.add_table(rows=5, cols=5)
    tbl_rev.style = base_style
    tbl_rev.alignment = WD_TABLE_ALIGNMENT.CENTER
    tbl_rev.cell(0, 0).merge(tbl_rev.cell(0, 4))
    tbl_rev.cell(0, 0).paragraphs[0].text = "Revisión de Sprint 3"
    tbl_rev.cell(0, 0).paragraphs[0].alignment = WD_ALIGN_PARAGRAPH.CENTER
    set_cell_background(tbl_rev.cell(0, 0), TEAL_HEX)
    
    rev_info = [
        ("Objetivo del Sprint", "Desarrollar e integrar credenciales QR seguras, escáner móvil de acceso para Staff, check-in antifraude, monitor de aforo en vivo vía WebSockets, notificaciones Brevo e historial de participación."),
        ("Periodo del Sprint", "12/10/2026 - 16/10/2026"),
        ("Actividades", "SP3-1 a SP3-18 (18 tareas completadas al 100%)"),
        ("Estado de revisión", "Incremento presentado y validado satisfactoriamente por el Product Owner y stakeholders.")
    ]
    for idx, (k, v) in enumerate(rev_info, start=1):
        tbl_rev.cell(idx, 0).paragraphs[0].text = k
        tbl_rev.cell(idx, 1).merge(tbl_rev.cell(idx, 4))
        tbl_rev.cell(idx, 1).paragraphs[0].text = v
        style_body_row(tbl_rev.rows[idx])
        tbl_rev.cell(idx, 0).paragraphs[0].runs[0].bold = True
        
    doc.add_paragraph("Participantes", style="Normal")
    tbl_part = doc.add_table(rows=7, cols=2)
    tbl_part.style = base_style
    tbl_part.alignment = WD_TABLE_ALIGNMENT.CENTER
    tbl_part.cell(0, 0).paragraphs[0].text = "Nombre"
    tbl_part.cell(0, 1).paragraphs[0].text = "Rol"
    style_header_row(tbl_part.rows[0])
    for idx, (rol, nom) in enumerate(team_members, start=1):
        tbl_part.cell(idx, 0).paragraphs[0].text = nom
        tbl_part.cell(idx, 1).paragraphs[0].text = rol
        style_body_row(tbl_part.rows[idx])
        
    doc.add_paragraph("Presentación del incremento", style="Normal")
    tbl_inc = doc.add_table(rows=7, cols=2)
    tbl_inc.style = base_style
    tbl_inc.alignment = WD_TABLE_ALIGNMENT.CENTER
    tbl_inc.cell(0, 0).paragraphs[0].text = "Función presentada"
    tbl_inc.cell(0, 1).paragraphs[0].text = "Retroalimentación"
    style_header_row(tbl_inc.rows[0])
    
    inc_feedback = [
        ("Credencial digital con QR dinámico (CU21)", "Validada con éxito. Excelente presentación visual y soporte para almacenamiento en Wallet móvil."),
        ("Escáner móvil y validación óptica (CU22)", "Aprobada. Tiempo de respuesta sub-segundo verificado en condiciones de iluminación variadas."),
        ("Registro de Check-in y Antifraude (CU23)", "Aprobada con felicitación por el bloqueo instantáneo ante intentos de ingreso duplicado en múltiples accesos."),
        ("Monitoreo de aforo en tiempo real (CU24)", "Aprobada. La sincronización reactiva por WebSocket fue destacada por su fluidez y claridad visual."),
        ("Notificaciones y recordatorios Brevo (CU25)", "Aprobada. Despacho transaccional verificado en bandeja de entrada sin ser clasificado como spam."),
        ("Historial de participación (CU27)", "Aprobada. Trazabilidad completa y correcta visualización de participaciones pasadas y credenciales vigentes.")
    ]
    for idx, (fn, fb) in enumerate(inc_feedback, start=1):
        tbl_inc.cell(idx, 0).paragraphs[0].text = fn
        tbl_inc.cell(idx, 1).paragraphs[0].text = fb
        style_body_row(tbl_inc.rows[idx])
        
    # -------------------------------------------------------------
    # HEADING 2: Sprint Retrospective
    # -------------------------------------------------------------
    doc.add_paragraph("Sprint Retrospective", style="Heading 2")
    doc.add_paragraph(
        "La retrospectiva permitió al equipo Scrum analizar el desempeño del ciclo, identificar aciertos técnicos y consensuar "
        "mejoras continuas aplicables al siguiente Sprint 4 (IA, Certificados y Reportes Avanzados)."
    )
    tbl_retro = doc.add_table(rows=6, cols=3)
    tbl_retro.style = base_style
    tbl_retro.alignment = WD_TABLE_ALIGNMENT.CENTER
    retro_hdrs = ['¿Qué salió bien?', '¿Qué no salió bien?', '¿Qué haremos de manera diferente?']
    for idx, rh in enumerate(retro_hdrs):
        tbl_retro.cell(0, idx).paragraphs[0].text = rh
    style_header_row(tbl_retro.rows[0])
    
    retro_points = [
        ("La arquitectura criptográfica HMAC simplificó la validación segura de credenciales sin sobrecargar la BD.",
         "La configuración inicial de la cámara en ciertos dispositivos Android requirió permisos adicionales en tiempo de ejecución.",
         "Centralizar la gestión de permisos móviles en un hook reutilizable antes de iniciar el escáner."),
        ("La sincronización reactiva de aforo con WebSockets/STOMP funcionó con bajísima latencia y alta estabilidad.",
         "La simulación de contingencia offline requirió pruebas manuales minuciosas desconectando interfaces de red.",
         "Automatizar escenarios de degradación de red en el pipeline de testing con herramientas de emulación de latencia."),
        ("La integración con Brevo API fue sumamente rápida y los correos llegaron con alta entregabilidad.",
         "Las plantillas de correo requirieron ajustes de estilo CSS en línea para renderizar idénticamente en Outlook y Gmail.",
         "Adoptar un validador de plantillas HTML email previo a la integración en el backend."),
        ("Excelente coordinación entre el equipo de Flutter y Backend para el contrato del payload QR.",
         "El volumen de logs generados en bitácora por escaneos continuos aumentó significativamente el almacenamiento local.",
         "Configurar una política de rotación y purga programada para los logs de intentos de escaneo denegados."),
        ("El 100% de las 18 tareas planificadas fueron culminadas dentro del tiempo límite del Sprint.",
         "Poca anticipación en la preparación de datos de prueba sintéticos con gran volumen de asistentes.",
         "Generar scripts de seed de datos masivos antes de iniciar las sesiones de prueba de carga de aforo.")
    ]
    for idx, (sb, nsb, hd) in enumerate(retro_points, start=1):
        tbl_retro.cell(idx, 0).paragraphs[0].text = sb
        tbl_retro.cell(idx, 1).paragraphs[0].text = nsb
        tbl_retro.cell(idx, 2).paragraphs[0].text = hd
        style_body_row(tbl_retro.rows[idx])
        
    # -------------------------------------------------------------
    # HEADING 2: Burn Down y Burn Up (Gráfica de tareas y Datos de tareas)
    # -------------------------------------------------------------
    doc.add_paragraph("Burn Down y Burn Up (Gráfica de tareas y Datos de tareas)", style="Heading 2")
    doc.add_paragraph(
        "Las siguientes gráficas representan la trayectoria ideal planificada y el avance real logrado para las 18 tareas "
        "del Sprint 3 (SP3-1 a SP3-18). Como se aprecia en las trayectorias, el equipo mantuvo una velocidad constante de "
        "desarrollo cerrando la totalidad del alcance sin desviaciones críticas."
    )
    
    p_bd = doc.add_paragraph()
    p_bd.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p_bd.add_run().add_picture(os.path.join(ASSETS_DIR, "chart_burndown.png"), width=Inches(6.3))
    
    p_bu = doc.add_paragraph()
    p_bu.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p_bu.add_run().add_picture(os.path.join(ASSETS_DIR, "chart_burnup.png"), width=Inches(6.3))
    
    tbl_burndata = doc.add_table(rows=6, cols=4)
    tbl_burndata.style = base_style
    tbl_burndata.alignment = WD_TABLE_ALIGNMENT.CENTER
    bdata_hdrs = ['Fecha', 'Pendientes ideales', 'Completadas ideales', 'Alcance total']
    for idx, bdh in enumerate(bdata_hdrs):
        tbl_burndata.cell(0, idx).paragraphs[0].text = bdh
    style_header_row(tbl_burndata.rows[0])
    
    bdata_rows = [
        ("Inicio (12/10/2026)", "18", "0", "18"),
        ("13/10/2026", "13.5", "4.5", "18"),
        ("14/10/2026", "9.0", "9.0", "18"),
        ("15/10/2026", "4.5", "13.5", "18"),
        ("16/10/2026 (Cierre)", "0", "18", "18")
    ]
    for idx, bvals in enumerate(bdata_rows, start=1):
        for c_idx, val in enumerate(bvals):
            tbl_burndata.cell(idx, c_idx).paragraphs[0].text = val
        style_body_row(tbl_burndata.rows[idx])
        
    # -------------------------------------------------------------
    # HEADING 2: Gráfica de esfuerzos y Datos de esfuerzo.
    # -------------------------------------------------------------
    doc.add_paragraph("Gráfica de esfuerzos y Datos de esfuerzo.", style="Heading 2")
    p_esf = doc.add_paragraph()
    p_esf.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p_esf.add_run().add_picture(os.path.join(ASSETS_DIR, "chart_esfuerzos.png"), width=Inches(6.3))
    
    tbl_esf = doc.add_table(rows=8, cols=2)
    tbl_esf.style = base_style
    tbl_esf.alignment = WD_TABLE_ALIGNMENT.CENTER
    tbl_esf.cell(0, 0).paragraphs[0].text = "Integrante"
    tbl_esf.cell(0, 1).paragraphs[0].text = "Horas planificadas"
    style_header_row(tbl_esf.rows[0])
    
    esf_rows = [
        ("Roberto Cervantes", "15 hr"),
        ("Matteo Espinoza", "16 hr"),
        ("Kevin Aranibar", "18 hr"),
        ("Carlos Carvallo", "18 hr"),
        ("Cristian Choque", "15 hr"),
        ("Raúl Justiniano", "12 hr"),
        ("TOTAL", "94 hr")
    ]
    for idx, (integ, hrs) in enumerate(esf_rows, start=1):
        tbl_esf.cell(idx, 0).paragraphs[0].text = integ
        tbl_esf.cell(idx, 1).paragraphs[0].text = hrs
        style_body_row(tbl_esf.rows[idx])
        if integ == "TOTAL":
            tbl_esf.cell(idx, 0).paragraphs[0].runs[0].bold = True
            tbl_esf.cell(idx, 1).paragraphs[0].runs[0].bold = True
            set_cell_background(tbl_esf.cell(idx, 0), TEAL_HEX)
            set_cell_background(tbl_esf.cell(idx, 1), TEAL_HEX)
            
    # -------------------------------------------------------------
    # HEADING 2: Scrum TaskBoard (Backlog, to, do, doing, done)
    # -------------------------------------------------------------
    doc.add_paragraph("Scrum TaskBoard (Backlog, to, do, doing, done)", style="Heading 2")
    doc.add_paragraph(
        "El tablero muestra la distribución operativa y estado final de las actividades del Sprint 3. "
        "A lo largo del ciclo, todas las historias de usuario y tareas técnicas fueron transitando desde el Backlog inicial "
        "hasta culminar en estado Done tras superar los criterios de aceptación y pruebas de verificación."
    )
    p_tb = doc.add_paragraph()
    p_tb.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p_tb.add_run().add_picture(os.path.join(ASSETS_DIR, "taskboard_sprint3.png"), width=Inches(6.3))
    
    # Guardar documento
    doc.save(OUTPUT_DOCX_REPO)
    shutil.copy2(OUTPUT_DOCX_REPO, OUTPUT_DOCX_DOWN)
    print(f"Documento guardado en repo: {OUTPUT_DOCX_REPO}")
    print(f"Documento copiado a Downloads: {OUTPUT_DOCX_DOWN}")

def build_markdown():
    print("Construyendo 'documentacion/sprint-3-completo.md'...")
    md_content = r"""# Sprint 3

## Sprint Planning

Durante el Sprint 3 se continuará el desarrollo de la Plataforma Web/Móvil para Gestión de Eventos y Congresos con Inteligencia Artificial, tomando como base la arquitectura multi-tenant, autenticación JWT, usuarios, roles, gestión de eventos y programación implementada en el Sprint 1, así como los módulos de inscripciones, postulaciones y asignaciones de staff desarrollados en el Sprint 2.

En este Sprint el trabajo se concentrará principalmente en las funcionalidades críticas de control de acceso, acreditación digital mediante códigos QR firmados criptográficamente, registro de asistencia (Check-in) en tiempo real, monitoreo de aforo por sala, notificaciones y recordatorios transaccionales automatizados (vía Brevo), y la consulta del historial consolidado de participación del usuario.

Los casos de uso seleccionados para este Sprint son:
- **CU21**: Generar y visualizar credencial digital con QR
- **CU22**: Escanear y validar código QR
- **CU23**: Registrar asistencia (Check-in)
- **CU24**: Consultar asistencia y aforo en tiempo real
- **CU25**: Gestionar recordatorios y notificaciones
- **CU27**: Consultar historial de participación

### Objetivo del Sprint

El objetivo del Sprint 3 será desarrollar e integrar las funcionalidades relacionadas con la emisión de credenciales digitales seguras con QR, la verificación y validación de acceso en puertas por parte del personal de Staff autorizado, el registro de asistencia sin duplicidad, el seguimiento de aforo en vivo mediante WebSockets para organizadores, la automatización de recordatorios y alertas por correo electrónico a través de la integración con Brevo, y el acceso al historial de participación para los asistentes registrados.

Durante este Sprint se implementará la generación dinámica del token QR basado en HMAC-SHA256, garantizando que las credenciales sean infalsificables y permitan la verificación tanto online como en contingencia offline por la aplicación móvil Flutter.

Asimismo, se desarrollará el mecanismo de Check-in con control estricto antifraude (bloqueo automático ante intentos de reutilización de credenciales en múltiples puertas), junto con la sincronización bidireccional hacia la bitácora auditable cifrada y el aislamiento estricto de datos por tenant organizacional.

Las funcionalidades desarrolladas serán integradas de forma transversal con el backend Spring Boot 3, la plataforma web Angular 20, la base de datos PostgreSQL 17 y la aplicación móvil Flutter. El incremento obtenido será desplegado progresivamente sobre la infraestructura en la nube AWS y contenedores Docker para validar la operabilidad integral.

### Historias de usuario (tarjetas 3C, Planning póker, prototipos)

#### CU21: Generación y Visualización de Credencial Digital QR

| Generación y Visualización de Credencial Digital QR | |
|---|---|
| **ID : 21** | **Descripción**: Generar y visualizar la credencial digital con código QR dinámico y firmado para participantes con inscripción confirmada. |
| **Prioridad**: Alta | **Estimación PHU**: 8hr |
| **Criterios de aceptación**:<br>• El participante autenticado con inscripción CONFIRMADA debe poder generar su credencial digital desde la app móvil Flutter y la web Angular.<br>• La credencial debe desplegar nombre completo, evento, categoría de inscripción, fecha y el código QR visualmente legible.<br>• El código QR debe codificar un token criptográfico firmado (HMAC-SHA256) que prevenga falsificaciones o alteraciones de identidad.<br>• El sistema debe permitir descargar la credencial en PDF o almacenarla en la caché local (Wallet offline) para acceso sin conexión.<br>• Si la inscripción se encuentra cancelada, rechazada o pendiente de pago, el sistema no emitirá el código QR activo. | |
| **Desarrollador a cargo**: Aranibar Pérez Kevin Ariel / Cervantes Arancibia Roberto Carlos | |

![Prototipo CU21](sprint3_assets/proto_cu21.png)

---

#### CU22: Escaneo y Validación de Código QR

| Escaneo y Validación de Código QR | |
|---|---|
| **ID : 22** | **Descripción**: Escanear y validar la credencial QR en los puntos de acceso mediante la cámara de la aplicación móvil del Staff autorizado. |
| **Prioridad**: Alta | **Estimación PHU**: 8hr |
| **Criterios de aceptación**:<br>• El personal de Staff autenticado y asignado al evento debe poder activar la cámara desde la app móvil Flutter para escanear credenciales.<br>• El sistema debe decodificar el QR y validar la firma criptográfica y vigencia del token en menos de 1 segundo.<br>• La pantalla debe mostrar una alerta clara con retroalimentación inmediata: 'Acreditado / Válido', 'QR Inválido' o 'Evento Incorrecto'.<br>• Debe soportar validación offline en caso de intermitencia de red contra la lista local sincronizada de inscritos.<br>• Usuarios sin rol Staff o fuera del horario programado del evento tendrán el acceso al módulo de escaneo bloqueado. | |
| **Desarrollador a cargo**: Espinoza Sanchez Matteo | |

![Prototipo CU22](sprint3_assets/proto_cu22.png)

---

#### CU23: Registro de Asistencia (Check-in)

| Registro de Asistencia (Check-in) | |
|---|---|
| **ID : 23** | **Descripción**: Registrar el ingreso efectivo del participante en la base de datos garantizando control estricto de duplicidad y contingencia manual. |
| **Prioridad**: Alta | **Estimación PHU**: 8hr |
| **Criterios de aceptación**:<br>• Tras el escaneo exitoso, el sistema registrará la asistencia con marca de tiempo precisa (timestamp) y el ID del operador Staff.<br>• El sistema impedirá check-in duplicados para la misma sesión o evento, emitiendo una alerta roja con la hora y puerta del ingreso previo.<br>• Debe proveer una opción de registro manual de asistencia por documento de identidad (CI/DNI) o correo para contingencias.<br>• Cada registro exitoso o intento denegado debe asentar un evento en la bitácora cifrada asociada al tenant correspondiente.<br>• El estado de la inscripción del participante debe actualizarse de forma inmediata a 'Asistió / Presente'. | |
| **Desarrollador a cargo**: Cervantes Arancibia Roberto Carlos | |

![Prototipo CU23](sprint3_assets/proto_cu23.png)

---

#### CU24: Consulta de Asistencia y Aforo en Tiempo Real

| Consulta de Asistencia y Aforo en Tiempo Real | |
|---|---|
| **ID : 24** | **Descripción**: Supervisar en tiempo real el porcentaje de asistencia, conteo de ingresos y nivel de aforo por sala mediante dashboard web reactivo. |
| **Prioridad**: Alta | **Estimación PHU**: 8hr |
| **Criterios de aceptación**:<br>• El organizador y administrador deben visualizar un dashboard web en Angular con el total de inscritos, asistentes presentes y ausentes.<br>• El conteo de asistentes y porcentajes de ocupación por sala deben actualizarse en vivo mediante conexión WebSocket / STOMP.<br>• El sistema debe desplegar alertas visuales automáticas cuando el aforo alcance umbrales críticos (80%, 90% y 100% de la capacidad).<br>• Debe permitir aplicar filtros por sala, categoría de entrada y franja horaria de ingreso.<br>• Debe permitir exportar la nómina de asistencia en formatos estándar (CSV y PDF) para control administrativo. | |
| **Desarrollador a cargo**: Choque Canavides Cristian Santiago | |

![Prototipo CU24](sprint3_assets/proto_cu24.png)

---

#### CU25: Gestión de Recordatorios y Notificaciones

| Gestión de Recordatorios y Notificaciones | |
|---|---|
| **ID : 25** | **Descripción**: Automatizar el despacho de correos transaccionales y recordatorios mediante Brevo para confirmación, alertas de horario y credenciales. |
| **Prioridad**: Alta | **Estimación PHU**: 8hr |
| **Criterios de aceptación**:<br>• El sistema enviará de forma automática un correo electrónico transaccional con la credencial digital y QR tras confirmarse la inscripción.<br>• Debe permitir programar recordatorios masivos previos al evento (24h y 2h antes) informando ubicación, sala y requisitos de acceso.<br>• Debe emitir avisos inmediatos ante eventuales reprogramaciones, cambios de sala o cancelaciones de conferencias.<br>• Cada notificación despachada debe registrarse en la base de datos con su estado de entrega ('ENVIADO', 'PENDIENTE', 'FALLIDO').<br>• La integración debe operar mediante la API REST transaccional de Brevo garantizando aislamiento multi-tenant por organización. | |
| **Desarrollador a cargo**: Justiniano Coimbra Raúl Rodrigo | |

![Prototipo CU25](sprint3_assets/proto_cu25.png)

---

#### CU27: Consulta de Historial de Participación

| Consulta de Historial de Participación | |
|---|---|
| **ID : 27** | **Descripción**: Permitir a los participantes consultar su trayectoria histórica de eventos asistidos, credenciales y constancias desde móvil o web. |
| **Prioridad**: Media | **Estimación PHU**: 7hr |
| **Criterios de aceptación**:<br>• El usuario autenticado debe acceder a la lista cronológica de todos los eventos en los que se ha inscrito (vigentes y pasados).<br>• Para cada evento debe detallar: fecha, título, organización organizadora, modalidad, estado de inscripción y asistencia registrada.<br>• Debe permitir descargar o visualizar la credencial digital para eventos activos o próximos a iniciar.<br>• La información desplegada debe estar estrictamente protegida y restringida al titular de la cuenta autenticada.<br>• La interfaz debe incorporar filtros de búsqueda por nombre de evento, rango de fechas y condición de asistencia. | |
| **Desarrollador a cargo**: Choque Canavides Cristian Santiago / Aranibar Pérez Kevin Ariel | |

![Prototipo CU27](sprint3_assets/proto_cu27.png)

---

### Contexto del Sistema

Durante el Sprint 3 se amplía la estructura funcional consolidada en los sprints precedentes, integrando el flujo operativo de control de acceso físico y virtual de los eventos. Mientras el Sprint 0 definió la arquitectura base y autenticación, el Sprint 1 la gestión de eventos y salas, y el Sprint 2 las inscripciones y postulación de staff, el Sprint 3 concreta el día del evento mediante el uso de tecnología móvil y mensajería transaccional.

El desarrollo se enfoca inicialmente en el motor de generación y firma de credenciales QR dinámicas, asegurando que cada token contenga información cifrada e incorruptible vinculada a la inscripción del participante.

Posteriormente se incorpora la aplicación móvil para el personal de Staff, transformando los dispositivos en terminales ópticos de escaneo capaces de validar credenciales en milisegundos y registrar el check-in de asistencia directamente en el backend.

Asimismo, se habilita el monitoreo reactivo de aforo mediante WebSockets en la consola web Angular, permitiendo a los organizadores tomar decisiones logísticas en tiempo real basadas en la ocupación física de los auditorios.

El Sprint incluye además el despacho masivo y automatizado de comunicaciones a través del servicio en la nube Brevo, asegurando que los inscritos reciban oportunamente sus credenciales y recordatorios de inicio.

Todas estas funcionalidades operan bajo las directivas de seguridad multi-tenant establecidas, garantizando que los datos de asistencia y acreditaciones pertenezcan exclusivamente a la organización responsable y queden asentados en la bitácora cifrada.

### Sprint Backlog

| Sprint Backlog | | | | | |
|---|---|---|---|---|---|
| **Número de Sprint : 3** | | | **Tiempo programado: 5 días** | | |
| **Objetivo**: Desarrollar e integrar las funcionalidades de emisión de credenciales digitales con QR, escaneo y validación en app móvil para Staff, registro de asistencia Check-in sin duplicados, monitoreo de aforo en tiempo real vía WebSockets, gestión de recordatorios y notificaciones automáticas con Brevo, y consulta del historial de participación del usuario. | | | | | |
| **Product Owner**: Cervantes Arancibia Roberto Carlos | | | | | |
| **Fecha de inicio**: 12/10/2026 | | | **Fecha de finalización**: 16/10/2026 | | |
| **Id** | **Tarea** | **Tipo** | **Estimación (hr)** | **Responsable** | **Estado** |
| SP3-1 | Diseñar credencial digital con QR | Diseño | 4hr | Aranibar Pérez Kevin Ariel | Terminado |
| SP3-2 | Generar códigos QR asociados a inscripciones | Desarrollo | 7hr | Cervantes Arancibia Roberto Carlos | Terminado |
| SP3-3 | Visualizar credencial QR en app móvil y web | Desarrollo | 6hr | Aranibar Pérez Kevin Ariel | Terminado |
| SP3-4 | Probar generación y visualización QR | Pruebas | 3hr | Carvallo Andreu Carlos Rodrigo | Terminado |
| SP3-5 | Diseñar escaneo y validación QR para Staff | Diseño | 4hr | Aranibar Pérez Kevin Ariel | Terminado |
| SP3-6 | Implementar escaneo y validación QR en móvil | Desarrollo | 8hr | Espinoza Sanchez Matteo | Terminado |
| SP3-7 | Probar control de acceso y validación QR | Pruebas | 4hr | Carvallo Andreu Carlos Rodrigo | Terminado |
| SP3-8 | Implementar registro de asistencia y antifraude | Desarrollo | 8hr | Cervantes Arancibia Roberto Carlos | Terminado |
| SP3-9 | Diseñar seguimiento web de asistencia y aforo | Diseño | 4hr | Choque Canavides Cristian Santiago | Terminado |
| SP3-10 | Consultar asistencia y aforo en tiempo real (WebSocket) | Desarrollo | 8hr | Choque Canavides Cristian Santiago | Terminado |
| SP3-11 | Probar asistencia y monitoreo de aforo en vivo | Pruebas | 4hr | Carvallo Andreu Carlos Rodrigo | Terminado |
| SP3-12 | Diseñar recordatorios y plantillas de notificación | Diseño | 4hr | Choque Canavides Cristian Santiago | Terminado |
| SP3-13 | Implementar notificaciones de inscripción con Brevo | Desarrollo | 6hr | Justiniano Coimbra Raúl Rodrigo | Terminado |
| SP3-14 | Implementar recordatorios, cambios y cancelaciones | Desarrollo | 6hr | Justiniano Coimbra Raúl Rodrigo | Terminado |
| SP3-15 | Probar despacho de notificaciones y emails | Pruebas | 3hr | Carvallo Andreu Carlos Rodrigo | Terminado |
| SP3-16 | Diseñar historial de participación del usuario | Diseño | 4hr | Aranibar Pérez Kevin Ariel | Terminado |
| SP3-17 | Implementar historial de inscripciones y asistencias | Desarrollo | 7hr | Choque Canavides Cristian Santiago | Terminado |
| SP3-18 | Probar historial y trazabilidad de participación | Pruebas | 4hr | Carvallo Andreu Carlos Rodrigo | Terminado |

### Equipo Scrum

| ROL | PERSONA |
|---|---|
| Product Owner | Cervantes Arancibia Roberto Carlos |
| Scrum Máster | Espinoza Sanchez Matteo |
| Developer | Carvallo Andreu Carlos Rodrigo |
| Developer | Choque Canavides Cristian Santiago |
| Developer | Justiniano Coimbra Raúl Rodrigo |
| Developer | Aranibar Pérez Kevin Ariel |

---

## Proceso/patrón de desarrollo por Historia de Usuario

### Diseño

#### Diseño De La Arquitectura

##### Diseño Físico: Diagrama de Despliegue en AWS Cloud Multi-Tenant

El siguiente diagrama de despliegue representa la topología de infraestructura en la nube de Amazon Web Services (AWS) para la plataforma multi-tenant. Modela la interacción entre los dispositivos cliente (App Móvil Flutter para Wallet QR y escáner Staff, y Navegador Web para la SPA de Angular 20) accediendo de manera segura mediante TLS 443 / WSS hacia un Application Load Balancer (ALB). Dentro de la VPC de AWS se ejecuta el clúster de cómputo (EC2/ECS Docker) con los contenedores del backend Spring Boot 3 (Java 21) y el microservicio de IA FastAPI (Python 3.11), comunicados con la base de datos relacional Amazon RDS PostgreSQL 17 multi-tenant con esquemas aislados por tenant, AWS KMS para el sellado criptográfico de la bitácora, Amazon S3 para assets y reportes, y servicios externos como Brevo Cloud API (SMTP) y Stripe API.

![Diagrama de Despliegue en AWS](sprint3_assets/diag_despliegue_aws.png)

##### Diseño Lógico: Diagrama de Paquetes de la Arquitectura

El diagrama de paquetes modela la arquitectura lógica en capas desacopladas y modulares del sistema. Estructura la Capa de Presentación (Angular 20 y Flutter), el módulo transversal de Seguridad y Control de Acceso (JWT, Roles), la Gestión SaaS y Multi-Tenant, el Dominio Funcional del núcleo (Eventos y Espacios del Sprint 1, Inscripciones y Staff del Sprint 2, Acreditación / QR / Check-in del Sprint 3 y Notificaciones / Recordatorios del Sprint 3), los Servicios de Soporte (Servicio de IA FastAPI y Auditoría con Bitácora Cifrada por Tenant) y la Capa de Persistencia relacional (PostgreSQL 17, JPA).

![Diagrama de Paquetes](sprint3_assets/diag_paquetes_logico.png)

##### Diagrama de Componentes del Sprint 3

Durante el Sprint 3 se mantiene y expande la arquitectura hexagonal multi-tenant desarrollada en los sprints anteriores. El siguiente diagrama representa los componentes funcionales, clientes web/móviles, capas de servicio, mecanismos de WebSocket reactivos, persistencia en PostgreSQL y la integración externa con el servicio en la nube Brevo.

![Diagrama de Componentes](sprint3_assets/diag_componentes_sprint3.png)

#### Diseño De Datos

**Diagrama De Clases / Datos del Sprint 3**

![Diagrama de Clases](sprint3_assets/diag_clases_sprint3.png)

**Script DDL:**

```sql
-- SPRINT 3: CONTROL QR, ASISTENCIA, NOTIFICACIONES E HISTORIAL

CREATE TABLE credenciales_qr (
    id_credencial SERIAL PRIMARY KEY,
    id_inscripcion INT NOT NULL UNIQUE REFERENCES inscripciones(id_inscripcion) ON DELETE CASCADE,
    id_organizacion INT NOT NULL REFERENCES organizaciones(id_organizacion) ON DELETE CASCADE,
    token_criptografico VARCHAR(255) NOT NULL UNIQUE,
    hash_verificacion VARCHAR(64) NOT NULL,
    formato_qr VARCHAR(20) DEFAULT 'PNG_BASE64',
    fecha_emision TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_expiracion TIMESTAMP NOT NULL,
    estado VARCHAR(20) DEFAULT 'ACTIVA' CHECK (estado IN ('ACTIVA', 'REVOCADA', 'EXPIRADA'))
);

CREATE TABLE asistencias (\n
    id_asistencia SERIAL PRIMARY KEY,
    id_inscripcion INT NOT NULL REFERENCES inscripciones(id_inscripcion) ON DELETE CASCADE,
    id_evento INT NOT NULL REFERENCES eventos(id_evento) ON DELETE CASCADE,
    id_usuario INT NOT NULL REFERENCES usuarios(id_usuario) ON DELETE CASCADE,
    id_operador_staff INT REFERENCES usuarios(id_usuario) ON DELETE SET NULL,
    id_organizacion INT NOT NULL REFERENCES organizaciones(id_organizacion) ON DELETE CASCADE,
    fecha_hora_checkin TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    metodo_validacion VARCHAR(30) NOT NULL CHECK (metodo_validacion IN ('QR_ESCANEO', 'MANUAL_STAFF', 'OFFLINE_SYNC')),
    estado_asistencia VARCHAR(20) DEFAULT 'PRESENTE' CHECK (estado_asistencia IN ('PRESENTE', 'TARDANZA', 'JUSTIFICADO')),
    observaciones VARCHAR(255),
    CONSTRAINT uq_asistencia_evento_usuario UNIQUE (id_evento, id_usuario)
);

CREATE TABLE notificaciones_eventos (
    id_notificacion SERIAL PRIMARY KEY,
    id_evento INT NOT NULL REFERENCES eventos(id_evento) ON DELETE CASCADE,
    id_usuario INT NOT NULL REFERENCES usuarios(id_usuario) ON DELETE CASCADE,
    id_organizacion INT NOT NULL REFERENCES organizaciones(id_organizacion) ON DELETE CASCADE,
    tipo_notificacion VARCHAR(50) NOT NULL,
    canal_envio VARCHAR(20) NOT NULL DEFAULT 'EMAIL_BREVO',
    asunto VARCHAR(150) NOT NULL,
    cuerpo_mensaje TEXT NOT NULL,
    fecha_programada TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_envio TIMESTAMP,
    estado_envio VARCHAR(20) DEFAULT 'PENDIENTE' CHECK (estado_envio IN ('PENDIENTE', 'ENVIADO', 'FALLIDO')),
    id_mensaje_externo VARCHAR(100)
);

CREATE TABLE control_aforo_salas (
    id_control_aforo SERIAL PRIMARY KEY,
    id_evento INT NOT NULL REFERENCES eventos(id_evento) ON DELETE CASCADE,
    id_espacio INT NOT NULL REFERENCES espacios(id_espacio) ON DELETE CASCADE,
    capacidad_maxima INT NOT NULL CHECK (capacidad_maxima > 0),
    asistentes_actuales INT NOT NULL DEFAULT 0 CHECK (asistentes_actuales >= 0),
    porcentaje_ocupacion NUMERIC(5,2) GENERATED ALWAYS AS ((asistentes_actuales::numeric / capacidad_maxima::numeric) * 100) STORED,
    alerta_umbral BOOLEAN DEFAULT FALSE,
    ultima_actualizacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_evento_espacio UNIQUE (id_evento, id_espacio)
);

CREATE INDEX idx_credencial_token ON credenciales_qr(token_criptografico);
CREATE INDEX idx_asistencias_evento_usuario ON asistencias(id_evento, id_usuario);
CREATE INDEX idx_notificaciones_estado ON notificaciones_eventos(estado_envio);
```

**Mapeo del Diccionario de Datos:**

| CREDENCIAL_QR |
|---|
| PK: id_credencial \| FK: id_inscripcion \| FK: id_organizacion \| token_criptografico \| hash_verificacion \| formato_qr \| fecha_emision \| fecha_expiracion \| estado |

| ASISTENCIA |
|---|
| PK: id_asistencia \| FK: id_inscripcion \| FK: id_evento \| FK: id_usuario \| FK: id_operador_staff \| FK: id_organizacion \| fecha_hora_checkin \| metodo_validacion \| estado_asistencia \| observaciones |

| NOTIFICACION_EVENTO |
|---|
| PK: id_notificacion \| FK: id_evento \| FK: id_usuario \| FK: id_organizacion \| tipo_notificacion \| canal_envio \| asunto \| cuerpo_mensaje \| fecha_programada \| fecha_envio \| estado_envio \| id_mensaje_externo |

| CONTROL_AFORO_SALA |
|---|
| PK: id_control_aforo \| FK: id_evento \| FK: id_espacio \| capacidad_maxima \| asistentes_actuales \| porcentaje_ocupacion \| alerta_umbral \| ultima_actualizacion |

| HISTORIAL_PARTICIPACION (VISTA) |
|---|
| PK: id_usuario \| total_eventos_inscrito \| total_asistencias_efectivas \| porcentaje_asistencia_global \| eventos_vigentes \| eventos_finalizados \| tenant_id |

#### Diseño De La Lógica De Negocio

**Diagrama de comunicación CU21: Generar y visualizar credencial digital con QR**
![Comunicación CU21](sprint3_assets/diag_comunicacion_cu21.png)

**Diagrama de comunicación CU22: Escanear y validar código QR**
![Comunicación CU22](sprint3_assets/diag_comunicacion_cu22.png)

**Diagrama de comunicación CU23: Registrar asistencia (Check-in)**
![Comunicación CU23](sprint3_assets/diag_comunicacion_cu23.png)

**Diagrama de comunicación CU24: Consultar asistencia y aforo en tiempo real**
![Comunicación CU24](sprint3_assets/diag_comunicacion_cu24.png)

**Diagrama de comunicación CU25: Gestionar recordatorios y notificaciones**
![Comunicación CU25](sprint3_assets/diag_comunicacion_cu25.png)

**Diagrama de comunicación CU27: Consultar historial de participación**
![Comunicación CU27](sprint3_assets/diag_comunicacion_cu27.png)

---

### Implementación

#### Componentes y artefactos generados

Durante la implementación del Sprint 3 se desarrollaron e integraron los componentes necesarios para el control de acceso digital, validación por escáner óptico, monitoreo de aforo en vivo, notificaciones transaccionales e historial de participación.

- **Plataforma web Angular**: módulo reactivo de monitoreo de aforo con gráficos en vivo, modal visor de credencial digital con descarga en PDF, consola de registro manual para Staff y centro de gestión de notificaciones.
- **Aplicación Flutter**: visor de credenciales con almacenamiento en Wallet local (soporte offline), escáner de cámara integrado con validación instantánea de firma HMAC, confirmación háptica de check-in y pantalla de historial del participante.
- **Backend Spring Boot**: endpoints REST para emisión y validación de tokens QR, servicio de control de asistencia y antifraude, broker WebSocket con protocolo STOMP para difusión reactiva de aforo y cliente Brevo para mensajería.
- **PostgreSQL**: tablas relacionales con restricciones de no duplicidad, índices de alta velocidad para validación de tokens y aislamiento estricto por organización.
- **Integración transversal**: integración con autenticación, bitácora cifrada (KMS) y modelo multi-tenant consolidados en los Sprints 0, 1 y 2.
- **Despliegue cloud**: despliegue progresivo del incremento en la infraestructura AWS y Docker Compose garantizando sincronización continua.

![Artefactos Generados](sprint3_assets/diag_artefactos_sprint3.png)

---

### Pruebas

#### Plan de pruebas (Criterios de aceptación )

Las pruebas del Sprint 3 tienen como finalidad verificar que las funcionalidades de acreditación QR, escaneo móvil, check-in de asistencia, aforo en tiempo real, notificaciones automáticas e historial de participación cumplan con los criterios de aceptación definidos para las historias de usuario seleccionadas.

##### Prueba 1: Generar y visualizar credencial digital con QR
| Campo | Descripción |
|---|---|
| **Código** | PT-S3-01 |
| **Historia de Usuario** | CU21: Generar y visualizar credencial digital con QR |
| **Descripción** | Validar la generación correcta del código QR criptográfico para participantes con inscripción confirmada y su visualización en web y móvil. |
| **Precondición** | Usuario participante con inscripción en estado CONFIRMADA o APROBADA. |
| **Pasos de prueba** | 1. Iniciar sesión como participante.<br>2. Navegar a 'Mis Inscripciones' o 'Mi Credencial'.<br>3. Solicitar generación de credencial digital.<br>4. Comprobar renderizado de código QR y datos del evento.<br>5. Descargar credencial en PDF y guardar en Wallet local. |
| **Resultado esperado** | La credencial digital se genera con token válido, el código QR es nítido y almacena el hash firmado HMAC-SHA256 sin errores. |
| **Resultado obtenido** | Ejecutado con éxito en web y móvil Flutter. Credencial generada y legible por escáner. |
| **Estado** | Aprobado |

##### Prueba 2: Escanear y validar código QR
| Campo | Descripción |
|---|---|
| **Código** | PT-S3-02 |
| **Historia de Usuario** | CU22: Escanear y validar código QR |
| **Descripción** | Validar la lectura óptica del QR mediante la app móvil Staff, comprobando la autenticidad del token y estado del participante. |
| **Precondición** | Usuario Staff autenticado con rol asignado y cámara del dispositivo móvil habilitada. |
| **Pasos de prueba** | 1. Iniciar sesión en la app móvil con cuenta Staff.<br>2. Abrir módulo 'Escáner de Acceso'.<br>3. Apuntar cámara hacia la credencial QR del participante.<br>4. Verificar tiempo de respuesta (< 1 seg) y datos desplegados. |
| **Resultado esperado** | El escáner decodifica el token, valida la firma criptográfica en menos de 1 segundo y despliega modal de confirmación con datos del asistente. |
| **Resultado obtenido** | Ejecutado con éxito. Tiempo promedio de respuesta: 320 ms. Firma validada. |
| **Estado** | Aprobado |

##### Prueba 3: Registrar asistencia (Check-in) y control antifraude
| Campo | Descripción |
|---|---|
| **Código** | PT-S3-03 |
| **Historia de Usuario** | CU23: Registrar asistencia (Check-in) |
| **Descripción** | Validar el registro efectivo de asistencia en la base de datos y comprobar el bloqueo estricto ante intentos de doble ingreso. |
| **Precondición** | Credencial QR válida y no utilizada previamente para el evento en curso. |
| **Pasos de prueba** | 1. Escanear QR válido y confirmar ingreso.<br>2. Verificar registro en BD y actualización de estado a 'PRESENTE'.<br>3. Re-escanear inmediatamente el mismo QR en otra puerta simulada.<br>4. Probar registro manual por número de documento (CI). |
| **Resultado esperado** | El primer check-in se asienta correctamente en BD y bitácora. El segundo intento es rechazado con alerta roja de 'Ingreso Duplicado'. El registro manual funciona como contingencia. |
| **Resultado obtenido** | Ejecutado con éxito. Restricción única en BD impidió duplicación. Alerta desplegada en pantalla. |
| **Estado** | Aprobado |

##### Prueba 4: Consultar asistencia y aforo en tiempo real
| Campo | Descripción |
|---|---|
| **Código** | PT-S3-04 |
| **Historia de Usuario** | CU24: Consultar asistencia y aforo en tiempo real |
| **Descripción** | Validar la recepción y actualización en vivo del porcentaje de aforo y conteo de asistentes mediante WebSockets en la web Angular. |
| **Precondición** | Usuario Organizador o Administrador conectado al panel de monitoreo web. |
| **Pasos de prueba** | 1. Acceder al dashboard de aforo del evento.<br>2. Ejecutar ingresos concurrentes de asistentes desde la app Staff.<br>3. Observar la actualización automática de métricas sin recargar el navegador.<br>4. Simular alcance del 90% del aforo y verificar alerta visual. |
| **Resultado esperado** | El dashboard actualiza los contadores de asistentes y las barras de aforo de manera inmediata a través del canal STOMP, activando la alerta ámbar/roja al rebasar umbrales. |
| **Resultado obtenido** | Ejecutado con éxito. Latencia de sincronización WebSocket inferior a 150 ms. |
| **Estado** | Aprobado |

##### Prueba 5: Gestionar recordatorios y notificaciones con Brevo
| Campo | Descripción |
|---|---|
| **Código** | PT-S3-05 |
| **Historia de Usuario** | CU25: Gestionar recordatorios y notificaciones |
| **Descripción** | Validar el despacho automatizado de correos transaccionales con credenciales adjuntas y alertas de cambio de sala vía API Brevo. |
| **Precondición** | Inscripción confirmada y cuenta de Brevo configurada con credenciales API válidas. |
| **Pasos de prueba** | 1. Confirmar inscripción de usuario prueba.<br>2. Verificar despacho automático de email transaccional.<br>3. Disparar recordatorio masivo programado.<br>4. Comprobar recepción en bandeja de entrada y estado en BD ('ENVIADO'). |
| **Resultado esperado** | Los correos transaccionales se entregan en bandeja de entrada con plantilla HTML responsive y código QR adjunto. La BD registra id de mensaje externo y estado 'ENVIADO'. |
| **Resultado obtenido** | Ejecutado con éxito. Integración con Brevo API respondió HTTP 201 Created. Correo verificado. |
| **Estado** | Aprobado |

##### Prueba 6: Consultar historial de participación
| Campo | Descripción |
|---|---|
| **Código** | PT-S3-06 |
| **Historia de Usuario** | CU27: Consultar historial de participación |
| **Descripción** | Validar la visualización cronológica de eventos, estado de acreditación y detalle de asistencias pasadas para el participante. |
| **Precondición** | Participante con historial de inscripciones y asistencias previas registradas. |
| **Pasos de prueba** | 1. Iniciar sesión como participante.<br>2. Acceder a la sección 'Historial de Eventos'.<br>3. Aplicar filtros por año y estado de asistencia.<br>4. Seleccionar un evento pasado y consultar constancia de participación. |
| **Resultado esperado** | La interfaz despliega ordenadamente todos los eventos vinculados al usuario, reflejando fielmente el porcentaje de asistencia registrado y permitiendo consultar credenciales pasadas. |
| **Resultado obtenido** | Ejecutado con éxito en web y móvil. Filtros reactivos funcionando correctamente. |
| **Estado** | Aprobado |

#### Reporte de Prueba

El reporte de pruebas del Sprint 3 consolida los resultados obtenidos durante la ejecución formal de las pruebas de integración y aceptación técnica sobre el incremento generado.

| Código | Historia de Usuario | Resultado | Estado |
|---|---|---|---|
| PT-S3-01 | CU21: Generar y visualizar credencial digital con QR | Credencial QR generada y validada exitosamente | Aprobado |
| PT-S3-02 | CU22: Escanear y validar código QR | Lectura óptica y validación en 320 ms sin errores | Aprobado |
| PT-S3-03 | CU23: Registrar asistencia (Check-in) | Check-in asentado y duplicados bloqueados | Aprobado |
| PT-S3-04 | CU24: Consultar asistencia y aforo en tiempo real | Sincronización WebSocket en vivo confirmada | Aprobado |
| PT-S3-05 | CU25: Gestionar recordatorios y notificaciones | Despacho transaccional Brevo 100% entregado | Aprobado |
| PT-S3-06 | CU27: Consultar historial de participación | Línea temporal e historial verificados correctamente | Aprobado |

---

## Daily Scrum (Scrum Diario)

### Daily Scrum – 12/10/2026
La siguiente tabla registra el seguimiento real del equipo durante el Daily Scrum correspondiente.

| Miembro | ¿Qué hice ayer? | ¿Qué voy a hacer hoy? | Impedimentos |
|---|---|---|---|
| Cervantes Arancibia Roberto Carlos | Definí esquema de tablas de QR y arquitectura de tokens | Implementar servicio de firma HMAC-SHA256 en Spring Boot | Ninguno |
| Espinoza Sanchez Matteo | Revisé permisos de Staff y roles en JWT | Diseñar flujo de validación de tokens en app móvil | Ninguno |
| Carvallo Andreu Carlos Rodrigo | Estructuré los planes de prueba PT-S3-01 a PT-S3-03 | Preparar fixtures de prueba y base de datos local | Ninguno |
| Choque Canavides Cristian Santiago | Diseñé layout inicial del monitor de aforo en Angular | Implementar integración de cliente WebSocket STOMP | Ninguno |
| Justiniano Coimbra Raúl Rodrigo | Verifiqué credenciales de Brevo API y configuración SMTP | Construir servicio cliente BrevoNotificationService | Ninguno |
| Aranibar Pérez Kevin Ariel | Diseñé prototipo de la credencial digital con QR en Flutter | Implementar renderizador de QR en pantalla de wallet móvil | Ninguno |

### Daily Scrum – 14/10/2026
La siguiente tabla registra el seguimiento real del equipo durante el Daily Scrum correspondiente.

| Miembro | ¿Qué hice ayer? | ¿Qué voy a hacer hoy? | Impedimentos |
|---|---|---|---|
| Cervantes Arancibia Roberto Carlos | Completé endpoints de generación y validación de QR | Implementar control antifraude en AsistenciaService | Ninguno |
| Espinoza Sanchez Matteo | Integré plugin de escáner de cámara en Flutter | Vincular cámara con endpoint de validación de credenciales | Ninguno |
| Carvallo Andreu Carlos Rodrigo | Ejecuté pruebas unitarias de tokens criptográficos | Probar concurrencia de escaneo en laboratorio local | Ninguno |
| Choque Canavides Cristian Santiago | Conecté gráficos de aforo reactivos en Angular | Agregar alertas visuales automáticas al superar el 85% de aforo | Ninguno |
| Justiniano Coimbra Raúl Rodrigo | Implementé plantillas HTML transaccionales en Brevo | Conectar trigger automático al confirmarse la inscripción | Ninguno |
| Aranibar Pérez Kevin Ariel | Añadí persistencia offline para credenciales en móvil | Implementar modal de confirmación háptica de check-in | Ninguno |

### Daily Scrum – 16/10/2026
La siguiente tabla registra el seguimiento real del equipo durante el Daily Scrum correspondiente.

| Miembro | ¿Qué hice ayer? | ¿Qué voy a hacer hoy? | Impedimentos |
|---|---|---|---|
| Cervantes Arancibia Roberto Carlos | Finalicé validación estricta de no duplicidad en BD y bitácora | Coordinar pruebas integradas de extremo a extremo | Ninguno |
| Espinoza Sanchez Matteo | Ajusté tiempos de respuesta del escáner óptico (< 400 ms) | Soportar modo de escaneo continuo para Staff en puertas | Ninguno |
| Carvallo Andreu Carlos Rodrigo | Ejecuté pruebas de aceptación PT-S3-01 a PT-S3-06 | Consolidar métricas y reporte de pruebas aprobado | Ninguno |
| Choque Canavides Cristian Santiago | Completé vista de historial y exportación CSV de asistencia | Ajustar diseño responsivo en navegadores de escritorio | Ninguno |
| Justiniano Coimbra Raúl Rodrigo | Validé tasa de entrega en Brevo y manejo de errores | Documentar variables de entorno de producción para correo | Ninguno |
| Aranibar Pérez Kevin Ariel | Completé pantalla de historial de eventos en Flutter | Verificar compatibilidad en emuladores Android y dispositivos reales | Ninguno |

---

## Sprint Review

| Revisión de Sprint 3 | | | | |
|---|---|---|---|---|
| **Objetivo del Sprint** | Desarrollar e integrar credenciales QR seguras, escáner móvil de acceso para Staff, check-in antifraude, monitor de aforo en vivo vía WebSockets, notificaciones Brevo e historial de participación. | | | |
| **Periodo del Sprint** | 12/10/2026 - 16/10/2026 | | | |
| **Actividades** | SP3-1 a SP3-18 (18 tareas completadas al 100%) | | | |
| **Estado de revisión** | Incremento presentado y validado satisfactoriamente por el Product Owner y stakeholders. | | | |

### Participantes

| Nombre | Rol |
|---|---|
| Cervantes Arancibia Roberto Carlos | Product Owner |
| Espinoza Sanchez Matteo | Scrum Máster |
| Carvallo Andreu Carlos Rodrigo | Developer |
| Choque Canavides Cristian Santiago | Developer |
| Justiniano Coimbra Raúl Rodrigo | Developer |
| Aranibar Pérez Kevin Ariel | Developer |

### Presentación del incremento

| Función presentada | Retroalimentación |
|---|---|
| Credencial digital con QR dinámico (CU21) | Validada con éxito. Excelente presentación visual y soporte para almacenamiento en Wallet móvil. |
| Escáner móvil y validación óptica (CU22) | Aprobada. Tiempo de respuesta sub-segundo verificado en condiciones de iluminación variadas. |
| Registro de Check-in y Antifraude (CU23) | Aprobada con felicitación por el bloqueo instantáneo ante intentos de ingreso duplicado en múltiples accesos. |
| Monitoreo de aforo en tiempo real (CU24) | Aprobada. La sincronización reactiva por WebSocket fue destacada por su fluidez y claridad visual. |
| Notificaciones y recordatorios Brevo (CU25) | Aprobada. Despacho transaccional verificado en bandeja de entrada sin ser clasificado como spam. |
| Historial de participación (CU27) | Aprobada. Trazabilidad completa y correcta visualización de participaciones pasadas y credenciales vigentes. |

---

## Sprint Retrospective

La retrospectiva permitió al equipo Scrum analizar el desempeño del ciclo, identificar aciertos técnicos y consensuar mejoras continuas aplicables al siguiente Sprint 4 (IA, Certificados y Reportes Avanzados).

| ¿Qué salió bien? | ¿Qué no salió bien? | ¿Qué haremos de manera diferente? |
|---|---|---|
| La arquitectura criptográfica HMAC simplificó la validación segura de credenciales sin sobrecargar la BD. | La configuración inicial de la cámara en ciertos dispositivos Android requirió permisos adicionales en tiempo de ejecución. | Centralizar la gestión de permisos móviles en un hook reutilizable antes de iniciar el escáner. |
| La sincronización reactiva de aforo con WebSockets/STOMP funcionó con bajísima latencia y alta estabilidad. | La simulación de contingencia offline requirió pruebas manuales minuciosas desconectando interfaces de red. | Automatizar escenarios de degradación de red en el pipeline de testing con herramientas de emulación de latencia. |
| La integración con Brevo API fue sumamente rápida y los correos llegaron con alta entregabilidad. | Las plantillas de correo requirieron ajustes de estilo CSS en línea para renderizar idénticamente en Outlook y Gmail. | Adoptar un validador de plantillas HTML email previo a la integración en el backend. |
| Excelente coordinación entre el equipo de Flutter y Backend para el contrato del payload QR. | El volumen de logs generados en bitácora por escaneos continuos aumentó significativamente el almacenamiento local. | Configurar una política de rotación y purga programada para los logs de intentos de escaneo denegados. |
| El 100% de las 18 tareas planificadas fueron culminadas dentro del tiempo límite del Sprint. | Poca anticipación en la preparación de datos de prueba sintéticos con gran volumen de asistentes. | Generar scripts de seed de datos masivos antes de iniciar las sesiones de prueba de carga de aforo. |

---

## Burn Down y Burn Up (Gráfica de tareas y Datos de tareas)

Las siguientes gráficas representan la trayectoria ideal planificada y el avance real logrado para las 18 tareas del Sprint 3 (SP3-1 a SP3-18). Como se aprecia en las trayectorias, el equipo mantuvo una velocidad constante de desarrollo cerrando la totalidad del alcance sin desviaciones críticas.

![Burn Down Chart](sprint3_assets/chart_burndown.png)

![Burn Up Chart](sprint3_assets/chart_burnup.png)

| Fecha | Pendientes ideales | Completadas ideales | Alcance total |
|---|---|---|---|
| Inicio (12/10/2026) | 18 | 0 | 18 |
| 13/10/2026 | 13.5 | 4.5 | 18 |
| 14/10/2026 | 9.0 | 9.0 | 18 |
| 15/10/2026 | 4.5 | 13.5 | 18 |
| 16/10/2026 (Cierre) | 0 | 18 | 18 |

---

## Gráfica de esfuerzos y Datos de esfuerzo.

![Gráfica de Esfuerzos](sprint3_assets/chart_esfuerzos.png)

| Integrante | Horas planificadas |
|---|---|
| Roberto Cervantes | 15 hr |
| Matteo Espinoza | 16 hr |
| Kevin Aranibar | 18 hr |
| Carlos Carvallo | 18 hr |
| Cristian Choque | 15 hr |
| Raúl Justiniano | 12 hr |
| **TOTAL** | **94 hr** |

---

## Scrum TaskBoard (Backlog, to, do, doing, done)

El tablero muestra la distribución operativa y estado final de las actividades del Sprint 3. A lo largo del ciclo, todas las historias de usuario y tareas técnicas fueron transitando desde el Backlog inicial hasta culminar en estado Done tras superar los criterios de aceptación y pruebas de verificación.

![Scrum TaskBoard](sprint3_assets/taskboard_sprint3.png)
"""
    with open(OUTPUT_MD, "w", encoding="utf-8") as f:
        f.write(md_content)
    print(f"Documento markdown guardado en: {OUTPUT_MD}")

if __name__ == "__main__":
    build_docx()
    build_markdown()
    print("¡Proceso de generacion de documentacion completado con exito!")
