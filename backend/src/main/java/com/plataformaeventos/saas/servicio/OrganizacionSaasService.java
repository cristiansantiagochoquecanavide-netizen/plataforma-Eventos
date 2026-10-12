package com.plataformaeventos.saas.servicio;

import com.plataformaeventos.excepciones.ExcepcionNoEncontrado;
import com.plataformaeventos.audit.servicio.AuditoriaContexto;
import com.plataformaeventos.audit.servicio.BitacoraService;
import com.plataformaeventos.organizaciones.modelo.Organizacion;
import com.plataformaeventos.organizaciones.repositorio.OrganizacionRepository;
import com.plataformaeventos.saas.dto.ActualizarEstadoOrganizacionSaasSolicitud;
import com.plataformaeventos.saas.dto.OrganizacionSaasRespuesta;
import com.plataformaeventos.saas.dto.PaginaOrganizacionesSaasRespuesta;
import com.plataformaeventos.usuarios.repositorio.UsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.time.OffsetDateTime;
import java.util.Locale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrganizacionSaasService {
    private static final String SLUG_PRINCIPAL = "principal";

    private final OrganizacionRepository organizaciones;
    private final BitacoraService bitacora;
    private final UsuarioRepository usuarios;

    public OrganizacionSaasService(OrganizacionRepository organizaciones) {
        this(organizaciones, null, null);
    }

    @Autowired
    public OrganizacionSaasService(OrganizacionRepository organizaciones, BitacoraService bitacora,
                                   UsuarioRepository usuarios) {
        this.organizaciones = organizaciones;
        this.bitacora = bitacora;
        this.usuarios = usuarios;
    }

    @Transactional(readOnly = true)
    public PaginaOrganizacionesSaasRespuesta listar(int pagina, int tamano, String busqueda, String estado) {
        String consulta = busqueda == null || busqueda.isBlank() ? "" : busqueda.trim().toLowerCase(Locale.ROOT);
        String estadoNormalizado = estado == null || estado.isBlank() ? "" : estado.trim().toUpperCase(Locale.ROOT);
        Page<OrganizacionSaasRespuesta> resultados = organizaciones
                .listarParaAdministracionSaas(consulta, estadoNormalizado, PageRequest.of(pagina, tamano))
                .map(fila -> OrganizacionSaasRespuesta.desde(fila, OffsetDateTime.now()));
        return new PaginaOrganizacionesSaasRespuesta(
                resultados.getContent(), resultados.getNumber(), resultados.getSize(),
                resultados.getTotalElements(), resultados.getTotalPages());
    }

    @Transactional
    public void actualizarEstado(Long idOrganizacion, ActualizarEstadoOrganizacionSaasSolicitud solicitud) {
        actualizarEstado(idOrganizacion, solicitud, null, null);
    }

    @Transactional
    public void actualizarEstado(Long idOrganizacion, ActualizarEstadoOrganizacionSaasSolicitud solicitud,
                                 Authentication actor, HttpServletRequest request) {
        Organizacion organizacion = organizaciones.findById(idOrganizacion)
                .orElseThrow(() -> new ExcepcionNoEncontrado("Organizacion no encontrada"));
        if (SLUG_PRINCIPAL.equals(organizacion.getSlug()) && "INACTIVA".equals(solicitud.estado())) {
            throw new IllegalArgumentException("La organizacion principal de la plataforma no se puede inactivar");
        }
        String estadoAnterior = organizacion.getEstado();
        organizacion.actualizarEstado(solicitud.estado());
        if (bitacora != null && usuarios != null && actor != null
                && !estadoAnterior.equals(organizacion.getEstado())) {
            usuarios.findByCorreo(actor.getName()).ifPresent(usuario -> bitacora.registrar(
                    usuario, organizacion,
                    "ACTIVA".equals(organizacion.getEstado()) ? "REACTIVAR" : "DESACTIVAR",
                    "SAAS", "ORGANIZACION", organizacion.getId(),
                    AuditoriaContexto.ip(request), true, "Estado de organización actualizado"));
        }
    }
}
