package edu.dosw.styleradar.estadisticas.domain.port.in;

import edu.dosw.styleradar.estadisticas.domain.model.Tablero;
import edu.dosw.styleradar.estadisticas.domain.model.TipoTablero;

import java.time.LocalDate;
import java.util.UUID;

public interface ConsultarTableroUseCase {
    Tablero consultarTablero(TipoTablero tipo, UUID idDueño, LocalDate fechaInicio, LocalDate fechaFin);
}
