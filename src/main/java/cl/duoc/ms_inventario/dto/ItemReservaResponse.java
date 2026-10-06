package cl.duoc.ms_inventario.dto;


import cl.duoc.ms_inventario.model.Reserva;

import java.util.UUID;

public record ItemReservaResponse(
        UUID productoId,
        Integer cantidad
) {
    public static ItemReservaResponse desde(Reserva reserva) {
        return new ItemReservaResponse(reserva.getProductoId(), reserva.getCantidad());
    }
}