package com.plataformaeventos.eventos;

import com.plataformaeventos.categorias.modelo.Categoria;
import com.plataformaeventos.categorias.repositorio.CategoriaRepository;
import com.plataformaeventos.eventos.modelo.EstadoEvento;
import com.plataformaeventos.eventos.modelo.Evento;
import com.plataformaeventos.eventos.repositorio.EventoRepository;
import com.plataformaeventos.organizaciones.modelo.Organizacion;
import com.plataformaeventos.organizaciones.repositorio.OrganizacionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class EventoRepositoryTenantIsolationTest {

    @Autowired private EventoRepository eventos;
    @Autowired private OrganizacionRepository organizaciones;
    @Autowired private CategoriaRepository categorias;

    @Test
    void consultaPorIdNoExponeElEventoAlOtroTenant() {
        Organizacion tenantA = organizaciones.saveAndFlush(new Organizacion("Tenant A", "tenant-a"));
        Organizacion tenantB = organizaciones.saveAndFlush(new Organizacion("Tenant B", "tenant-b"));
        Categoria categoriaA = categorias.saveAndFlush(new Categoria("Categoria A", null, tenantA));
        Evento eventoA = new Evento(
                "Evento privado de A",
                null,
                OffsetDateTime.now().plusDays(1),
                null,
                "Auditorio A",
                EstadoEvento.PLANIFICADO,
                categoriaA
        );
        eventoA.asignarOrganizacion(tenantA);
        eventoA = eventos.saveAndFlush(eventoA);

        assertThat(eventos.findByIdAndOrganizacionId(eventoA.getId(), tenantA.getId()))
                .isPresent()
                .get()
                .extracting(Evento::getNombre)
                .isEqualTo("Evento privado de A");
        assertThat(eventos.findByIdAndOrganizacionId(eventoA.getId(), tenantB.getId())).isEmpty();
        assertThat(eventos.findByOrganizacionIdAndEstadoNotOrderByFechaInicioAsc(tenantB.getId(), EstadoEvento.CANCELADO))
                .isEmpty();
    }
}
