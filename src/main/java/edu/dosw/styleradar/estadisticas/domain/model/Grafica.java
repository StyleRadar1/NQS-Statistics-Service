package edu.dosw.styleradar.estadisticas.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Grafica {
    private UUID id;
    private String titulo;

    @Builder.Default
    private List<Serie> series = new ArrayList<>();
}
