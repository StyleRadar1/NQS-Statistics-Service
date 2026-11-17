package edu.dosw.styleradar.estadisticas.domain.port.out;

import edu.dosw.styleradar.estadisticas.domain.model.Grafica;
import edu.dosw.styleradar.estadisticas.domain.model.Punto;
import edu.dosw.styleradar.estadisticas.domain.model.Tablero;
import edu.dosw.styleradar.estadisticas.domain.model.TipoTablero;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConsultarTemporalidadPort {
    Optional<Grafica> findGraficaById(UUID id);
    Optional<Grafica> findGraficaByTableroIdAndTitulo(UUID tableroId, String titulo);
    Optional<Tablero> findTableroByTipoAndIdDueño(TipoTablero tipo, UUID idDueño);
    Grafica saveGrafica(Grafica grafica, UUID tableroId);
    List<Punto> findPuntosBySerieIdAndPeriodo(UUID serieId, LocalDate fechaInicio, LocalDate fechaFin);
}
