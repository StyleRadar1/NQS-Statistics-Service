package edu.dosw.styleradar.estadisticas.application.usecase;

import edu.dosw.styleradar.estadisticas.domain.exception.GraficaNotFoundException;
import edu.dosw.styleradar.estadisticas.domain.exception.InvalidPeriodoException;
import edu.dosw.styleradar.estadisticas.domain.model.Grafica;
import edu.dosw.styleradar.estadisticas.domain.model.Periodo;
import edu.dosw.styleradar.estadisticas.domain.model.Punto;
import edu.dosw.styleradar.estadisticas.domain.model.Serie;
import edu.dosw.styleradar.estadisticas.domain.model.Tablero;
import edu.dosw.styleradar.estadisticas.domain.model.TipoSerie;
import edu.dosw.styleradar.estadisticas.domain.model.TipoTablero;
import edu.dosw.styleradar.estadisticas.domain.port.in.ConsultarGraficaUseCase;
import edu.dosw.styleradar.estadisticas.domain.port.out.ConsultarTemporalidadPort;
import edu.dosw.styleradar.estadisticas.domain.port.out.TableroRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConsultarGraficaUseCaseImpl implements ConsultarGraficaUseCase {

    private final ConsultarTemporalidadPort consultarTemporalidadPort;
    private final TableroRepositoryPort tableroRepositoryPort;

    @Override
    public Grafica consultarGrafica(UUID id, TipoSerie tipo, LocalDate fechaInicio, LocalDate fechaFin) {
        if (id == null) {
            throw new IllegalArgumentException("El identificador es requerido");
        }

        Periodo periodo = resolverPeriodo(fechaInicio, fechaFin);
        TipoSerie tipoSerie = (tipo != null) ? tipo : TipoSerie.DIARIA;

        Optional<Grafica> graficaOpt = consultarTemporalidadPort.findGraficaById(id);
        if (graficaOpt.isPresent()) {
            Grafica grafica = graficaOpt.get();
            cargarPuntosPeriodo(grafica, periodo);

            if (tipoSerie == TipoSerie.DIARIA) {
                if (grafica.getTitulo() != null && grafica.getTitulo().toLowerCase().contains("actividad")) {
                    grafica.asegurarSeriesActividadDiaria(periodo);
                } else {
                    for (Serie serie : grafica.getSeries()) {
                        serie.rellenarDiasFaltantes(periodo);
                    }
                }
            }
            return grafica;
        }

        Optional<Tablero> tableroOpt = consultarTemporalidadPort.findTableroByTipoAndIdDueño(TipoTablero.TIENDA, id);
        if (tableroOpt.isPresent()) {
            return consultarActividadDiariaTienda(id, fechaInicio, fechaFin);
        }

        throw new GraficaNotFoundException("Gráfica no encontrada para el identificador: " + id);
    }

    @Override
    public List<Serie> consultarSeries(UUID id, TipoSerie tipo, LocalDate fechaInicio, LocalDate fechaFin) {
        Grafica grafica = consultarGrafica(id, tipo, fechaInicio, fechaFin);
        if (tipo != null && grafica.getSeries() != null) {
            List<Serie> filtradas = grafica.getSeries().stream()
                    .filter(s -> s.getTipo() == tipo)
                    .toList();
            if (!filtradas.isEmpty()) {
                return filtradas;
            }
        }
        return grafica.getSeries() != null ? grafica.getSeries() : new ArrayList<>();
    }

    @Override
    public Grafica consultarActividadDiariaTienda(UUID idDueño, LocalDate fechaInicio, LocalDate fechaFin) {
        if (idDueño == null) {
            throw new IllegalArgumentException("El identificador del dueño o tienda es requerido");
        }

        Periodo periodo = resolverPeriodo(fechaInicio, fechaFin);

        Tablero tablero = tableroRepositoryPort.findByTipoAndIdDueño(TipoTablero.TIENDA, idDueño)
                .orElseGet(() -> {
                    Tablero nuevoTablero = Tablero.crearTableroTiendaConCeros(idDueño, periodo);
                    return tableroRepositoryPort.save(nuevoTablero);
                });

        Optional<Grafica> graficaOpt = consultarTemporalidadPort.findGraficaByTableroIdAndTitulo(
                tablero.getId(), Grafica.TITULO_ACTIVIDAD_DIARIA
        );

        Grafica grafica;
        if (graficaOpt.isPresent()) {
            grafica = graficaOpt.get();
            cargarPuntosPeriodo(grafica, periodo);
            grafica.asegurarSeriesActividadDiaria(periodo);
        } else {
            grafica = Grafica.crearActividadDiariaTiendaConCeros(UUID.randomUUID(), periodo);
            grafica = consultarTemporalidadPort.saveGrafica(grafica, tablero.getId());
        }

        return grafica;
    }

    private void cargarPuntosPeriodo(Grafica grafica, Periodo periodo) {
        if (grafica.getSeries() == null) {
            return;
        }
        for (Serie serie : grafica.getSeries()) {
            if (serie.getId() != null) {
                List<Punto> puntosPeriodo = consultarTemporalidadPort.findPuntosBySerieIdAndPeriodo(
                        serie.getId(), periodo.getInicio(), periodo.getFin()
                );
                serie.setPuntos(new ArrayList<>(puntosPeriodo));
            }
        }
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
}
