package com.plataformaeventos.categorias;

import com.plataformaeventos.categorias.dto.GuardarCategoriaSolicitud;
import com.plataformaeventos.categorias.modelo.Categoria;
import com.plataformaeventos.categorias.repositorio.CategoriaRepository;
import com.plataformaeventos.categorias.servicio.CategoriaService;
import com.plataformaeventos.eventos.repositorio.EventoRepository;
import com.plataformaeventos.excepciones.ExcepcionNoEncontrado;
import com.plataformaeventos.organizaciones.modelo.Organizacion;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CategoriaTenantServiceTest {
    private final CategoriaRepository categorias = mock(CategoriaRepository.class);
    private final EventoRepository eventos = mock(EventoRepository.class);
    private final CategoriaService servicio = new CategoriaService(categorias, eventos);

    @Test
    void obtenerCategoriaNoPermiteAccesoCruzado() {
        Organizacion orgA = org(1L);
        when(categorias.findByIdAndOrganizacionId(7L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.obtenerCategoria(7L, orgA))
                .isInstanceOf(ExcepcionNoEncontrado.class);
        verify(categorias).findByIdAndOrganizacionId(7L, 1L);
    }

    @Test
    void crearCategoriaAsignaLaOrganizacionActiva() {
        Organizacion orgA = org(1L);
        when(categorias.existsByNombreIgnoreCaseAndOrganizacionId("General", 1L)).thenReturn(false);
        when(categorias.saveAndFlush(any(Categoria.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        servicio.crearCategoria(new GuardarCategoriaSolicitud("General", null), orgA);

        verify(categorias).saveAndFlush(argThat(categoria -> categoria.getOrganizacion() == orgA));
    }

    private Organizacion org(Long id) {
        Organizacion org = new Organizacion("Org", "org" + id);
        ReflectionTestUtils.setField(org, "id", id);
        return org;
    }
}
