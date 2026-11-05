package com.abittech.platform.database;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import com.abittech.platform.customer.entity.CustomerProfile;
import com.abittech.platform.customer.entity.CustomerType;
import com.abittech.platform.customer.repository.CustomerProfileRepository;
import com.abittech.platform.student.entity.StudentProfile;
import com.abittech.platform.student.repository.StudentProfileRepository;
import com.abittech.platform.support.IntegrationTestBase;
import com.abittech.platform.user.entity.Role;
import com.abittech.platform.user.entity.User;
import com.abittech.platform.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Verifies that data integrity is enforced by the database itself, not only by application code.
 */
class DatabaseConstraintsTest extends IntegrationTestBase {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CustomerProfileRepository customerProfileRepository;
    @Autowired
    private StudentProfileRepository studentProfileRepository;
    @Autowired
    private JdbcTemplate jdbc;

    private User newUser(Role role) {
        return userRepository.saveAndFlush(
                new User("DB Test", "db-" + UUID.randomUUID() + "@example.com", "hash", role));
    }

    private CustomerProfile customerProfile(User user) {
        CustomerProfile profile = new CustomerProfile(user);
        profile.setCustomerType(CustomerType.INDIVIDUAL);
        return profile;
    }

    private StudentProfile studentProfile(User user, String number) {
        StudentProfile profile = new StudentProfile(user);
        profile.setUniversity("Constraint University");
        profile.setProgramme("Computer Science");
        profile.setStudentNumber(number);
        return profile;
    }

    @Test
    void emailMustBeUnique() {
        User first = newUser(Role.CUSTOMER);
        assertThatThrownBy(() -> userRepository.saveAndFlush(
                new User("Other", first.getEmail(), "hash", Role.CUSTOMER)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void emailMustBeStoredLowerCase() {
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO users (id, full_name, email, password_hash, role) VALUES (?, ?, ?, ?, ?)",
                UUID.randomUUID(), "Upper", "Upper-" + UUID.randomUUID() + "@Example.com", "hash", "STUDENT"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void roleMustBeOneOfTheKnownRoles() {
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO users (id, full_name, email, password_hash, role) VALUES (?, ?, ?, ?, ?)",
                UUID.randomUUID(), "Bad Role", "role-" + UUID.randomUUID() + "@example.com", "hash", "SUPERUSER"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void userCanHaveOnlyOneCustomerProfile() {
        User user = newUser(Role.CUSTOMER);
        customerProfileRepository.saveAndFlush(customerProfile(user));
        assertThatThrownBy(() -> customerProfileRepository.saveAndFlush(customerProfile(user)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void userCanHaveOnlyOneStudentProfile() {
        User user = newUser(Role.STUDENT);
        studentProfileRepository.saveAndFlush(studentProfile(user, null));
        assertThatThrownBy(() -> studentProfileRepository.saveAndFlush(studentProfile(user, null)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void studentNumberIsUniquePerUniversityButMissingNumbersAreAllowed() {
        studentProfileRepository.saveAndFlush(studentProfile(newUser(Role.STUDENT), "S-100"));
        assertThatThrownBy(() -> studentProfileRepository.saveAndFlush(studentProfile(newUser(Role.STUDENT), "S-100")))
                .isInstanceOf(DataIntegrityViolationException.class);

        studentProfileRepository.saveAndFlush(studentProfile(newUser(Role.STUDENT), null));
        studentProfileRepository.saveAndFlush(studentProfile(newUser(Role.STUDENT), null));
    }

    @Test
    void yearOfStudyMustBeInRange() {
        StudentProfile profile = studentProfile(newUser(Role.STUDENT), null);
        profile.setYearOfStudy(11);
        assertThatThrownBy(() -> studentProfileRepository.saveAndFlush(profile))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void profileRequiresAnExistingUser() {
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO customer_profiles (id, user_id, customer_type) VALUES (?, ?, ?)",
                UUID.randomUUID(), UUID.randomUUID(), "INDIVIDUAL"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deletingAUserWithAProfileIsRestricted() {
        User user = newUser(Role.CUSTOMER);
        customerProfileRepository.saveAndFlush(customerProfile(user));
        assertThatThrownBy(() -> jdbc.update("DELETE FROM users WHERE id = ?", user.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(userRepository.existsById(user.getId())).isTrue();
    }
}
