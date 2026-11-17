package edu.dosw.styleradar.estadisticas.domain.exception;

public class InvalidTimeSeriesRequestException extends IllegalArgumentException {

    public InvalidTimeSeriesRequestException(String message) {
        super(message);
    }
}
