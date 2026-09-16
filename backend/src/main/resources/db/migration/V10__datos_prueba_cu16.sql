-- CU16: datos temporales de prueba para validar la postulacion movil desde Flutter.
-- Sigue el mismo patron de siembra de datos que V6 (eventos de ejemplo).
-- El evento queda PLANIFICADO y en el futuro; la convocatoria es ABIERTA con
-- fecha limite vigente y anterior al inicio del evento (reglas de ConvocatoriaService).

INSERT INTO evento (nombre, descripcion, fecha_inicio, fecha_fin, lugar_principal, estado)
VALUES (
    'Evento prueba CU16',
    'Evento temporal para probar el flujo de postulaciones moviles (CU16).',
    CURRENT_TIMESTAMP + INTERVAL '10 days',
    CURRENT_TIMESTAMP + INTERVAL '12 days',
    'Auditorio Movil',
    'PLANIFICADO'
);

INSERT INTO convocatoria_personal (id_evento, tipo_personal, descripcion, cantidad_requerida, fecha_limite, estado)
SELECT id_evento, 'STAFF', 'Convocatoria para probar postulación móvil', 5, CURRENT_DATE + 5, 'ABIERTA'
FROM evento
WHERE nombre = 'Evento prueba CU16';