package com.plataformaeventos.aforo;

import com.plataformaeventos.excepciones.ExcepcionNoEncontrado;
import com.plataformaeventos.inscripciones.repositorio.InscripcionRepository;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import java.time.OffsetDateTime;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class AforoService {
    private final NamedParameterJdbcTemplate jdbc;
    private final InscripcionRepository inscripciones;
    public AforoService(NamedParameterJdbcTemplate jdbc, InscripcionRepository inscripciones) {
        this.jdbc = jdbc; this.inscripciones = inscripciones;
    }
    public record Resumen(long eventoId, String evento, Long salaId, String sala, Integer capacidad,
                          long inscritos, long presentes, long ausentes, long ingresosFranja,
                          long ingresosSala, double porcentajeAsistencia, Double porcentajeAforo, String alerta) {}
    public record Fila(long inscripcionId, String nombre, String correo, String evento, String sala,
                       String categoria, OffsetDateTime ingreso, String staff) {}
    public record Nomina(List<Fila> filas, long total, int pagina, int tamano) {}
    public record Panel(List<Resumen> resumen, Nomina nomina, OffsetDateTime actualizado) {}
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public Panel panel(long org, AforoFiltro f, int pagina) {
        return new Panel(resumen(org,f), nomina(org,f,pagina,25), OffsetDateTime.now());
    }

    public List<Map<String, Object>> opciones(long org) {
        return jdbc.queryForList("""
            SELECT e.id_evento AS id, e.nombre, s.id_espacio AS "salaId", s.nombre AS sala
            FROM evento e LEFT JOIN espacio s ON s.id_espacio=e.id_espacio AND s.id_organizacion=e.id_organizacion
            WHERE e.id_organizacion=:org ORDER BY e.fecha_inicio DESC, e.id_evento DESC
            """, Map.of("org", org));
    }
    public List<String> categorias(long org) {
        return jdbc.queryForList("SELECT DISTINCT categoria_entrada FROM inscripcion WHERE id_organizacion=:org AND categoria_entrada IS NOT NULL ORDER BY categoria_entrada", Map.of("org", org), String.class);
    }
    private MapSqlParameterSource parametros(long org, AforoFiltro f) {
        var p = new MapSqlParameterSource("org", org);
        if (f.eventoId() != null) {
            Long existe = jdbc.queryForObject("SELECT COUNT(*) FROM evento WHERE id_organizacion=:org AND id_evento=:evento", p.addValue("evento", f.eventoId()), Long.class);
            if (existe == null || existe == 0) throw new ExcepcionNoEncontrado("Evento no encontrado");
        }
        if (f.salaId() != null) {
            Long existe = jdbc.queryForObject("SELECT COUNT(*) FROM espacio WHERE id_organizacion=:org AND id_espacio=:sala", p.addValue("sala", f.salaId()), Long.class);
            if (existe == null || existe == 0) throw new ExcepcionNoEncontrado("Sala no encontrada");
        }
        if (f.categoria() != null) p.addValue("categoria", f.categoria());
        if (f.desde() != null) p.addValue("desde", f.desde());
        if (f.hasta() != null) p.addValue("hasta", f.hasta());
        return p;
    }
    private String eventos(AforoFiltro f) {
        return " e.id_organizacion=:org" + (f.eventoId() == null ? "" : " AND e.id_evento=:evento")
                + (f.salaId() == null ? "" : " AND e.id_espacio=:sala");
    }
    private String categoria(AforoFiltro f) {
        return f.categoria() == null ? "1=1" : f.categoria().isEmpty() ? "i.categoria_entrada IS NULL" : "i.categoria_entrada=:categoria";
    }
    private String horario(AforoFiltro f) {
        return (f.desde() == null ? "" : " AND a.fecha_hora>=:desde") + (f.hasta() == null ? "" : " AND a.fecha_hora<:hasta");
    }
    public List<Resumen> resumen(long org, AforoFiltro f) {
        var p = parametros(org, f);
        String cat = categoria(f);
        String sql = """
            SELECT e.id_evento, e.nombre, s.id_espacio, s.nombre AS sala,
            CASE WHEN e.aforo_maximo IS NULL THEN s.capacidad WHEN s.capacidad IS NULL THEN e.aforo_maximo
                 WHEN e.aforo_maximo<s.capacidad THEN e.aforo_maximo ELSE s.capacidad END AS capacidad,
            SUM(CASE WHEN i.id_inscripcion IS NOT NULL AND (%s) THEN 1 ELSE 0 END) AS inscritos,
            SUM(CASE WHEN a.id_asistencia IS NOT NULL AND (%s) THEN 1 ELSE 0 END) AS presentes,
            SUM(CASE WHEN a.id_asistencia IS NOT NULL AND (%s) %s THEN 1 ELSE 0 END) AS franja,
            COUNT(a.id_asistencia) AS ingresos
            FROM evento e
            LEFT JOIN espacio s ON s.id_espacio=e.id_espacio AND s.id_organizacion=e.id_organizacion
            LEFT JOIN inscripcion i ON i.id_evento=e.id_evento AND i.id_organizacion=e.id_organizacion
                AND (i.estado='CONFIRMADA' OR EXISTS (SELECT 1 FROM asistencia ax WHERE ax.id_inscripcion=i.id_inscripcion AND ax.id_organizacion=:org))
            LEFT JOIN asistencia a ON a.id_inscripcion=i.id_inscripcion AND a.id_organizacion=:org
            WHERE %s
            GROUP BY e.id_evento, e.nombre, s.id_espacio, s.nombre, s.capacidad, e.aforo_maximo
            ORDER BY e.nombre, e.id_evento
            """.formatted(cat, cat, cat, horario(f), eventos(f));
        return jdbc.query(sql, p, (rs, n) -> {
            long inscritos = rs.getLong("inscritos"), presentes = rs.getLong("presentes"), ingresos = rs.getLong("ingresos");
            Integer capacidad = rs.getObject("capacidad", Integer.class);
            Double porcentaje = capacidad == null || capacidad <= 0 ? null : porcentaje(ingresos, capacidad);
            return new Resumen(rs.getLong("id_evento"), rs.getString("nombre"), rs.getObject("id_espacio", Long.class),
                    rs.getString("sala"), capacidad, inscritos, presentes, inscritos-presentes, rs.getLong("franja"), ingresos,
                    porcentaje(presentes, inscritos), porcentaje, alerta(porcentaje));
        });
    }
    public static double porcentaje(long cantidad, long total) { return total <= 0 ? 0 : cantidad * 100.0 / total; }
    public static String alerta(Double porcentaje) {
        return porcentaje == null ? "SIN_CAPACIDAD" : porcentaje >= 100 ? "COMPLETO" : porcentaje >= 90 ? "CRITICO" : porcentaje >= 80 ? "PRECAUCION" : "NORMAL";
    }
    private String baseNomina(AforoFiltro f) {
        return """
            FROM inscripcion i JOIN evento e ON e.id_evento=i.id_evento AND e.id_organizacion=i.id_organizacion
            JOIN usuario u ON u.id_usuario=i.id_participante
            LEFT JOIN espacio s ON s.id_espacio=e.id_espacio AND s.id_organizacion=e.id_organizacion
            LEFT JOIN asistencia a ON a.id_inscripcion=i.id_inscripcion AND a.id_organizacion=i.id_organizacion
            LEFT JOIN usuario staff ON staff.id_usuario=a.id_usuario_staff
            WHERE i.id_organizacion=:org AND (i.estado='CONFIRMADA' OR a.id_asistencia IS NOT NULL) AND
            """ + eventos(f) + " AND (" + categoria(f) + ")" + horario(f);
    }
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public Nomina nomina(long org, AforoFiltro f, int pagina, int tamano) {
        if (pagina < 0 || pagina > 1000000 || tamano < 1 || tamano > 5000) throw new IllegalArgumentException("Paginacion invalida");
        var p = parametros(org, f).addValue("limite", tamano).addValue("offset", (long) pagina*tamano);
        String base = baseNomina(f);
        long total = Objects.requireNonNull(jdbc.queryForObject("SELECT COUNT(*) " + base, p, Long.class));
        var filas = jdbc.query("""
            SELECT i.id_inscripcion, u.nombres, u.apellidos, u.correo, e.nombre AS evento, s.nombre AS sala,
                   i.categoria_entrada, a.fecha_hora, staff.nombres AS staff_nombre, staff.apellidos AS staff_apellido
            """ + base + " ORDER BY u.apellidos, u.nombres, i.id_inscripcion LIMIT :limite OFFSET :offset", p, (rs, n) ->
                new Fila(rs.getLong("id_inscripcion"), rs.getString("nombres") + " " + rs.getString("apellidos"), rs.getString("correo"),
                    rs.getString("evento"), rs.getString("sala"), rs.getString("categoria_entrada"), rs.getObject("fecha_hora", OffsetDateTime.class),
                    rs.getString("staff_nombre") == null ? null : rs.getString("staff_nombre") + " " + rs.getString("staff_apellido")));
        return new Nomina(filas, total, pagina, tamano);
    }
    @Transactional
    @com.plataformaeventos.audit.servicio.OperacionAuditable(modulo="ASISTENCIAS", entidad="INSCRIPCION", accion="CLASIFICAR_ENTRADA")
    public void clasificar(long org, long id, String categoria) {
        var i = inscripciones.bloquearPorIdEOrganizacion(id, org).orElseThrow(() -> new ExcepcionNoEncontrado("Inscripcion no encontrada"));
        i.clasificarEntrada(categoria);
    }
}
