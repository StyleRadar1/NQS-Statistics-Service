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

    public static final String TITULO_ACTIVIDAD_DIARIA = "Actividad diaria de la tienda";

    private UUID id;
    private String titulo;

    @Builder.Default
    private List<Serie> series = new ArrayList<>();

    public static Grafica crearActividadDiariaTiendaConCeros(UUID graficaId, Periodo periodo) {
        List<Serie> seriesPorDefecto = List.of(
                Serie.crearSerieDiariaConCeros(Serie.SERIE_VISITAS, periodo),
                Serie.crearSerieDiariaConCeros(Serie.SERIE_GUARDADOS, periodo),
                Serie.crearSerieDiariaConCeros(Serie.SERIE_MENSAJES, periodo)
        );

        return Grafica.builder()
                .id(graficaId != null ? graficaId : UUID.randomUUID())
                .titulo(TITULO_ACTIVIDAD_DIARIA)
                .series(new ArrayList<>(seriesPorDefecto))
                .build();
    }

    public Grafica asegurarSeriesActividadDiaria(Periodo periodo) {
        if (this.series == null) {
            this.series = new ArrayList<>();
        }

        List<String> nombresRequeridos = List.of(
                Serie.SERIE_VISITAS,
                Serie.SERIE_GUARDADOS,
                Serie.SERIE_MENSAJES
        );

        List<Serie> resultado = new ArrayList<>();
        for (String nombreRequerido : nombresRequeridos) {
            Serie encontrada = this.series.stream()
                    .filter(s -> esMismaSerie(s.getNombre(), nombreRequerido))
                    .findFirst()
                    .orElse(null);

            if (encontrada != null) {
                encontrada.rellenarDiasFaltantes(periodo);
                resultado.add(encontrada);
            } else {
                resultado.add(Serie.crearSerieDiariaConCeros(nombreRequerido, periodo));
            }
        }

        // Mantener cualquier otra serie adicional que pudiera tener la gráfica
        for (Serie otra : this.series) {
            boolean yaIncluida = resultado.stream()
                    .anyMatch(r -> r.getId() != null && r.getId().equals(otra.getId()));
            if (!yaIncluida) {
                otra.rellenarDiasFaltantes(periodo);
                resultado.add(otra);
            }
        }

        this.series = resultado;
        return this;
    }

    private static boolean esMismaSerie(String nombreActual, String nombreRequerido) {
        if (nombreActual == null) return false;
        String normal = nombreActual.toLowerCase();
        if (nombreRequerido.equalsIgnoreCase(Serie.SERIE_VISITAS)) {
            return normal.contains("visita");
        }
        if (nombreRequerido.equalsIgnoreCase(Serie.SERIE_GUARDADOS)) {
            return normal.contains("guarda") || normal.contains("wishlist");
        }
        if (nombreRequerido.equalsIgnoreCase(Serie.SERIE_MENSAJES)) {
            return normal.contains("mensaje");
        }
        return normal.equalsIgnoreCase(nombreRequerido);
    }
}
