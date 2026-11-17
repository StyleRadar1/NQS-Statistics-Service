package edu.dosw.styleradar.estadisticas.domain.model;

import java.util.Objects;

public record CategoryCode(String value) {

    public static final CategoryCode AVAILABLE = new CategoryCode("AVAILABLE");
    public static final CategoryCode LOW_STOCK = new CategoryCode("LOW_STOCK");
    public static final CategoryCode OUT_OF_STOCK = new CategoryCode("OUT_OF_STOCK");
    public static final CategoryCode PLAN_RADAR = new CategoryCode("PLAN_RADAR");
    public static final CategoryCode PLAN_RADAR_PLUS = new CategoryCode("PLAN_RADAR_PLUS");
    public static final CategoryCode ORIGIN_STORE = new CategoryCode("ORIGIN_STORE");
    public static final CategoryCode ORIGIN_USER = new CategoryCode("ORIGIN_USER");

    public CategoryCode {
        Objects.requireNonNull(value, "Category code must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("Category code must not be blank");
        }
    }
}
