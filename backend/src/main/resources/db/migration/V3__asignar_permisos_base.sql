INSERT INTO rol_permiso (id_rol, id_permiso)
SELECT rol.id_rol, permiso.id_permiso
FROM rol
CROSS JOIN permiso
WHERE rol.nombre = 'ADMINISTRADOR';

INSERT INTO rol_permiso (id_rol, id_permiso)
SELECT rol.id_rol, permiso.id_permiso
FROM rol
CROSS JOIN permiso
WHERE rol.nombre IN ('ORGANIZADOR', 'STAFF', 'PARTICIPANTE')
  AND permiso.nombre IN ('PERFIL_VER', 'PERFIL_EDITAR');
