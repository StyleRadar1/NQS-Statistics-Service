package edu.dosw.styleradar.estadisticas.domain.model;

import java.util.Objects;

public record CategoryCode(String value) {

    public static final CategoryCode PLAN_RADAR = new CategoryCode("PLAN_RADAR");
    public static final CategoryCode PLAN_RADAR_PLUS = new CategoryCode("PLAN_RADAR_PLUS");
    public static final CategoryCode PLAN_STORE_MONTHLY = new CategoryCode("PLAN_STORE_MONTHLY");
    public static final CategoryCode SUSPENDED = new CategoryCode("SUSPENDED");

    public CategoryCode {
        Objects.requireNonNull(value, "Category code must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("Category code must not be blank");
        }
    }
}
