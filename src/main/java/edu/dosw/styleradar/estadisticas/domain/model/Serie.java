package edu.dosw.styleradar.estadisticas.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Serie {

    public static final String SERIE_VISITAS = "Visitas";
    public static final String SERIE_GUARDADOS = "Prendas guardadas";
    public static final String SERIE_MENSAJES = "Mensajes";

    private UUID id;
    private String nombre;
    private TipoSerie tipo;

    @Builder.Default
    private List<Punto> puntos = new ArrayList<>();

    public Serie rellenarDiasFaltantes(Periodo periodo) {
        if (periodo == null) {
            return this;
        }

        Map<LocalDate, Punto> puntosPorFecha = (this.puntos != null ? this.puntos : Collections.<Punto>emptyList())
                .stream()
                .filter(p -> p != null && p.getFecha() != null)
                .collect(Collectors.toMap(Punto::getFecha, p -> p, (p1, p2) -> p1));

        List<Punto> puntosCompletos = periodo.generarDias().stream()
                .map(fecha -> {
                    Punto existente = puntosPorFecha.get(fecha);
                    if (existente != null && existente.getValor() != null) {
                        return existente;
                    }
                    return Punto.builder()
                            .id(existente != null && existente.getId() != null ? existente.getId() : UUID.randomUUID())
                            .fecha(fecha)
                            .valor(BigDecimal.ZERO)
                            .build();
                })
                .sorted(Comparator.comparing(Punto::getFecha))
                .collect(Collectors.toCollection(ArrayList::new));

        this.puntos = puntosCompletos;
        return this;
    }

    public static Serie crearSerieDiariaConCeros(String nombre, Periodo periodo) {
        Serie serie = Serie.builder()
                .id(UUID.randomUUID())
                .nombre(nombre)
                .tipo(TipoSerie.DIARIA)
                .puntos(new ArrayList<>())
                .build();
        return serie.rellenarDiasFaltantes(periodo);
    }
}
