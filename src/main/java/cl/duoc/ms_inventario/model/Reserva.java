package cl.duoc.ms_inventario.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * La restriccion UNIQUE (orden_id, producto_id) es lo que hace idempotente
 * la reserva: si Ordenes reintenta la misma llamada, el insert choca y se
 * responde 200 con la reserva existente, en vez de descontar stock dos veces.
 */
@Entity
@Table(
    name = "reservas",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_reserva_orden_producto",
        columnNames = {"orden_id", "producto_id"}
    )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * Id de la orden en ms-ordenes. Sin FK: vive en otra base de datos
     * (Database per Service).
     */
    @Column(name = "orden_id", nullable = false)
    private UUID ordenId;

    @Column(name = "producto_id", nullable = false)
    private UUID productoId;

    @Column(name = "cantidad", nullable = false)
    private Integer cantidad;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", length = 20, nullable = false)
    private EstadoReserva estado;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private LocalDateTime actualizadoEn;

    @PrePersist
    protected void onCreate() {
        LocalDateTime ahora = LocalDateTime.now();
        this.creadoEn = ahora;
        this.actualizadoEn = ahora;
        if (this.estado == null) {
            this.estado = EstadoReserva.RESERVADA;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.actualizadoEn = LocalDateTime.now();
    }
}
