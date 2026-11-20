package com.plataformaeventos.certificados.controlador;

import com.plataformaeventos.audit.servicio.OperacionAuditable;
import com.plataformaeventos.certificados.dto.CertificadoRespuesta;
import com.plataformaeventos.certificados.servicio.CertificadoService;
import com.plataformaeventos.organizaciones.modelo.Organizacion;
import com.plataformaeventos.organizaciones.servicio.ContextoOrganizacion;
import com.plataformaeventos.usuarios.modelo.Usuario;
import com.plataformaeventos.usuarios.repositorio.UsuarioRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/certificados")
@Tag(name = "Certificados Digitales", description = "Gestion, emision y descarga de certificados digitales de asistencia (CU28)")
public class CertificadoController {

    private final CertificadoService certificadoService;
    private final ContextoOrganizacion contextoOrganizacion;
    private final UsuarioRepository usuarioRepository;

    public CertificadoController(
            CertificadoService certificadoService,
            ContextoOrganizacion contextoOrganizacion,
            UsuarioRepository usuarioRepository
    ) {
        this.certificadoService = certificadoService;
        this.contextoOrganizacion = contextoOrganizacion;
        this.usuarioRepository = usuarioRepository;
    }

    @PostMapping("/inscripcion/{idInscripcion}")
    @PreAuthorize("@contextoOrganizacion.tieneAutoridad(authentication, #request, 'CERTIFICADO_EMITIR')")
    @OperacionAuditable(modulo = "CERTIFICADOS", entidad = "CERTIFICADO", accion = "EMITIR")
    @Operation(summary = "Emitir certificado digital para una inscripcion con asistencia verificada")
    public ResponseEntity<CertificadoRespuesta> emitirCertificado(
            @PathVariable Long idInscripcion,
            Authentication auth,
            HttpServletRequest request
    ) {
        Organizacion org = contextoOrganizacion.resolver(auth, request);
        CertificadoRespuesta respuesta = certificadoService.emitirCertificado(idInscripcion, org.getId());
        return ResponseEntity.ok(respuesta);
    }

    @PostMapping("/evento/{idEvento}/emitir-masivo")
    @PreAuthorize("@contextoOrganizacion.tieneAutoridad(authentication, #request, 'CERTIFICADO_EMITIR')")
    @OperacionAuditable(modulo = "CERTIFICADOS", entidad = "EVENTO", accion = "EMITIR_MASIVO")
    @Operation(summary = "Emitir certificados masivos para todos los asistentes con asistencia de un evento")
    public ResponseEntity<List<CertificadoRespuesta>> emitirMasivo(
            @PathVariable Long idEvento,
            Authentication auth,
            HttpServletRequest request
    ) {
        Organizacion org = contextoOrganizacion.resolver(auth, request);
        List<CertificadoRespuesta> respuesta = certificadoService.emitirMasivoParaEvento(idEvento, org.getId());
        return ResponseEntity.ok(respuesta);
    }

    @GetMapping("/mis-certificados")
    @PreAuthorize("@contextoOrganizacion.tieneAutoridad(authentication, #request, 'CERTIFICADO_CONSULTAR')")
    @Operation(summary = "Obtener los certificados digitales del participante autenticado")
    public ResponseEntity<List<CertificadoRespuesta>> obtenerMisCertificados(
            Authentication auth,
            HttpServletRequest request
    ) {
        Organizacion org = contextoOrganizacion.resolver(auth, request);
        Usuario usuario = usuarioRepository.findByCorreo(auth.getName())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        List<CertificadoRespuesta> certificados = certificadoService.obtenerMisCertificados(usuario.getId(), org.getId());
        return ResponseEntity.ok(certificados);
    }

    @GetMapping("/evento/{idEvento}")
    @PreAuthorize("@contextoOrganizacion.tieneAutoridad(authentication, #request, 'CERTIFICADO_CONSULTAR')")
    @Operation(summary = "Listar los certificados emitidos para un evento")
    public ResponseEntity<List<CertificadoRespuesta>> listarPorEvento(
            @PathVariable Long idEvento,
            Authentication auth,
            HttpServletRequest request
    ) {
        Organizacion org = contextoOrganizacion.resolver(auth, request);
        List<CertificadoRespuesta> certificados = certificadoService.obtenerCertificadosPorEvento(idEvento, org.getId());
        return ResponseEntity.ok(certificados);
    }

    @GetMapping("/{idCertificado}/descargar")
    @PreAuthorize("@contextoOrganizacion.tieneAutoridad(authentication, #request, 'CERTIFICADO_CONSULTAR')")
    @Operation(summary = "Descargar certificado en formato PDF")
    public ResponseEntity<byte[]> descargarPdf(
            @PathVariable Long idCertificado,
            Authentication auth,
            HttpServletRequest request
    ) {
        Organizacion org = contextoOrganizacion.resolver(auth, request);
        Usuario usuario = usuarioRepository.findByCorreo(auth.getName())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        boolean esAdminUOrganizador = contextoOrganizacion.tieneAutoridad(auth, request, "CERTIFICADO_EMITIR");

        byte[] pdfBytes = certificadoService.descargarPdf(idCertificado, org.getId(), usuario.getId(), esAdminUOrganizador);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("inline", "certificado-" + idCertificado + ".pdf");
        headers.setContentLength(pdfBytes.length);

        return ResponseEntity.ok().headers(headers).body(pdfBytes);
    }
}
