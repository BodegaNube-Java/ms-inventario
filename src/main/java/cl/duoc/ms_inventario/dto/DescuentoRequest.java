package cl.duoc.ms_inventario.dto;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** Cuerpo de PATCH /inventario/{productoId}/descuento */
public record DescuentoRequest(

        @NotNull(message = "El ordenId es obligatorio")
        UUID ordenId
) {
}
