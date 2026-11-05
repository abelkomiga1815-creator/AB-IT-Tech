package com.abittech.platform;

import static org.assertj.core.api.Assertions.assertThat;

import com.abittech.platform.auth.controller.AuthController;
import com.abittech.platform.common.exception.GlobalExceptionHandler;
import com.abittech.platform.common.health.HealthController;
import com.abittech.platform.common.logging.RequestIdFilter;
import com.abittech.platform.customer.controller.CustomerProfileController;
import com.abittech.platform.profile.controller.ProfileController;
import com.abittech.platform.security.JwtService;
import com.abittech.platform.student.controller.StudentProfileController;
import com.abittech.platform.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.crypto.password.PasswordEncoder;

class ApplicationContextTest extends IntegrationTestBase {

    @Autowired
    private ApplicationContext context;

    @Test
    void contextLoadsWithAllModules() {
        assertThat(context.getBean(HealthController.class)).isNotNull();
        assertThat(context.getBean(AuthController.class)).isNotNull();
        assertThat(context.getBean(ProfileController.class)).isNotNull();
        assertThat(context.getBean(CustomerProfileController.class)).isNotNull();
        assertThat(context.getBean(StudentProfileController.class)).isNotNull();
        assertThat(context.getBean(GlobalExceptionHandler.class)).isNotNull();
        assertThat(context.getBean(RequestIdFilter.class)).isNotNull();
        assertThat(context.getBean(JwtService.class)).isNotNull();
    }

    @Test
    void passwordsAreHashedWithBCrypt() {
        PasswordEncoder encoder = context.getBean(PasswordEncoder.class);
        assertThat(encoder.encode("any-password")).startsWith("$2");
    }
}
