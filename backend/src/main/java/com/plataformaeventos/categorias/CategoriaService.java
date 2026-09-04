package com.plataformaeventos.categorias;

import com.plataformaeventos.eventos.EventoRepository;
import com.plataformaeventos.excepciones.ExcepcionNoEncontrado;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final EventoRepository eventoRepository;

    public CategoriaService(CategoriaRepository categoriaRepository, EventoRepository eventoRepository) {
        this.categoriaRepository = categoriaRepository;
        this.eventoRepository = eventoRepository;
    }

    @Transactional(readOnly = true)
    public List<CategoriaRespuesta> listarCategorias() {
        return categoriaRepository.findAll(Sort.by(Sort.Direction.ASC, "nombre"))
                .stream()
                .map(this::convertirARespuesta)
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoriaRespuesta obtenerCategoria(Long idCategoria) {
        return convertirARespuesta(buscarCategoria(idCategoria));
    }

    @Transactional
    public CategoriaRespuesta crearCategoria(GuardarCategoriaSolicitud solicitud) {
        String nombre = normalizarNombre(solicitud.nombre());
        if (categoriaRepository.existsByNombreIgnoreCase(nombre)) {
            throw new IllegalArgumentException("Ya existe una categoria con el nombre " + nombre);
        }

        Categoria categoria = new Categoria(nombre, limpiarTexto(solicitud.descripcion()));
        return guardarCategoria(categoria, "Ya existe una categoria con el nombre " + nombre);
    }

    @Transactional
    public CategoriaRespuesta actualizarCategoria(Long idCategoria, GuardarCategoriaSolicitud solicitud) {
        Categoria categoria = buscarCategoria(idCategoria);
        String nombre = normalizarNombre(solicitud.nombre());
        if (categoriaRepository.existsByNombreIgnoreCaseAndIdNot(nombre, idCategoria)) {
            throw new IllegalArgumentException("Ya existe otra categoria con el nombre " + nombre);
        }

        categoria.actualizarDatos(nombre, limpiarTexto(solicitud.descripcion()));
        return guardarCategoria(categoria, "Ya existe otra categoria con el nombre " + nombre);
    }

    @Transactional
    public void eliminarCategoria(Long idCategoria) {
        Categoria categoria = buscarCategoria(idCategoria);
        if (eventoRepository.existsByCategoriaId(idCategoria)) {
            throw new IllegalArgumentException("No se puede eliminar una categoria asociada a eventos");
        }
        categoriaRepository.delete(categoria);
    }

    private Categoria buscarCategoria(Long idCategoria) {
        return categoriaRepository.findById(idCategoria)
                .orElseThrow(() -> new ExcepcionNoEncontrado("Categoria no encontrada con id " + idCategoria));
    }

    private String normalizarNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre de la categoria es obligatorio");
        }
        return nombre.trim();
    }

    private String limpiarTexto(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        return texto.trim();
    }

    private CategoriaRespuesta guardarCategoria(Categoria categoria, String mensajeDuplicado) {
        try {
            return convertirARespuesta(categoriaRepository.saveAndFlush(categoria));
        } catch (DataIntegrityViolationException excepcion) {
            throw new IllegalArgumentException(mensajeDuplicado, excepcion);
        }
    }

    private CategoriaRespuesta convertirARespuesta(Categoria categoria) {
        return new CategoriaRespuesta(
                categoria.getId(),
                categoria.getNombre(),
                categoria.getDescripcion(),
                categoria.getFechaCreacion(),
                categoria.getFechaActualizacion()
        );
    }
}
