INSERT INTO rol (nombre, descripcion, estado) VALUES
    ('ADMINISTRADOR', 'Administra usuarios, roles, permisos y configuracion de seguridad.', 'ACTIVO'),
    ('ORGANIZADOR', 'Gestiona su perfil y, en Sprint futuros, eventos a su cargo.', 'ACTIVO'),
    ('STAFF', 'Gestiona su perfil y, en Sprint futuros, tareas de apoyo.', 'ACTIVO'),
    ('PARTICIPANTE', 'Gestiona su perfil y participa en funcionalidades futuras.', 'ACTIVO');

INSERT INTO permiso (nombre, descripcion) VALUES
    ('PERFIL_VER', 'Consultar el perfil propio.'),
    ('PERFIL_EDITAR', 'Actualizar los datos permitidos del perfil propio.'),
    ('USUARIO_VER', 'Consultar usuarios del sistema.'),
    ('USUARIO_CREAR', 'Crear usuarios del sistema.'),
    ('USUARIO_EDITAR', 'Actualizar usuarios del sistema.'),
    ('USUARIO_CAMBIAR_ESTADO', 'Activar o inactivar usuarios del sistema.'),
    ('ROL_VER', 'Consultar roles del sistema.'),
    ('ROL_CREAR', 'Crear roles del sistema.'),
    ('ROL_EDITAR', 'Actualizar roles del sistema.'),
    ('PERMISO_VER', 'Consultar permisos del sistema.'),
    ('PERMISO_ASIGNAR', 'Asignar permisos a roles.');
