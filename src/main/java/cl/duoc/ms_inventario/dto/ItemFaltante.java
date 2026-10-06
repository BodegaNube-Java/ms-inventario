package cl.duoc.ms_inventario.dto;
import java.util.UUID;

/** Detalle de un producto sin stock suficiente, para la respuesta 409 */
public record ItemFaltante(
        UUID productoId,
        Integer solicitado,
        Integer disponible
) {
}