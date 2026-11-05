package com.abittech.platform.customer.service;

import java.util.UUID;

import com.abittech.platform.common.exception.ApiException;
import com.abittech.platform.common.exception.ErrorCode;
import com.abittech.platform.common.util.Strings;
import com.abittech.platform.customer.dto.CustomerProfileData;
import com.abittech.platform.customer.dto.CustomerProfileResponse;
import com.abittech.platform.customer.entity.CustomerProfile;
import com.abittech.platform.customer.repository.CustomerProfileRepository;
import com.abittech.platform.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Customer profile use cases. Every method takes the id of the authenticated user, so a caller can
 * only ever reach their own profile.
 */
@Service
public class CustomerProfileService {

    private static final Logger log = LoggerFactory.getLogger(CustomerProfileService.class);

    private final CustomerProfileRepository profileRepository;
    private final UserRepository userRepository;

    public CustomerProfileService(CustomerProfileRepository profileRepository, UserRepository userRepository) {
        this.profileRepository = profileRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public CustomerProfileResponse getMyProfile(UUID userId) {
        return CustomerProfileResponse.from(find(userId));
    }

    @Transactional
    public CustomerProfileResponse create(UUID userId, CustomerProfileData data) {
        if (profileRepository.existsByUserId(userId)) {
            throw new ApiException(ErrorCode.CUSTOMER_PROFILE_ALREADY_EXISTS);
        }
        validateBusinessRules(data);
        CustomerProfile profile = new CustomerProfile(userRepository.getReferenceById(userId));
        apply(profile, data);
        CustomerProfile saved = profileRepository.save(profile);
        log.info("Customer profile created: userId={}, type={}", userId, saved.getCustomerType());
        return CustomerProfileResponse.from(saved);
    }

    @Transactional
    public CustomerProfileResponse update(UUID userId, CustomerProfileData data) {
        CustomerProfile profile = find(userId);
        validateBusinessRules(data);
        apply(profile, data);
        CustomerProfile saved = profileRepository.saveAndFlush(profile);
        log.info("Customer profile updated: userId={}", userId);
        return CustomerProfileResponse.from(saved);
    }

    private CustomerProfile find(UUID userId) {
        return profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.CUSTOMER_PROFILE_NOT_FOUND));
    }

    private void validateBusinessRules(CustomerProfileData data) {
        if (data.customerType().requiresOrganizationName() && Strings.trimToNull(data.organizationName()) == null) {
            throw new ApiException(ErrorCode.INVALID_PROFILE_DATA,
                    "Organization name is required for customer type " + data.customerType());
        }
    }

    private void apply(CustomerProfile profile, CustomerProfileData data) {
        profile.setCustomerType(data.customerType());
        profile.setOrganizationName(Strings.trimToNull(data.organizationName()));
        profile.setPhone(Strings.trimToNull(data.phone()));
        profile.setAddress(Strings.trimToNull(data.address()));
        profile.setCity(Strings.trimToNull(data.city()));
        profile.setCountry(Strings.trimToNull(data.country()));
    }
}
