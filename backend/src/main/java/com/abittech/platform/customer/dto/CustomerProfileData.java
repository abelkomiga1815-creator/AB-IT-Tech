package com.abittech.platform.customer.dto;

import com.abittech.platform.customer.entity.CustomerType;

/**
 * The editable fields of a customer profile, shared by the create and update requests so that
 * the service applies them in one place.
 */
public interface CustomerProfileData {

    CustomerType customerType();

    String organizationName();

    String phone();

    String address();

    String city();

    String country();
}
