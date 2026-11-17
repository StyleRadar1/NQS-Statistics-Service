package edu.dosw.styleradar.estadisticas.domain.model;

public record GarmentSavesCount(String itemId, String name, long savesCount) {

    public GarmentSavesCount {
        if (itemId == null || itemId.isBlank()) {
            throw new IllegalArgumentException("Item id must not be blank");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Item name must not be blank");
        }
        if (savesCount < 0) {
            throw new IllegalArgumentException("Saves count must not be negative");
        }
    }
}
