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
public class SerieResponseDto {
    private UUID id;
    private String nombre;
    private String tipo;

    @Builder.Default
    private List<PuntoResponseDto> puntos = new ArrayList<>();
}
