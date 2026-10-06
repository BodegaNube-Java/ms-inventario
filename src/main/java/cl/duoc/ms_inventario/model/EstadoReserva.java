package cl.duoc.ms_inventario.model;

public enum EstadoReserva {
    /** Stock comprometido, aun no despachado */
    RESERVADA,
    /** El operario confirmo el picking y se descuento del stock real */
    CONFIRMADA,
    /** La reserva se libero (orden cancelada o expirada) */
    LIBERADA
}