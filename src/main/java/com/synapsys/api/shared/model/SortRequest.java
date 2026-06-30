package com.synapsys.api.shared.model;

public record SortRequest(String field, boolean ascending) {

    public static SortRequest by(String field) {
        return new SortRequest(field, true);
    }

    public static SortRequest descBy(String field) {
        return new SortRequest(field, false);
    }
}