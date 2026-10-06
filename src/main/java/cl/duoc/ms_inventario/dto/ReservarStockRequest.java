package cl.duoc.ms_inventario.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * Cuerpo de POST /inventario/reservar.
 * Lo envia ms-ordenes, nunca Lambda ni un usuario final.
 */
public record ReservarStockRequest(

        @NotNull(message = "El ordenId es obligatorio")
        UUID ordenId,

        @NotEmpty(message = "Debe incluir al menos un item")
        @Valid
        List<ItemReservaRequest> items
) {
}