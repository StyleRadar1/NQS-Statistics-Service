package edu.dosw.styleradar.estadisticas.domain.model;

public record RankedItem(String itemId, String name, int position, long savesCount) {

    public RankedItem {
        if (itemId == null || itemId.isBlank()) {
            throw new IllegalArgumentException("Item id must not be blank");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Item name must not be blank");
        }
        if (position < 1) {
            throw new IllegalArgumentException("Position must be positive");
        }
        if (savesCount < 0) {
            throw new IllegalArgumentException("Saves count must not be negative");
        }
    }
}
