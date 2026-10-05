INSERT INTO rol (nombre, descripcion, estado)
SELECT 'SAAS_ADMINISTRADOR', 'Administra roles y permisos globales de la plataforma SaaS.', 'ACTIVO'
WHERE NOT EXISTS (
    SELECT 1 FROM rol WHERE nombre = 'SAAS_ADMINISTRADOR'
);

INSERT INTO rol_permiso (id_rol, id_permiso)
SELECT rol.id_rol, permiso.id_permiso
FROM rol
JOIN permiso ON permiso.nombre IN ('ROL_VER', 'ROL_CREAR', 'ROL_EDITAR', 'PERMISO_VER', 'PERMISO_ASIGNAR')
WHERE rol.nombre = 'SAAS_ADMINISTRADOR'
ON CONFLICT DO NOTHING;

INSERT INTO usuario_rol (id_usuario, id_rol)
SELECT usuario_rol.id_usuario, rol_saas.id_rol
FROM usuario_rol
JOIN rol rol_legacy ON rol_legacy.id_rol = usuario_rol.id_rol
JOIN rol rol_saas ON rol_saas.nombre = 'SAAS_ADMINISTRADOR'
WHERE rol_legacy.nombre = 'ADMINISTRADOR'
ON CONFLICT DO NOTHING;

DELETE FROM usuario_rol
USING rol
WHERE usuario_rol.id_rol = rol.id_rol
  AND rol.nombre = 'ADMINISTRADOR';
