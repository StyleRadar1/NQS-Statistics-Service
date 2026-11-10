package edu.dosw.styleradar.estadisticas.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IndicadorResponseDto {
    private UUID id;
    private String nombre;
    private BigDecimal valor;
    private String unidad;
    private LocalDateTime fecha;
    private PeriodoResponseDto periodo;
    private ComparacionResponseDto comparacion;
}
