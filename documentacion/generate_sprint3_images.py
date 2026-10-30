# -*- coding: utf-8 -*-
"""
Generador de diagramas, prototipos y graficas para la documentacion del Sprint 3.
Plataforma Web/Movil para Gestion de Eventos y Congresos con IA.
Grupo #11 - Sistemas de Informacion II - UAGRM.
"""

import os
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
import matplotlib.patches as patches
from PIL import Image, ImageDraw, ImageFont
import numpy as np

OUTPUT_DIR = r"c:\Proyecto-GestionEventos\plataforma-gestion-eventos\documentacion\sprint3_assets"
os.makedirs(OUTPUT_DIR, exist_ok=True)

# -------------------------------------------------------------
# 1. GRAFICAS AGIL (BURNDOWN, BURNUP, ESFUERZOS, TASKBOARD)
# -------------------------------------------------------------

def generar_burndown():
    fig, ax = plt.subplots(figsize=(8, 4.5), dpi=150)
    dias = ['Inicio (12/10)', 'Dia 1 (13/10)', 'Dia 2 (14/10)', 'Dia 3 (15/10)', 'Cierre (16/10)']
    ideal = [18, 13.5, 9.0, 4.5, 0]
    real = [18, 14, 8, 3, 0]

    ax.plot(dias, ideal, color='#153D63', linestyle='--', marker='o', linewidth=2.5, label='Linea Ideal (18 tareas)')
    ax.plot(dias, real, color='#2E7D32', marker='s', linewidth=2.5, label='Avance Real')
    ax.fill_between(dias, real, color='#81C784', alpha=0.2)

    for i, txt in enumerate(real):
        ax.annotate(f"{txt} tareas", (dias[i], real[i] + 0.6), ha='center', fontsize=9, fontweight='bold', color='#1B5E20')

    ax.set_title('Sprint 3: Grafica Burn Down de Tareas (SP3-1 a SP3-18)', fontsize=13, fontweight='bold', color='#153D63', pad=12)
    ax.set_ylabel('Tareas Pendientes', fontsize=11, fontweight='bold', color='#153D63')
    ax.set_ylim(-1, 21)
    ax.grid(True, linestyle=':', alpha=0.6)
    ax.legend(loc='upper right', frameon=True, facecolor='#F5F7FA')
    plt.tight_layout()
    path = os.path.join(OUTPUT_DIR, "chart_burndown.png")
    fig.savefig(path)
    plt.close(fig)
    print("BurnDown generado:", path)

def generar_burnup():
    fig, ax = plt.subplots(figsize=(8, 4.5), dpi=150)
    dias = ['Inicio (12/10)', 'Dia 1 (13/10)', 'Dia 2 (14/10)', 'Dia 3 (15/10)', 'Cierre (16/10)']
    alcance_total = [18, 18, 18, 18, 18]
    completadas = [0, 4, 10, 15, 18]

    ax.plot(dias, alcance_total, color='#C62828', linestyle='-', linewidth=2.5, marker='D', label='Alcance Total (18 tareas)')
    ax.plot(dias, completadas, color='#153D63', linestyle='-', marker='o', linewidth=2.5, label='Tareas Completadas')
    ax.fill_between(dias, completadas, color='#A8CFC8', alpha=0.35)

    for i, txt in enumerate(completadas):
        ax.annotate(f"{txt}", (dias[i], completadas[i] + 0.6), ha='center', fontsize=9, fontweight='bold', color='#153D63')

    ax.set_title('Sprint 3: Grafica Burn Up de Tareas Completadas', fontsize=13, fontweight='bold', color='#153D63', pad=12)
    ax.set_ylabel('Tareas Acumuladas', fontsize=11, fontweight='bold', color='#153D63')
    ax.set_ylim(-1, 21)
    ax.grid(True, linestyle=':', alpha=0.6)
    ax.legend(loc='lower right', frameon=True, facecolor='#F5F7FA')
    plt.tight_layout()
    path = os.path.join(OUTPUT_DIR, "chart_burnup.png")
    fig.savefig(path)
    plt.close(fig)
    print("BurnUp generado:", path)

def generar_esfuerzos():
    fig, ax = plt.subplots(figsize=(8, 4.5), dpi=150)
    integrantes = [
        'Roberto Cervantes (PO/Backend)',
        'Matteo Espinoza (SM/Seguridad)',
        'Carlos Carvallo (Backend/QA)',
        'Cristian Choque (Frontend Web)',
        'Raul Justiniano (Integracion Brevo)',
        'Kevin Aranibar (Movil Flutter)'
    ]
    horas = [15, 16, 18, 15, 12, 18]
    colores = ['#153D63', '#2A6F97', '#468FAF', '#61A5C2', '#89C2D9', '#A8CFC8']

    y_pos = np.arange(len(integrantes))
    bars = ax.barh(y_pos, horas, align='center', color=colores, edgecolor='#012A4A', linewidth=1.2, height=0.65)
    ax.set_yticks(y_pos)
    ax.set_yticklabels(integrantes, fontsize=9.5, fontweight='bold', color='#153D63')
    ax.invert_yaxis()
    ax.set_xlabel('Horas Planificadas (PHU)', fontsize=11, fontweight='bold', color='#153D63')
    ax.set_xlim(0, 22)
    ax.set_title('Sprint 3: Distribucion de Esfuerzos por Integrante (Total: 94 hr)', fontsize=13, fontweight='bold', color='#153D63', pad=12)

    for bar in bars:
        w = bar.get_width()
        ax.annotate(f'{w} hr',
                    xy=(w, bar.get_y() + bar.get_height() / 2),
                    xytext=(6, 0), textcoords="offset points",
                    ha='left', va='center', fontsize=9.5, fontweight='bold', color='#012A4A')

    ax.grid(axis='x', linestyle=':', alpha=0.6)
    plt.tight_layout()
    path = os.path.join(OUTPUT_DIR, "chart_esfuerzos.png")
    fig.savefig(path)
    plt.close(fig)
    print("Grafica de esfuerzos generada:", path)

def generar_taskboard():
    fig, ax = plt.subplots(figsize=(10, 5.5), dpi=150)
    ax.set_xlim(0, 100)
    ax.set_ylim(0, 60)
    ax.axis('off')

    col_names = ['BACKLOG (HU)', 'TO DO (Pendiente)', 'IN PROGRESS (En Curso)', 'DONE (Terminado)']
    col_colors = ['#E2E8F0', '#FEF3C7', '#DBEAFE', '#DCFCE7']
    header_colors = ['#475569', '#D97706', '#2563EB', '#16A34A']
    col_x = [2, 26, 50, 74]
    col_w = 23

    for i in range(4):
        # Column body
        rect = patches.FancyBboxPatch((col_x[i], 2), col_w, 52, boxstyle="round,pad=0.5",
                                      facecolor=col_colors[i], edgecolor='#94A3B8', linewidth=1.5)
        ax.add_patch(rect)
        # Header box
        hdr = patches.FancyBboxPatch((col_x[i], 48), col_w, 6, boxstyle="round,pad=0.3",
                                     facecolor=header_colors[i], edgecolor='none')
        ax.add_patch(hdr)
        ax.text(col_x[i] + col_w/2, 51, col_names[i], color='white', ha='center', va='center',
                fontsize=9.5, fontweight='bold')

    # Cards in Backlog
    hu_cards = [
        "CU21: Credencial QR\n[Participante / Movil]",
        "CU22: Escaneo y Validacion\n[Staff / Movil]",
        "CU23: Check-in Asistencia\n[Staff / Web-Movil]",
        "CU24: Monitoreo Aforo\n[Organizador / Web]",
        "CU25: Notificaciones Brevo\n[Sistema / Transversal]",
        "CU27: Historial Participante\n[Participante / Movil-Web]"
    ]
    for idx, card in enumerate(hu_cards[:5]):
        y = 40 - idx * 8.5
        box = patches.FancyBboxPatch((col_x[0] + 1, y), col_w - 2, 7, boxstyle="round,pad=0.2",
                                     facecolor='white', edgecolor='#64748B', linewidth=1)
        ax.add_patch(box)
        ax.text(col_x[0] + 2, y + 3.5, card, fontsize=7.5, va='center', color='#0F172A', fontweight='bold')

    # Cards in To Do
    todo_cards = [
        "SP3-12: Diseno notif. (4h)",
        "SP3-13: Notif. inscrip. (6h)",
        "SP3-14: Recordatorios (6h)",
        "SP3-15: Pruebas notif. (3h)",
        "SP3-16: Diseno historial (4h)",
        "SP3-17: Impl. historial (7h)"
    ]
    for idx, card in enumerate(todo_cards[:5]):
        y = 40 - idx * 8.5
        box = patches.FancyBboxPatch((col_x[1] + 1, y), col_w - 2, 7, boxstyle="round,pad=0.2",
                                     facecolor='white', edgecolor='#D97706', linewidth=1)
        ax.add_patch(box)
        ax.text(col_x[1] + 2, y + 3.5, card, fontsize=7.5, va='center', color='#78350F')

    # Cards in Progress
    prog_cards = [
        "SP3-6: Impl. escaneo QR\n(8h - Matteo)",
        "SP3-8: Registrar asistencia\n(8h - Roberto)",
        "SP3-10: Asistencia realtime\n(8h - Cristian)"
    ]
    for idx, card in enumerate(prog_cards):
        y = 40 - idx * 10
        box = patches.FancyBboxPatch((col_x[2] + 1, y), col_w - 2, 8.5, boxstyle="round,pad=0.2",
                                     facecolor='white', edgecolor='#2563EB', linewidth=1.2)
        ax.add_patch(box)
        ax.text(col_x[2] + 2, y + 4.2, card, fontsize=7.5, va='center', color='#1E3A8A', fontweight='bold')

    # Cards in Done
    done_cards = [
        "SP3-1: Disenar credencial QR",
        "SP3-2: Generar QR token seguro",
        "SP3-3: Visualizar credencial movil",
        "SP3-4: Pruebas generacion QR",
        "SP3-5: Diseno camara escaneo"
    ]
    for idx, card in enumerate(done_cards):
        y = 40 - idx * 8.5
        box = patches.FancyBboxPatch((col_x[3] + 1, y), col_w - 2, 7, boxstyle="round,pad=0.2",
                                     facecolor='white', edgecolor='#16A34A', linewidth=1)
        ax.add_patch(box)
        ax.text(col_x[3] + 2, y + 3.5, f"✓ {card}", fontsize=7.5, va='center', color='#14532D', fontweight='bold')

    ax.set_title("Scrum TaskBoard - Sprint 3: Control de Acceso, Asistencia y Notificaciones",
                 fontsize=13, fontweight='bold', color='#153D63', pad=15)
    plt.tight_layout()
    path = os.path.join(OUTPUT_DIR, "taskboard_sprint3.png")
    fig.savefig(path)
    plt.close(fig)
    print("TaskBoard generado:", path)

# -------------------------------------------------------------
# 2. DIAGRAMAS DE ARQUITECTURA, CLASES Y ARTEFACTOS
# -------------------------------------------------------------

def generar_diagrama_componentes():
    fig, ax = plt.subplots(figsize=(10, 6), dpi=150)
    ax.set_xlim(0, 100)
    ax.set_ylim(0, 60)
    ax.axis('off')

    # Background frame
    frame = patches.FancyBboxPatch((1, 1), 98, 58, boxstyle="round,pad=0.5",
                                   facecolor='#F8FAFC', edgecolor='#153D63', linewidth=2)
    ax.add_patch(frame)
    ax.text(50, 56.5, "Diagrama de Componentes - Sprint 3 (Control QR, Asistencia y Notificaciones)",
            fontsize=12, fontweight='bold', color='#153D63', ha='center')

    # Capa Cliente
    ax.text(18, 52, "CAPA CLIENTE", fontsize=9.5, fontweight='bold', color='#0369A1')
    # Flutter Mobile
    box_flutter = patches.FancyBboxPatch((3, 30), 30, 20, boxstyle="round,pad=0.3",
                                        facecolor='#E0F2FE', edgecolor='#0284C7', linewidth=1.5)
    ax.add_patch(box_flutter)
    ax.text(18, 46.5, "Aplicacion Movil (Flutter)", fontsize=9, fontweight='bold', color='#0369A1', ha='center')
    ax.text(18, 41, "- Wallet Credencial QR (CU21)\n- Camara Escaner Staff (CU22)\n- Check-in Rapido (CU23)\n- Historial Asistencia (CU27)",
            fontsize=8, color='#0F172A', ha='center')

    # Angular Web
    box_web = patches.FancyBboxPatch((3, 6), 30, 21, boxstyle="round,pad=0.3",
                                    facecolor='#FEE2E2', edgecolor='#DC2626', linewidth=1.5)
    ax.add_patch(box_web)
    ax.text(18, 23.5, "Plataforma Web (Angular 20)", fontsize=9, fontweight='bold', color='#991B1B', ha='center')
    ax.text(18, 16.5, "- Monitor Aforo Realtime (CU24)\n- Dashboard Acreditaciones (CU24)\n- Centro de Notificaciones (CU25)\n- Credencial Web y Descarga PDF",
            fontsize=8, color='#0F172A', ha='center')

    # Gateway / Router
    box_gw = patches.FancyBboxPatch((38, 18), 12, 28, boxstyle="round,pad=0.3",
                                    facecolor='#FEF3C7', edgecolor='#D97706', linewidth=1.5)
    ax.add_patch(box_gw)
    ax.text(44, 38, "API Gateway\n/ Seguridad", fontsize=8.5, fontweight='bold', color='#92400E', ha='center')
    ax.text(44, 28, "JWT Filter\nTenant ID\nCORS / SSL\nWebSocket\nSTOMP", fontsize=7.5, color='#78350F', ha='center')

    # Capa Backend Spring Boot
    ax.text(68, 52, "CAPA LOGICA NEGOCIO (Spring Boot 3)", fontsize=9.5, fontweight='bold', color='#15803D')
    box_backend = patches.FancyBboxPatch((54, 8), 28, 42, boxstyle="round,pad=0.3",
                                         facecolor='#DCFCE7', edgecolor='#16A34A', linewidth=1.5)
    ax.add_patch(box_backend)
    ax.text(68, 46.5, "Servicios del Dominio Sprint 3", fontsize=9, fontweight='bold', color='#166534', ha='center')
    ax.text(68, 38.5, "• CredencialQrService\n  (Generacion HMAC-SHA256, ZXing)", fontsize=8, color='#0F172A', ha='center')
    ax.text(68, 30.5, "• AsistenciaService\n  (Validacion Antifraude, Check-in)", fontsize=8, color='#0F172A', ha='center')
    ax.text(68, 22.5, "• AforoRealtimeWebSocketService\n  (Contadores en vivo, Alertas aforo)", fontsize=8, color='#0F172A', ha='center')
    ax.text(68, 14.5, "• NotificacionBrevoService\n  (Transaccional Email, Recordatorios)", fontsize=8, color='#0F172A', ha='center')

    # Capa Datos y Servicios Externos
    box_db = patches.FancyBboxPatch((85, 28), 13, 22, boxstyle="round,pad=0.3",
                                    facecolor='#EDE9FE', edgecolor='#7C3AED', linewidth=1.5)
    ax.add_patch(box_db)
    ax.text(91.5, 46, "PostgreSQL 17", fontsize=8.5, fontweight='bold', color='#5B21B6', ha='center')
    ax.text(91.5, 37, "Tablas Tenant:\n- credencial_qr\n- asistencia\n- notificacion\n- bitacora_aud", fontsize=7.5, color='#4C1D95', ha='center')

    box_brevo = patches.FancyBboxPatch((85, 8), 13, 17, boxstyle="round,pad=0.3",
                                      facecolor='#FFEDD5', edgecolor='#EA580C', linewidth=1.5)
    ax.add_patch(box_brevo)
    ax.text(91.5, 21.5, "Brevo Cloud API", fontsize=8.5, fontweight='bold', color='#9A3412', ha='center')
    ax.text(91.5, 14.5, "SMTP / HTTP\nEnvio Masivo\nPlantillas QR", fontsize=7.5, color='#7C2D12', ha='center')

    # Connectors
    ax.annotate('', xy=(38, 40), xytext=(33, 40), arrowprops=dict(arrowstyle="<->", color="#0284C7", lw=2))
    ax.annotate('', xy=(38, 22), xytext=(33, 22), arrowprops=dict(arrowstyle="<->", color="#DC2626", lw=2))
    ax.annotate('', xy=(54, 32), xytext=(50, 32), arrowprops=dict(arrowstyle="<->", color="#D97706", lw=2))
    ax.annotate('', xy=(85, 39), xytext=(82, 39), arrowprops=dict(arrowstyle="<->", color="#7C3AED", lw=2))
    ax.annotate('', xy=(85, 16), xytext=(82, 16), arrowprops=dict(arrowstyle="->", color="#EA580C", lw=2))

    plt.tight_layout()
    path = os.path.join(OUTPUT_DIR, "diag_componentes_sprint3.png")
    fig.savefig(path)
    plt.close(fig)
    print("Diagrama de componentes generado:", path)

def generar_diagrama_clases():
    fig, ax = plt.subplots(figsize=(10, 6.5), dpi=150)
    ax.set_xlim(0, 100)
    ax.set_ylim(0, 65)
    ax.axis('off')

    frame = patches.FancyBboxPatch((1, 1), 98, 63, boxstyle="round,pad=0.5",
                                   facecolor='#F8FAFC', edgecolor='#153D63', linewidth=2)
    ax.add_patch(frame)
    ax.text(50, 61.5, "Diagrama de Clases / Diseno de Datos - Dominio Sprint 3",
            fontsize=12, fontweight='bold', color='#153D63', ha='center')

    # Class 1: Inscripcion
    b1 = patches.Rectangle((4, 35), 26, 22, facecolor='#E2E8F0', edgecolor='#334155', linewidth=1.5)
    ax.add_patch(b1)
    ax.text(17, 54, "«Entity»\nInscripcion", fontsize=8.5, fontweight='bold', ha='center')
    ax.plot([4, 30], [51, 51], color='#334155', lw=1)
    ax.text(5, 43, "- id_inscripcion: Long [PK]\n- id_evento: Long [FK]\n- id_usuario: Long [FK]\n- id_organizacion: Long [FK]\n- estado: EstadoInscripcion\n- fecha_inscripcion: DateTime", fontsize=7.5)

    # Class 2: CredencialQr
    b2 = patches.Rectangle((37, 35), 26, 22, facecolor='#E0F2FE', edgecolor='#0369A1', linewidth=1.5)
    ax.add_patch(b2)
    ax.text(50, 54, "«Entity»\nCredencialQr", fontsize=8.5, fontweight='bold', ha='center', color='#0369A1')
    ax.plot([37, 63], [51, 51], color='#0369A1', lw=1)
    ax.text(38, 41, "- id_credencial: Long [PK]\n- id_inscripcion: Long [FK]\n- token_criptografico: String\n- hash_verificacion: String\n- fecha_emision: DateTime\n- fecha_expiracion: DateTime\n- estado: EstadoCredencial", fontsize=7.5)

    # Class 3: Asistencia
    b3 = patches.Rectangle((70, 35), 26, 22, facecolor='#DCFCE7', edgecolor='#15803D', linewidth=1.5)
    ax.add_patch(b3)
    ax.text(83, 54, "«Entity»\nAsistencia", fontsize=8.5, fontweight='bold', ha='center', color='#15803D')
    ax.plot([70, 96], [51, 51], color='#15803D', lw=1)
    ax.text(71, 40, "- id_asistencia: Long [PK]\n- id_inscripcion: Long [FK]\n- id_evento: Long [FK]\n- id_operador_staff: Long [FK]\n- fecha_hora_checkin: DateTime\n- metodo: MetodoRegistro\n- estado: EstadoAsistencia\n- observaciones: String", fontsize=7.5)

    # Class 4: NotificacionEvento
    b4 = patches.Rectangle((4, 6), 28, 24, facecolor='#FEF3C7', edgecolor='#B45309', linewidth=1.5)
    ax.add_patch(b4)
    ax.text(18, 26.5, "«Entity»\nNotificacionEvento", fontsize=8.5, fontweight='bold', ha='center', color='#B45309')
    ax.plot([4, 32], [24, 24], color='#B45309', lw=1)
    ax.text(5, 14, "- id_notificacion: Long [PK]\n- id_evento: Long [FK]\n- id_usuario: Long [FK]\n- tipo: TipoNotificacion\n- canal: CanalEnvio (BREVO)\n- asunto: String\n- cuerpo: String\n- fecha_envio: DateTime\n- estado: EstadoEnvio", fontsize=7.5)

    # Class 5: HistorialParticipacion (DTO / Vista)
    b5 = patches.Rectangle((37, 6), 26, 24, facecolor='#EDE9FE', edgecolor='#6D28D9', linewidth=1.5)
    ax.add_patch(b5)
    ax.text(50, 26.5, "«View / DTO»\nHistorialParticipacion", fontsize=8.5, fontweight='bold', ha='center', color='#6D28D9')
    ax.plot([37, 63], [24, 24], color='#6D28D9', lw=1)
    ax.text(38, 14, "- id_usuario: Long\n- total_eventos_inscrito: Int\n- total_asistencias: Int\n- porcentaje_asistencia: Double\n- lista_participaciones: List\n- eventos_certificados: List\n- tenant_id: Long", fontsize=7.5)

    # Class 6: ControlAforoRealtime (Cache / In-Memory)
    b6 = patches.Rectangle((70, 6), 26, 24, facecolor='#FCE7F3', edgecolor='#BE185D', linewidth=1.5)
    ax.add_patch(b6)
    ax.text(83, 26.5, "«State / WebSocket»\nControlAforoRealtime", fontsize=8.5, fontweight='bold', ha='center', color='#BE185D')
    ax.plot([70, 96], [24, 24], color='#BE185D', lw=1)
    ax.text(71, 14, "- id_evento: Long\n- id_espacio: Long\n- aforo_maximo: Int\n- asistentes_actuales: Int\n- porcentaje_ocupacion: Double\n- alerta_umbral: Boolean\n- ultima_actualizacion: DateTime", fontsize=7.5)

    # Relations
    # Inscripcion to CredencialQr (1 to 1)
    ax.annotate('', xy=(37, 46), xytext=(30, 46), arrowprops=dict(arrowstyle="->", color="#334155", lw=1.5))
    ax.text(33.5, 47.5, "1:1", fontsize=8, fontweight='bold', ha='center')

    # CredencialQr to Asistencia (1 to 0..*)
    ax.annotate('', xy=(70, 46), xytext=(63, 46), arrowprops=dict(arrowstyle="->", color="#334155", lw=1.5))
    ax.text(66.5, 47.5, "1:0..*", fontsize=8, fontweight='bold', ha='center')

    # Inscripcion to Notificacion
    ax.annotate('', xy=(17, 30), xytext=(17, 35), arrowprops=dict(arrowstyle="->", color="#334155", lw=1.5))
    ax.text(19, 32.5, "1:N", fontsize=8, fontweight='bold')

    plt.tight_layout()
    path = os.path.join(OUTPUT_DIR, "diag_clases_sprint3.png")
    fig.savefig(path)
    plt.close(fig)
    print("Diagrama de clases generado:", path)

def generar_diagrama_artefactos():
    fig, ax = plt.subplots(figsize=(10, 5), dpi=150)
    ax.set_xlim(0, 100)
    ax.set_ylim(0, 50)
    ax.axis('off')

    frame = patches.FancyBboxPatch((1, 1), 98, 48, boxstyle="round,pad=0.5",
                                   facecolor='#F1F5F9', edgecolor='#153D63', linewidth=2)
    ax.add_patch(frame)
    ax.text(50, 46, "Componentes y Artefactos de Implementacion Generados - Sprint 3",
            fontsize=12, fontweight='bold', color='#153D63', ha='center')

    modules = [
        ("Backend Spring Boot 3", "#E0F2FE", "#0284C7", [
            "• CredencialQrController.java",
            "• AsistenciaController.java",
            "• NotificacionController.java",
            "• AforoWebSocketController.java",
            "• CredencialQrServiceImpl.java",
            "• AsistenciaServiceImpl.java",
            "• BrevoNotificationClient.java"
        ], 3),
        ("Frontend Angular 20", "#FEE2E2", "#DC2626", [
            "• monitor-aforo.component.ts/html",
            "• credencial-modal.component.ts",
            "• checkin-manual.component.ts",
            "• centro-notificaciones.component.ts",
            "• aforo-socket.service.ts",
            "• credencial-qr.service.ts"
        ], 27),
        ("App Movil Flutter", "#DCFCE7", "#16A34A", [
            "• qr_credential_wallet_screen.dart",
            "• staff_qr_scanner_screen.dart",
            "• checkin_confirmation_modal.dart",
            "• participation_history_screen.dart",
            "• qr_crypto_validator_service.dart",
            "• offline_cache_service.dart"
        ], 51),
        ("BD PostgreSQL & Cloud", "#FEF3C7", "#D97706", [
            "• V3__sprint3_qr_asistencia.sql",
            "• credencial_qr (tabla & indices)",
            "• asistencia (tabla & constraints)",
            "• notificacion_evento (tabla)",
            "• Brevo Transactional Templates",
            "• Bitacora cifrada KMS hook"
        ], 75)
    ]

    for title, fcol, bcol, items, x in modules:
        box = patches.FancyBboxPatch((x, 4), 22, 38, boxstyle="round,pad=0.3",
                                     facecolor=fcol, edgecolor=bcol, linewidth=1.5)
        ax.add_patch(box)
        ax.text(x + 11, 38, title, fontsize=8.5, fontweight='bold', color=bcol, ha='center')
        ax.plot([x, x+22], [35, 35], color=bcol, lw=1)
        txt = "\n".join(items)
        ax.text(x + 1, 20, txt, fontsize=7.2, color='#0F172A', va='center')

    plt.tight_layout()
    path = os.path.join(OUTPUT_DIR, "diag_artefactos_sprint3.png")
    fig.savefig(path)
    plt.close(fig)
    print("Diagrama de artefactos generado:", path)

# -------------------------------------------------------------
# 3. DIAGRAMAS DE COMUNICACION POR CASO DE USO
# -------------------------------------------------------------

def generar_diagrama_comunicacion(cu_id, titulo, actor, steps, filename):
    fig, ax = plt.subplots(figsize=(10, 3.2), dpi=150)
    ax.set_xlim(0, 100)
    ax.set_ylim(0, 35)
    ax.axis('off')

    frame = patches.FancyBboxPatch((1, 1), 98, 33, boxstyle="round,pad=0.4",
                                   facecolor='#FFFFFF', edgecolor='#153D63', linewidth=1.5)
    ax.add_patch(frame)
    ax.text(50, 31, f"Diagrama de Comunicacion {cu_id}: {titulo}", fontsize=10.5, fontweight='bold', color='#153D63', ha='center')

    nodes = [
        (actor, 8, 16, '#E0F2FE', '#0284C7'),
        ("Controller REST\n/ UI Client", 32, 16, '#F1F5F9', '#475569'),
        ("Domain Service\n(Logica Negocio)", 58, 16, '#DCFCE7', '#16A34A'),
        ("Repository\n/ DB PostgreSQL", 85, 16, '#FEF3C7', '#D97706')
    ]

    for name, x, y, fc, ec in nodes:
        b = patches.FancyBboxPatch((x-7, y-5), 14, 10, boxstyle="round,pad=0.2",
                                   facecolor=fc, edgecolor=ec, linewidth=1.2)
        ax.add_patch(b)
        ax.text(x, y, name, fontsize=7.5, fontweight='bold', ha='center', va='center', color='#0F172A')

    # Draw arrows and numbered steps
    # Link 1: Actor <-> Controller
    ax.plot([15, 25], [16, 16], color='#153D63', lw=1.5)
    # Link 2: Controller <-> Service
    ax.plot([39, 51], [16, 16], color='#153D63', lw=1.5)
    # Link 3: Service <-> Repo
    ax.plot([65, 78], [16, 16], color='#153D63', lw=1.5)

    # Text steps display
    step_y = 6
    txt_steps = "  |  ".join([f"{i+1}: {st}" for i, st in enumerate(steps)])
    ax.text(50, step_y, txt_steps, fontsize=7.5, color='#1E293B', ha='center', va='center',
            bbox=dict(boxstyle="round,pad=0.3", facecolor='#F8FAFC', edgecolor='#CBD5E1', lw=1))

    plt.tight_layout()
    path = os.path.join(OUTPUT_DIR, filename)
    fig.savefig(path)
    plt.close(fig)
    print(f"Diagrama comunicacion {cu_id} generado:", path)

def generar_todos_los_diagramas_comunicacion():
    generar_diagrama_comunicacion(
        "CU21", "Generar y Visualizar Credencial Digital con QR",
        "Participante\n(App/Web)",
        ["solicitarCredencial(idInscripcion)", "validarInscripcionPagada()", "generarTokenQR(HMAC)", "persistirYRetornarQR()"],
        "diag_comunicacion_cu21.png"
    )
    generar_diagrama_comunicacion(
        "CU22", "Escanear y Validar Codigo QR",
        "Staff de Acceso\n(App Movil)",
        ["escanearPayloadQR(camara)", "validarFirmaHMAC(token)", "verificarEstadoEvento()", "retornarEstadoValidacion()"],
        "diag_comunicacion_cu22.png"
    )
    generar_diagrama_comunicacion(
        "CU23", "Registrar Asistencia (Check-in)",
        "Staff / Sistema\n(App/Web)",
        ["confirmarIngreso(idInscripcion)", "comprobarNoDuplicado()", "guardarAsistencia(timestamp)", "notificarBitacoraAuditoria()"],
        "diag_comunicacion_cu23.png"
    )
    generar_diagrama_comunicacion(
        "CU24", "Consultar Asistencia y Aforo en Tiempo Real",
        "Organizador\n(Web)",
        ["suscribirWebSocket(idEvento)", "calcularAforoYPorcentaje()", "emitirMetricasSTOMP()", "renderizarDashboardAforo()"],
        "diag_comunicacion_cu24.png"
    )
    generar_diagrama_comunicacion(
        "CU25", "Gestionar Recordatorios y Notificaciones",
        "Sistema / Admin\n(Backend Brevo)",
        ["despacharNotificacion(eventoId)", "construirTemplateBrevo()", "enviarEmailTransaccional()", "registrarLogEnvio()"],
        "diag_comunicacion_cu25.png"
    )
    generar_diagrama_comunicacion(
        "CU27", "Consultar Historial de Participacion",
        "Participante\n(App/Web)",
        ["consultarHistorial(usuarioId)", "obtenerEventosYAsistencias()", "calcularEstadisticas()", "retornarTimelineEventos()"],
        "diag_comunicacion_cu27.png"
    )

# -------------------------------------------------------------
# 4. PROTOTIPOS DE INTERFAZ (UI MOCKUPS)
# -------------------------------------------------------------

def generar_prototipo_cu21():
    # Credencial digital móvil con QR
    fig, ax = plt.subplots(figsize=(8, 4.5), dpi=150)
    ax.set_xlim(0, 100)
    ax.set_ylim(0, 60)
    ax.axis('off')

    bg = patches.FancyBboxPatch((1, 1), 98, 58, boxstyle="round,pad=0.5", facecolor='#F1F5F9', edgecolor='#CBD5E1')
    ax.add_patch(bg)
    ax.text(50, 56.5, "Prototipo CU21: Credencial Digital con Codigo QR (App Movil / Web Wallet)",
            fontsize=11, fontweight='bold', color='#153D63', ha='center')

    # Phone mockup container
    phone = patches.FancyBboxPatch((28, 4), 44, 49, boxstyle="round,pad=0.5", facecolor='#0F172A', edgecolor='#334155', lw=2)
    ax.add_patch(phone)
    screen = patches.FancyBboxPatch((30, 6), 40, 45, boxstyle="round,pad=0.3", facecolor='#FFFFFF', edgecolor='none')
    ax.add_patch(screen)

    # Header badge
    hdr = patches.FancyBboxPatch((32, 42), 36, 7, boxstyle="round,pad=0.2", facecolor='#153D63', edgecolor='none')
    ax.add_patch(hdr)
    ax.text(50, 46.5, "CONGRESO INTERNACIONAL IA 2026", color='white', fontsize=7.5, fontweight='bold', ha='center')
    ax.text(50, 43.5, "Santa Cruz de la Sierra - UAGRM", color='#A8CFC8', fontsize=6, ha='center')

    # Attendee info
    ax.text(50, 39, "Ing. Carlos Mendoza", fontsize=9, fontweight='bold', color='#0F172A', ha='center')
    ax.text(50, 36.5, "Rol: Participante General  |  Cat: Profesional", fontsize=6.8, color='#475569', ha='center')

    # Mock QR Code
    qr_box = patches.Rectangle((40, 18), 20, 16, facecolor='white', edgecolor='#0F172A', lw=1.5)
    ax.add_patch(qr_box)
    # Draw simple grid pattern to simulate QR code
    np.random.seed(42)
    grid = np.random.choice([0, 1], size=(8, 8), p=[0.45, 0.55])
    # corners
    grid[0:2, 0:2] = 1
    grid[0:2, 6:8] = 1
    grid[6:8, 0:2] = 1
    for r in range(8):
        for c in range(8):
            if grid[r, c] == 1:
                sq = patches.Rectangle((40.5 + c*2.35, 18.5 + (7-r)*1.85), 2.1, 1.6, facecolor='#0F172A', edgecolor='none')
                ax.add_patch(sq)

    # Security hash & Token ID
    ax.text(50, 15.5, "ID: #INS-2026-8942  •  Token: 8f4c...3e1a", fontsize=6.5, color='#64748B', ha='center')
    # Status Pill
    pill = patches.FancyBboxPatch((38, 10.5), 24, 3.8, boxstyle="round,pad=0.2", facecolor='#DCFCE7', edgecolor='#16A34A')
    ax.add_patch(pill)
    ax.text(50, 12.4, "✓ CREDENCIAL ACTIVA / PAGADA", fontsize=6.5, fontweight='bold', color='#15803D', ha='center')

    # Bottom buttons
    b_down = patches.FancyBboxPatch((33, 7), 16, 2.5, boxstyle="round,pad=0.1", facecolor='#E2E8F0', edgecolor='none')
    ax.add_patch(b_down)
    ax.text(41, 8.2, "Guardar PDF", fontsize=5.8, color='#334155', ha='center')

    b_offline = patches.FancyBboxPatch((51, 7), 17, 2.5, boxstyle="round,pad=0.1", facecolor='#E0F2FE', edgecolor='none')
    ax.add_patch(b_offline)
    ax.text(59.5, 8.2, "Guardar Wallet", fontsize=5.8, color='#0369A1', ha='center')

    plt.tight_layout()
    path = os.path.join(OUTPUT_DIR, "proto_cu21.png")
    fig.savefig(path)
    plt.close(fig)
    print("Prototipo CU21 generado:", path)

def generar_prototipo_cu22():
    # Escaneo y validación QR cámara móvil
    fig, ax = plt.subplots(figsize=(8, 4.5), dpi=150)
    ax.set_xlim(0, 100)
    ax.set_ylim(0, 60)
    ax.axis('off')

    bg = patches.FancyBboxPatch((1, 1), 98, 58, boxstyle="round,pad=0.5", facecolor='#F1F5F9', edgecolor='#CBD5E1')
    ax.add_patch(bg)
    ax.text(50, 56.5, "Prototipo CU22: Escaneo y Validacion de Credencial QR (App Staff Movil)",
            fontsize=11, fontweight='bold', color='#153D63', ha='center')

    # Phone mockup container
    phone = patches.FancyBboxPatch((28, 4), 44, 49, boxstyle="round,pad=0.5", facecolor='#0F172A', edgecolor='#334155', lw=2)
    ax.add_patch(phone)
    screen = patches.FancyBboxPatch((30, 6), 40, 45, boxstyle="round,pad=0.3", facecolor='#1E293B', edgecolor='none')
    ax.add_patch(screen)

    # Camera viewfinder overlay
    ax.text(50, 47, "Escaner Staff - Puerta Principal A", color='white', fontsize=7.5, fontweight='bold', ha='center')
    ax.text(50, 44.5, "Apunte la camara hacia el codigo QR del participante", color='#94A3B8', fontsize=6, ha='center')

    # Viewfinder box with glowing corners
    vf = patches.Rectangle((37, 24), 26, 18, facecolor='none', edgecolor='#22C55E', lw=2, linestyle='--')
    ax.add_patch(vf)
    ax.text(50, 33, "[ Codigo QR Detectado ]\nVerificando firma criptografica...", color='#4ADE80', fontsize=6.8, ha='center', va='center')

    # Validation Popup Modal
    modal = patches.FancyBboxPatch((32, 8), 36, 14, boxstyle="round,pad=0.3", facecolor='#FFFFFF', edgecolor='#16A34A', lw=1.5)
    ax.add_patch(modal)
    ax.text(50, 19, "✓ ¡ACREDITACION EXITOSA!", color='#15803D', fontsize=8, fontweight='bold', ha='center')
    ax.text(50, 16, "Ing. Carlos Mendoza  •  General", color='#0F172A', fontsize=7, fontweight='bold', ha='center')
    ax.text(50, 13.5, "Sala: Paraninfo  •  Hora: 08:42:15  •  Token Valido", color='#475569', fontsize=6, ha='center')

    b_conf = patches.FancyBboxPatch((35, 9.5), 30, 3, boxstyle="round,pad=0.1", facecolor='#16A34A', edgecolor='none')
    ax.add_patch(b_conf)
    ax.text(50, 11, "Confirmar Ingreso (Check-in)", color='white', fontsize=6.5, fontweight='bold', ha='center')

    plt.tight_layout()
    path = os.path.join(OUTPUT_DIR, "proto_cu22.png")
    fig.savefig(path)
    plt.close(fig)
    print("Prototipo CU22 generado:", path)

def generar_prototipo_cu23():
    # Registro de asistencia y control antifraude duplicado
    fig, ax = plt.subplots(figsize=(8, 4.5), dpi=150)
    ax.set_xlim(0, 100)
    ax.set_ylim(0, 60)
    ax.axis('off')

    bg = patches.FancyBboxPatch((1, 1), 98, 58, boxstyle="round,pad=0.5", facecolor='#F8FAFC', edgecolor='#CBD5E1')
    ax.add_patch(bg)
    ax.text(50, 56.5, "Prototipo CU23: Registro de Asistencia (Check-in y Control Antifraude)",
            fontsize=11, fontweight='bold', color='#153D63', ha='center')

    # Card 1: Check-in exitoso
    c1 = patches.FancyBboxPatch((6, 12), 42, 40, boxstyle="round,pad=0.4", facecolor='#FFFFFF', edgecolor='#16A34A', lw=1.5)
    ax.add_patch(c1)
    ax.text(27, 48, "Caso A: Check-in Exitoso", fontsize=9, fontweight='bold', color='#15803D', ha='center')
    ax.plot([8, 46], [45, 45], color='#16A34A', lw=1)
    ax.text(8, 41, "• Participante: Maria Elena Suarez\n• CI: 6723910 SCZ\n• Evento: Congreso IA & Tech 2026\n• Tipo Entrada: Estudiante UAGRM\n• Metodo: QR Escaneado (Camara Movil)\n• Hora Registro: 12/10/2026 08:35:10\n• Operador Staff: Roberto Cervantes\n• Bitacora: Log cifrado #AUD-9104",
            fontsize=7.2, color='#1E293B', va='top')
    badge1 = patches.FancyBboxPatch((12, 14), 30, 4, boxstyle="round,pad=0.2", facecolor='#DCFCE7', edgecolor='#16A34A')
    ax.add_patch(badge1)
    ax.text(27, 16, "✓ ASISTENCIA REGISTRADA (100%)", fontsize=7, fontweight='bold', color='#15803D', ha='center')

    # Card 2: Intento duplicado rechazado
    c2 = patches.FancyBboxPatch((52, 12), 42, 40, boxstyle="round,pad=0.4", facecolor='#FFFFFF', edgecolor='#DC2626', lw=1.5)
    ax.add_patch(c2)
    ax.text(73, 48, "Caso B: Alerta Antifraude (Duplicado)", fontsize=9, fontweight='bold', color='#B91C1C', ha='center')
    ax.plot([54, 92], [45, 45], color='#DC2626', lw=1)
    ax.text(54, 41, "• Token QR: Ya utilizado previamente\n• Participante: Ing. Carlos Mendoza\n• Ingreso Original: 08:42:15 (Puerta A)\n• Intento Actual: 08:49:02 (Puerta B)\n• Accion del Sistema: RECHAZO AUTOMATICO\n• Alerta: Notificacion al Supervisor Staff\n• Motivo: Prevencion de reingreso / clonacion",
            fontsize=7.2, color='#1E293B', va='top')
    badge2 = patches.FancyBboxPatch((58, 14), 30, 4, boxstyle="round,pad=0.2", facecolor='#FEE2E2', edgecolor='#DC2626')
    ax.add_patch(badge2)
    ax.text(73, 16, "[!] INGRESO DUPLICADO DENEGADO", fontsize=7, fontweight='bold', color='#B91C1C', ha='center')

    plt.tight_layout()
    path = os.path.join(OUTPUT_DIR, "proto_cu23.png")
    fig.savefig(path)
    plt.close(fig)
    print("Prototipo CU23 generado:", path)

def generar_prototipo_cu24():
    # Dashboard web monitoreo aforo y asistencia realtime
    fig, ax = plt.subplots(figsize=(8, 4.5), dpi=150)
    ax.set_xlim(0, 100)
    ax.set_ylim(0, 60)
    ax.axis('off')

    bg = patches.FancyBboxPatch((1, 1), 98, 58, boxstyle="round,pad=0.5", facecolor='#F8FAFC', edgecolor='#CBD5E1')
    ax.add_patch(bg)
    ax.text(50, 56.5, "Prototipo CU24: Monitoreo de Asistencia y Aforo en Tiempo Real (Web Angular)",
            fontsize=11, fontweight='bold', color='#153D63', ha='center')

    # Status Bar
    bar = patches.FancyBboxPatch((4, 48), 92, 5, boxstyle="round,pad=0.2", facecolor='#153D63', edgecolor='none')
    ax.add_patch(bar)
    ax.text(6, 50.5, "Congreso IA & Tech 2026 - Panel de Control de Acceso", color='white', fontsize=8, fontweight='bold')
    ax.text(94, 50.5, "[EN VIVO] WebSocket Conectado (12/10/2026 10:30)", color='#A8CFC8', fontsize=7.5, ha='right')

    # Metric Cards
    kpis = [
        ("Total Inscritos", "500", "#E0F2FE", "#0284C7", 4),
        ("Asistentes (Check-in)", "382", "#DCFCE7", "#16A34A", 28),
        ("Pendientes Ingreso", "118", "#FEF3C7", "#D97706", 52),
        ("Ocupacion Sala", "76.4%", "#F3E8FF", "#7C3AED", 76)
    ]
    for lbl, val, bgc, fgc, x in kpis:
        c = patches.FancyBboxPatch((x, 34), 20, 12, boxstyle="round,pad=0.3", facecolor=bgc, edgecolor=fgc, lw=1.2)
        ax.add_patch(c)
        ax.text(x+10, 42, lbl, fontsize=7, color=fgc, ha='center', fontweight='bold')
        ax.text(x+10, 36.5, val, fontsize=12, color='#0F172A', ha='center', fontweight='bold')

    # Rooms Capacity Bars
    c_rooms = patches.FancyBboxPatch((4, 8), 58, 24, boxstyle="round,pad=0.3", facecolor='#FFFFFF', edgecolor='#CBD5E1')
    ax.add_patch(c_rooms)
    ax.text(6, 28.5, "Ocupacion por Sala / Espacio:", fontsize=8, fontweight='bold', color='#1E293B')

    rooms_data = [
        ("Auditorio Paraninfo", 382, 400, "95.5% (Alerta Ambar)", '#F59E0B'),
        ("Sala de Conferencias B", 120, 200, "60.0% (Normal)", '#10B981'),
        ("Taller Robotica C", 42, 50, "84.0% (Optimo)", '#3B82F6')
    ]
    for idx, (name, act, max_cap, pct_str, bar_col) in enumerate(rooms_data):
        y = 22 - idx * 6.5
        ax.text(6, y+2, f"{name}: {act}/{max_cap} ({pct_str})", fontsize=6.8, color='#334155')
        # Background bar
        ax.add_patch(patches.Rectangle((6, y), 54, 2, facecolor='#E2E8F0', edgecolor='none'))
        # Active bar
        w = 54 * (act / max_cap)
        ax.add_patch(patches.Rectangle((6, y), w, 2, facecolor=bar_col, edgecolor='none'))

    # Quick Actions & Filter panel
    c_act = patches.FancyBboxPatch((66, 8), 30, 24, boxstyle="round,pad=0.3", facecolor='#FFFFFF', edgecolor='#CBD5E1')
    ax.add_patch(c_act)
    ax.text(81, 28.5, "Acciones Rapidas", fontsize=8, fontweight='bold', color='#1E293B', ha='center')

    btns = [
        ("Exportar Asistentes (CSV)", '#E2E8F0', '#0F172A'),
        ("Reporte de Aforo (PDF)", '#E2E8F0', '#0F172A'),
        ("Enviar Alerta Brevo", '#FEF3C7', '#B45309'),
        ("Busqueda Manual Staff", '#E0F2FE', '#0369A1')
    ]
    for idx, (btxt, bfc, btc) in enumerate(btns):
        by = 22 - idx * 4.8
        btn_box = patches.FancyBboxPatch((68, by), 26, 3.6, boxstyle="round,pad=0.2", facecolor=bfc, edgecolor='none')
        ax.add_patch(btn_box)
        ax.text(81, by + 1.8, btxt, fontsize=6.5, color=btc, ha='center', va='center', fontweight='bold')

    plt.tight_layout()
    path = os.path.join(OUTPUT_DIR, "proto_cu24.png")
    fig.savefig(path)
    plt.close(fig)
    print("Prototipo CU24 generado:", path)

def generar_prototipo_cu25():
    # Gestion de recordatorios y notificaciones Brevo
    fig, ax = plt.subplots(figsize=(8, 4.5), dpi=150)
    ax.set_xlim(0, 100)
    ax.set_ylim(0, 60)
    ax.axis('off')

    bg = patches.FancyBboxPatch((1, 1), 98, 58, boxstyle="round,pad=0.5", facecolor='#F8FAFC', edgecolor='#CBD5E1')
    ax.add_patch(bg)
    ax.text(50, 56.5, "Prototipo CU25: Gestion de Recordatorios y Notificaciones (Email Brevo / Web)",
            fontsize=11, fontweight='bold', color='#153D63', ha='center')

    # Card 1: Campanas / Triggers
    c1 = patches.FancyBboxPatch((4, 10), 44, 43, boxstyle="round,pad=0.3", facecolor='#FFFFFF', edgecolor='#CBD5E1')
    ax.add_patch(c1)
    ax.text(26, 49, "Plantillas y Triggers Transaccionales", fontsize=8.5, fontweight='bold', color='#153D63', ha='center')
    ax.plot([6, 46], [46, 46], color='#CBD5E1', lw=1)

    triggers = [
        ("Confirmacion de Inscripcion", "Envio inmediato con Credencial QR adjunta", "✓ Activo (100% entrega)"),
        ("Recordatorio 24 Horas Antes", "Recordatorio de fecha, sala y horarios", "✓ Programado (11/10/2026)"),
        ("Alerta Cambio de Sala / Horario", "Despacho masivo ante imprevistos", "✓ Listo para disparo"),
        ("Agradecimiento y Encuesta", "Despacho posterior al evento", "[Pendiente] En espera de cierre")
    ]
    for idx, (t_title, t_desc, t_st) in enumerate(triggers):
        y = 38 - idx * 9
        ax.text(6, y+4, t_title, fontsize=7.2, fontweight='bold', color='#0F172A')
        ax.text(6, y+1.5, t_desc, fontsize=6.2, color='#64748B')
        ax.text(6, y-1, t_st, fontsize=6.2, color='#15803D', fontweight='bold')

    # Card 2: Email Preview
    c2 = patches.FancyBboxPatch((52, 10), 44, 43, boxstyle="round,pad=0.3", facecolor='#FFFFFF', edgecolor='#0284C7', lw=1.2)
    ax.add_patch(c2)
    ax.text(74, 49, "Vista Previa: Correo Brevo", fontsize=8.5, fontweight='bold', color='#0284C7', ha='center')
    ax.plot([54, 94], [46, 46], color='#0284C7', lw=1)

    # Email header mockup
    ehdr = patches.Rectangle((54, 41), 40, 4.5, facecolor='#153D63', edgecolor='none')
    ax.add_patch(ehdr)
    ax.text(74, 43.2, "Congreso IA & Tech 2026", color='white', fontsize=7, fontweight='bold', ha='center')

    ax.text(55, 38, "Estimado(a) Ing. Carlos Mendoza,", fontsize=7, fontweight='bold', color='#0F172A')
    ax.text(55, 33, "Tu inscripcion ha sido confirmada exitosamente.\nPresenta la credencial adjunta en el acceso del evento.\n\nFecha: 12 de Octubre de 2026 - 08:00 AM\nLugar: Auditorio Paraninfo Universitario", fontsize=6.5, color='#334155')

    # Mock QR attachment
    qr_att = patches.Rectangle((68, 17), 12, 12, facecolor='#F1F5F9', edgecolor='#0F172A', lw=1)
    ax.add_patch(qr_att)
    ax.text(74, 23, "[ QR Code ]\n#INS-8942", fontsize=6, ha='center', va='center', color='#0F172A')

    ax.text(74, 13, "Remitente: acreditaciones@eventos-ia.org  •  Powered by Brevo", fontsize=5.8, color='#94A3B8', ha='center')

    plt.tight_layout()
    path = os.path.join(OUTPUT_DIR, "proto_cu25.png")
    fig.savefig(path)
    plt.close(fig)
    print("Prototipo CU25 generado:", path)

def generar_prototipo_cu27():
    # Historial de participacion movil / web
    fig, ax = plt.subplots(figsize=(8, 4.5), dpi=150)
    ax.set_xlim(0, 100)
    ax.set_ylim(0, 60)
    ax.axis('off')

    bg = patches.FancyBboxPatch((1, 1), 98, 58, boxstyle="round,pad=0.5", facecolor='#F1F5F9', edgecolor='#CBD5E1')
    ax.add_patch(bg)
    ax.text(50, 56.5, "Prototipo CU27: Historial de Participacion del Usuario (App Movil / Web)",
            fontsize=11, fontweight='bold', color='#153D63', ha='center')

    # Phone mockup container
    phone = patches.FancyBboxPatch((28, 4), 44, 49, boxstyle="round,pad=0.5", facecolor='#0F172A', edgecolor='#334155', lw=2)
    ax.add_patch(phone)
    screen = patches.FancyBboxPatch((30, 6), 40, 45, boxstyle="round,pad=0.3", facecolor='#FFFFFF', edgecolor='none')
    ax.add_patch(screen)

    # Header
    ax.text(50, 47, "Mi Historial de Eventos", color='#153D63', fontsize=8.5, fontweight='bold', ha='center')
    ax.text(50, 44.5, "Carlos Mendoza • 3 Eventos Registrados", color='#64748B', fontsize=6.2, ha='center')

    # History Event Cards
    events = [
        ("Congreso Internacional IA 2026", "12/10/2026 - Paraninfo", "Asistio (Check-in 08:42)", "#DCFCE7", "#15803D", "[ Ver Credencial ]"),
        ("Simposio Cloud Computing 2026", "15/05/2026 - Auditorio B", "Asistio (100% Asistencia)", "#DCFCE7", "#15803D", "[ Ver Constancia ]"),
        ("Workshop Seguridad & Blockchain", "20/02/2026 - Sala C", "No Asistio (Ausente)", "#FEE2E2", "#B91C1C", "[ Detalle Evento ]")
    ]

    for idx, (ename, edate, estatus, sbg, sfg, btn_txt) in enumerate(events):
        y = 32 - idx * 11
        ecard = patches.FancyBboxPatch((32, y), 36, 9.5, boxstyle="round,pad=0.2", facecolor='#F8FAFC', edgecolor='#E2E8F0', lw=1)
        ax.add_patch(ecard)
        ax.text(34, y + 7.2, ename, fontsize=6.5, fontweight='bold', color='#0F172A')
        ax.text(34, y + 4.8, edate, fontsize=5.8, color='#64748B')

        # Status badge
        sbadge = patches.FancyBboxPatch((34, y + 1.2), 20, 2.8, boxstyle="round,pad=0.1", facecolor=sbg, edgecolor='none')
        ax.add_patch(sbadge)
        ax.text(44, y + 2.5, estatus, fontsize=5.2, fontweight='bold', color=sfg, ha='center')

        # Action button
        abtn = patches.FancyBboxPatch((55, y + 1.2), 11.5, 2.8, boxstyle="round,pad=0.1", facecolor='#E0F2FE', edgecolor='none')
        ax.add_patch(abtn)
        ax.text(60.7, y + 2.5, btn_txt, fontsize=5, color='#0369A1', ha='center')

    plt.tight_layout()
    path = os.path.join(OUTPUT_DIR, "proto_cu27.png")
    fig.savefig(path)
    plt.close(fig)
    print("Prototipo CU27 generado:", path)

def main():
    print("=== Generando graficas de Scrum ===")
    generar_burndown()
    generar_burnup()
    generar_esfuerzos()
    generar_taskboard()

    print("\n=== Generando diagramas de arquitectura, clases y artefactos ===")
    generar_diagrama_componentes()
    generar_diagrama_clases()
    generar_diagrama_artefactos()

    print("\n=== Generando diagramas de comunicacion por caso de uso ===")
    generar_todos_los_diagramas_comunicacion()

    print("\n=== Generando prototipos de interfaz de usuario ===")
    generar_prototipo_cu21()
    generar_prototipo_cu22()
    generar_prototipo_cu23()
    generar_prototipo_cu24()
    generar_prototipo_cu25()
    generar_prototipo_cu27()

    print("\n¡Todas las imagenes han sido generadas exitosamente!")

if __name__ == '__main__':
    main()
