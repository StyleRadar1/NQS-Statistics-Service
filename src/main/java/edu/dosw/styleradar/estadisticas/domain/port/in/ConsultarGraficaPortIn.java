package edu.dosw.styleradar.estadisticas.domain.port.in;

import edu.dosw.styleradar.estadisticas.domain.model.Grafica;
import edu.dosw.styleradar.estadisticas.domain.model.Serie;
import edu.dosw.styleradar.estadisticas.domain.model.TipoSerie;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ConsultarGraficaPortIn {
    Grafica consultarGrafica(UUID id, TipoSerie tipo, LocalDate fechaInicio, LocalDate fechaFin);
    List<Serie> consultarSeries(UUID id, TipoSerie tipo, LocalDate fechaInicio, LocalDate fechaFin);
    Grafica consultarActividadDiariaTienda(UUID idDueño, LocalDate fechaInicio, LocalDate fechaFin);
}
