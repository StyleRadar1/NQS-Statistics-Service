package edu.dosw.styleradar.estadisticas.domain.model;

public enum ModerationDecision {
    APROBAR("APROBADA"),
    RECHAZAR("RECHAZADA");

    private final String resultingState;

    ModerationDecision(String resultingState) {
        this.resultingState = resultingState;
    }

    public String resultingState() {
        return resultingState;
    }
}
