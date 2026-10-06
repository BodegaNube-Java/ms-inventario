package cl.duoc.ms_inventario.exception;

import java.util.UUID;

public class ProductoNoEncontradoException extends RuntimeException {

    private ProductoNoEncontradoException(String mensaje) {
        super(mensaje);
    }

    public static ProductoNoEncontradoException porId(UUID productoId) {
        return new ProductoNoEncontradoException("Producto no encontrado: " + productoId);
    }

    public static ProductoNoEncontradoException porSku(String sku) {
        return new ProductoNoEncontradoException("Producto no encontrado con SKU: " + sku);
    }
}