package edu.dosw.styleradar.estadisticas.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableroResponseDto {
    private UUID id;
    private UUID idDueño;
    private String tipo;

    @Builder.Default
    private List<IndicadorResponseDto> indicadores = new ArrayList<>();

    @Builder.Default
    private List<GraficaResponseDto> graficas = new ArrayList<>();
}
