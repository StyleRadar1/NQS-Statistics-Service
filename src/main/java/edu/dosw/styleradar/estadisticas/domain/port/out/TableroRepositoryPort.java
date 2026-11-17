package edu.dosw.styleradar.estadisticas.domain.port.out;

import edu.dosw.styleradar.estadisticas.domain.model.Indicador;
import edu.dosw.styleradar.estadisticas.domain.model.Tablero;
import edu.dosw.styleradar.estadisticas.domain.model.TipoTablero;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TableroRepositoryPort {
    Optional<Tablero> findByTipoAndIdDueño(TipoTablero tipo, UUID idDueño);
    Tablero save(Tablero tablero);
    List<Indicador> findIndicadoresByTableroIdAndPeriodo(UUID tableroId, LocalDate fechaInicio, LocalDate fechaFin);
}
