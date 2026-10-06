package cl.duoc.ms_inventario.repository;

import cl.duoc.ms_inventario.model.EstadoReserva;
import cl.duoc.ms_inventario.model.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReservaRepository extends JpaRepository<Reserva, UUID> {

    /** Clave de la idempotencia: si ya existe, no se vuelve a reservar */
    Optional<Reserva> findByOrdenIdAndProductoId(UUID ordenId, UUID productoId);

    List<Reserva> findByOrdenId(UUID ordenId);

    List<Reserva> findByOrdenIdAndEstado(UUID ordenId, EstadoReserva estado);
}
