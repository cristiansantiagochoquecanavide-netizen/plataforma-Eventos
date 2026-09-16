package com.plataformaeventos.convocatorias.postulaciones;

import com.plataformaeventos.categorias.modelo.Categoria;
import com.plataformaeventos.categorias.repositorio.CategoriaRepository;
import com.plataformaeventos.convocatorias.modelo.Convocatoria;
import com.plataformaeventos.convocatorias.repositorio.ConvocatoriaRepository;
import com.plataformaeventos.convocatorias.modelo.TipoPersonal;
import com.plataformaeventos.eventos.modelo.EstadoEvento;
import com.plataformaeventos.eventos.modelo.Evento;
import com.plataformaeventos.eventos.repositorio.EventoRepository;
import com.plataformaeventos.usuarios.modelo.EstadoUsuario;
import com.plataformaeventos.usuarios.modelo.Usuario;
import com.plataformaeventos.usuarios.repositorio.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Ejecutar unicamente contra PostgreSQL temporal con CU16_INTEGRACION=true. */
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "CU16_INTEGRACION", matches = "true")
class PostulacionStaffIntegracionTest {
    @Autowired private PostulacionStaffService servicio;
    @Autowired private ConvocatoriaRepository convocatorias;
    @Autowired private EventoRepository eventos;
    @Autowired private CategoriaRepository categorias;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PlatformTransactionManager transactionManager;

    @Test
    void flywayJpaPermisosYRestriccionesSonCoherentes() {
        assertThat(jdbc.queryForObject("SELECT success FROM flyway_schema_history WHERE version = '9'", Boolean.class)).isTrue();
        assertThat(jdbc.queryForList("""
                SELECT r.nombre FROM rol r JOIN rol_permiso rp USING (id_rol)
                JOIN permiso p USING (id_permiso) WHERE p.nombre = 'CONVOCATORIA_POSTULARSE'
                """, String.class)).containsExactlyInAnyOrder("STAFF", "PARTICIPANTE");
        Usuario usuario = usuarios.saveAndFlush(new Usuario("Prueba", "CU16", "cu16-integracion@example.com", "hash", EstadoUsuario.ACTIVO));
        Evento evento = eventos.saveAndFlush(new Evento("CU16", "Prueba", OffsetDateTime.now().plusDays(3),
                OffsetDateTime.now().plusDays(4), "Auditorio", EstadoEvento.PUBLICADO,
                categorias.saveAndFlush(new Categoria("CU16-" + System.nanoTime(), "Prueba CU16"))));
        Convocatoria convocatoria = convocatorias.saveAndFlush(new Convocatoria(evento, TipoPersonal.STAFF,
                "Apoyo", 2, LocalDate.now().plusDays(1)));
        var respuesta = servicio.postularse(convocatoria.getId(), usuario.getCorreo());
        assertThat(respuesta.fechaPostulacion()).isNotNull();
        assertThat(servicio.consultar(convocatoria.getId(), usuario.getCorreo())).isPresent();
        assertThat(servicio.listarMias(usuario.getCorreo())).hasSize(1);
        assertThatThrownBy(() -> jdbc.update("INSERT INTO postulacion_staff (id_convocatoria, id_usuario) VALUES (?, ?)",
                convocatoria.getId(), usuario.getId())).isInstanceOf(DataIntegrityViolationException.class);
        assertThat(servicio.retirar(respuesta.id(), usuario.getCorreo()).estado()).isEqualTo(EstadoPostulacion.RETIRADA);
        assertThatThrownBy(() -> servicio.postularse(convocatoria.getId(), usuario.getCorreo()))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("ya se ha postulado");
    }

    @Test
    void lecturaParaPostularBloqueaCierreConcurrente() {
        Evento evento = eventos.saveAndFlush(new Evento("Concurrencia CU16", "Prueba", OffsetDateTime.now().plusDays(3),
                OffsetDateTime.now().plusDays(4), "Auditorio", EstadoEvento.PUBLICADO,
                categorias.saveAndFlush(new Categoria("CU16-" + System.nanoTime(), "Prueba CU16"))));
        Convocatoria convocatoria = convocatorias.saveAndFlush(new Convocatoria(evento, TipoPersonal.STAFF,
                "Apoyo", 2, LocalDate.now().plusDays(1)));
        TransactionTemplate transaccion = new TransactionTemplate(transactionManager);
        try (var executor = Executors.newSingleThreadExecutor()) {
            transaccion.executeWithoutResult(status -> {
                assertThat(convocatorias.buscarParaPostulacion(convocatoria.getId())).isPresent();
                var resultado = executor.submit(() -> {
                    assertThatThrownBy(() -> transaccion.executeWithoutResult(otra -> {
                        jdbc.execute("SET LOCAL lock_timeout = '200ms'");
                        jdbc.update("UPDATE convocatoria_personal SET estado = 'CERRADA' WHERE id_convocatoria = ?", convocatoria.getId());
                    })).isInstanceOf(DataAccessException.class);
                });
                try {
                    resultado.get(5, TimeUnit.SECONDS);
                } catch (Exception error) {
                    throw new AssertionError("No se verifico el bloqueo concurrente", error);
                }
            });
        }
        assertThat(jdbc.update("UPDATE convocatoria_personal SET estado = 'CERRADA' WHERE id_convocatoria = ?", convocatoria.getId())).isEqualTo(1);
    }
}
