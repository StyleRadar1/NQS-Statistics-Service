package edu.dosw.styleradar.estadisticas.application.usecase;

import edu.dosw.styleradar.estadisticas.domain.exception.InvalidPeriodoException;
import edu.dosw.styleradar.estadisticas.domain.model.Comparacion;
import edu.dosw.styleradar.estadisticas.domain.model.Indicador;
import edu.dosw.styleradar.estadisticas.domain.model.Periodo;
import edu.dosw.styleradar.estadisticas.domain.model.Tablero;
import edu.dosw.styleradar.estadisticas.domain.model.TipoTablero;
import edu.dosw.styleradar.estadisticas.domain.model.UnidadMedida;
import edu.dosw.styleradar.estadisticas.domain.port.in.ConsultarTableroUseCase;
import edu.dosw.styleradar.estadisticas.domain.port.out.TableroRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConsultarTableroUseCaseImpl implements ConsultarTableroUseCase {

    public static final String INDICADOR_VISITAS = "Visitas a la página";
    public static final String INDICADOR_MENSAJES = "Mensajes de clientes";
    public static final String INDICADOR_WISHLIST = "Prendas guardadas en wishlist";

    private final TableroRepositoryPort tableroRepositoryPort;

    @Override
    public Tablero consultarTablero(TipoTablero tipo, UUID idDueño, LocalDate fechaInicio, LocalDate fechaFin) {
        if (tipo == null || idDueño == null) {
            throw new IllegalArgumentException("El tipo de tablero y el id del dueño son requeridos");
        }

        Periodo periodo = resolverPeriodo(fechaInicio, fechaFin);
        Periodo periodoAnterior = periodo.calcularPeriodoAnterior();

        Tablero tablero = tableroRepositoryPort.findByTipoAndIdDueño(tipo, idDueño)
                .orElseGet(() -> {
                    if (tipo == TipoTablero.TIENDA) {
                        Tablero nuevoTablero = Tablero.crearTableroTiendaConCeros(idDueño, periodo);
                        return tableroRepositoryPort.save(nuevoTablero);
                    }
                    Tablero nuevoTablero = Tablero.builder()
                            .id(UUID.randomUUID())
                            .idDueño(idDueño)
                            .tipo(tipo)
                            .indicadores(new ArrayList<>())
                            .graficas(new ArrayList<>())
                            .build();
                    return tableroRepositoryPort.save(nuevoTablero);
                });

        if (tipo == TipoTablero.TIENDA) {
            List<Indicador> indicadoresActuales = tableroRepositoryPort.findIndicadoresByTableroIdAndPeriodo(
                    tablero.getId(), periodo.getInicio(), periodo.getFin()
            );

            List<Indicador> indicadoresAnteriores = tableroRepositoryPort.findIndicadoresByTableroIdAndPeriodo(
                    tablero.getId(), periodoAnterior.getInicio(), periodoAnterior.getFin()
            );

            List<Indicador> indicadoresTienda = consolidarIndicadoresTienda(
                    indicadoresActuales,
                    indicadoresAnteriores,
                    periodo
            );
            tablero.setIndicadores(indicadoresTienda);
        }

        return tablero;
    }

    private Periodo resolverPeriodo(LocalDate fechaInicio, LocalDate fechaFin) {
        if (fechaInicio == null && fechaFin == null) {
            LocalDate hoy = LocalDate.now();
            return new Periodo(hoy.withDayOfMonth(1), hoy);
        }
        if (fechaInicio == null || fechaFin == null) {
            throw new InvalidPeriodoException("Debe especificar tanto la fecha de inicio como la fecha de fin");
        }
        return new Periodo(fechaInicio, fechaFin);
    }

    private List<Indicador> consolidarIndicadoresTienda(
            List<Indicador> actuales,
            List<Indicador> anteriores,
            Periodo periodo
    ) {
        Map<String, Indicador> actualMap = actuales.stream()
                .collect(Collectors.toMap(Indicador::getNombre, i -> i, (i1, i2) -> i1));

        Map<String, Indicador> anteriorMap = anteriores.stream()
                .collect(Collectors.toMap(Indicador::getNombre, i -> i, (i1, i2) -> i1));

        List<IndicadorConfig> configs = List.of(
                new IndicadorConfig(INDICADOR_VISITAS, UnidadMedida.VISITAS),
                new IndicadorConfig(INDICADOR_MENSAJES, UnidadMedida.MENSAJES),
                new IndicadorConfig(INDICADOR_WISHLIST, UnidadMedida.PRENDAS)
        );

        LocalDateTime ahora = LocalDateTime.now();
        List<Indicador> resultado = new ArrayList<>();

        for (IndicadorConfig config : configs) {
            Indicador actual = actualMap.get(config.nombre());
            Indicador anterior = anteriorMap.get(config.nombre());

            BigDecimal valorActual = (actual != null && actual.getValor() != null)
                    ? actual.getValor()
                    : BigDecimal.ZERO;

            BigDecimal valorAnterior = (anterior != null && anterior.getValor() != null)
                    ? anterior.getValor()
                    : BigDecimal.ZERO;

            Comparacion comparacion = Comparacion.calcular(valorActual, valorAnterior);

            Indicador consolidado = Indicador.builder()
                    .id(actual != null ? actual.getId() : UUID.randomUUID())
                    .nombre(config.nombre())
                    .valor(valorActual)
                    .unidad(config.unidad())
                    .fecha(actual != null && actual.getFecha() != null ? actual.getFecha() : ahora)
                    .periodo(periodo)
                    .comparacion(comparacion)
                    .build();

            resultado.add(consolidado);
        }

        return resultado;
    }

    private record IndicadorConfig(String nombre, UnidadMedida unidad) {}
}
