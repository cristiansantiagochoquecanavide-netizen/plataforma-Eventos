package com.plataformaeventos.categorias;

import com.plataformaeventos.eventos.EventoRepository;
import com.plataformaeventos.excepciones.ExcepcionNoEncontrado;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CategoriaServiceTest {

    private final CategoriaRepository categoriaRepository = mock(CategoriaRepository.class);
    private final EventoRepository eventoRepository = mock(EventoRepository.class);
    private final CategoriaService categoriaService = new CategoriaService(
            categoriaRepository,
            eventoRepository
    );

    @Test
    void crearCategoriaNormalizaDatosYValidaNombreUnico() {
        GuardarCategoriaSolicitud solicitud = new GuardarCategoriaSolicitud(
                " Congreso academico ",
                " Eventos de investigacion y difusion "
        );
        ArgumentCaptor<Categoria> captor = ArgumentCaptor.forClass(Categoria.class);

        when(categoriaRepository.existsByNombreIgnoreCase("Congreso academico")).thenReturn(false);
        when(categoriaRepository.saveAndFlush(any(Categoria.class))).thenAnswer(invocacion -> {
            Categoria categoria = invocacion.getArgument(0);
            ReflectionTestUtils.setField(categoria, "id", 12L);
            return categoria;
        });

        CategoriaRespuesta respuesta = categoriaService.crearCategoria(solicitud);

        verify(categoriaRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getNombre()).isEqualTo("Congreso academico");
        assertThat(captor.getValue().getDescripcion()).isEqualTo("Eventos de investigacion y difusion");
        assertThat(respuesta.id()).isEqualTo(12L);
        assertThat(respuesta.nombre()).isEqualTo("Congreso academico");
    }

    @Test
    void crearCategoriaFallaSiNombreYaExiste() {
        GuardarCategoriaSolicitud solicitud = new GuardarCategoriaSolicitud("General", null);

        when(categoriaRepository.existsByNombreIgnoreCase("General")).thenReturn(true);

        assertThatThrownBy(() -> categoriaService.crearCategoria(solicitud))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Ya existe una categoria");
    }

    @Test
    void crearCategoriaMapeaColisionConcurrenteAErrorDeNegocio() {
        GuardarCategoriaSolicitud solicitud = new GuardarCategoriaSolicitud("General", null);

        when(categoriaRepository.existsByNombreIgnoreCase("General")).thenReturn(false);
        when(categoriaRepository.saveAndFlush(any(Categoria.class)))
                .thenThrow(new DataIntegrityViolationException("duplicado"));

        assertThatThrownBy(() -> categoriaService.crearCategoria(solicitud))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Ya existe una categoria");
    }

    @Test
    void listarCategoriasUsaOrdenPorNombre() {
        Categoria categoria = categoriaConId(3L, "Academico", null);
        when(categoriaRepository.findAll(any(Sort.class))).thenReturn(List.of(categoria));

        List<CategoriaRespuesta> categorias = categoriaService.listarCategorias();

        assertThat(categorias).hasSize(1);
        assertThat(categorias.getFirst().nombre()).isEqualTo("Academico");
        verify(categoriaRepository).findAll(any(Sort.class));
    }

    @Test
    void actualizarCategoriaFallaSiNoExiste() {
        when(categoriaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoriaService.actualizarCategoria(
                99L,
                new GuardarCategoriaSolicitud("Nueva", null)
        ))
                .isInstanceOf(ExcepcionNoEncontrado.class)
                .hasMessageContaining("Categoria no encontrada");
    }

    @Test
    void eliminarCategoriaFallaSiTieneEventosAsociados() {
        Categoria categoria = categoriaConId(5L, "Tecnologia", null);
        when(categoriaRepository.findById(5L)).thenReturn(Optional.of(categoria));
        when(eventoRepository.existsByCategoriaId(5L)).thenReturn(true);

        assertThatThrownBy(() -> categoriaService.eliminarCategoria(5L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("asociada a eventos");
    }

    @Test
    void eliminarCategoriaSinEventosLaBorra() {
        Categoria categoria = categoriaConId(6L, "Cultura", null);
        when(categoriaRepository.findById(6L)).thenReturn(Optional.of(categoria));
        when(eventoRepository.existsByCategoriaId(6L)).thenReturn(false);

        categoriaService.eliminarCategoria(6L);

        verify(categoriaRepository).delete(categoria);
    }

    private Categoria categoriaConId(Long id, String nombre, String descripcion) {
        Categoria categoria = new Categoria(nombre, descripcion);
        ReflectionTestUtils.setField(categoria, "id", id);
        return categoria;
    }
}
