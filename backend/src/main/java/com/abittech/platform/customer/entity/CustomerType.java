package com.abittech.platform.customer.entity;

/**
 * Controlled list of customer kinds. {@link #requiresOrganizationName()} drives the validation rule:
 * organisation details are never forced on individuals.
 */
public enum CustomerType {
    INDIVIDUAL(false),
    BUSINESS(true),
    ORGANIZATION(true),
    INSTITUTION(true),
    STARTUP(true),
    OTHER(false);

    private final boolean requiresOrganizationName;

    CustomerType(boolean requiresOrganizationName) {
        this.requiresOrganizationName = requiresOrganizationName;
    }

    public boolean requiresOrganizationName() {
        return requiresOrganizationName;
    }
}
