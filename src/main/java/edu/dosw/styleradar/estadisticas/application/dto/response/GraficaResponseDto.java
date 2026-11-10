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
public class GraficaResponseDto {
    private UUID id;
    private String titulo;

    @Builder.Default
    private List<SerieResponseDto> series = new ArrayList<>();
}
