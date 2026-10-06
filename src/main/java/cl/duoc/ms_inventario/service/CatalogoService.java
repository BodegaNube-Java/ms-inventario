package cl.duoc.ms_inventario.service;

import cl.duoc.ms_inventario.dto.ProductoRequest;
import cl.duoc.ms_inventario.dto.ProductoResponse;
import cl.duoc.ms_inventario.exception.ProductoNoEncontradoException;
import cl.duoc.ms_inventario.model.Producto;
import cl.duoc.ms_inventario.model.Stock;
import cl.duoc.ms_inventario.repository.ProductoRepository;
import cl.duoc.ms_inventario.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Administracion del catalogo. Separado de StockService (SRP): el catalogo
 * cambia cuando se agregan productos; el stock cambia en cada venta.
 */
@Service
@RequiredArgsConstructor
public class CatalogoService {

    private final ProductoRepository productoRepository;
    private final StockRepository stockRepository;

    @Transactional(readOnly = true)
    public List<ProductoResponse> listarActivos() {
        return productoRepository.findByActivoTrue().stream()
                .map(producto -> ProductoResponse.desde(
                        producto,
                        stockRepository.findById(producto.getId()).orElse(null)
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductoResponse obtenerPorId(UUID productoId) {
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> ProductoNoEncontradoException.porId(productoId));

        return ProductoResponse.desde(
                producto,
                stockRepository.findById(productoId).orElse(null)
        );
    }

    /**
     * Crea el producto y su registro de stock en la misma transaccion:
     * un producto sin stock asociado dejaria el modelo inconsistente.
     */
    @Transactional
    public ProductoResponse crear(ProductoRequest request) {
        if (productoRepository.existsBySku(request.sku())) {
            throw new IllegalArgumentException("Ya existe un producto con SKU: " + request.sku());
        }

        Producto producto = Producto.builder()
                .sku(request.sku())
                .nombre(request.nombre())
                .descripcion(request.descripcion())
                .precio(request.precio())
                .activo(true)
                .build();

        producto = productoRepository.save(producto);

        Stock stock = Stock.builder()
                .producto(producto)
                .cantidadDisponible(request.cantidadInicial())
                .cantidadReservada(0)
                .build();

        stock = stockRepository.save(stock);

        return ProductoResponse.desde(producto, stock);
    }
}