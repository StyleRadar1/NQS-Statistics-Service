package edu.dosw.styleradar.estadisticas.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Indicador {
    private UUID id;
    private String nombre;
    private BigDecimal valor;
    private UnidadMedida unidad;
    private LocalDateTime fecha;
    private Periodo periodo;
    private Comparacion comparacion;
}
