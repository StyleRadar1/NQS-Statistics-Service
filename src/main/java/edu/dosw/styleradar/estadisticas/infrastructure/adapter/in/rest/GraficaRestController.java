package edu.dosw.styleradar.estadisticas.infrastructure.adapter.in.rest;

import edu.dosw.styleradar.estadisticas.application.dto.response.GraficaResponseDto;
import edu.dosw.styleradar.estadisticas.application.dto.response.SerieResponseDto;
import edu.dosw.styleradar.estadisticas.application.mapper.TableroDtoMapper;
import edu.dosw.styleradar.estadisticas.domain.model.Grafica;
import edu.dosw.styleradar.estadisticas.domain.model.Serie;
import edu.dosw.styleradar.estadisticas.domain.model.TipoSerie;
import edu.dosw.styleradar.estadisticas.domain.port.in.ConsultarGraficaUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/estadisticas/graficas")
@RequiredArgsConstructor
@Tag(name = "Gráficas de Estadísticas", description = "Endpoints para la consulta de gráficas temporales y series de actividad diaria")
public class GraficaRestController {

    private final ConsultarGraficaUseCase consultarGraficaUseCase;
    private final TableroDtoMapper tableroDtoMapper;

    @GetMapping("/{id}/series")
    @Operation(
            summary = "Consultar series de una gráfica o actividad diaria de la tienda (HU-05)",
            description = "Permite a un administrador de almacén consultar la actividad diaria de visitas, prendas guardadas y mensajes en un intervalo de días. " +
                    "Si para un día específico no hubo actividad, dicho día se muestra con valor cero sin omitirse."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Series de actividad diaria obtenidas exitosamente",
                    content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = SerieResponseDto.class)))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Periodo inválido o identificador erróneo",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Gráfica o tienda no encontrada",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Error interno del servidor",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<List<SerieResponseDto>> consultarSeries(
            @Parameter(description = "Identificador UUID de la gráfica o de la tienda (dueño)", required = true, example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
            @PathVariable("id") UUID id,

            @Parameter(description = "Tipo de serie: DIARIA, SEMANAL, MENSUAL", example = "DIARIA")
            @RequestParam(value = "tipo", required = false, defaultValue = "DIARIA") TipoSerie tipo,

            @Parameter(description = "Fecha inicial del intervalo (ISO: YYYY-MM-DD)", example = "2026-11-01")
            @RequestParam(value = "fechaInicio", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,

            @Parameter(description = "Fecha final del intervalo (ISO: YYYY-MM-DD)", example = "2026-11-07")
            @RequestParam(value = "fechaFin", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin
    ) {
        List<Serie> series = consultarGraficaUseCase.consultarSeries(id, tipo, fechaInicio, fechaFin);
        List<SerieResponseDto> responseDtos = tableroDtoMapper.toSerieDtoList(series);
        return ResponseEntity.ok(responseDtos);
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Consultar gráfica completa con sus series temporales",
            description = "Retorna la gráfica completa identificada por su ID o por el ID del dueño con todas sus series de puntos en el periodo."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Gráfica obtenida exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = GraficaResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Parámetros de consulta inválidos",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Gráfica no encontrada",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<GraficaResponseDto> consultarGrafica(
            @Parameter(description = "Identificador UUID de la gráfica o de la tienda", required = true)
            @PathVariable("id") UUID id,

            @Parameter(description = "Tipo de serie: DIARIA, SEMANAL, MENSUAL", example = "DIARIA")
            @RequestParam(value = "tipo", required = false, defaultValue = "DIARIA") TipoSerie tipo,

            @Parameter(description = "Fecha inicial del intervalo (ISO: YYYY-MM-DD)")
            @RequestParam(value = "fechaInicio", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,

            @Parameter(description = "Fecha final del intervalo (ISO: YYYY-MM-DD)")
            @RequestParam(value = "fechaFin", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin
    ) {
        Grafica grafica = consultarGraficaUseCase.consultarGrafica(id, tipo, fechaInicio, fechaFin);
        GraficaResponseDto responseDto = tableroDtoMapper.toDto(grafica);
        return ResponseEntity.ok(responseDto);
    }

    @GetMapping("/tiendas/{idTienda}/actividad-diaria")
    @Operation(
            summary = "Consultar actividad diaria de la tienda (HU-05)",
            description = "Endpoint especializado para administradores de almacén que presenta la gráfica de actividad diaria (visitas, guardados y mensajes) para un intervalo de días, con ceros en días sin actividad."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Gráfica de actividad diaria obtenida exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = GraficaResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Intervalo de fechas inválido",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<GraficaResponseDto> consultarActividadDiariaTienda(
            @Parameter(description = "Identificador UUID de la tienda (dueño)", required = true)
            @PathVariable("idTienda") UUID idTienda,

            @Parameter(description = "Fecha inicial del intervalo (ISO: YYYY-MM-DD)")
            @RequestParam(value = "fechaInicio", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,

            @Parameter(description = "Fecha final del intervalo (ISO: YYYY-MM-DD)")
            @RequestParam(value = "fechaFin", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin
    ) {
        Grafica grafica = consultarGraficaUseCase.consultarActividadDiariaTienda(idTienda, fechaInicio, fechaFin);
        GraficaResponseDto responseDto = tableroDtoMapper.toDto(grafica);
        return ResponseEntity.ok(responseDto);
    }

    @GetMapping("/tiendas/{idTienda}/actividad-diaria/series")
    @Operation(
            summary = "Consultar serie diaria de actividad de la tienda (HU-05)",
            description = "Retorna directamente la lista de series de actividad diaria (visitas, guardados y mensajes) con valores cero en días sin actividad."
    )
    public ResponseEntity<List<SerieResponseDto>> consultarSeriesActividadDiariaTienda(
            @Parameter(description = "Identificador UUID de la tienda (dueño)", required = true)
            @PathVariable("idTienda") UUID idTienda,

            @Parameter(description = "Fecha inicial del intervalo (ISO: YYYY-MM-DD)")
            @RequestParam(value = "fechaInicio", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,

            @Parameter(description = "Fecha final del intervalo (ISO: YYYY-MM-DD)")
            @RequestParam(value = "fechaFin", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin
    ) {
        Grafica grafica = consultarGraficaUseCase.consultarActividadDiariaTienda(idTienda, fechaInicio, fechaFin);
        List<SerieResponseDto> responseDtos = tableroDtoMapper.toSerieDtoList(grafica.getSeries());
        return ResponseEntity.ok(responseDtos);
    }
}
