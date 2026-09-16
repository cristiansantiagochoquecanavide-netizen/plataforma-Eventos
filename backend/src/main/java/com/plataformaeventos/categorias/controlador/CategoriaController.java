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
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import com.plataformaeventos.organizaciones.servicio.ContextoOrganizacion;

import java.util.List;

@RestController
@RequestMapping("/api/categorias")
@Tag(name = "Categorias", description = "Gestion de categorias de eventos")
@SecurityRequirement(name = "bearerAuth")
public class CategoriaController {

    private final CategoriaService categoriaService;
    private final ContextoOrganizacion contexto;

    public CategoriaController(CategoriaService categoriaService, ContextoOrganizacion contexto) {
        this.categoriaService = categoriaService;
        this.contexto = contexto;
    }

    @GetMapping
    @PreAuthorize("@contextoOrganizacion.tieneAutoridad(authentication, #request, 'EVENTO_VER')")
    @Operation(summary = "Listar categorias")
    @ApiResponse(responseCode = "200", description = "Categorias consultadas")
    public ResponseEntity<List<CategoriaRespuesta>> listarCategorias(Authentication auth, HttpServletRequest request) {
        return ResponseEntity.ok(categoriaService.listarCategorias(contexto.resolver(auth, request)));
    }

    @GetMapping("/{idCategoria}")
    @PreAuthorize("@contextoOrganizacion.tieneAutoridad(authentication, #request, 'EVENTO_VER')")
    @Operation(summary = "Consultar categoria por identificador")
    @ApiResponse(responseCode = "200", description = "Categoria consultada")
    @ApiResponse(responseCode = "404", description = "Categoria no encontrada", content = @Content)
    public ResponseEntity<CategoriaRespuesta> obtenerCategoria(@PathVariable Long idCategoria, Authentication auth, HttpServletRequest request) {
        return ResponseEntity.ok(categoriaService.obtenerCategoria(idCategoria, contexto.resolver(auth, request)));
    }

    @PostMapping
    @PreAuthorize("@contextoOrganizacion.tieneAutoridad(authentication, #request, 'EVENTO_CREAR')")
    @Operation(summary = "Crear categoria")
    @ApiResponse(responseCode = "201", description = "Categoria creada")
    @ApiResponse(responseCode = "400", description = "Datos invalidos", content = @Content)
    public ResponseEntity<CategoriaRespuesta> crearCategoria(
             @Valid @RequestBody GuardarCategoriaSolicitud solicitud
            , Authentication auth, HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoriaService.crearCategoria(solicitud, contexto.resolver(auth, request)));
    }

    @PutMapping("/{idCategoria}")
    @PreAuthorize("@contextoOrganizacion.tieneAutoridad(authentication, #request, 'EVENTO_EDITAR')")
    @Operation(summary = "Actualizar categoria")
    @ApiResponse(responseCode = "200", description = "Categoria actualizada")
    @ApiResponse(responseCode = "400", description = "Datos invalidos", content = @Content)
    @ApiResponse(responseCode = "404", description = "Categoria no encontrada", content = @Content)
    public ResponseEntity<CategoriaRespuesta> actualizarCategoria(
            @PathVariable Long idCategoria,
            @Valid @RequestBody GuardarCategoriaSolicitud solicitud, Authentication auth, HttpServletRequest request
    ) {
        return ResponseEntity.ok(categoriaService.actualizarCategoria(idCategoria, solicitud, contexto.resolver(auth, request)));
    }

    @DeleteMapping("/{idCategoria}")
    @PreAuthorize("@contextoOrganizacion.tieneAutoridad(authentication, #request, 'EVENTO_ELIMINAR')")
    @Operation(summary = "Eliminar categoria")
    @ApiResponse(responseCode = "204", description = "Categoria eliminada")
    @ApiResponse(responseCode = "400", description = "Categoria asociada a eventos", content = @Content)
    @ApiResponse(responseCode = "404", description = "Categoria no encontrada", content = @Content)
    public ResponseEntity<Void> eliminarCategoria(@PathVariable Long idCategoria, Authentication auth, HttpServletRequest request) {
        categoriaService.eliminarCategoria(idCategoria, contexto.resolver(auth, request));
        return ResponseEntity.noContent().build();
    }
}
