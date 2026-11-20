package com.plataformaeventos.audit.servicio;

import com.plataformaeventos.audit.cifrado.BitacoraCifradoService;
import com.plataformaeventos.audit.cifrado.BitacoraIndiceService;
import com.plataformaeventos.audit.cifrado.BitacoraPayload;
import com.plataformaeventos.audit.cifrado.BitacoraPayloadCifrado;
import com.plataformaeventos.audit.dto.BitacoraRespuesta;
import com.plataformaeventos.audit.modelo.Bitacora;
import com.plataformaeventos.audit.repositorio.BitacoraRepository;
import com.plataformaeventos.organizaciones.modelo.Organizacion;
import com.plataformaeventos.usuarios.modelo.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;

@Service
public class BitacoraService {
    private final BitacoraRepository repositorio;
    private final BitacoraCifradoService cifradoService;
    private final BitacoraIndiceService indiceService;

    public BitacoraService(BitacoraRepository repositorio, BitacoraCifradoService cifradoService,
                           BitacoraIndiceService indiceService) {
        this.repositorio = repositorio;
        this.cifradoService = cifradoService;
        this.indiceService = indiceService;
    }

    @Transactional
    public void registrar(Usuario actor, Organizacion organizacion, String accion, String modulo,
                          String entidad, Long idEntidad, String ip, boolean exito, String resultado) {
        if (organizacion == null || modulo == null || entidad == null) return;
        Long tenantId = organizacion.getId();
        BitacoraPayload payload = new BitacoraPayload(
                actor == null ? null : actor.getId(), accion, modulo, entidad, idEntidad, ip, exito, resultado);
        var envelope = cifradoService.cifrar(tenantId, payload);
        repositorio.save(Bitacora.cifrada(
                organizacion,
                envelope,
                indiceService.token(tenantId, "actor", payload.actorId()),
                indiceService.token(tenantId, "ip", payload.ip()),
                indiceService.token(tenantId, "accion", payload.accion()),
                indiceService.token(tenantId, "modulo", payload.modulo()),
                indiceService.token(tenantId, "entidad", payload.entidad()),
                indiceService.token(tenantId, "idEntidad", payload.entidadId()),
                indiceService.token(tenantId, "exito", payload.exito())
        ));
    }

    /** Compatibility for existing Staff audit calls; entity is explicitly the audited resource. */
    @Transactional
    public void registrar(Usuario actor, Organizacion organizacion, String accion, String modulo,
                          Long idEntidad, boolean exito, String resultado) {
        registrar(actor, organizacion, accion, modulo, modulo, idEntidad, null, exito, resultado);
    }

    @Transactional(readOnly = true)
    public Page<BitacoraRespuesta> listar(Long organizacion, String modulo, String entidad, String accion,
                                 Boolean exito, Long actor, OffsetDateTime desde, OffsetDateTime hasta,
                                 Pageable pageable) {
        OffsetDateTime fDesde = desde != null ? desde : OffsetDateTime.of(1970, 1, 1, 0, 0, 0, 0, java.time.ZoneOffset.UTC);
        OffsetDateTime fHasta = hasta != null ? hasta : OffsetDateTime.of(3000, 1, 1, 0, 0, 0, 0, java.time.ZoneOffset.UTC);
        return repositorio.buscar(
                organizacion,
                indiceService.token(organizacion, "modulo", modulo), indiceService.normalizar(modulo),
                indiceService.token(organizacion, "entidad", entidad), indiceService.normalizar(entidad),
                indiceService.token(organizacion, "accion", accion), indiceService.normalizar(accion),
                indiceService.token(organizacion, "exito", exito), exito,
                indiceService.token(organizacion, "actor", actor), actor,
                fDesde, fHasta, ordenarSoloPorMetadatos(pageable)
        ).map(this::respuestaAutorizada);
    }

    private BitacoraRespuesta respuestaAutorizada(Bitacora bitacora) {
        if (!bitacora.estaCifrada()) {
            return BitacoraRespuesta.desde(bitacora);
        }
        Long tenantId = bitacora.getOrganizacion().getId();
        BitacoraPayload payload = cifradoService.descifrar(tenantId, new BitacoraPayloadCifrado(
                bitacora.getPayloadCiphertext(),
                bitacora.getPayloadDekCifrada(),
                bitacora.getPayloadNonce(),
                bitacora.getCryptoVersion()
        ));
        return BitacoraRespuesta.desde(bitacora, payload);
    }

    private Pageable ordenarSoloPorMetadatos(Pageable pageable) {
        if (pageable == null || pageable.isUnpaged()) {
            return Pageable.unpaged();
        }
        Sort sort = Sort.by(pageable.getSort().stream()
                .filter(order -> "fecha".equals(order.getProperty()) || "id".equals(order.getProperty()))
                .toList());
        if (sort.isUnsorted()) {
            sort = Sort.by(Sort.Direction.DESC, "fecha");
        }
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
    }
}
