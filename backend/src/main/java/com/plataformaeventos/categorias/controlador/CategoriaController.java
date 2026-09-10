package com.plataformaeventos.categorias.controlador;

import com.plataformaeventos.categorias.dto.CategoriaRespuesta;
import com.plataformaeventos.categorias.dto.GuardarCategoriaSolicitud;
import com.plataformaeventos.categorias.servicio.CategoriaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/categorias")
@Tag(name = "Categorias", description = "Gestion de categorias de eventos")
@SecurityRequirement(name = "bearerAuth")
public class CategoriaController {

    private final CategoriaService categoriaService;

    public CategoriaController(CategoriaService categoriaService) {
        this.categoriaService = categoriaService;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('EVENTO_VER', 'ROLE_ADMINISTRADOR', 'ROLE_ORGANIZADOR')")
    @Operation(summary = "Listar categorias")
    @ApiResponse(responseCode = "200", description = "Categorias consultadas")
    public ResponseEntity<List<CategoriaRespuesta>> listarCategorias() {
        return ResponseEntity.ok(categoriaService.listarCategorias());
    }

    @GetMapping("/{idCategoria}")
    @PreAuthorize("hasAnyAuthority('EVENTO_VER', 'ROLE_ADMINISTRADOR', 'ROLE_ORGANIZADOR')")
    @Operation(summary = "Consultar categoria por identificador")
    @ApiResponse(responseCode = "200", description = "Categoria consultada")
    @ApiResponse(responseCode = "404", description = "Categoria no encontrada", content = @Content)
    public ResponseEntity<CategoriaRespuesta> obtenerCategoria(@PathVariable Long idCategoria) {
        return ResponseEntity.ok(categoriaService.obtenerCategoria(idCategoria));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('EVENTO_CREAR')")
    @Operation(summary = "Crear categoria")
    @ApiResponse(responseCode = "201", description = "Categoria creada")
    @ApiResponse(responseCode = "400", description = "Datos invalidos", content = @Content)
    public ResponseEntity<CategoriaRespuesta> crearCategoria(
            @Valid @RequestBody GuardarCategoriaSolicitud solicitud
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoriaService.crearCategoria(solicitud));
    }

    @PutMapping("/{idCategoria}")
    @PreAuthorize("hasAuthority('EVENTO_EDITAR')")
    @Operation(summary = "Actualizar categoria")
    @ApiResponse(responseCode = "200", description = "Categoria actualizada")
    @ApiResponse(responseCode = "400", description = "Datos invalidos", content = @Content)
    @ApiResponse(responseCode = "404", description = "Categoria no encontrada", content = @Content)
    public ResponseEntity<CategoriaRespuesta> actualizarCategoria(
            @PathVariable Long idCategoria,
            @Valid @RequestBody GuardarCategoriaSolicitud solicitud
    ) {
        return ResponseEntity.ok(categoriaService.actualizarCategoria(idCategoria, solicitud));
    }

    @DeleteMapping("/{idCategoria}")
    @PreAuthorize("hasAuthority('EVENTO_ELIMINAR')")
    @Operation(summary = "Eliminar categoria")
    @ApiResponse(responseCode = "204", description = "Categoria eliminada")
    @ApiResponse(responseCode = "400", description = "Categoria asociada a eventos", content = @Content)
    @ApiResponse(responseCode = "404", description = "Categoria no encontrada", content = @Content)
    public ResponseEntity<Void> eliminarCategoria(@PathVariable Long idCategoria) {
        categoriaService.eliminarCategoria(idCategoria);
        return ResponseEntity.noContent().build();
    }
}
