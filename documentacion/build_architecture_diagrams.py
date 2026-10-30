# -*- coding: utf-8 -*-
"""
Generador de Diagramas de Arquitectura en Enterprise Architect 15:
1. Diseño Fisico: Diagrama de Despliegue en AWS Cloud Multi-Tenant
2. Diseño Logico: Diagrama de Paquetes de la Arquitectura
Exporta imagenes independientes PNG en alta resolucion y sincroniza el archivo .eap.
NO modifica ni pega nada en el documento del Sprint.
"""

import os
import shutil
import win32com.client

MODEL_PATH = r"C:\Users\ASUS\Diagramas Si2.eap"
EXPORT_DIR = r"c:\Proyecto-GestionEventos\plataforma-gestion-eventos\documentacion\diagramas"
DOWNLOADS_DIR = r"C:\Users\ASUS\Downloads"
os.makedirs(EXPORT_DIR, exist_ok=True)
os.makedirs(DOWNLOADS_DIR, exist_ok=True)


def build_architecture_diagrams():
    print(f"Conectando con Enterprise Architect: {MODEL_PATH}")
    rep = win32com.client.Dispatch("EA.Repository")
    opened = rep.OpenFile(MODEL_PATH)
    if not opened:
        raise RuntimeError("No se pudo abrir el modelo en EA.")

    pkg_root = rep.GetPackageByID(2) # Diagramas/SI2
    project = rep.GetProjectInterface()

    try:
        # 1. Crear o limpiar el paquete 'Diseño de Arquitectura'
        pkg_arch = None
        for i in range(pkg_root.Packages.Count - 1, -1, -1):
            p = pkg_root.Packages.GetAt(i)
            if "Diseño de Arquitectura" in p.Name or "Diseno de Arquitectura" in p.Name or "Arquitectura" in p.Name:
                print(f"  Eliminando paquete previo: {p.Name}")
                pkg_root.Packages.Delete(i)
        pkg_root.Packages.Refresh()

        # También limpiar diagramas sueltos de despliegue o paquetes en la raíz de Diagramas/SI2
        for i in range(pkg_root.Diagrams.Count - 1, -1, -1):
            d = pkg_root.Diagrams.GetAt(i)
            if "Despliegue" in d.Name or "Paquetes" in d.Name:
                print(f"  Limpiando diagrama suelto previo: {d.Name}")
                pkg_root.Diagrams.Delete(i)
        pkg_root.Diagrams.Refresh()

        pkg_arch = pkg_root.Packages.AddNew("Diseño de Arquitectura", "")
        pkg_arch.Update()
        pkg_root.Packages.Refresh()
        print(f"Paquete creado: {pkg_arch.Name} (ID: {pkg_arch.PackageID})")

        # ==============================================================
        # 1. DISEÑO FÍSICO: DIAGRAMA DE DESPLIEGUE EN AWS
        # ==============================================================
        print("\n-------------------------------------------------------")
        print("Generando: Diseño Fisico - Diagrama de Despliegue en AWS")
        print("-------------------------------------------------------")
        diag_desp = pkg_arch.Diagrams.AddNew("Diseño Fisico - Diagrama de Despliegue en AWS", "Deployment")
        diag_desp.Update()

        # Nodos Cliente (Izquierda)
        node_movil = pkg_arch.Elements.AddNew("Dispositivo Movil\n(Android / iOS)", "Device")
        node_movil.Stereotype = "device"
        node_movil.Update()

        art_flutter = pkg_arch.Elements.AddNew("App Movil Flutter\n(Wallet QR / Staff Scanner)", "Artifact")
        art_flutter.Stereotype = "artifact"
        art_flutter.Update()

        node_pc = pkg_arch.Elements.AddNew("Estacion de Trabajo\n(PC / Laptop / Tablet)", "Device")
        node_pc.Stereotype = "device"
        node_pc.Update()

        env_browser = pkg_arch.Elements.AddNew("Navegador Web\n(Chrome / Firefox / Edge)", "ExecutionEnvironment")
        env_browser.Stereotype = "executionEnvironment"
        env_browser.Update()

        art_angular = pkg_arch.Elements.AddNew("Frontend Web Angular 20\n(SPA Build / Dist)", "Artifact")
        art_angular.Stereotype = "artifact"
        art_angular.Update()

        # Red Internet (Centro-Izquierda)
        node_internet = pkg_arch.Elements.AddNew("Internet Publica\n(HTTPS / WSS / TLS 1.3)", "Node")
        node_internet.Stereotype = "network"
        node_internet.Update()

        # Nube AWS VPC (Centro-Derecha)
        node_aws = pkg_arch.Elements.AddNew("Amazon Web Services (AWS VPC Multi-Tenant)", "Node")
        node_aws.Stereotype = "cloud"
        node_aws.Update()

        node_alb = pkg_arch.Elements.AddNew("AWS Application Load Balancer\n(ALB / SSL / Reverse Proxy)", "Node")
        node_alb.Stereotype = "loadBalancer"
        node_alb.Update()

        node_ec2 = pkg_arch.Elements.AddNew("AWS EC2 / ECS Cluster\n(Docker Runtime Linux)", "Node")
        node_ec2.Stereotype = "server"
        node_ec2.Update()

        art_spring = pkg_arch.Elements.AddNew("Contenedor Backend Principal\n(Spring Boot 3 / Java 21 / REST & WSS)", "Artifact")
        art_spring.Stereotype = "artifact"
        art_spring.Update()

        art_fastapi = pkg_arch.Elements.AddNew("Contenedor Microservicio IA\n(FastAPI / Python 3.11 / ML)", "Artifact")
        art_fastapi.Stereotype = "artifact"
        art_fastapi.Update()

        # Datos y Servicios AWS
        node_rds = pkg_arch.Elements.AddNew("Amazon RDS PostgreSQL 17\n(Instancia Multi-Tenant / Esquemas Aislados)", "Node")
        node_rds.Stereotype = "database"
        node_rds.Update()

        node_kms = pkg_arch.Elements.AddNew("AWS KMS\n(Cifrado Bitacora y Claves)", "Node")
        node_kms.Stereotype = "security"
        node_kms.Update()

        node_s3 = pkg_arch.Elements.AddNew("Amazon S3 Bucket\n(PDFs, QR Assets y Backups)", "Node")
        node_s3.Stereotype = "storage"
        node_s3.Update()

        # Servicios Externos (Abajo, fuera de AWS)
        node_stripe = pkg_arch.Elements.AddNew("Stripe Cloud API\n(Pasarela Pagos SaaS)", "Node")
        node_stripe.Stereotype = "external"
        node_stripe.Update()

        node_brevo = pkg_arch.Elements.AddNew("Brevo Cloud API\n(SMTP / Email Transaccional)", "Node")
        node_brevo.Stereotype = "external"
        node_brevo.Update()

        # Ubicación visual precisa en el diagrama de despliegue
        desp_pos = [
            # Clientes izquierda
            (node_movil, 30, 240, -50, -180),
            (art_flutter, 50, 220, -100, -165),

            (node_pc, 30, 240, -220, -410),
            (env_browser, 45, 225, -270, -335),
            (art_angular, 55, 215, -345, -395),

            # Internet
            (node_internet, 280, 420, -150, -270),

            # Contenedor AWS Cloud
            (node_aws, 460, 1360, -20, -480),

            # Elementos internos de AWS
            (node_alb, 490, 680, -150, -270),
            (node_ec2, 730, 1070, -50, -440),
            (art_spring, 755, 1045, -120, -225),
            (art_fastapi, 755, 1045, -280, -385),

            (node_rds, 1120, 1330, -50, -190),
            (node_kms, 1120, 1330, -210, -320),
            (node_s3, 1120, 1330, -340, -450),

            # Servicios externos (abajo de AWS)
            (node_stripe, 540, 750, -520, -640),
            (node_brevo, 820, 1030, -520, -640)
        ]

        for el, l, r, t, b in desp_pos:
            obj = diag_desp.DiagramObjects.AddNew("", "")
            obj.ElementID = el.ElementID
            obj.left = l; obj.right = r; obj.top = t; obj.bottom = b
            obj.Update()

        # Enlaces de comunicación con protocolos explícitos
        desp_conns = [
            (node_movil, node_internet, "Association", "HTTPS / WSS"),
            (node_pc, node_internet, "Association", "HTTPS (443)"),
            (node_internet, node_alb, "Association", "TLS 443"),
            (node_alb, art_spring, "Association", "Reverse Proxy (HTTP/WSS)"),
            (art_spring, art_fastapi, "Association", "REST Inter-service (8000)"),
            (art_spring, node_rds, "Association", "JDBC TCP 5432 (TLS)"),
            (art_fastapi, node_rds, "Association", "asyncpg TCP 5432"),
            (art_spring, node_kms, "Association", "AWS SDK KMS Cifrado"),
            (art_spring, node_s3, "Association", "AWS SDK S3 Uploads"),
            (art_spring, node_stripe, "Association", "HTTPS REST Webhooks"),
            (art_spring, node_brevo, "Association", "SMTP TLS 587 / REST v3")
        ]

        for src, tgt, ctype, cname in desp_conns:
            conn = src.Connectors.AddNew(cname, ctype)
            conn.SupplierID = tgt.ElementID
            conn.Direction = "Source -> Destination"
            conn.Update()

        diag_desp.Update()
        rep.SaveDiagram(diag_desp.DiagramID)

        out_desp_ea = os.path.join(EXPORT_DIR, "Diagrama_Despliegue_AWS.png")
        rep.OpenDiagram(diag_desp.DiagramID)
        project.SaveDiagramImageToFile(out_desp_ea)
        print(f"Diagrama de Despliegue exportado a: {out_desp_ea}")
        shutil.copy2(out_desp_ea, os.path.join(DOWNLOADS_DIR, "Diagrama_Despliegue_AWS.png"))

        # ==============================================================
        # 2. DISEÑO LÓGICO: DIAGRAMA DE PAQUETES
        # ==============================================================
        print("\n-------------------------------------------------------")
        print("Generando: Diseño Logico - Diagrama de Paquetes")
        print("-------------------------------------------------------")
        diag_paq = pkg_arch.Diagrams.AddNew("Diseño Logico - Diagrama de Paquetes", "Package")
        diag_paq.Update()

        # Nivel 1: Capa de Presentación
        p_pres = pkg_arch.Elements.AddNew("Capa de Presentacion\n(Angular 20 SPA / Flutter Mobile)", "Package")
        p_pres.Update()

        # Nivel 2: Capa Transversal Seguridad y SaaS Multi-Tenant
        p_sec = pkg_arch.Elements.AddNew("Seguridad y Control Acceso\n(JWT, Roles, Permisos, Auth)", "Package")
        p_sec.Update()

        p_saas = pkg_arch.Elements.AddNew("Gestion SaaS y Multi-Tenant\n(Aislamiento Tenants, Stripe Billing)", "Package")
        p_saas.Update()

        # Nivel 3: Dominio Central (Módulos de Negocio)
        p_eventos = pkg_arch.Elements.AddNew("Gestion Eventos y Espacios\n(Sprint 1: Eventos, Salas, Recursos)", "Package")
        p_eventos.Update()

        p_inscrip = pkg_arch.Elements.AddNew("Inscripciones y Staff\n(Sprint 2: Inscritos, Pagos, Postulaciones)", "Package")
        p_inscrip.Update()

        p_acred = pkg_arch.Elements.AddNew("Acreditacion y Control Acceso\n(Sprint 3: Credenciales QR, Check-in, Aforo WSS)", "Package")
        p_acred.Update()

        p_notif = pkg_arch.Elements.AddNew("Notificaciones y Recordatorios\n(Sprint 3: Brevo Mailer, Plantillas)", "Package")
        p_notif.Update()

        # Nivel 4: Servicios de Soporte
        p_ia = pkg_arch.Elements.AddNew("Servicio Inteligencia Artificial\n(Sprint 4: FastAPI, Recomendador ML)", "Package")
        p_ia.Update()

        p_audit = pkg_arch.Elements.AddNew("Auditoria y Bitacora Cifrada\n(KMS Criptografia, Logs Inmutables)", "Package")
        p_audit.Update()

        # Nivel 5: Persistencia
        p_persist = pkg_arch.Elements.AddNew("Persistencia y Base de Datos\n(PostgreSQL 17, JPA, Flyway)", "Package")
        p_persist.Update()

        # Distribución en capas simétricas y limpias
        paq_pos = [
            # Nivel 1: Presentación (arriba centrado)
            (p_pres, 460, 860, -30, -150),

            # Nivel 2: Seguridad y SaaS (lado a lado)
            (p_sec, 100, 420, -200, -320),
            (p_saas, 900, 1220, -200, -320),

            # Nivel 3: Dominio (4 bloques ordenados horizontalmente con suficiente separación)
            (p_eventos, 40, 310, -380, -500),
            (p_inscrip, 350, 620, -380, -500),
            (p_acred, 660, 950, -380, -500),
            (p_notif, 990, 1300, -380, -500),

            # Nivel 4: Servicios de Soporte
            (p_ia, 230, 530, -560, -680),
            (p_audit, 720, 1020, -560, -680),

            # Nivel 5: Persistencia (abajo centrado)
            (p_persist, 460, 860, -740, -860)
        ]

        for el, l, r, t, b in paq_pos:
            obj = diag_paq.DiagramObjects.AddNew("", "")
            obj.ElementID = el.ElementID
            obj.left = l; obj.right = r; obj.top = t; obj.bottom = b
            obj.Update()

        # Dependencias limpias que descienden de capa a capa
        paq_deps = [
            # Nivel 1 -> Nivel 2 y 3
            (p_pres, p_sec, "«use» autentica"),
            (p_pres, p_inscrip, "«use» gestiona"),
            (p_pres, p_acred, "«use» credencial QR"),
            (p_pres, p_saas, "«use» tenant"),

            # Nivel 3 entre sí
            (p_inscrip, p_eventos, "«use» valida eventos"),
            (p_acred, p_inscrip, "«import» verifica inscripcion"),
            (p_acred, p_notif, "«use» emite alertas"),

            # Nivel 3 hacia Nivel 2 (SaaS)
            (p_notif, p_saas, "«use» config tenant"),

            # Nivel 3 hacia Nivel 4 (Auditoría)
            (p_acred, p_audit, "«use» registra check-in"),

            # Nivel 4 hacia Nivel 3 (IA consume eventos)
            (p_ia, p_eventos, "«use» dataset eventos"),

            # Hacia Persistencia (Nivel 5)
            (p_eventos, p_persist, "«use» persiste"),
            (p_inscrip, p_persist, "«use» persiste"),
            (p_acred, p_persist, "«use» persiste"),
            (p_audit, p_persist, "«use» persiste")
        ]

        for src, tgt, dname in paq_deps:
            conn = src.Connectors.AddNew(dname, "Dependency")
            conn.SupplierID = tgt.ElementID
            conn.Direction = "Source -> Destination"
            conn.Update()

        diag_paq.Update()
        rep.SaveDiagram(diag_paq.DiagramID)

        out_paq_ea = os.path.join(EXPORT_DIR, "Diagrama_Paquetes_Diseno_Logico.png")
        rep.OpenDiagram(diag_paq.DiagramID)
        project.SaveDiagramImageToFile(out_paq_ea)
        print(f"Diagrama de Paquetes exportado a: {out_paq_ea}")
        shutil.copy2(out_paq_ea, os.path.join(DOWNLOADS_DIR, "Diagrama_Paquetes_Diseno_Logico.png"))

        pkg_arch.Update()
        print("\n¡Ambos diagramas fueron generados y exportados exitosamente!")

    finally:
        try:
            rep.Exit()
        except Exception:
            pass
        print("Sesion de Enterprise Architect finalizada limpiamente.")

    # Sincronizar archivo .eap en repositorio
    repo_eap = os.path.join(r"c:\Proyecto-GestionEventos\plataforma-gestion-eventos\documentacion", "Diagramas_Si2.eap")
    try:
        shutil.copy2(MODEL_PATH, repo_eap)
        print(f"Archivo EA versionado en: {repo_eap}")
    except Exception as e:
        print(f"Aviso copia repo: {e}")


if __name__ == "__main__":
    build_architecture_diagrams()
