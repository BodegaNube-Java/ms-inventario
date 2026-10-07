package cl.duoc.ms_inventario.controller;

import cl.duoc.ms_inventario.dto.DescuentoRequest;
import cl.duoc.ms_inventario.dto.ProductoResponse;
import cl.duoc.ms_inventario.dto.ReservaResponse;
import cl.duoc.ms_inventario.dto.ReservarStockRequest;
import cl.duoc.ms_inventario.service.CatalogoService;
import cl.duoc.ms_inventario.service.StockService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/inventario")
@RequiredArgsConstructor
public class InventarioController {

    private final StockService stockService;
    private final CatalogoService catalogoService;

    /** GET /inventario/{productoId} — consulta stock */
    @GetMapping("/{productoId}")
    public ResponseEntity<ProductoResponse> consultarStock(@PathVariable UUID productoId) {
        return ResponseEntity.ok(catalogoService.obtenerPorId(productoId));
    }

    /**
     * POST /inventario/reservar — lo invoca ms-ordenes.
     *
     * 200 -> stock reservado (o ya estaba reservado: es idempotente)
     * 409 -> sin stock suficiente (lo maneja GlobalExceptionHandler)
     */
    @PostMapping("/reservar")
    public ResponseEntity<ReservaResponse> reservar(
            @Valid @RequestBody ReservarStockRequest request) {
        return ResponseEntity.ok(stockService.reservar(request));
    }

    /**
     * PATCH /inventario/{productoId}/descuento — lo invoca el OPERARIO al
     * confirmar el picking fisico. Distinto de la reserva automatica.
     */
    @PatchMapping("/{productoId}/descuento")
    public ResponseEntity<Void> confirmarDescuento(
            @PathVariable UUID productoId,
            @Valid @RequestBody DescuentoRequest request) {

        stockService.confirmarDescuento(request.ordenId(), productoId);
        return ResponseEntity.noContent().build();
    }

    /** DELETE /inventario/reservas/{ordenId} — libera reservas de una orden cancelada */
    @DeleteMapping("/reservas/{ordenId}")
    public ResponseEntity<Void> liberar(@PathVariable UUID ordenId) {
        stockService.liberar(ordenId);
        return ResponseEntity.noContent().build();
    }
}