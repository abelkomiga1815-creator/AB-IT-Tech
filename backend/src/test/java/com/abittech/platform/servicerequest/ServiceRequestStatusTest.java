package com.abittech.platform.servicerequest;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.EnumSet;

import com.abittech.platform.servicerequest.entity.ServiceRequestStatus;
import org.junit.jupiter.api.Test;

import static com.abittech.platform.servicerequest.entity.ServiceRequestStatus.ACCEPTED;
import static com.abittech.platform.servicerequest.entity.ServiceRequestStatus.CANCELLED;
import static com.abittech.platform.servicerequest.entity.ServiceRequestStatus.COMPLETED;
import static com.abittech.platform.servicerequest.entity.ServiceRequestStatus.IN_PROGRESS;
import static com.abittech.platform.servicerequest.entity.ServiceRequestStatus.IN_REVIEW;
import static com.abittech.platform.servicerequest.entity.ServiceRequestStatus.REJECTED;
import static com.abittech.platform.servicerequest.entity.ServiceRequestStatus.SUBMITTED;

class ServiceRequestStatusTest {

    @Test
    void happyPathIsAllowedStepByStep() {
        assertThat(SUBMITTED.canTransitionTo(IN_REVIEW)).isTrue();
        assertThat(IN_REVIEW.canTransitionTo(ACCEPTED)).isTrue();
        assertThat(ACCEPTED.canTransitionTo(IN_PROGRESS)).isTrue();
        assertThat(IN_PROGRESS.canTransitionTo(COMPLETED)).isTrue();
    }

    @Test
    void stepsCannotBeSkippedOrReversed() {
        assertThat(SUBMITTED.canTransitionTo(COMPLETED)).isFalse();
        assertThat(SUBMITTED.canTransitionTo(IN_PROGRESS)).isFalse();
        assertThat(IN_PROGRESS.canTransitionTo(IN_REVIEW)).isFalse();
        assertThat(ACCEPTED.canTransitionTo(REJECTED)).isFalse();
    }

    @Test
    void finalStatusesHaveNoWayOut() {
        for (ServiceRequestStatus finalStatus : EnumSet.of(COMPLETED, REJECTED, CANCELLED)) {
            for (ServiceRequestStatus target : ServiceRequestStatus.values()) {
                assertThat(finalStatus.canTransitionTo(target)).as("%s -> %s", finalStatus, target).isFalse();
            }
        }
    }

    @Test
    void staysInTheSameStatusIsNotATransition() {
        for (ServiceRequestStatus status : ServiceRequestStatus.values()) {
            assertThat(status.canTransitionTo(status)).isFalse();
        }
    }

    @Test
    void customersMayOnlyCancelBeforeWorkIsAccepted() {
        assertThat(SUBMITTED.isCancellableByCustomer()).isTrue();
        assertThat(IN_REVIEW.isCancellableByCustomer()).isTrue();
        assertThat(ACCEPTED.isCancellableByCustomer()).isFalse();
        assertThat(IN_PROGRESS.isCancellableByCustomer()).isFalse();
        assertThat(COMPLETED.isCancellableByCustomer()).isFalse();
    }
}
