package edu.dosw.styleradar.estadisticas.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComparacionResponseDto {
    private BigDecimal valorAnterior;
    private BigDecimal variacionPorcentual;
}
