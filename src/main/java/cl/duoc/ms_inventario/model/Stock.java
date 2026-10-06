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
 * Relacion 1:1 con Producto. Separada a proposito: el catalogo cambia poco,
 * el stock cambia en cada venta.
 */
@Entity
@Table(name = "stock")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Stock {

    @Id
    @Column(name = "producto_id", updatable = false, nullable = false)
    private UUID productoId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "producto_id")
    private Producto producto;

    /** Lo que se puede vender ahora mismo */
    @Column(name = "cantidad_disponible", nullable = false)
    private Integer cantidadDisponible;

    /** Comprometido por una orden, aun no despachado */
    @Column(name = "cantidad_reservada", nullable = false)
    private Integer cantidadReservada;

    /**
     * Bloqueo optimista. Lambda escala en paralelo: sin esto, dos reservas
     * simultaneas del mismo producto venderian stock inexistente.
     */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "actualizado_en", nullable = false)
    private LocalDateTime actualizadoEn;

    @PrePersist
    @PreUpdate
    protected void onSave() {
        this.actualizadoEn = LocalDateTime.now();
        if (this.cantidadReservada == null) {
            this.cantidadReservada = 0;
        }
    }

    public boolean hayDisponible(int cantidad) {
        return this.cantidadDisponible >= cantidad;
    }

    public void reservar(int cantidad) {
        this.cantidadDisponible -= cantidad;
        this.cantidadReservada += cantidad;
    }

    public void confirmarDescuento(int cantidad) {
        this.cantidadReservada -= cantidad;
    }

    public void liberar(int cantidad) {
        this.cantidadReservada -= cantidad;
        this.cantidadDisponible += cantidad;
    }
}