package edu.dosw.styleradar.estadisticas.application.mapper;

import edu.dosw.styleradar.estadisticas.application.dto.response.ComparacionResponseDto;
import edu.dosw.styleradar.estadisticas.application.dto.response.GraficaResponseDto;
import edu.dosw.styleradar.estadisticas.application.dto.response.IndicadorResponseDto;
import edu.dosw.styleradar.estadisticas.application.dto.response.PeriodoResponseDto;
import edu.dosw.styleradar.estadisticas.application.dto.response.PuntoResponseDto;
import edu.dosw.styleradar.estadisticas.application.dto.response.SerieResponseDto;
import edu.dosw.styleradar.estadisticas.application.dto.response.TableroResponseDto;
import edu.dosw.styleradar.estadisticas.domain.model.Comparacion;
import edu.dosw.styleradar.estadisticas.domain.model.Grafica;
import edu.dosw.styleradar.estadisticas.domain.model.Indicador;
import edu.dosw.styleradar.estadisticas.domain.model.Periodo;
import edu.dosw.styleradar.estadisticas.domain.model.Punto;
import edu.dosw.styleradar.estadisticas.domain.model.Serie;
import edu.dosw.styleradar.estadisticas.domain.model.Tablero;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class TableroDtoMapper {

    public TableroResponseDto toDto(Tablero domain) {
        if (domain == null) {
            return null;
        }

        return TableroResponseDto.builder()
                .id(domain.getId())
                .idDueño(domain.getIdDueño())
                .tipo(domain.getTipo() != null ? domain.getTipo().name() : null)
                .indicadores(toIndicadorDtoList(domain.getIndicadores()))
                .graficas(toGraficaDtoList(domain.getGraficas()))
                .build();
    }

    public List<IndicadorResponseDto> toIndicadorDtoList(List<Indicador> indicadores) {
        if (indicadores == null) {
            return Collections.emptyList();
        }
        return indicadores.stream().map(this::toDto).toList();
    }

    public IndicadorResponseDto toDto(Indicador domain) {
        if (domain == null) {
            return null;
        }
        return IndicadorResponseDto.builder()
                .id(domain.getId())
                .nombre(domain.getNombre())
                .valor(domain.getValor())
                .unidad(domain.getUnidad() != null ? domain.getUnidad().name() : null)
                .fecha(domain.getFecha())
                .periodo(toDto(domain.getPeriodo()))
                .comparacion(toDto(domain.getComparacion()))
                .build();
    }

    public PeriodoResponseDto toDto(Periodo domain) {
        if (domain == null) {
            return null;
        }
        return PeriodoResponseDto.builder()
                .inicio(domain.getInicio())
                .fin(domain.getFin())
                .build();
    }

    public ComparacionResponseDto toDto(Comparacion domain) {
        if (domain == null) {
            return null;
        }
        return ComparacionResponseDto.builder()
                .valorAnterior(domain.getValorAnterior())
                .variacionPorcentual(domain.getVariacionPorcentual())
                .build();
    }

    public List<GraficaResponseDto> toGraficaDtoList(List<Grafica> graficas) {
        if (graficas == null) {
            return Collections.emptyList();
        }
        return graficas.stream().map(this::toDto).toList();
    }

    public GraficaResponseDto toDto(Grafica domain) {
        if (domain == null) {
            return null;
        }
        return GraficaResponseDto.builder()
                .id(domain.getId())
                .titulo(domain.getTitulo())
                .series(domain.getSeries() != null
                        ? domain.getSeries().stream().map(this::toDto).toList()
                        : Collections.emptyList())
                .build();
    }

    public List<SerieResponseDto> toSerieDtoList(List<Serie> series) {
        if (series == null) {
            return Collections.emptyList();
        }
        return series.stream().map(this::toDto).toList();
    }

    public SerieResponseDto toDto(Serie domain) {
        if (domain == null) {
            return null;
        }
        return SerieResponseDto.builder()
                .id(domain.getId())
                .nombre(domain.getNombre())
                .tipo(domain.getTipo() != null ? domain.getTipo().name() : null)
                .puntos(domain.getPuntos() != null
                        ? domain.getPuntos().stream().map(this::toDto).toList()
                        : Collections.emptyList())
                .build();
    }

    public PuntoResponseDto toDto(Punto domain) {
        if (domain == null) {
            return null;
        }
        return PuntoResponseDto.builder()
                .id(domain.getId())
                .fecha(domain.getFecha())
                .valor(domain.getValor())
                .build();
    }
}
