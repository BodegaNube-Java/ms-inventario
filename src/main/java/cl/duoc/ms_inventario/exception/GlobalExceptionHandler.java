package cl.duoc.ms_inventario.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 409 Conflict. Se loguea como INFO, no como ERROR: la falta de stock es
     * un resultado esperado del negocio, no una falla del sistema.
     */
    @ExceptionHandler(StockInsuficienteException.class)
    public ResponseEntity<Map<String, Object>> manejarStockInsuficiente(
            StockInsuficienteException ex) {

        log.info("Reserva rechazada por stock insuficiente: {}", ex.getFaltantes());

        Map<String, Object> cuerpo = new HashMap<>();
        cuerpo.put("timestamp", LocalDateTime.now());
        cuerpo.put("status", HttpStatus.CONFLICT.value());
        cuerpo.put("error", "Stock insuficiente");
        cuerpo.put("faltantes", ex.getFaltantes());

        return ResponseEntity.status(HttpStatus.CONFLICT).body(cuerpo);
    }

    @ExceptionHandler(ProductoNoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> manejarProductoNoEncontrado(
            ProductoNoEncontradoException ex) {

        log.warn(ex.getMessage());
        return construir(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> manejarValidacion(
            MethodArgumentNotValidException ex) {

        Map<String, String> errores = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> errores.put(error.getField(), error.getDefaultMessage()));

        Map<String, Object> cuerpo = new HashMap<>();
        cuerpo.put("timestamp", LocalDateTime.now());
        cuerpo.put("status", HttpStatus.BAD_REQUEST.value());
        cuerpo.put("error", "Datos invalidos");
        cuerpo.put("detalles", errores);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(cuerpo);
    }

    private ResponseEntity<Map<String, Object>> construir(HttpStatus status, String mensaje) {
        Map<String, Object> cuerpo = new HashMap<>();
        cuerpo.put("timestamp", LocalDateTime.now());
        cuerpo.put("status", status.value());
        cuerpo.put("error", mensaje);
        return ResponseEntity.status(status).body(cuerpo);
    }

    @ExceptionHandler(SkuDuplicadoException.class)
    public ResponseEntity<Map<String, Object>> handleSkuDuplicado(SkuDuplicadoException ex) {
        log.info(ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "error", ex.getMessage(),
                "timestamp", LocalDateTime.now().toString(),
                "status", 409));
    }
}