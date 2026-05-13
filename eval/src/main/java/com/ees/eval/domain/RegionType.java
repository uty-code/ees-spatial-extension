package com.ees.eval.domain;

public enum RegionType {
    URBAN_CORE(300),
    GENERAL_CITY(500),
    SUBURBAN(800);

    private final int defaultRadius;

    RegionType(int defaultRadius) {
        this.defaultRadius = defaultRadius;
    }

    public int getDefaultRadius() {
        return defaultRadius;
    }
}
