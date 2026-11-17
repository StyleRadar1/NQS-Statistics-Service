package edu.dosw.styleradar.estadisticas.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Tablero {
    private UUID id;
    private UUID idDueño;
    private TipoTablero tipo;

    @Builder.Default
    private List<Indicador> indicadores = new ArrayList<>();

    @Builder.Default
    private List<Grafica> graficas = new ArrayList<>();

    public static Tablero crearTableroTiendaConCeros(UUID idDueño, Periodo periodo) {
        UUID tableroId = UUID.randomUUID();
        LocalDateTime ahora = LocalDateTime.now();
        Comparacion comparacionCero = new Comparacion(BigDecimal.ZERO, BigDecimal.ZERO);

        List<Indicador> indicadoresPorDefecto = List.of(
            Indicador.builder()
                .id(UUID.randomUUID())
                .nombre("Visitas a la página")
                .valor(BigDecimal.ZERO)
                .unidad(UnidadMedida.VISITAS)
                .fecha(ahora)
                .periodo(periodo)
                .comparacion(comparacionCero)
                .build(),
            Indicador.builder()
                .id(UUID.randomUUID())
                .nombre("Mensajes de clientes")
                .valor(BigDecimal.ZERO)
                .unidad(UnidadMedida.MENSAJES)
                .fecha(ahora)
                .periodo(periodo)
                .comparacion(comparacionCero)
                .build(),
            Indicador.builder()
                .id(UUID.randomUUID())
                .nombre("Prendas guardadas en wishlist")
                .valor(BigDecimal.ZERO)
                .unidad(UnidadMedida.PRENDAS)
                .fecha(ahora)
                .periodo(periodo)
                .comparacion(comparacionCero)
                .build()
        );

        return Tablero.builder()
            .id(tableroId)
            .idDueño(idDueño)
            .tipo(TipoTablero.TIENDA)
            .indicadores(new ArrayList<>(indicadoresPorDefecto))
            .graficas(new ArrayList<>())
            .build();
    }
}
