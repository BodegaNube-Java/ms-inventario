package cl.duoc.ms_inventario.service;
import cl.duoc.ms_inventario.dto.*;
import cl.duoc.ms_inventario.exception.ProductoNoEncontradoException;
import cl.duoc.ms_inventario.exception.StockInsuficienteException;
import cl.duoc.ms_inventario.model.EstadoReserva;
import cl.duoc.ms_inventario.model.Reserva;
import cl.duoc.ms_inventario.model.Stock;
import cl.duoc.ms_inventario.repository.ReservaRepository;
import cl.duoc.ms_inventario.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StockService {

    private static final Logger log = LoggerFactory.getLogger(StockService.class);

    private final StockRepository stockRepository;
    private final ReservaRepository reservaRepository;

    /**
     * POST /inventario/reservar — lo invoca ms-ordenes.
     *
     * Es IDEMPOTENTE: si la reserva ya existe para ese (ordenId, productoId),
     * devuelve la existente sin volver a descontar stock.
     *
     * @Transactional sin readOnly: el bloqueo pesimista lo exige.
     */
    @Transactional
    public ReservaResponse reservar(ReservarStockRequest request) {

        // 1. Idempotencia: si TODOS los items ya estan reservados, devolver lo existente
        List<Reserva> existentes = reservaRepository.findByOrdenId(request.ordenId());
        if (existentes.size() == request.items().size()) {
            log.info("Reserva ya existente para orden {} - respuesta idempotente", request.ordenId());
            return ReservaResponse.desde(request.ordenId(), existentes);
        }

        // 2. Verificar disponibilidad de TODO antes de descontar nada
        List<ItemFaltante> faltantes = new ArrayList<>();
        List<Stock> stocksBloqueados = new ArrayList<>();

        for (ItemReservaRequest item : request.items()) {
            Stock stock = stockRepository.findByProductoIdConBloqueo(item.productoId())
                    .orElseThrow(() -> ProductoNoEncontradoException.porId(item.productoId()));

            if (!stock.hayDisponible(item.cantidad())) {
                faltantes.add(new ItemFaltante(
                        item.productoId(),
                        item.cantidad(),
                        stock.getCantidadDisponible()
                ));
            }
            stocksBloqueados.add(stock);
        }

        // 3. Si falta algo, no se reserva NADA (todo o nada)
        if (!faltantes.isEmpty()) {
            throw new StockInsuficienteException(faltantes);
        }

        // 4. Reservar
        List<Reserva> reservas = new ArrayList<>();

        for (int i = 0; i < request.items().size(); i++) {
            ItemReservaRequest item = request.items().get(i);
            Stock stock = stocksBloqueados.get(i);

            // Puede existir ya este item puntual aunque no estuvieran todos
            Optional<Reserva> yaExiste = reservaRepository
                    .findByOrdenIdAndProductoId(request.ordenId(), item.productoId());

            if (yaExiste.isPresent()) {
                reservas.add(yaExiste.get());
                continue;
            }

            stock.reservar(item.cantidad());
            stockRepository.save(stock);

            Reserva reserva = Reserva.builder()
                    .ordenId(request.ordenId())
                    .productoId(item.productoId())
                    .cantidad(item.cantidad())
                    .estado(EstadoReserva.RESERVADA)
                    .build();

            reservas.add(reservaRepository.save(reserva));
        }

        log.info("Stock reservado para orden {}: {} item(s)", request.ordenId(), reservas.size());
        return ReservaResponse.desde(request.ordenId(), reservas);
    }

    /**
     * PATCH /inventario/{productoId}/descuento — lo invoca el OPERARIO al
     * confirmar el picking fisico.
     *
     * Distinto de reservar(): aqui el stock sale definitivamente del inventario.
     */
    @Transactional
    public void confirmarDescuento(UUID ordenId, UUID productoId) {
        Reserva reserva = reservaRepository
                .findByOrdenIdAndProductoId(ordenId, productoId)
                .orElseThrow(() -> ProductoNoEncontradoException.porId(productoId));

        if (reserva.getEstado() == EstadoReserva.CONFIRMADA) {
            log.info("Descuento ya confirmado para orden {} producto {}", ordenId, productoId);
            return;
        }

        Stock stock = stockRepository.findByProductoIdConBloqueo(productoId)
                .orElseThrow(() -> ProductoNoEncontradoException.porId(productoId));

        stock.confirmarDescuento(reserva.getCantidad());
        stockRepository.save(stock);

        reserva.setEstado(EstadoReserva.CONFIRMADA);
        reservaRepository.save(reserva);

        log.info("Descuento confirmado: orden {} producto {}", ordenId, productoId);
    }

    /** Libera una reserva (orden cancelada): el stock vuelve a estar disponible */
    @Transactional
    public void liberar(UUID ordenId) {
        List<Reserva> reservas = reservaRepository
                .findByOrdenIdAndEstado(ordenId, EstadoReserva.RESERVADA);

        for (Reserva reserva : reservas) {
            Stock stock = stockRepository.findByProductoIdConBloqueo(reserva.getProductoId())
                    .orElseThrow(() -> ProductoNoEncontradoException.porId(reserva.getProductoId()));

            stock.liberar(reserva.getCantidad());
            stockRepository.save(stock);

            reserva.setEstado(EstadoReserva.LIBERADA);
            reservaRepository.save(reserva);
        }

        log.info("Liberadas {} reserva(s) de la orden {}", reservas.size(), ordenId);
    }
}