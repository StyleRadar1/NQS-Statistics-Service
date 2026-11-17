package edu.dosw.styleradar.estadisticas.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PuntoResponseDto {
    private UUID id;
    private LocalDate fecha;
    private BigDecimal valor;
}
