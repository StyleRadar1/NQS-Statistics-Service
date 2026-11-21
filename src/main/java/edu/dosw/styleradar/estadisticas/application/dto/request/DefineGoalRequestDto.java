package edu.dosw.styleradar.estadisticas.application.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record DefineGoalRequestDto(
        @NotNull(message = "El valor de la meta es obligatorio")
        @DecimalMin(value = "0.01", message = "El valor de la meta debe ser mayor que cero")
        BigDecimal targetValue
) {
}
