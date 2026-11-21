package edu.dosw.styleradar.estadisticas.infrastructure.messaging.amqp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AlertaCreadaEventPayload(
        String eventoId,
        Long alertaId,
        Long usuarioId,
        String tipoAlerta,
        String itemId,
        String busquedaGuardadaId,
        String canal,
        LocalDateTime fechaCreacion
) {
}
