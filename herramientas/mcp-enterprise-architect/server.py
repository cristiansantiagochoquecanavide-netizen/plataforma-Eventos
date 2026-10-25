"""MCP bridge for Enterprise Architect 15 Automation Interface on Windows."""

from __future__ import annotations

import json
import os
from concurrent.futures import ThreadPoolExecutor
from functools import wraps
from pathlib import Path
from typing import Any

from mcp.server.fastmcp import FastMCP


mcp = FastMCP("enterprise-architect")


class EAConnection:
    def __init__(self) -> None:
        self.repository: Any | None = None
        self.model_path: str | None = None
        self.executor = ThreadPoolExecutor(max_workers=1, initializer=self._initialize_com)

    @staticmethod
    def _initialize_com() -> None:
        import pythoncom

        pythoncom.CoInitialize()

    def run(self, function: Any, *args: Any, **kwargs: Any) -> Any:
        return self.executor.submit(function, *args, **kwargs).result()

    def connect(self, model_path: str | None = None) -> Any:
        if self.repository is not None:
            if model_path and Path(model_path).resolve() != Path(self.model_path or "").resolve():
                raise RuntimeError("Ya hay un modelo abierto. Cierra el modelo antes de abrir otro.")
            return self.repository

        try:
            import win32com.client
        except ImportError as error:
            raise RuntimeError(
                "Falta pywin32. Instala las dependencias con: python -m pip install -r requirements.txt"
            ) from error

        path = model_path or os.getenv("EA_MODEL_PATH")
        if not path:
            raise ValueError("Debes indicar model_path o definir EA_MODEL_PATH.")
        resolved = str(Path(path).expanduser().resolve())
        if not Path(resolved).exists():
            raise FileNotFoundError(f"No existe el modelo de Enterprise Architect: {resolved}")

        repository = win32com.client.Dispatch("EA.Repository")
        if not repository.OpenFile(resolved):
            raise RuntimeError(f"Enterprise Architect no pudo abrir el modelo: {resolved}")
        self.repository = repository
        self.model_path = resolved
        return repository

    def require(self) -> Any:
        if self.repository is None:
            raise RuntimeError("No hay un modelo abierto. Usa ea_open_model primero.")
        return self.repository

    def close(self) -> None:
        if self.repository is not None:
            repository = self.repository
            repository.CloseFile()
            repository.Exit()
        self.repository = None
        self.model_path = None


connection = EAConnection()


def _result(data: Any) -> str:
    return json.dumps(data, ensure_ascii=True, indent=2, default=str)


def ea_tool() -> Any:
    """Register a synchronous MCP tool on the single COM apartment thread."""
    def decorator(function: Any) -> Any:
        @wraps(function)
        def wrapper(*args: Any, **kwargs: Any) -> Any:
            return connection.run(function, *args, **kwargs)

        return mcp.tool()(wrapper)

    return decorator


def _collection_items(collection: Any) -> list[Any]:
    return [collection.GetAt(index) for index in range(collection.Count)]


def _element_summary(element: Any) -> dict[str, Any]:
    return {
        "id": int(element.ElementID),
        "name": str(element.Name),
        "type": str(element.Type),
        "stereotype": str(element.Stereotype or ""),
        "package_id": int(element.PackageID),
    }


@ea_tool()
def ea_status() -> str:
    """Devuelve el estado de la conexion y del modelo abierto."""
    return _result({"connected": connection.repository is not None, "model_path": connection.model_path})


@ea_tool()
def ea_open_model(model_path: str | None = None) -> str:
    """Abre un modelo .qea/.eap/.eapx existente en Enterprise Architect."""
    repository = connection.connect(model_path)
    return _result({"opened": True, "model_path": connection.model_path, "version": str(repository.LibraryVersion)})


@ea_tool()
def ea_close_model() -> str:
    """Cierra el modelo actual sin eliminar ni modificar el archivo."""
    connection.close()
    return _result({"closed": True})


@ea_tool()
def ea_list_packages(parent_package_id: int = 0) -> str:
    """Lista paquetes raíz o hijos de un paquete concreto."""
    repository = connection.require()
    if parent_package_id == 0:
        packages = _collection_items(repository.Models)
    else:
        packages = _collection_items(repository.GetPackageByID(parent_package_id).Packages)
    return _result([{"id": int(package.PackageID), "name": str(package.Name)} for package in packages])


@ea_tool()
def ea_list_elements(package_id: int) -> str:
    """Lista los elementos UML contenidos directamente en un paquete."""
    package = connection.require().GetPackageByID(package_id)
    return _result([_element_summary(element) for element in _collection_items(package.Elements)])


@ea_tool()
def ea_get_element(element_id: int) -> str:
    """Obtiene un elemento UML y sus atributos, operaciones y conectores."""
    element = connection.require().GetElementByID(element_id)
    data = _element_summary(element)
    data["attributes"] = [
        {"name": str(attribute.Name), "type": str(attribute.Type), "visibility": str(attribute.Visibility)}
        for attribute in _collection_items(element.Attributes)
    ]
    data["methods"] = [
        {"name": str(method.Name), "return_type": str(method.ReturnType), "visibility": str(method.Visibility)}
        for method in _collection_items(element.Methods)
    ]
    data["connectors"] = [
        {
            "type": str(connector.Type),
            "client_id": int(connector.ClientID),
            "supplier_id": int(connector.SupplierID),
            "stereotype": str(connector.Stereotype or ""),
        }
        for connector in _collection_items(element.Connectors)
    ]
    return _result(data)


@ea_tool()
def ea_create_class_diagram(
    package_id: int,
    diagram_name: str,
    classes: list[dict[str, Any]],
    relationships: list[dict[str, Any]] | None = None,
) -> str:
    """Crea un diagrama de clases nativo en EA a partir de una especificacion JSON.

    Cada clase admite name, stereotype, attributes y methods. Las relaciones usan
    source, target, type y stereotype, donde source y target son nombres de clase.
    """
    repository = connection.require()
    package = repository.GetPackageByID(package_id)
    if package is None:
        raise ValueError(f"No existe el paquete {package_id}.")
    if not classes:
        raise ValueError("Debes indicar al menos una clase.")
    if len(classes) > 100:
        raise ValueError("El limite por diagrama es de 100 clases.")

    names = [str(specification.get("name", "")).strip() for specification in classes]
    if any(not name for name in names):
        raise ValueError("Cada clase debe tener un nombre no vacio.")
    if len(names) != len(set(names)):
        raise ValueError("No puede haber nombres de clase duplicados.")
    allowed_relationships = {"Association", "Aggregation", "Composition", "Generalization", "Dependency"}
    for relationship in relationships or []:
        source = str(relationship.get("source", ""))
        target = str(relationship.get("target", ""))
        relationship_type = str(relationship.get("type", "Association"))
        if source not in names or target not in names:
            raise ValueError(f"Relacion invalida: {source} -> {target}.")
        if relationship_type not in allowed_relationships:
            raise ValueError(f"Tipo de relacion no soportado: {relationship_type}.")

    elements: dict[str, Any] = {}
    for specification in classes:
        name = str(specification["name"])
        element = package.Elements.AddNew(name, "Class")
        element.Stereotype = str(specification.get("stereotype", ""))
        element.Update()
        for attribute_spec in specification.get("attributes", []):
            attribute = element.Attributes.AddNew(
                str(attribute_spec["name"]), str(attribute_spec.get("type", ""))
            )
            attribute.Visibility = str(attribute_spec.get("visibility", "Public"))
            attribute.Update()
        for method_spec in specification.get("methods", []):
            method = element.Methods.AddNew(str(method_spec["name"]), str(method_spec.get("return_type", "void")))
            method.Visibility = str(method_spec.get("visibility", "Public"))
            method.Update()
        elements[name] = element

    for relationship in relationships or []:
        source = elements[str(relationship["source"])]
        target = elements[str(relationship["target"])]
        connector = source.Connectors.AddNew(
            str(relationship.get("name", "")), str(relationship.get("type", "Association"))
        )
        connector.SupplierID = target.ElementID
        connector.Stereotype = str(relationship.get("stereotype", ""))
        connector.Direction = str(relationship.get("direction", "Unspecified"))
        connector.Update()

    diagram = package.Diagrams.AddNew(diagram_name, "Class")
    diagram.Update()
    for element in elements.values():
        diagram_object = diagram.DiagramObjects.AddNew("", "")
        diagram_object.ElementID = element.ElementID
        diagram_object.Style = "l=0;r=240;t=0;b=120;"
        diagram_object.Update()
    diagram.Update()
    repository.SaveDiagram(diagram.DiagramID)
    return _result(
        {
            "created": True,
            "diagram_id": int(diagram.DiagramID),
            "diagram_name": diagram_name,
            "package_id": package_id,
            "elements": [_element_summary(element) for element in elements.values()],
            "relationships": len(relationships or []),
        }
    )


@ea_tool()
def ea_export_diagram(
    diagram_id: int, output_path: str, image_type: str = "PNG", overwrite: bool = False
) -> str:
    """Exporta un diagrama EA a una imagen para la documentacion."""
    repository = connection.require()
    destination = str(Path(output_path).expanduser().resolve())
    if not Path(destination).parent.exists():
        raise FileNotFoundError(f"No existe la carpeta de salida: {Path(destination).parent}")
    allowed_types = {"PNG", "JPG", "JPEG", "GIF", "BMP"}
    normalized_type = image_type.upper()
    if normalized_type not in allowed_types:
        raise ValueError(f"Formato no soportado. Usa uno de: {', '.join(sorted(allowed_types))}.")
    if Path(destination).resolve() == Path(connection.model_path or "").resolve():
        raise ValueError("La exportacion no puede sobrescribir el archivo del modelo.")
    if Path(destination).exists() and not overwrite:
        raise FileExistsError(f"Ya existe el archivo {destination}. Usa overwrite=true para reemplazarlo.")
    project = repository.GetProjectInterface()
    save_image = getattr(project, "SaveDiagramImageToFile", None)
    if save_image is not None:
        repository.OpenDiagram(diagram_id)
        result = save_image(destination)
    else:
        exporter = getattr(project, "ExportDiagram", None)
        if exporter is None:
            raise RuntimeError("La Automation Interface de EA no expone una operacion de exportacion compatible.")
        result = exporter(diagram_id, destination, normalized_type)
    if result is False or not Path(destination).exists():
        raise RuntimeError(f"Enterprise Architect no pudo exportar el diagrama {diagram_id}.")
    return _result({"exported": True, "diagram_id": diagram_id, "output_path": destination})


@ea_tool()
def ea_create_sequence_diagram(
    package_id: int,
    diagram_name: str,
    participants: list[dict[str, Any]],
    messages: list[dict[str, Any]],
) -> str:
    """Crea un diagrama de secuencia nativo para un caso de uso.

    Participants use name and optional stereotype. Messages use source, target,
    message and optional sequence. Participant names are used as message endpoints.
    """
    repository = connection.require()
    package = repository.GetPackageByID(package_id)
    if package is None:
        raise ValueError(f"No existe el paquete {package_id}.")
    if not participants:
        raise ValueError("Debes indicar al menos un participante.")
    participant_names = [str(item.get("name", "")).strip() for item in participants]
    if any(not name for name in participant_names):
        raise ValueError("Cada participante debe tener un nombre no vacio.")
    if len(participant_names) != len(set(participant_names)):
        raise ValueError("No puede haber participantes duplicados.")
    if len(participant_names) > 30 or len(messages) > 100:
        raise ValueError("El limite es de 30 participantes y 100 mensajes.")
    for message in messages:
        source = str(message.get("source", ""))
        target = str(message.get("target", ""))
        if source not in participant_names or target not in participant_names:
            raise ValueError(f"Mensaje invalido: {source} -> {target}.")
        if not str(message.get("message", "")).strip():
            raise ValueError("Cada mensaje debe tener texto.")

    elements: dict[str, Any] = {}
    for participant in participants:
        name = str(participant["name"]).strip()
        element = package.Elements.AddNew(name, "Object")
        element.Stereotype = str(participant.get("stereotype", ""))
        element.Update()
        elements[name] = element

    connectors: list[Any] = []
    for index, message in enumerate(messages, start=1):
        source = elements[str(message["source"])]
        target = elements[str(message["target"])]
        connector = source.Connectors.AddNew(str(message["message"]), "Sequence")
        connector.SupplierID = target.ElementID
        connector.SequenceNo = str(message.get("sequence", index))
        connector.Notes = str(message["message"])
        connector.Direction = "Source -> Destination"
        connector.Update()
        connectors.append(connector)

    diagram = package.Diagrams.AddNew(diagram_name, "Sequence")
    diagram.Update()
    for index, element in enumerate(elements.values()):
        left = 40 + index * 220
        diagram_object = diagram.DiagramObjects.AddNew("", "")
        diagram_object.ElementID = element.ElementID
        diagram_object.Style = f"l={left};r={left + 160};t=40;b=140;"
        diagram_object.Update()
    for connector in connectors:
        diagram_link = diagram.DiagramLinks.AddNew("", "")
        diagram_link.ConnectorID = connector.ConnectorID
        diagram_link.Update()
    diagram.Update()
    repository.SaveDiagram(diagram.DiagramID)
    return _result(
        {
            "created": True,
            "diagram_id": int(diagram.DiagramID),
            "diagram_name": diagram_name,
            "package_id": package_id,
            "participants": [_element_summary(element) for element in elements.values()],
            "messages": len(messages),
        }
    )


if __name__ == "__main__":
    mcp.run(transport="stdio")
