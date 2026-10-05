package com.plataformaeventos.participantes.servicio;

import com.plataformaeventos.audit.servicio.OperacionAuditable;
import com.plataformaeventos.excepciones.ExcepcionNoEncontrado;
import com.plataformaeventos.inscripciones.modelo.EstadoInscripcion;
import com.plataformaeventos.inscripciones.modelo.Inscripcion;
import com.plataformaeventos.inscripciones.repositorio.InscripcionRepository;
import com.plataformaeventos.organizaciones.modelo.Organizacion;
import com.plataformaeventos.participantes.dto.CambiarEstadoParticipanteSolicitud;
import com.plataformaeventos.participantes.dto.InscripcionResumenRespuesta;
import com.plataformaeventos.participantes.dto.PaginaParticipantesRespuesta;
import com.plataformaeventos.participantes.dto.ParticipanteDetalleRespuesta;
import com.plataformaeventos.participantes.dto.ParticipanteResumenRespuesta;
import com.plataformaeventos.participantes.repositorio.ParticipanteRepository;
import com.plataformaeventos.usuarios.modelo.EstadoUsuario;
import com.plataformaeventos.usuarios.modelo.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ParticipanteService {

    private final ParticipanteRepository participanteRepository;
    private final InscripcionRepository inscripcionRepository;

    public ParticipanteService(
            ParticipanteRepository participanteRepository,
            InscripcionRepository inscripcionRepository
    ) {
        this.participanteRepository = participanteRepository;
        this.inscripcionRepository = inscripcionRepository;
    }

    @Transactional(readOnly = true)
    public PaginaParticipantesRespuesta listarParticipantes(
            Long eventoId,
            String busqueda,
            EstadoUsuario estado,
            int pagina,
            int tamano,
            Organizacion organizacion
    ) {
        String query = (busqueda != null && !busqueda.trim().isBlank()) ? busqueda.trim() : null;
        Pageable pageable = PageRequest.of(Math.max(0, pagina), Math.max(1, tamano));
        Page<Usuario> resultado = participanteRepository.buscarParticipantes(
                organizacion.getId(),
                eventoId,
                query,
                estado,
                pageable
        );

        List<ParticipanteResumenRespuesta> elementos = resultado.getContent()
                .stream()
                .map(u -> mapearResumen(u, organizacion.getId()))
                .toList();

        return new PaginaParticipantesRespuesta(
                elementos,
                resultado.getNumber(),
                resultado.getSize(),
                resultado.getTotalElements(),
                resultado.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public ParticipanteDetalleRespuesta obtenerDetalleParticipante(Long idParticipante, Organizacion organizacion) {
        Usuario usuario = participanteRepository.buscarPorIdYOrganizacion(idParticipante, organizacion.getId())
                .orElseThrow(() -> new ExcepcionNoEncontrado("Participante no encontrado con identificador " + idParticipante));

        List<Inscripcion> inscripciones = inscripcionRepository
                .findByParticipanteIdAndOrganizacionIdConEvento(idParticipante, organizacion.getId());

        long total = inscripciones.size();
        long confirmadas = inscripciones.stream().filter(i -> i.getEstado() == EstadoInscripcion.CONFIRMADA).count();
        long canceladas = inscripciones.stream().filter(i -> i.getEstado() == EstadoInscripcion.CANCELADA).count();

        List<InscripcionResumenRespuesta> historial = inscripciones.stream()
                .map(InscripcionResumenRespuesta::desdeEntidad)
                .toList();

        EstadoUsuario estadoParticipante = participanteRepository
                .buscarEstadoParticipante(usuario.getId(), organizacion.getId())
                .orElse(usuario.getEstado());

        return new ParticipanteDetalleRespuesta(
                usuario.getId(),
                usuario.getNombres(),
                usuario.getApellidos(),
                usuario.getCorreo(),
                estadoParticipante,
                usuario.getFechaCreacion(),
                total,
                confirmadas,
                canceladas,
                historial
        );
    }

    @Transactional
    @OperacionAuditable(modulo = "PARTICIPANTES", entidad = "PARTICIPANTE", accion = "CAMBIAR_ESTADO")
    public ParticipanteResumenRespuesta cambiarEstado(
            Long idParticipante,
            CambiarEstadoParticipanteSolicitud solicitud,
            Organizacion organizacion
    ) {
        Usuario usuario = participanteRepository.buscarPorIdYOrganizacion(idParticipante, organizacion.getId())
                .orElseThrow(() -> new ExcepcionNoEncontrado("Participante no encontrado con identificador " + idParticipante));

        int actualizados = participanteRepository.cambiarEstadoParticipante(
                idParticipante,
                organizacion.getId(),
                solicitud.estado()
        );
        if (actualizados == 0) {
            throw new ExcepcionNoEncontrado("Membresia de participante no encontrada con identificador " + idParticipante);
        }
        return mapearResumen(usuario, organizacion.getId());
    }

    @Transactional(readOnly = true)
    public byte[] exportarParticipantesCsv(
            Long eventoId,
            String busqueda,
            EstadoUsuario estado,
            Organizacion organizacion
    ) {
        String query = (busqueda != null && !busqueda.trim().isBlank()) ? busqueda.trim() : null;
        List<Usuario> lista = participanteRepository.listarTodosParaExportacion(
                organizacion.getId(),
                eventoId,
                query,
                estado
        );

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        // Agregar UTF-8 BOM para que Excel en Windows renderice correctamente caracteres especiales
        baos.write(0xEF);
        baos.write(0xBB);
        baos.write(0xBF);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        try (PrintWriter writer = new PrintWriter(baos, true, StandardCharsets.UTF_8)) {
            writer.println("ID,Nombres,Apellidos,Correo,Estado,Total Inscripciones,Inscripciones Confirmadas,Inscripciones Canceladas,Ultima Inscripcion,Fecha Registro");
            for (Usuario u : lista) {
                ParticipanteResumenRespuesta resumen = mapearResumen(u, organizacion.getId());
                writer.printf(
                        "\"%d\",\"%s\",\"%s\",\"%s\",\"%s\",%d,%d,%d,\"%s\",\"%s\"%n",
                        resumen.idUsuario(),
                        escaparCsv(resumen.nombres()),
                        escaparCsv(resumen.apellidos()),
                        escaparCsv(resumen.correo()),
                        resumen.estado(),
                        resumen.totalInscripciones(),
                        resumen.inscripcionesConfirmadas(),
                        resumen.inscripcionesCanceladas(),
                        resumen.ultimaInscripcion() != null ? resumen.ultimaInscripcion().format(formatter) : "N/A",
                        resumen.fechaRegistro() != null ? resumen.fechaRegistro().format(formatter) : "N/A"
                );
            }
        }

        return baos.toByteArray();
    }

    private ParticipanteResumenRespuesta mapearResumen(Usuario usuario, Long organizacionId) {
        long total = inscripcionRepository.countByParticipanteIdAndOrganizacionId(usuario.getId(), organizacionId);
        long confirmadas = inscripcionRepository.countByParticipanteIdAndOrganizacionIdAndEstado(
                usuario.getId(),
                organizacionId,
                EstadoInscripcion.CONFIRMADA
        );
        long canceladas = inscripcionRepository.countByParticipanteIdAndOrganizacionIdAndEstado(
                usuario.getId(),
                organizacionId,
                EstadoInscripcion.CANCELADA
        );
        OffsetDateTime ultima = inscripcionRepository
                .findFirstByParticipanteIdAndOrganizacionIdOrderByFechaInscripcionDesc(usuario.getId(), organizacionId)
                .map(Inscripcion::getFechaInscripcion)
                .orElse(null);

        EstadoUsuario estadoParticipante = participanteRepository
                .buscarEstadoParticipante(usuario.getId(), organizacionId)
                .orElse(usuario.getEstado());

        return new ParticipanteResumenRespuesta(
                usuario.getId(),
                usuario.getNombres(),
                usuario.getApellidos(),
                usuario.getCorreo(),
                estadoParticipante,
                usuario.getFechaCreacion(),
                total,
                confirmadas,
                canceladas,
                ultima
        );
    }

    private String escaparCsv(String valor) {
        if (valor == null) return "";
        return valor.replace("\"", "\"\"");
    }
}
