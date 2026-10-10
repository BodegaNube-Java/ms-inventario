package cl.duoc.ms_inventario.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ProductoUpdateRequest(
        @NotBlank String nombre,
        String descripcion,
        @NotNull @DecimalMin(value = "0.01") BigDecimal precio
) {}