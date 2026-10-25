package com.plataformaeventos.componentesui.servicio;

import com.plataformaeventos.componentesui.modelo.EstadoPrivilegioUi;
import com.plataformaeventos.componentesui.modelo.TipoComponenteUi;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Component
public class ComponenteUiRegistry {
    private static final Set<EstadoPrivilegioUi> SOLO_VISIBLE = Set.of(EstadoPrivilegioUi.VISIBLE);
    private static final Set<EstadoPrivilegioUi> INTERACTIVO = Set.of(EstadoPrivilegioUi.VISIBLE, EstadoPrivilegioUi.HABILITADO);
    private static final Set<EstadoPrivilegioUi> EDITABLE = Set.of(EstadoPrivilegioUi.VISIBLE, EstadoPrivilegioUi.EDITABLE);

    private final Map<String, DefinicionComponenteUi> definiciones;

    public ComponenteUiRegistry() {
        Map<String, DefinicionComponenteUi> base = new LinkedHashMap<>();
        registrar(base, "profile.menu", TipoComponenteUi.MENU, "Profile menu", SOLO_VISIBLE, false);
        registrar(base, "users.menu", TipoComponenteUi.MENU, "Users menu", SOLO_VISIBLE, false);
        registrar(base, "users.list", TipoComponenteUi.TEXTO_INFORMATIVO, "User account list", SOLO_VISIBLE, false);
        registrar(base, "users.create", TipoComponenteUi.ACCION, "Create user action", INTERACTIVO, false);
        registrar(base, "users.edit", TipoComponenteUi.FORMULARIO, "Edit user form", EDITABLE, false);
        registrar(base, "users.activate", TipoComponenteUi.ACCION, "Activate user action", INTERACTIVO, false);
        registrar(base, "users.deactivate", TipoComponenteUi.ACCION, "Deactivate user action", INTERACTIVO, false);
        registrar(base, "users.save", TipoComponenteUi.ACCION, "Save user action", INTERACTIVO, false);
        registrar(base, "participants.menu", TipoComponenteUi.MENU, "Participants menu", SOLO_VISIBLE, false);
        registrar(base, "roles.menu", TipoComponenteUi.MENU, "Roles and permissions menu", SOLO_VISIBLE, false);
        registrar(base, "roles.list", TipoComponenteUi.TEXTO_INFORMATIVO, "Role list", SOLO_VISIBLE, false);
        registrar(base, "roles.select", TipoComponenteUi.ACCION, "Select role action", INTERACTIVO, false);
        registrar(base, "roles.create", TipoComponenteUi.ACCION, "Create role action", INTERACTIVO, false);
        registrar(base, "roles.edit", TipoComponenteUi.FORMULARIO, "Edit role form", EDITABLE, false);
        registrar(base, "roles.permissions.assign", TipoComponenteUi.FORMULARIO, "Assign role permissions form", EDITABLE, false);
        registrar(base, "roles.permissions.save", TipoComponenteUi.ACCION, "Save role permissions action", INTERACTIVO, false);
        registrar(base, "roles.save", TipoComponenteUi.ACCION, "Save role action", INTERACTIVO, false);
        registrar(base, "members.menu", TipoComponenteUi.MENU, "Organization members menu", SOLO_VISIBLE, false);
        registrar(base, "members.list", TipoComponenteUi.TEXTO_INFORMATIVO, "Organization member list", SOLO_VISIBLE, false);
        registrar(base, "members.add", TipoComponenteUi.ACCION, "Add organization member action", INTERACTIVO, false);
        registrar(base, "members.toggle-state", TipoComponenteUi.ACCION, "Toggle organization member state action", INTERACTIVO, false);
        registrar(base, "members.form.user", TipoComponenteUi.CAMPO, "Organization member user field", EDITABLE, false);
        registrar(base, "members.form.roles", TipoComponenteUi.CAMPO, "Organization member roles field", EDITABLE, false);
        registrar(base, "audit.menu", TipoComponenteUi.MENU, "Audit log menu", SOLO_VISIBLE, false);
        registrar(base, "events.search.menu", TipoComponenteUi.MENU, "Event search menu", SOLO_VISIBLE, false);
        registrar(base, "events.menu", TipoComponenteUi.MENU, "Events menu", SOLO_VISIBLE, false);
        registrar(base, "events.list", TipoComponenteUi.TEXTO_INFORMATIVO, "Event list", SOLO_VISIBLE, false);
        registrar(base, "events.filters", TipoComponenteUi.FORMULARIO, "Event search filters form", EDITABLE, false);
        registrar(base, "events.create", TipoComponenteUi.ACCION, "Create event action", INTERACTIVO, false);
        registrar(base, "events.edit", TipoComponenteUi.ACCION, "Edit event action", INTERACTIVO, false);
        registrar(base, "categories.menu", TipoComponenteUi.MENU, "Categories menu", SOLO_VISIBLE, false);
        registrar(base, "categories.list", TipoComponenteUi.TEXTO_INFORMATIVO, "Category list", SOLO_VISIBLE, false);
        registrar(base, "categories.create", TipoComponenteUi.ACCION, "Create category action", INTERACTIVO, false);
        registrar(base, "categories.edit", TipoComponenteUi.ACCION, "Edit category action", INTERACTIVO, false);
        registrar(base, "categories.delete", TipoComponenteUi.ACCION, "Delete category action", INTERACTIVO, false);
        registrar(base, "resources.menu", TipoComponenteUi.MENU, "Resources menu", SOLO_VISIBLE, false);
        registrar(base, "resources.inventory.list", TipoComponenteUi.TEXTO_INFORMATIVO, "Resource inventory list", SOLO_VISIBLE, false);
        registrar(base, "resources.create", TipoComponenteUi.ACCION, "Create resource action", INTERACTIVO, false);
        registrar(base, "resources.edit", TipoComponenteUi.ACCION, "Edit resource action", INTERACTIVO, false);
        registrar(base, "resources.change-state", TipoComponenteUi.ACCION, "Change resource state action", INTERACTIVO, false);
        registrar(base, "resources.assign", TipoComponenteUi.ACCION, "Assign resource action", INTERACTIVO, false);
        registrar(base, "resource-assignments.list", TipoComponenteUi.TEXTO_INFORMATIVO, "Resource assignment list", SOLO_VISIBLE, false);
        registrar(base, "resource-assignments.create", TipoComponenteUi.ACCION, "Create resource assignment action", INTERACTIVO, false);
        registrar(base, "resource-assignments.release", TipoComponenteUi.ACCION, "Release resource assignment action", INTERACTIVO, false);
        registrar(base, "spaces.menu", TipoComponenteUi.MENU, "Spaces menu", SOLO_VISIBLE, false);
        registrar(base, "spaces.list", TipoComponenteUi.TEXTO_INFORMATIVO, "Space list", SOLO_VISIBLE, false);
        registrar(base, "spaces.create", TipoComponenteUi.ACCION, "Create space action", INTERACTIVO, false);
        registrar(base, "spaces.edit", TipoComponenteUi.ACCION, "Edit space action", INTERACTIVO, false);
        registrar(base, "convocations.menu", TipoComponenteUi.MENU, "Convocations menu", SOLO_VISIBLE, false);
        registrar(base, "convocations.list", TipoComponenteUi.TEXTO_INFORMATIVO, "Convocation list", SOLO_VISIBLE, false);
        registrar(base, "convocations.create", TipoComponenteUi.ACCION, "Create convocation action", INTERACTIVO, false);
        registrar(base, "convocations.close", TipoComponenteUi.ACCION, "Close convocation action", INTERACTIVO, false);
        registrar(base, "convocations.cancel", TipoComponenteUi.ACCION, "Cancel convocation action", INTERACTIVO, false);
        registrar(base, "staff-applications.menu", TipoComponenteUi.MENU, "Staff applications menu", SOLO_VISIBLE, false);
        registrar(base, "staff-applications.create", TipoComponenteUi.ACCION, "Create staff application action", INTERACTIVO, false);
        registrar(base, "staff-applications.manage", TipoComponenteUi.TEXTO_INFORMATIVO, "Staff application management", SOLO_VISIBLE, false);
        registrar(base, "staff-applications.accept", TipoComponenteUi.ACCION, "Accept staff application action", INTERACTIVO, false);
        registrar(base, "staff-applications.reject", TipoComponenteUi.ACCION, "Reject staff application action", INTERACTIVO, false);
        registrar(base, "staff-assignments.menu", TipoComponenteUi.MENU, "Staff assignments menu", SOLO_VISIBLE, false);
        registrar(base, "staff-assignments.create", TipoComponenteUi.ACCION, "Create staff assignment action", INTERACTIVO, false);
        registrar(base, "staff-assignments.cancel", TipoComponenteUi.ACCION, "Cancel staff assignment action", INTERACTIVO, false);
        registrar(base, "audit.list", TipoComponenteUi.TEXTO_INFORMATIVO, "Audit log list", SOLO_VISIBLE, false);
        registrar(base, "audit.filters", TipoComponenteUi.FORMULARIO, "Audit log filters form", EDITABLE, false);
        registrar(base, "groups.menu", TipoComponenteUi.MENU, "Tenant group administration menu", SOLO_VISIBLE, true);
        registrar(base, "groups.list", TipoComponenteUi.TEXTO_INFORMATIVO, "Tenant group list", SOLO_VISIBLE, true);
        registrar(base, "groups.create", TipoComponenteUi.ACCION, "Create tenant group action", INTERACTIVO, true);
        registrar(base, "groups.edit", TipoComponenteUi.FORMULARIO, "Edit tenant group form", EDITABLE, true);
        registrar(base, "groups.activate", TipoComponenteUi.ACCION, "Activate tenant group action", INTERACTIVO, false);
        registrar(base, "groups.deactivate", TipoComponenteUi.ACCION, "Deactivate tenant group action", INTERACTIVO, false);
        registrar(base, "groups.members.assign", TipoComponenteUi.FORMULARIO, "Assign members to tenant group form", EDITABLE, false);
        registrar(base, "groups.roles.assign", TipoComponenteUi.FORMULARIO, "Assign roles to tenant group form", EDITABLE, false);
        registrar(base, "groups.assignments.save", TipoComponenteUi.ACCION, "Save tenant group assignments action", INTERACTIVO, true);
        registrar(base, "ui-privileges.menu", TipoComponenteUi.MENU, "UI privilege administration menu", SOLO_VISIBLE, true);
        registrar(base, "ui-privileges.catalog", TipoComponenteUi.TEXTO_INFORMATIVO, "UI component catalog", SOLO_VISIBLE, true);
        registrar(base, "ui-privileges.group-grants", TipoComponenteUi.FORMULARIO, "Group UI privilege assignment form", EDITABLE, true);
        registrar(base, "ui-privileges.save", TipoComponenteUi.ACCION, "Save group UI privilege grants action", INTERACTIVO, true);
        registrar(base, "event.form", TipoComponenteUi.FORMULARIO, "Event create/edit form", EDITABLE, false);
        registrar(base, "event.submit", TipoComponenteUi.ACCION, "Event submit action", INTERACTIVO, false);
        registrar(base, "event.capacity", TipoComponenteUi.CAMPO, "Event capacity business field", EDITABLE, false);
        registrar(base, "event.summary", TipoComponenteUi.TEXTO_INFORMATIVO, "Event summary information", SOLO_VISIBLE, false);
        registrar(base, "event.registration.create", TipoComponenteUi.ACCION, "Event registration action", INTERACTIVO, false);
        registrar(base, "event.responsibles.assign", TipoComponenteUi.FORMULARIO, "Assign event responsibles form", EDITABLE, false);
        registrar(base, "participants.list", TipoComponenteUi.TEXTO_INFORMATIVO, "Participant list", SOLO_VISIBLE, false);
        registrar(base, "participants.filters", TipoComponenteUi.FORMULARIO, "Participant filters form", EDITABLE, false);
        registrar(base, "participants.export", TipoComponenteUi.ACCION, "Export participants action", INTERACTIVO, false);
        registrar(base, "participants.history", TipoComponenteUi.ACCION, "Participant history action", INTERACTIVO, false);
        registrar(base, "participants.status.toggle", TipoComponenteUi.ACCION, "Toggle participant status action", INTERACTIVO, false);
        registrar(base, "participants.detail", TipoComponenteUi.TEXTO_INFORMATIVO, "Participant detail information", SOLO_VISIBLE, false);
        registrar(base, "inscriptions.list", TipoComponenteUi.TEXTO_INFORMATIVO, "Participant inscription list", SOLO_VISIBLE, false);
        registrar(base, "inscriptions.cancel", TipoComponenteUi.ACCION, "Cancel participant inscription action", INTERACTIVO, false);
        registrar(base, "staff-applications.form", TipoComponenteUi.FORMULARIO, "Staff application mobile form", EDITABLE, false);
        registrar(base, "profile.edit", TipoComponenteUi.FORMULARIO, "Profile edit form", EDITABLE, false);
        registrar(base, "profile.save", TipoComponenteUi.ACCION, "Profile save action", INTERACTIVO, false);
        this.definiciones = Map.copyOf(base);
    }

    public Collection<DefinicionComponenteUi> listar() {
        return definiciones.values();
    }

    public Optional<DefinicionComponenteUi> buscar(String clave) {
        return Optional.ofNullable(definiciones.get(clave));
    }

    public Set<String> permisosBase(String clave) {
        if ("profile.menu".equals(clave)) return Set.of();
        if (clave.startsWith("users.")) {
            return switch (clave) {
                case "users.menu" -> Set.of("USUARIO_VER", "USUARIO_CREAR", "USUARIO_EDITAR", "USUARIO_CAMBIAR_ESTADO");
                case "users.create" -> Set.of("USUARIO_CREAR");
                case "users.edit", "users.save" -> Set.of("USUARIO_CREAR", "USUARIO_EDITAR");
                case "users.activate", "users.deactivate" -> Set.of("USUARIO_CAMBIAR_ESTADO");
                default -> Set.of("USUARIO_VER");
            };
        }
        if (clave.startsWith("participants.")) {
            return switch (clave) {
                case "participants.menu" -> Set.of("PARTICIPANTE_VER", "PARTICIPANTE_GESTIONAR", "USUARIO_VER", "USUARIO_CAMBIAR_ESTADO", "INSCRIPCION_VER", "INSCRIPCION_VER_TODAS");
                case "participants.status.toggle" -> Set.of("PARTICIPANTE_GESTIONAR", "USUARIO_CAMBIAR_ESTADO");
                default -> Set.of("PARTICIPANTE_VER", "USUARIO_VER", "INSCRIPCION_VER_TODAS");
            };
        }
        if (clave.startsWith("roles.")) {
            return Set.of("SAAS_ADMINISTRADOR");
        }
        if (clave.startsWith("members.")) return Set.of("__ADMINISTRADOR_TENANT__");
        if (clave.startsWith("groups.") || clave.startsWith("ui-privileges.")) return Set.of("__ADMINISTRADOR_TENANT__");
        if (clave.startsWith("audit.")) return Set.of("BITACORA_VER");
        if (clave.startsWith("events.")) {
            return switch (clave) {
                case "events.create" -> Set.of("EVENTO_CREAR");
                case "events.edit" -> Set.of("EVENTO_EDITAR");
                case "events.menu" -> Set.of("EVENTO_VER", "EVENTO_CREAR", "EVENTO_EDITAR", "EVENTO_ASIGNAR_RESPONSABLE");
                default -> Set.of("EVENTO_VER");
            };
        }
        if (clave.startsWith("event.")) {
            return switch (clave) {
                case "event.registration.create" -> Set.of("INSCRIPCION_CREAR");
                case "event.responsibles.assign" -> Set.of("EVENTO_ASIGNAR_RESPONSABLE");
                case "event.form", "event.submit", "event.capacity" -> Set.of("EVENTO_CREAR", "EVENTO_EDITAR");
                default -> Set.of("EVENTO_VER");
            };
        }
        if (clave.startsWith("categories.")) {
            return switch (clave) {
                case "categories.menu" -> Set.of("EVENTO_VER", "EVENTO_CREAR", "EVENTO_EDITAR", "EVENTO_ELIMINAR");
                case "categories.create" -> Set.of("EVENTO_CREAR");
                case "categories.edit" -> Set.of("EVENTO_EDITAR");
                case "categories.delete" -> Set.of("EVENTO_ELIMINAR");
                default -> Set.of("EVENTO_VER");
            };
        }
        if (clave.startsWith("resources.")) {
            return switch (clave) {
                case "resources.menu" -> Set.of("RECURSO_VER", "RECURSO_CREAR", "RECURSO_EDITAR", "RECURSO_CAMBIAR_ESTADO");
                case "resources.create" -> Set.of("RECURSO_CREAR");
                case "resources.edit" -> Set.of("RECURSO_EDITAR");
                case "resources.change-state" -> Set.of("RECURSO_CAMBIAR_ESTADO");
                case "resources.assign" -> Set.of("ASIGNACION_RECURSO_CREAR");
                default -> Set.of("RECURSO_VER");
            };
        }
        if (clave.startsWith("resource-assignments.")) {
            return switch (clave) {
                case "resource-assignments.create" -> Set.of("ASIGNACION_RECURSO_CREAR");
                case "resource-assignments.release" -> Set.of("ASIGNACION_RECURSO_LIBERAR");
                default -> Set.of("ASIGNACION_RECURSO_VER", "RECURSO_VER");
            };
        }
        if (clave.startsWith("spaces.")) {
            return switch (clave) {
                case "spaces.menu" -> Set.of("ESPACIO_VER", "ESPACIO_CREAR", "ESPACIO_EDITAR", "PROGRAMACION_VERIFICAR");
                case "spaces.create" -> Set.of("ESPACIO_CREAR");
                case "spaces.edit" -> Set.of("ESPACIO_EDITAR");
                default -> Set.of("ESPACIO_VER");
            };
        }
        if (clave.startsWith("convocations.")) {
            return switch (clave) {
                case "convocations.create" -> Set.of("CONVOCATORIA_CREAR");
                case "convocations.close" -> Set.of("CONVOCATORIA_CERRAR");
                case "convocations.cancel" -> Set.of("CONVOCATORIA_CANCELAR");
                default -> Set.of("CONVOCATORIA_VER");
            };
        }
        if (clave.startsWith("staff-applications.")) {
            return switch (clave) {
                case "staff-applications.create", "staff-applications.form" -> Set.of("POSTULACION_STAFF_CREAR");
                case "staff-applications.accept", "staff-applications.reject", "staff-applications.manage" -> Set.of("POSTULACION_STAFF_GESTIONAR");
                default -> Set.of("POSTULACION_STAFF_CREAR", "POSTULACION_STAFF_GESTIONAR");
            };
        }
        if (clave.startsWith("staff-assignments.")) {
            return switch (clave) {
                case "staff-assignments.menu" -> Set.of("ASIGNACION_STAFF_GESTIONAR", "POSTULACION_STAFF_CREAR");
                default -> Set.of("ASIGNACION_STAFF_GESTIONAR");
            };
        }
        if (clave.startsWith("inscriptions.")) {
            return switch (clave) {
                case "inscriptions.cancel" -> Set.of("INSCRIPCION_CANCELAR");
                default -> Set.of("INSCRIPCION_VER_PROPIAS", "INSCRIPCION_VER_TODAS");
            };
        }
        if (clave.startsWith("profile.")) {
            return "profile.save".equals(clave) || "profile.edit".equals(clave)
                    ? Set.of("PERFIL_EDITAR") : Set.of("PERFIL_VER");
        }
        return Set.of("__SIN_POLITICA__");
    }

    private void registrar(Map<String, DefinicionComponenteUi> destino, String clave, TipoComponenteUi tipo,
                           String descripcion, Set<EstadoPrivilegioUi> estados, boolean recuperacion) {
        destino.put(clave, new DefinicionComponenteUi(clave, tipo, descripcion, Set.copyOf(estados), recuperacion));
    }
}
