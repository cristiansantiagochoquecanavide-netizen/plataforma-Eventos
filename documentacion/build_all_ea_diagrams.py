# -*- coding: utf-8 -*-
"""
Script maestro para Enterprise Architect 15:
1. Construye los 6 Diagramas de Comunicacion del Sprint 3 siguiendo exactamente el modelo de guia:
   - Enlace horizontal solido 'Association' entre Actor, Boundary, Control y Entity.
   - Esferas compactas y estilizadas (50x50 px), alineadas a la cintura/pecho del actor (Y=-137).
   - 4 mensajes CRUD apilados sobre la linea con flechas (-->):
     * Actor -> Boundary: 4: eliminar() --> / 3: editar() --> / 2: crear() --> / 1: mostrar() -->
     * Boundary -> Control: 4.1: destroy() --> / 3.1: update() --> / 2.1: store() --> / 1.1: index() -->
     * Control -> Entity: 4.2: delete() --> / 3.2: update() --> / 2.2: create() --> / 1.2: get() -->
2. Construye el Diagrama de Despliegue en AWS (Diseno Fisico) con alta fidelidad tecnica.
3. Construye el Diagrama de Paquetes (Diseno Logico en Capas) con trazabilidad tecnica.
4. Exporta las imagenes en alta resolucion PNG a documentacion/diagramas y documentacion/sprint3_assets.
5. Sincroniza el archivo 'Diagramas Si2.eap'.
"""

import os
import shutil
import win32com.client

MODEL_PATH = r"C:\Users\ASUS\Diagramas Si2.eap"
EXPORT_DIR = r"c:\Proyecto-GestionEventos\plataforma-gestion-eventos\documentacion\diagramas"
ASSETS_DIR = r"c:\Proyecto-GestionEventos\plataforma-gestion-eventos\documentacion\sprint3_assets"
os.makedirs(EXPORT_DIR, exist_ok=True)
os.makedirs(ASSETS_DIR, exist_ok=True)

COMM_DIAGRAMS_DEF = [
    {
        "cu": "CU21",
        "title": "CU21 - Generar y visualizar credencial digital con QR",
        "actor": "Participante",
        "boundary": "UI_credencial",
        "control": "CredencialController",
        "entity": "CredencialQR"
    },
    {
        "cu": "CU22",
        "title": "CU22 - Escanear y validar codigo QR",
        "actor": "Staff",
        "boundary": "UI_escaner",
        "control": "ValidacionController",
        "entity": "ValidacionQR"
    },
    {
        "cu": "CU23",
        "title": "CU23 - Registrar asistencia (Check-in)",
        "actor": "Staff",
        "boundary": "UI_asistencia",
        "control": "AsistenciaController",
        "entity": "Asistencia"
    },
    {
        "cu": "CU24",
        "title": "CU24 - Consultar asistencia y aforo en tiempo real",
        "actor": "Organizador",
        "boundary": "UI_aforo",
        "control": "AforoController",
        "entity": "ControlAforo"
    },
    {
        "cu": "CU25",
        "title": "CU25 - Gestionar recordatorios y notificaciones",
        "actor": "Administrador",
        "boundary": "UI_notificacion",
        "control": "NotificacionController",
        "entity": "Notificacion"
    },
    {
        "cu": "CU27",
        "title": "CU27 - Consultar historial de participacion",
        "actor": "Participante",
        "boundary": "UI_historial",
        "control": "HistorialController",
        "entity": "Historial"
    }
]

def clean_temporary_packages(pkg_parent):
    """Elimina paquetes de prueba previos para mantener el modelo impecable."""
    temp_names = ["Test Single", "Test Multi", "Test Collaboration"]
    for i in range(pkg_parent.Packages.Count - 1, -1, -1):
        p = pkg_parent.Packages.GetAt(i)
        if p.Name in temp_names or p.Name.startswith("Test"):
            print(f"  Eliminando paquete temporal: {p.Name}")
            pkg_parent.Packages.Delete(i)
    pkg_parent.Packages.Refresh()


def build_communication_diagrams(rep, pkg_parent, project):
    print("\n=======================================================")
    print("1. GENERANDO DIAGRAMAS DE COMUNICACION (SPRINT 3)")
    print("=======================================================")

    # Limpiar cualquier paquete previo de Sprint 3 de comunicacion
    for i in range(pkg_parent.Packages.Count - 1, -1, -1):
        p = pkg_parent.Packages.GetAt(i)
        if "Sprint 3 - Diagramas de Comunicacion" in p.Name:
            print(f"  Eliminando paquete previo: {p.Name}")
            pkg_parent.Packages.Delete(i)
    pkg_parent.Packages.Refresh()

    pkg_comm = pkg_parent.Packages.AddNew("Sprint 3 - Diagramas de Comunicacion", "")
    pkg_comm.Update()
    pkg_parent.Packages.Refresh()
    print(f"Paquete creado: {pkg_comm.Name} (ID: {pkg_comm.PackageID})")

    # Coordenadas: Actor (alto 75, ancho 45), Esferas (50x50 compactas)
    # Eje horizontal central en Y = -137
    # Actor: X=50..95, Y=-100..-175
    # Boundary: X=300..350, Y=-112..-162 (centro Y=-137)
    # Control: X=580..630, Y=-112..-162 (centro Y=-137)
    # Entity: X=860..910, Y=-112..-162 (centro Y=-137)
    layout_coords = {
        "actor": (50, 95, -100, -175),
        "boundary": (300, 350, -112, -162),
        "control": (580, 630, -112, -162),
        "entity": (860, 910, -112, -162)
    }

    # Textos de los 4 mensajes apilados según el modelo
    t1_text = "4: eliminar()      -->\n3: editar()        -->\n2: crear()         -->\n1: mostrar()       -->"
    t2_text = "4.1: destroy()     -->\n3.1: update()      -->\n2.1: store()       -->\n1.1: index()       -->"
    t3_text = "4.2: delete()      -->\n3.2: update()      -->\n2.2: create()      -->\n1.2: get()         -->"

    text_coords = [
        (t1_text, 130, 275, -50, -125),
        (t2_text, 395, 555, -50, -125),
        (t3_text, 675, 835, -50, -125)
    ]

    for item in COMM_DIAGRAMS_DEF:
        cu_code = item["cu"]
        diag_title = item["title"]
        print(f"\n-> Procesando {cu_code}: {diag_title}")

        diag = pkg_comm.Diagrams.AddNew(diag_title, "Communication")
        diag.Update()

        # Elementos BCE
        el_actor = pkg_comm.Elements.AddNew(item["actor"], "Actor")
        el_actor.Update()

        el_boundary = pkg_comm.Elements.AddNew(item["boundary"], "Class")
        el_boundary.Stereotype = "boundary"
        el_boundary.Update()

        el_control = pkg_comm.Elements.AddNew(item["control"], "Class")
        el_control.Stereotype = "control"
        el_control.Update()

        el_entity = pkg_comm.Elements.AddNew(item["entity"], "Class")
        el_entity.Stereotype = "entity"
        el_entity.Update()

        elements = {
            "actor": el_actor,
            "boundary": el_boundary,
            "control": el_control,
            "entity": el_entity
        }

        # Ubicar los elementos principales en el diagrama
        for key, (l, r, t, b) in layout_coords.items():
            el = elements[key]
            d_obj = diag.DiagramObjects.AddNew("", "")
            d_obj.ElementID = el.ElementID
            d_obj.left = l
            d_obj.right = r
            d_obj.top = t
            d_obj.bottom = b
            d_obj.Update()

        # 1. ENLACES BASE: Linea horizontal solida 'Association' sin texto para maxima limpieza
        a1 = el_actor.Connectors.AddNew("", "Association")
        a1.SupplierID = el_boundary.ElementID
        a1.Direction = "Unspecified"
        a1.Update()

        a2 = el_boundary.Connectors.AddNew("", "Association")
        a2.SupplierID = el_control.ElementID
        a2.Direction = "Unspecified"
        a2.Update()

        a3 = el_control.Connectors.AddNew("", "Association")
        a3.SupplierID = el_entity.ElementID
        a3.Direction = "Unspecified"
        a3.Update()

        # 2. BLOQUES DE MENSAJES CRUD APILADOS SOBRE CADA LINEA (Elementos Text)
        for txt_content, l, r, t, b in text_coords:
            txt_el = pkg_comm.Elements.AddNew("", "Text")
            txt_el.Notes = txt_content
            txt_el.Update()

            t_obj = diag.DiagramObjects.AddNew("", "")
            t_obj.ElementID = txt_el.ElementID
            t_obj.left = l
            t_obj.right = r
            t_obj.top = t
            t_obj.bottom = b
            t_obj.Update()

        diag.Update()
        rep.SaveDiagram(diag.DiagramID)

        # Exportar imagen PNG de alta resolucion
        out_name = f"diag_comunicacion_{cu_code.lower()}_ea.png"
        out_path = os.path.join(EXPORT_DIR, out_name)
        rep.OpenDiagram(diag.DiagramID)
        project.SaveDiagramImageToFile(out_path)
        print(f"   Exportado a: {out_path}")

        # Copiar al directorio de assets para el documento
        asset_target = os.path.join(ASSETS_DIR, f"diag_comunicacion_{cu_code.lower()}.png")
        if os.path.exists(out_path):
            shutil.copy2(out_path, asset_target)

    pkg_comm.Update()


def build_deployment_diagram(rep, pkg_parent, project):
    print("\n=======================================================")
    print("2. GENERANDO DIAGRAMA DE DESPLIEGUE EN AWS (DISENO FISICO)")
    print("=======================================================")

    diag_title = "Diagrama de Despliegue - AWS Cloud Multi-Tenant"

    for i in range(pkg_parent.Diagrams.Count - 1, -1, -1):
        d = pkg_parent.Diagrams.GetAt(i)
        if "Despliegue" in d.Name and "AWS" in d.Name:
            pkg_parent.Diagrams.Delete(i)
    pkg_parent.Diagrams.Refresh()

    diag = pkg_parent.Diagrams.AddNew(diag_title, "Deployment")
    diag.Update()
    print(f"Diagrama de Despliegue creado: {diag.Name} (ID: {diag.DiagramID})")

    # 1. Nodos Cliente
    node_movil = pkg_parent.Elements.AddNew("Dispositivo Movil\n(Android / iOS)", "Device")
    node_movil.Stereotype = "device"
    node_movil.Update()

    art_flutter = pkg_parent.Elements.AddNew("App Movil Flutter\n(Wallet QR / Staff Scanner)", "Artifact")
    art_flutter.Stereotype = "artifact"
    art_flutter.Update()

    node_pc = pkg_parent.Elements.AddNew("Dispositivo del Usuario\n(PC / Laptop / Tablet)", "Device")
    node_pc.Stereotype = "device"
    node_pc.Update()

    env_browser = pkg_parent.Elements.AddNew("Navegador Web\n(Chrome / Firefox / Edge)", "ExecutionEnvironment")
    env_browser.Stereotype = "executionEnvironment"
    env_browser.Update()

    art_angular = pkg_parent.Elements.AddNew("Frontend Web Angular 20\n(SPA Build / Dist)", "Artifact")
    art_angular.Stereotype = "artifact"
    art_angular.Update()

    # 2. Red Internet
    node_internet = pkg_parent.Elements.AddNew("Internet\n(HTTPS / WSS / TLS)", "Node")
    node_internet.Stereotype = "network"
    node_internet.Update()

    # 3. AWS Cloud (Boundary / Cloud)
    node_aws = pkg_parent.Elements.AddNew("AWS Cloud (VPC Multi-Tenant)", "Node")
    node_aws.Stereotype = "cloud"
    node_aws.Update()

    node_alb = pkg_parent.Elements.AddNew("AWS Application Load Balancer\n(ALB / SSL / Reverse Proxy)", "Node")
    node_alb.Stereotype = "loadBalancer"
    node_alb.Update()

    node_ec2_backend = pkg_parent.Elements.AddNew("AWS EC2 / ECS Cluster\n(Docker Runtime)", "Node")
    node_ec2_backend.Stereotype = "server"
    node_ec2_backend.Update()

    art_spring = pkg_parent.Elements.AddNew("Contenedor Backend\n(Spring Boot 3 / Java 21 / REST & WSS)", "Artifact")
    art_spring.Stereotype = "artifact"
    art_spring.Update()

    art_fastapi = pkg_parent.Elements.AddNew("Contenedor Servicio IA\n(FastAPI / Python 3.11 / ML)", "Artifact")
    art_fastapi.Stereotype = "artifact"
    art_fastapi.Update()

    node_rds = pkg_parent.Elements.AddNew("Amazon RDS PostgreSQL 17\n(BD Multi-Tenant Aislada)", "Node")
    node_rds.Stereotype = "database"
    node_rds.Update()

    node_kms = pkg_parent.Elements.AddNew("AWS KMS\n(Cifrado Bitacora y Llaves)", "Node")
    node_kms.Stereotype = "security"
    node_kms.Update()

    node_s3 = pkg_parent.Elements.AddNew("Amazon S3 Bucket\n(Backups y Assets)", "Node")
    node_s3.Stereotype = "storage"
    node_s3.Update()

    # 4. Servicios Externos
    node_brevo = pkg_parent.Elements.AddNew("Brevo Cloud API\n(SMTP / Email Transaccional)", "Node")
    node_brevo.Stereotype = "external"
    node_brevo.Update()

    node_stripe = pkg_parent.Elements.AddNew("Stripe API Cloud\n(Pasarela de Pagos SaaS)", "Node")
    node_stripe.Stereotype = "external"
    node_stripe.Update()

    # Disposicion en el diagrama (DiagramObjects)
    objects_pos = [
        # Clientes izquierda
        (node_movil, 30, 230, -50, -190),
        (art_flutter, 50, 210, -110, -170),
        (node_pc, 30, 230, -230, -420),
        (env_browser, 45, 215, -280, -350),
        (art_angular, 55, 205, -360, -410),

        # Internet central
        (node_internet, 280, 420, -160, -280),

        # AWS Cloud contenedor grande
        (node_aws, 460, 1260, -20, -680),

        # Componentes dentro de AWS Cloud
        (node_alb, 490, 680, -160, -280),
        (node_ec2_backend, 730, 1020, -70, -400),
        (art_spring, 755, 995, -140, -240),
        (art_fastapi, 755, 995, -270, -370),

        (node_rds, 1070, 1240, -70, -230),
        (node_kms, 1070, 1240, -260, -390),
        (node_s3, 1070, 1240, -420, -550),

        # Externos derecha/inferior
        (node_brevo, 750, 960, -460, -580),
        (node_stripe, 500, 700, -460, -580)
    ]

    for el, l, r, t, b in objects_pos:
        obj = diag.DiagramObjects.AddNew("", "")
        obj.ElementID = el.ElementID
        obj.left = l
        obj.right = r
        obj.top = t
        obj.bottom = b
        obj.Update()

    # Conectores de Comunicacion
    conns_def = [
        (node_movil, node_internet, "Association", "HTTPS / WSS"),
        (node_pc, node_internet, "Association", "HTTPS"),
        (node_internet, node_alb, "Association", "TLS 443"),
        (node_alb, node_ec2_backend, "Association", "Reverse Proxy (HTTP/WSS)"),
        (art_spring, node_rds, "Association", "JDBC TCP 5432"),
        (art_fastapi, node_rds, "Association", "Postgres Driver"),
        (art_spring, art_fastapi, "Association", "REST Inter-service"),
        (art_spring, node_kms, "Association", "AWS SDK Cifrado"),
        (art_spring, node_s3, "Association", "AWS S3 SDK"),
        (art_spring, node_brevo, "Association", "SMTP 587 / REST"),
        (art_spring, node_stripe, "Association", "Stripe Webhooks / HTTPS")
    ]

    for src, tgt, ctype, cname in conns_def:
        conn = src.Connectors.AddNew(cname, ctype)
        conn.SupplierID = tgt.ElementID
        conn.Direction = "Source -> Destination"
        conn.Update()

    diag.Update()
    rep.SaveDiagram(diag.DiagramID)

    out_path = os.path.join(EXPORT_DIR, "diag_despliegue_aws_ea.png")
    rep.OpenDiagram(diag.DiagramID)
    project.SaveDiagramImageToFile(out_path)
    print(f"Diagrama de Despliegue exportado a: {out_path}")
    shutil.copy2(out_path, os.path.join(ASSETS_DIR, "diag_despliegue_aws.png"))


def build_package_diagram(rep, pkg_parent, project):
    print("\n=======================================================")
    print("3. GENERANDO DIAGRAMA DE PAQUETES (DISENO LOGICO)")
    print("=======================================================")

    diag_title = "Diagrama de Paquetes - Diseno Logico Arquitectura"

    for i in range(pkg_parent.Diagrams.Count - 1, -1, -1):
        d = pkg_parent.Diagrams.GetAt(i)
        if "Paquetes" in d.Name or "Logico" in d.Name:
            pkg_parent.Diagrams.Delete(i)
    pkg_parent.Diagrams.Refresh()

    diag = pkg_parent.Diagrams.AddNew(diag_title, "Package")
    diag.Update()
    print(f"Diagrama de Paquetes creado: {diag.Name} (ID: {diag.DiagramID})")

    # Crear Paquetes Logicos del Sistema
    p_presentacion = pkg_parent.Elements.AddNew("Capa de Presentacion\n(Angular 20 / Flutter)", "Package")
    p_presentacion.Update()

    p_seguridad = pkg_parent.Elements.AddNew("Seguridad y Control de Acceso\n(JWT, Roles, Permisos)", "Package")
    p_seguridad.Update()

    p_saas = pkg_parent.Elements.AddNew("Gestion SaaS y Multi-Tenant\n(Aislamiento, Organizaciones, Stripe)", "Package")
    p_saas.Update()

    p_eventos = pkg_parent.Elements.AddNew("Gestion de Eventos y Espacios\n(Sprint 1: Eventos, Salas, Recursos)", "Package")
    p_eventos.Update()

    p_inscripciones = pkg_parent.Elements.AddNew("Inscripciones y Staff\n(Sprint 2: Inscritos, Postulaciones)", "Package")
    p_inscripciones.Update()

    p_acreditacion = pkg_parent.Elements.AddNew("Control Acceso y Acreditacion\n(Sprint 3: QR, Check-in, Aforo WSS)", "Package")
    p_acreditacion.Update()

    p_notificaciones = pkg_parent.Elements.AddNew("Notificaciones y Recordatorios\n(Sprint 3: Brevo Mailer, Plantillas)", "Package")
    p_notificaciones.Update()

    p_ia = pkg_parent.Elements.AddNew("Servicio de Inteligencia Artificial\n(Sprint 4: FastAPI, Recomendador)", "Package")
    p_ia.Update()

    p_auditoria = pkg_parent.Elements.AddNew("Auditoria y Bitacora Cifrada\n(KMS, Log Inmutable por Tenant)", "Package")
    p_auditoria.Update()

    p_persistencia = pkg_parent.Elements.AddNew("Persistencia y Base de Datos\n(PostgreSQL 17, JPA, Flyway)", "Package")
    p_persistencia.Update()

    # Layout de Paquetes en el diagrama
    pkg_positions = [
        # Nivel 1: Presentacion (arriba)
        (p_presentacion, 380, 680, -40, -170),

        # Nivel 2: Seguridad y SaaS (lado a lado)
        (p_seguridad, 80, 360, -220, -350),
        (p_saas, 700, 980, -220, -350),

        # Nivel 3: Dominio Central (Eventos, Inscripciones, Acreditacion, Notificaciones)
        (p_eventos, 80, 360, -410, -540),
        (p_inscripciones, 390, 670, -410, -540),
        (p_acreditacion, 700, 980, -410, -540),
        (p_notificaciones, 1010, 1290, -410, -540),

        # Nivel 4: Servicios de Soporte (IA, Auditoria)
        (p_ia, 180, 460, -600, -730),
        (p_auditoria, 600, 880, -600, -730),

        # Nivel 5: Persistencia (Base)
        (p_persistencia, 380, 680, -790, -920)
    ]

    for el, l, r, t, b in pkg_positions:
        obj = diag.DiagramObjects.AddNew("", "")
        obj.ElementID = el.ElementID
        obj.left = l
        obj.right = r
        obj.top = t
        obj.bottom = b
        obj.Update()

    # Dependencias entre paquetes («use», «import»)
    dependencies_def = [
        (p_presentacion, p_seguridad, "«use» autentica"),
        (p_presentacion, p_acreditacion, "«use» valida QR"),
        (p_presentacion, p_eventos, "«use» consulta agenda"),
        (p_acreditacion, p_inscripciones, "«import» verifica estado"),
        (p_acreditacion, p_eventos, "«import» valida salas/aforo"),
        (p_acreditacion, p_notificaciones, "«use» emite recordatorios"),
        (p_acreditacion, p_auditoria, "«use» registra check-in"),
        (p_acreditacion, p_saas, "«use» aisla por tenant"),
        (p_inscripciones, p_saas, "«use» factura con Stripe"),
        (p_ia, p_eventos, "«use» analiza categorias"),
        (p_auditoria, p_saas, "«use» asocia tenant"),
        (p_eventos, p_persistencia, "«use» persiste"),
        (p_inscripciones, p_persistencia, "«use» persiste"),
        (p_acreditacion, p_persistencia, "«use» persiste"),
        (p_auditoria, p_persistencia, "«use» persiste")
    ]

    for src, tgt, dname in dependencies_def:
        conn = src.Connectors.AddNew(dname, "Dependency")
        conn.SupplierID = tgt.ElementID
        conn.Direction = "Source -> Destination"
        conn.Update()

    diag.Update()
    rep.SaveDiagram(diag.DiagramID)

    out_path = os.path.join(EXPORT_DIR, "diag_paquetes_logico_ea.png")
    rep.OpenDiagram(diag.DiagramID)
    project.SaveDiagramImageToFile(out_path)
    print(f"Diagrama de Paquetes exportado a: {out_path}")
    shutil.copy2(out_path, os.path.join(ASSETS_DIR, "diag_paquetes_logico.png"))


def main():
    print(f"Conectando a Enterprise Architect: {MODEL_PATH}")
    rep = win32com.client.Dispatch("EA.Repository")
    opened = rep.OpenFile(MODEL_PATH)
    if not opened:
        raise RuntimeError("No se pudo abrir el modelo en EA.")

    pkg_parent = rep.GetPackageByID(2) # Diagramas/SI2
    project = rep.GetProjectInterface()

    try:
        # 0. Limpiar paquetes de prueba
        clean_temporary_packages(pkg_parent)

        # 1. Corregir y regenerar los diagramas de comunicacion
        build_communication_diagrams(rep, pkg_parent, project)

        # 2. Crear diagrama de despliegue en AWS (Diseno Fisico)
        build_deployment_diagram(rep, pkg_parent, project)

        # 3. Crear diagrama de paquetes (Diseno Logico)
        build_package_diagram(rep, pkg_parent, project)

        try:
            pkg_parent.Update()
        except Exception:
            pass
        print("\n¡Todos los diagramas fueron construidos y exportados con exito!")

    finally:
        try:
            rep.Exit()
        except Exception:
            pass
        print("Conexion con Enterprise Architect cerrada limpiamente.")

    # Copiar archivo .eap actualizado al repositorio
    repo_eap_copy = os.path.join(r"c:\Proyecto-GestionEventos\plataforma-gestion-eventos\documentacion", "Diagramas_Si2.eap")
    try:
        shutil.copy2(MODEL_PATH, repo_eap_copy)
        print(f"Modelo EA actualizado y versionado en: {repo_eap_copy}")
    except Exception as e:
        print(f"Advertencia al copiar eap: {e}")


if __name__ == "__main__":
    main()
