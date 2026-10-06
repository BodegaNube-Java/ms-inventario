package cl.duoc.ms_inventario.dto;

import cl.duoc.ms_inventario.model.Producto;
import cl.duoc.ms_inventario.model.Stock;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductoResponse(
        UUID id,
        String sku,
        String nombre,
        String descripcion,
        BigDecimal precio,
        Integer cantidadDisponible,
        Integer cantidadReservada,
        Boolean activo
) {
    public static ProductoResponse desde(Producto producto, Stock stock) {
        return new ProductoResponse(
                producto.getId(),
                producto.getSku(),
                producto.getNombre(),
                producto.getDescripcion(),
                producto.getPrecio(),
                stock != null ? stock.getCantidadDisponible() : 0,
                stock != null ? stock.getCantidadReservada() : 0,
                producto.getActivo()
        );
    }
}
