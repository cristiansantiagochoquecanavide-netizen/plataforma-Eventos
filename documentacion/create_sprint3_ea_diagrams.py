# -*- coding: utf-8 -*-
"""
Generador nativo de Diagramas de Comunicacion / Colaboracion en Enterprise Architect 15
para todos los Casos de Uso del Sprint 3 con ESFERAS COMPACTAS (50x50) y MENSAJES SECUENCIALES APILADOS:
- CU21: Generar y visualizar credencial digital con QR
- CU22: Escanear y validar codigo QR
- CU23: Registrar asistencia (Check-in)
- CU24: Consultar asistencia y aforo en tiempo real
- CU25: Gestionar recordatorios y notificaciones
- CU27: Consultar historial de participacion

Sigue estrictamente el patron BCE (Boundary - Control - Entity) y dimensiones proporcionales al actor.
"""

import os
import shutil
import win32com.client

MODEL_PATH = r"C:\Users\ASUS\Diagramas Si2.eap"
EXPORT_DIR = r"c:\Proyecto-GestionEventos\plataforma-gestion-eventos\documentacion\diagramas"
ASSETS_DIR = r"c:\Proyecto-GestionEventos\plataforma-gestion-eventos\documentacion\sprint3_assets"
os.makedirs(EXPORT_DIR, exist_ok=True)
os.makedirs(ASSETS_DIR, exist_ok=True)

# Definicion exacta de los 6 Casos de Uso con el patron BCE y mensajes secuenciales apilados
DIAGRAMS_DEF = [
    {
        "cu": "CU21",
        "title": "CU21 - Generar y visualizar credencial digital con QR",
        "actor": "Participante",
        "boundary": "UI_credencial",
        "control": "CredencialController",
        "entity": "CredencialQR",
        "messages": [
            ("actor", "boundary", "1", "mostrar()"),
            ("actor", "boundary", "2", "crear()"),
            ("actor", "boundary", "3", "editar()"),
            ("actor", "boundary", "4", "eliminar()"),
            ("boundary", "control", "1.1", "index()"),
            ("boundary", "control", "2.1", "store()"),
            ("boundary", "control", "3.1", "update()"),
            ("boundary", "control", "4.1", "destroy()"),
            ("control", "entity", "1.2", "get()"),
            ("control", "entity", "2.2", "create()"),
            ("control", "entity", "3.2", "update()"),
            ("control", "entity", "4.2", "delete()")
        ]
    },
    {
        "cu": "CU22",
        "title": "CU22 - Escanear y validar codigo QR",
        "actor": "Staff",
        "boundary": "UI_escaner",
        "control": "ValidacionController",
        "entity": "ValidacionQR",
        "messages": [
            ("actor", "boundary", "1", "mostrar()"),
            ("actor", "boundary", "2", "escanear()"),
            ("actor", "boundary", "3", "validar()"),
            ("actor", "boundary", "4", "cancelar()"),
            ("boundary", "control", "1.1", "index()"),
            ("boundary", "control", "2.1", "scan()"),
            ("boundary", "control", "3.1", "validate()"),
            ("boundary", "control", "4.1", "reset()"),
            ("control", "entity", "1.2", "get()"),
            ("control", "entity", "2.2", "decode()"),
            ("control", "entity", "3.2", "verify()"),
            ("control", "entity", "4.2", "clear()")
        ]
    },
    {
        "cu": "CU23",
        "title": "CU23 - Registrar asistencia (Check-in)",
        "actor": "Staff",
        "boundary": "UI_asistencia",
        "control": "AsistenciaController",
        "entity": "Asistencia",
        "messages": [
            ("actor", "boundary", "1", "mostrar()"),
            ("actor", "boundary", "2", "registrar()"),
            ("actor", "boundary", "3", "editar()"),
            ("actor", "boundary", "4", "anular()"),
            ("boundary", "control", "1.1", "index()"),
            ("boundary", "control", "2.1", "store()"),
            ("boundary", "control", "3.1", "update()"),
            ("boundary", "control", "4.1", "destroy()"),
            ("control", "entity", "1.2", "get()"),
            ("control", "entity", "2.2", "create()"),
            ("control", "entity", "3.2", "update()"),
            ("control", "entity", "4.2", "delete()")
        ]
    },
    {
        "cu": "CU24",
        "title": "CU24 - Consultar asistencia y aforo en tiempo real",
        "actor": "Organizador",
        "boundary": "UI_aforo",
        "control": "AforoController",
        "entity": "ControlAforo",
        "messages": [
            ("actor", "boundary", "1", "mostrar()"),
            ("actor", "boundary", "2", "suscribir()"),
            ("actor", "boundary", "3", "configurar()"),
            ("actor", "boundary", "4", "exportar()"),
            ("boundary", "control", "1.1", "index()"),
            ("boundary", "control", "2.1", "connectWS()"),
            ("boundary", "control", "3.1", "updateThreshold()"),
            ("boundary", "control", "4.1", "exportCSV()"),
            ("control", "entity", "1.2", "get()"),
            ("control", "entity", "2.2", "getMetrics()"),
            ("control", "entity", "3.2", "update()"),
            ("control", "entity", "4.2", "getReport()")
        ]
    },
    {
        "cu": "CU25",
        "title": "CU25 - Gestionar recordatorios y notificaciones",
        "actor": "Administrador",
        "boundary": "UI_notificacion",
        "control": "NotificacionController",
        "entity": "Notificacion",
        "messages": [
            ("actor", "boundary", "1", "mostrar()"),
            ("actor", "boundary", "2", "crear()"),
            ("actor", "boundary", "3", "editar()"),
            ("actor", "boundary", "4", "eliminar()"),
            ("boundary", "control", "1.1", "index()"),
            ("boundary", "control", "2.1", "store()"),
            ("boundary", "control", "3.1", "update()"),
            ("boundary", "control", "4.1", "destroy()"),
            ("control", "entity", "1.2", "get()"),
            ("control", "entity", "2.2", "create()"),
            ("control", "entity", "3.2", "update()"),
            ("control", "entity", "4.2", "delete()")
        ]
    },
    {
        "cu": "CU27",
        "title": "CU27 - Consultar historial de participacion",
        "actor": "Participante",
        "boundary": "UI_historial",
        "control": "HistorialController",
        "entity": "Historial",
        "messages": [
            ("actor", "boundary", "1", "mostrar()"),
            ("actor", "boundary", "2", "filtrar()"),
            ("actor", "boundary", "3", "consultar()"),
            ("actor", "boundary", "4", "descargar()"),
            ("boundary", "control", "1.1", "index()"),
            ("boundary", "control", "2.1", "search()"),
            ("boundary", "control", "3.1", "show()"),
            ("boundary", "control", "4.1", "exportPDF()"),
            ("control", "entity", "1.2", "get()"),
            ("control", "entity", "2.2", "filter()"),
            ("control", "entity", "3.2", "getDetail()"),
            ("control", "entity", "4.2", "getCertificate()")
        ]
    }
]

def main():
    print(f"Conectando con Enterprise Architect: {MODEL_PATH}")
    rep = win32com.client.Dispatch("EA.Repository")
    opened = rep.OpenFile(MODEL_PATH)
    if not opened:
        raise RuntimeError("No se pudo abrir el modelo en Enterprise Architect.")

    # Obtener paquete Diagramas/SI2 (ID 2)
    pkg_parent = rep.GetPackageByID(2)
    print(f"Paquete padre: {pkg_parent.Name} (ID: {pkg_parent.PackageID})")

    # Limpiar cualquier paquete previo de test o sprint 3 para generacion limpia
    for i in range(pkg_parent.Packages.Count - 1, -1, -1):
        p = pkg_parent.Packages.GetAt(i)
        if "Sprint 3" in p.Name or "Test Layout" in p.Name:
            print(f"  Eliminando paquete previo: {p.Name}")
            pkg_parent.Packages.Delete(i)
    pkg_parent.Packages.Refresh()

    # Crear paquete definitivo
    pkg_s3 = pkg_parent.Packages.AddNew("Sprint 3 - Diagramas de Comunicacion", "")
    pkg_s3.Update()
    pkg_parent.Packages.Refresh()
    print(f"Paquete definitivo creado: {pkg_s3.Name} (ID: {pkg_s3.PackageID})")

    project = rep.GetProjectInterface()

    for item in DIAGRAMS_DEF:
        cu_code = item["cu"]
        diag_title = item["title"]
        print(f"\n--- Procesando {cu_code}: {diag_title} ---")

        # Crear diagrama tipo 'Communication'
        diag = pkg_s3.Diagrams.AddNew(diag_title, "Communication")
        diag.Update()
        print(f"  Diagrama creado ID: {diag.DiagramID}")

        # Crear elementos en el paquete
        # 1. Actor
        el_actor = pkg_s3.Elements.AddNew(item["actor"], "Actor")
        el_actor.Update()

        # 2. Boundary (esfera con linea vertical)
        el_boundary = pkg_s3.Elements.AddNew(item["boundary"], "Class")
        el_boundary.Stereotype = "boundary"
        el_boundary.Update()

        # 3. Control (esfera con flecha circular)
        el_control = pkg_s3.Elements.AddNew(item["control"], "Class")
        el_control.Stereotype = "control"
        el_control.Update()

        # 4. Entity (esfera con barra inferior)
        el_entity = pkg_s3.Elements.AddNew(item["entity"], "Class")
        el_entity.Stereotype = "entity"
        el_entity.Update()

        map_elements = {
            "actor": el_actor,
            "boundary": el_boundary,
            "control": el_control,
            "entity": el_entity
        }

        # COORDENADAS REFINADAS Y PROPORCIONADAS (ESFERAS COMPACTAS 50x50):
        # Actor: ancho=45, alto=75 (centrado verticalmente)
        # Esferas: ancho=50, alto=50 (alineadas a la altura del actor)
        layout_positions = {
            "actor": (40, 85, -100, -175),
            "boundary": (260, 310, -112, -162),
            "control": (530, 580, -112, -162),
            "entity": (800, 850, -112, -162)
        }

        # Agregar elementos al diagrama con las nuevas dimensiones compactas
        for key, (l, r, t, b) in layout_positions.items():
            el = map_elements[key]
            d_obj = diag.DiagramObjects.AddNew("", "")
            d_obj.ElementID = el.ElementID
            d_obj.left = l
            d_obj.right = r
            d_obj.top = t
            d_obj.bottom = b
            d_obj.Update()

        # Conectar elementos con mensajes de tipo 'Sequence' para que EA apile los mensajes
        # con sus respectivos numeros de secuencia (1, 1.1, 1.2...) y flechas de direccion
        for src_key, tgt_key, seq_num, op_name in item["messages"]:
            src_el = map_elements[src_key]
            tgt_el = map_elements[tgt_key]

            conn = src_el.Connectors.AddNew(op_name, "Sequence")
            conn.SupplierID = tgt_el.ElementID
            conn.SequenceNo = seq_num
            conn.Direction = "Source -> Destination"
            conn.Update()

        diag.Update()
        rep.SaveDiagram(diag.DiagramID)

        # Exportar imagen de alta resolucion directamente desde Enterprise Architect
        out_filename = f"diag_comunicacion_{cu_code.lower()}_ea.png"
        out_filepath = os.path.join(EXPORT_DIR, out_filename)
        rep.OpenDiagram(diag.DiagramID)
        project.SaveDiagramImageToFile(out_filepath)
        print(f"  Exportado exitosamente a: {out_filepath}")

        # Actualizar asset para el documento
        asset_target = os.path.join(ASSETS_DIR, f"diag_comunicacion_{cu_code.lower()}.png")
        if os.path.exists(out_filepath):
            shutil.copy2(out_filepath, asset_target)
            print(f"  Asset actualizado: {asset_target}")

    pkg_s3.Update()
    pkg_parent.Update()
    rep.CloseFile()
    rep.Exit()

    # Copiar version de respaldo al repositorio
    repo_eap_copy = os.path.join(r"c:\Proyecto-GestionEventos\plataforma-gestion-eventos\documentacion", "Diagramas_Si2.eap")
    shutil.copy2(MODEL_PATH, repo_eap_copy)
    print(f"\n¡Copia del archivo .eap actualizada en el repositorio!: {repo_eap_copy}")
    print("\n¡Todos los diagramas fueron actualizados con esferas compactas y mensajes apilados en Enterprise Architect!")

if __name__ == "__main__":
    main()
