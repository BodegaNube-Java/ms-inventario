package cl.duoc.ms_inventario.dto;

import cl.duoc.ms_inventario.model.EstadoReserva;
import cl.duoc.ms_inventario.model.Reserva;

import java.util.List;
import java.util.UUID;

/** Respuesta 200 OK de POST /inventario/reservar */
public record ReservaResponse(
        UUID ordenId,
        EstadoReserva estado,
        List<ItemReservaResponse> items
) {
    public static ReservaResponse desde(UUID ordenId, List<Reserva> reservas) {
        List<ItemReservaResponse> items = reservas.stream()
                .map(ItemReservaResponse::desde)
                .toList();

        EstadoReserva estado = reservas.isEmpty()
                ? EstadoReserva.RESERVADA
                : reservas.get(0).getEstado();

        return new ReservaResponse(ordenId, estado, items);
    }
}