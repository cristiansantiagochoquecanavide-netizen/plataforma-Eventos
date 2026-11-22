package com.plataformaeventos.certificados.servicio;

import com.plataformaeventos.asistencias.repositorio.AsistenciaRepository;
import com.plataformaeventos.certificados.dto.CertificadoRespuesta;
import com.plataformaeventos.certificados.dto.VerificacionCertificadoRespuesta;
import com.plataformaeventos.certificados.modelo.Certificado;
import com.plataformaeventos.certificados.repositorio.CertificadoRepository;
import com.plataformaeventos.eventos.repositorio.EventoRepository;
import com.plataformaeventos.inscripciones.modelo.EstadoInscripcion;
import com.plataformaeventos.inscripciones.modelo.Inscripcion;
import com.plataformaeventos.inscripciones.repositorio.InscripcionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
public class CertificadoService {

    private final CertificadoRepository certificadoRepository;
    private final InscripcionRepository inscripcionRepository;
    private final AsistenciaRepository asistenciaRepository;
    private final EventoRepository eventoRepository;
    private final GeneradorPdfCertificado generadorPdf;
    private final String frontendUrl;

    public CertificadoService(
            CertificadoRepository certificadoRepository,
            InscripcionRepository inscripcionRepository,
            AsistenciaRepository asistenciaRepository,
            EventoRepository eventoRepository,
            GeneradorPdfCertificado generadorPdf,
            @Value("${app.frontend.url:http://localhost:4200}") String frontendUrl
    ) {
        this.certificadoRepository = certificadoRepository;
        this.inscripcionRepository = inscripcionRepository;
        this.asistenciaRepository = asistenciaRepository;
        this.eventoRepository = eventoRepository;
        this.generadorPdf = generadorPdf;
        this.frontendUrl = frontendUrl;
    }

    @Transactional
    public CertificadoRespuesta emitirCertificado(Long idInscripcion, Long idOrganizacion) {
        Inscripcion inscripcion = inscripcionRepository.findById(idInscripcion)
                .orElseThrow(() -> new IllegalArgumentException("Inscripcion no encontrada"));

        if (!inscripcion.getOrganizacion().getId().equals(idOrganizacion)) {
            throw new IllegalArgumentException("La inscripcion no pertenece a la organizacion activa");
        }

        if (inscripcion.getEstado() != EstadoInscripcion.CONFIRMADA) {
            throw new IllegalStateException("La inscripcion no se encuentra en estado CONFIRMADA");
        }

        // Regla esencial de negocio: Debe haber registrado asistencia efectiva
        boolean asistio = asistenciaRepository.existsByInscripcionId(idInscripcion);
        if (!asistio) {
            throw new IllegalStateException("El participante no cuenta con registro de asistencia verificado para este evento");
        }

        // Si ya fue emitido, retornar el existente (idempotente)
        var existente = certificadoRepository.findByInscripcionIdAndOrganizacionId(idInscripcion, idOrganizacion);
        if (existente.isPresent()) {
            return mapearARespuesta(existente.get());
        }

        String codigo = generarCodigoVerificacion();
        String hash = generarHashSeguridad(inscripcion, codigo);

        Certificado nuevo = new Certificado(inscripcion, inscripcion.getOrganizacion(), codigo, hash);
        Certificado guardado = certificadoRepository.save(nuevo);

        return mapearARespuesta(guardado);
    }

    @Transactional
    public List<CertificadoRespuesta> emitirMasivoParaEvento(Long idEvento, Long idOrganizacion) {
        var evento = eventoRepository.findById(idEvento)
                .orElseThrow(() -> new IllegalArgumentException("Evento no encontrado"));

        if (!evento.getOrganizacion().getId().equals(idOrganizacion)) {
            throw new IllegalArgumentException("El evento no pertenece a la organizacion activa");
        }

        List<Inscripcion> inscripciones = inscripcionRepository.findConfirmedByEventoAndOrganizacion(idEvento, idOrganizacion);
        List<CertificadoRespuesta> emitidos = new ArrayList<>();

        for (Inscripcion inscripcion : inscripciones) {
            if (asistenciaRepository.existsByInscripcionId(inscripcion.getId())) {
                if (!certificadoRepository.existsByInscripcionIdAndOrganizacionId(inscripcion.getId(), idOrganizacion)) {
                    emitidos.add(emitirCertificado(inscripcion.getId(), idOrganizacion));
                }
            }
        }

        return emitidos;
    }

    @Transactional(readOnly = true)
    public List<CertificadoRespuesta> obtenerMisCertificados(Long idUsuario, Long idOrganizacion) {
        return certificadoRepository.findByUsuarioIdAndOrganizacionId(idUsuario, idOrganizacion)
                .stream()
                .map(this::mapearARespuesta)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CertificadoRespuesta> obtenerCertificadosPorEvento(Long idEvento, Long idOrganizacion) {
        return certificadoRepository.findByEventoIdAndOrganizacionId(idEvento, idOrganizacion)
                .stream()
                .map(this::mapearARespuesta)
                .toList();
    }

    @Transactional(readOnly = true)
    public byte[] descargarPdf(Long idCertificado, Long idOrganizacion, Long idUsuario, boolean esAdminUOrganizador) {
        Certificado certificado = certificadoRepository.findById(idCertificado)
                .orElseThrow(() -> new IllegalArgumentException("Certificado no encontrado"));

        if (!certificado.getOrganizacion().getId().equals(idOrganizacion)) {
            throw new IllegalArgumentException("El certificado no pertenece a la organizacion activa");
        }

        Long participanteId = certificado.getInscripcion().getParticipante().getId();
        if (!esAdminUOrganizador && !participanteId.equals(idUsuario)) {
            throw new IllegalStateException("No tienes autorizacion para descargar este certificado");
        }

        return generadorPdf.generar(certificado, frontendUrl);
    }

    @Transactional(readOnly = true)
    public VerificacionCertificadoRespuesta verificarPublico(String codigoVerificacion) {
        var opt = certificadoRepository.findByCodigoVerificacionConDetalles(codigoVerificacion.trim().toUpperCase());
        if (opt.isEmpty()) {
            return new VerificacionCertificadoRespuesta(
                    false,
                    codigoVerificacion,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    "Certificado no encontrado o codigo de verificacion invalido."
            );
        }

        Certificado cert = opt.get();
        var inscripcion = cert.getInscripcion();
        var participante = inscripcion.getParticipante();
        var evento = inscripcion.getEvento();
        var org = cert.getOrganizacion();

        return new VerificacionCertificadoRespuesta(
                true,
                cert.getCodigoVerificacion(),
                participante.getNombres() + " " + participante.getApellidos(),
                evento.getNombre(),
                org.getNombre(),
                cert.getFechaEmision(),
                evento.getFechaInicio(),
                cert.getHashSeguridad(),
                "Certificado valido y verificado en la plataforma oficial."
        );
    }

    private CertificadoRespuesta mapearARespuesta(Certificado c) {
        var ins = c.getInscripcion();
        var ev = ins.getEvento();
        var part = ins.getParticipante();
        var org = c.getOrganizacion();
        return new CertificadoRespuesta(
                c.getId(),
                ins.getId(),
                ev.getId(),
                ev.getNombre(),
                part.getId(),
                part.getNombres() + " " + part.getApellidos(),
                part.getCorreo(),
                c.getCodigoVerificacion(),
                c.getHashSeguridad(),
                c.getFechaEmision(),
                org.getId(),
                org.getNombre()
        );
    }

    private String generarCodigoVerificacion() {
        String uuid = UUID.randomUUID().toString().replace("-", "").toUpperCase();
        return "CERT-" + uuid.substring(0, 4) + "-" + uuid.substring(4, 8) + "-" + uuid.substring(8, 12);
    }

    private String generarHashSeguridad(Inscripcion inscripcion, String codigo) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String datos = inscripcion.getId() + ":" +
                    inscripcion.getOrganizacion().getId() + ":" +
                    inscripcion.getParticipante().getId() + ":" +
                    inscripcion.getEvento().getId() + ":" +
                    codigo + ":" +
                    System.currentTimeMillis();
            byte[] hash = digest.digest(datos.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            return UUID.randomUUID().toString();
        }
    }
}
