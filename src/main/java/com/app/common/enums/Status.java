package com.app.common.enums;

import java.util.HashMap;
import java.util.Map;

public enum Status {

    ACTIVE("ACTIVE"), INACTIVE("INACTIVE"), DELETED("DELETED"), SENT("SENT"),
    SUCCESS("SUCCESS"), FAILED("FAILED"), PENDING("PENDING");

    private static final Map<String, Status> BY_LABEL = new HashMap<>();

    static {
        for (Status e : values()) {
            BY_LABEL.put(e.label, e);
        }
    }

    private final String label;

    Status(String label) {
        this.label = label;
    }

    public static Status valueOfLabel(String label) {
        return BY_LABEL.get(label);
    }

    public String getLabel() {
        return label;
    }
}
