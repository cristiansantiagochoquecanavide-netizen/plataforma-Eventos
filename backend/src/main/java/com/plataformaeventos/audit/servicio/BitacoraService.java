package com.plataformaeventos.audit.servicio;

import com.plataformaeventos.audit.modelo.Bitacora;
import com.plataformaeventos.audit.repositorio.BitacoraRepository;
import com.plataformaeventos.organizaciones.modelo.Organizacion;
import com.plataformaeventos.usuarios.modelo.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;

@Service
public class BitacoraService {
    private final BitacoraRepository repositorio;
    public BitacoraService(BitacoraRepository repositorio) { this.repositorio = repositorio; }

    @Transactional
    public void registrar(Usuario actor, Organizacion organizacion, String accion, String modulo,
                          String entidad, Long idEntidad, String ip, boolean exito, String resultado) {
        if (organizacion == null || modulo == null || entidad == null) return;
        repositorio.save(new Bitacora(actor, organizacion, accion, modulo, entidad, idEntidad, ip, exito, resultado));
    }

    /** Compatibility for existing Staff audit calls; entity is explicitly the audited resource. */
    @Transactional
    public void registrar(Usuario actor, Organizacion organizacion, String accion, String modulo,
                          Long idEntidad, boolean exito, String resultado) {
        registrar(actor, organizacion, accion, modulo, modulo, idEntidad, null, exito, resultado);
    }

    @Transactional(readOnly = true)
    public Page<Bitacora> listar(Long organizacion, String modulo, String entidad, String accion,
                                 Boolean exito, Long actor, OffsetDateTime desde, OffsetDateTime hasta,
                                 Pageable pageable) {
        return repositorio.buscar(organizacion, modulo, entidad, accion, exito, actor, desde, hasta, pageable);
    }
}
