package com.abittech.platform.servicerequest.entity;

import java.util.EnumSet;
import java.util.Set;

/**
 * Lifecycle of a service request.
 * <pre>
 * SUBMITTED -> IN_REVIEW -> ACCEPTED -> IN_PROGRESS -> COMPLETED
 *     any open status can also end as CANCELLED; SUBMITTED and IN_REVIEW can end as REJECTED
 * </pre>
 * COMPLETED, REJECTED and CANCELLED are final.
 */
public enum ServiceRequestStatus {
    SUBMITTED,
    IN_REVIEW,
    ACCEPTED,
    IN_PROGRESS,
    COMPLETED,
    REJECTED,
    CANCELLED;

    public boolean canTransitionTo(ServiceRequestStatus target) {
        return allowedTargets().contains(target);
    }

    /** Customers may withdraw a request only before work has been accepted. */
    public boolean isCancellableByCustomer() {
        return this == SUBMITTED || this == IN_REVIEW;
    }

    private Set<ServiceRequestStatus> allowedTargets() {
        return switch (this) {
            case SUBMITTED -> EnumSet.of(IN_REVIEW, REJECTED, CANCELLED);
            case IN_REVIEW -> EnumSet.of(ACCEPTED, REJECTED, CANCELLED);
            case ACCEPTED -> EnumSet.of(IN_PROGRESS, CANCELLED);
            case IN_PROGRESS -> EnumSet.of(COMPLETED, CANCELLED);
            case COMPLETED, REJECTED, CANCELLED -> EnumSet.noneOf(ServiceRequestStatus.class);
        };
    }
}
