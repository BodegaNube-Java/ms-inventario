package cl.duoc.ms_inventario.exception;

import cl.duoc.ms_inventario.dto.ItemFaltante;
import lombok.Getter;

import java.util.List;

/**
 * NO es un fallo tecnico: es una regla de negocio.
 *
 * Se traduce a 409 Conflict. Ordenes lo captura, crea la orden en estado
 * PENDIENTE_STOCK y responde 201 a Lambda, de modo que el mensaje SQS se da
 * por procesado y NUNCA llega a la DLQ.
 */
@Getter
public class StockInsuficienteException extends RuntimeException {

    private final List<ItemFaltante> faltantes;

    public StockInsuficienteException(List<ItemFaltante> faltantes) {
        super("Stock insuficiente para " + faltantes.size() + " producto(s)");
        this.faltantes = faltantes;
    }
}
