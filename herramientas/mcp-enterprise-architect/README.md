# MCP para Enterprise Architect

Servidor MCP local para Enterprise Architect 15 mediante Automation Interface COM.
Solo funciona en Windows con Enterprise Architect instalado.

## Instalacion

```powershell
cd herramientas\mcp-enterprise-architect
python -m venv .venv
.venv\Scripts\python.exe -m pip install -r requirements.txt
```

El modelo puede abrirse con la herramienta `ea_open_model` o configurando `EA_MODEL_PATH`.
El servidor no abre ningun modelo automaticamente para evitar modificar un archivo equivocado.

## Configuracion del cliente MCP

Ejemplo para clientes que usan un archivo JSON de servidores MCP:

```json
{
  "mcpServers": {
    "enterprise-architect": {
      "command": "C:\\Proyecto-GestionEventos\\plataforma-gestion-eventos\\herramientas\\mcp-enterprise-architect\\.venv\\Scripts\\python.exe",
      "args": [
        "C:\\Proyecto-GestionEventos\\plataforma-gestion-eventos\\herramientas\\mcp-enterprise-architect\\server.py"
      ],
      "env": {
        "EA_MODEL_PATH": "C:\\ruta\\a\\tu-modelo.qea"
      }
    }
  }
}
```

## Herramientas disponibles

- `ea_status`: estado de la conexion.
- `ea_open_model`: abre un modelo EA.
- `ea_close_model`: cierra el modelo.
- `ea_list_packages`: lista paquetes.
- `ea_list_elements`: lista elementos de un paquete.
- `ea_get_element`: inspecciona atributos, operaciones y conectores.
- `ea_create_class_diagram`: crea clases, relaciones y un diagrama nativo.
- `ea_create_sequence_diagram`: crea participantes, mensajes y un diagrama de secuencia nativo.
- `ea_export_diagram`: exporta un diagrama a una imagen para la documentacion.

## Uso recomendado

1. Abrir el modelo con `ea_open_model`.
2. Leer paquetes y elementos antes de escribir.
3. Crear un diagrama en un paquete de trabajo o documentación.
4. Revisar el resultado en Enterprise Architect y guardar una copia del modelo antes de cambios masivos.

Las operaciones COM se serializan en un unico hilo para evitar problemas de apartamentos COM en Windows. La creacion de diagramas valida clases y relaciones antes de escribirlas. La exportacion no sobrescribe archivos existentes salvo que se indique `overwrite=true`.

La primera versión cubre diagramas de clases y secuencia. Los diagramas de casos de uso y componentes pueden añadirse después sobre la misma conexión COM.
