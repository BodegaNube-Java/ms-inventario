package cl.duoc.ms_inventario.repository;

import cl.duoc.ms_inventario.model.Stock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface StockRepository extends JpaRepository<Stock, UUID> {

    /**
     * Lectura con bloqueo pesimista para la operacion de reserva.
     *
     * PESSIMISTIC_WRITE hace un SELECT ... FOR UPDATE: bloquea la fila hasta
     * que termine la transaccion. Si dos Lambdas intentan reservar el mismo
     * producto a la vez, la segunda ESPERA en vez de fallar.
     *
     * Se usa solo aqui porque es el punto de alta contencion. Para lecturas
     * normales usar findById().
     */

    //bloquea esta fila hasta que pueda terminar
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Stock s WHERE s.productoId = :productoId")
    Optional<Stock> findByProductoIdConBloqueo(@Param("productoId") UUID productoId);
}