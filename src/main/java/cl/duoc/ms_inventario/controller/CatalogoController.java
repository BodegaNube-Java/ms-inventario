package cl.duoc.ms_inventario.controller;

import cl.duoc.ms_inventario.dto.ProductoRequest;
import cl.duoc.ms_inventario.dto.ProductoResponse;
import cl.duoc.ms_inventario.dto.ProductoUpdateRequest;
import cl.duoc.ms_inventario.service.CatalogoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/catalogo")
@RequiredArgsConstructor
public class CatalogoController {

    private final CatalogoService catalogoService;

    /** GET /catalogo — lista productos disponibles */
    @GetMapping
    public ResponseEntity<List<ProductoResponse>> listar() {
        return ResponseEntity.ok(catalogoService.listarActivos());
    }

    /** GET /catalogo/{productoId} */
    @GetMapping("/{productoId}")
    public ResponseEntity<ProductoResponse> obtener(@PathVariable UUID productoId) {
        return ResponseEntity.ok(catalogoService.obtenerPorId(productoId));
    }

    /** POST /catalogo/productos — crea un producto con su stock inicial */
    @PostMapping("/productos")
    public ResponseEntity<ProductoResponse> crear(@Valid @RequestBody ProductoRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(catalogoService.crear(request));
    }

    @PutMapping("/productos/{id}")
    public ResponseEntity<ProductoResponse> actualizar(
            @PathVariable UUID id,
            @Valid @RequestBody ProductoUpdateRequest request) {
        return ResponseEntity.ok(catalogoService.actualizar(id, request));
    }

    @DeleteMapping("/productos/{id}")
    public ResponseEntity<Void> desactivar(@PathVariable UUID id) {
        catalogoService.desactivar(id);
        return ResponseEntity.noContent().build();
    }
}