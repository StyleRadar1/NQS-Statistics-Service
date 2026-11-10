package edu.dosw.styleradar.estadisticas.infrastructure.adapter.in.rest;

import edu.dosw.styleradar.estadisticas.application.dto.response.TableroResponseDto;
import edu.dosw.styleradar.estadisticas.application.mapper.TableroDtoMapper;
import edu.dosw.styleradar.estadisticas.domain.model.Tablero;
import edu.dosw.styleradar.estadisticas.domain.model.TipoTablero;
import edu.dosw.styleradar.estadisticas.domain.port.in.ConsultarTableroUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import java.util.UUID;

@RestController
@RequestMapping("/estadisticas/tableros")
@RequiredArgsConstructor
@Tag(name = "Tableros de Estadísticas", description = "Endpoints para la consulta de tableros e indicadores de rendimiento")
public class TableroRestController {

    private final ConsultarTableroUseCase consultarTableroUseCase;
    private final TableroDtoMapper tableroDtoMapper;

    @GetMapping("/{tipo}/{idDueño}")
    @Operation(
            summary = "Consultar indicadores del tablero por tipo y dueño",
            description = "Permite consultar los indicadores analíticos clave (visitas a la página, mensajes de clientes, prendas en wishlist) " +
                    "para un periodo específico junto con su variación frente al periodo anterior. " +
                    "Si el almacén no cuenta con registros en el periodo, los valores se retornan en cero."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Tablero e indicadores obtenidos exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = TableroResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Periodo inválido o identificador erróneo",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Tablero no encontrado",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Error interno del servidor",
                    content = @Content(mediaType = "application/json")
            )
    })
    public ResponseEntity<TableroResponseDto> consultarTablero(
            @Parameter(description = "Tipo de tablero: ADMIN, TIENDA o FUNDACION", required = true, example = "TIENDA")
            @PathVariable("tipo") TipoTablero tipo,

            @Parameter(description = "Identificador UUID del dueño o local", required = true, example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
            @PathVariable("idDueño") UUID idDueño,

            @Parameter(description = "Fecha inicial del periodo de consulta (ISO: YYYY-MM-DD)", required = false, example = "2026-10-01")
            @RequestParam(value = "fechaInicio", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,

            @Parameter(description = "Fecha final del periodo de consulta (ISO: YYYY-MM-DD)", required = false, example = "2026-10-31")
            @RequestParam(value = "fechaFin", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin
    ) {
        Tablero tablero = consultarTableroUseCase.consultarTablero(tipo, idDueño, fechaInicio, fechaFin);
        TableroResponseDto responseDto = tableroDtoMapper.toDto(tablero);
        return ResponseEntity.ok(responseDto);
    }
}
